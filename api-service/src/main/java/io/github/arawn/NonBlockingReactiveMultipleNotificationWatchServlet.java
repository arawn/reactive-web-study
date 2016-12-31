package io.github.arawn;

import io.github.arawn.notification.Notification;
import io.github.arawn.notification.ReactiveMultipleNotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscription;
import reactor.core.Cancellation;
import reactor.core.publisher.Flux;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

@WebServlet(urlPatterns = "/notification/watch/non-blocking-reactive-multiple", asyncSupported = true)
public class NonBlockingReactiveMultipleNotificationWatchServlet extends HttpServlet {

    final Log log = LogFactory.getLog("NonBlockingReactiveMultiple");

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in non-blocking-reactive-multiple");

        AwkwardChaosMonkey.STOP.set(true);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(Integer.MAX_VALUE);

        ReactiveMultipleNotificationStream notificationStream = new ReactiveMultipleNotificationStream();
        Cancellation cancellation = notificationStream.watch()
                                                      .map(notification -> {
                                                          String content = "event: " + notification.getEvent() + "\n" +
                                                                           "data: " + notification.getData() + "\n\n";

                                                          return content.getBytes();
                                                      })
                                                      .zipWith(create(response.getOutputStream()))
                                                      .doOnError(throwable -> {
                                                          log.error("notification stream error!", throwable);
                                                          asyncContext.complete();
                                                      })
                                                      .doOnComplete(asyncContext::complete)
                                                      .subscribe(tuple -> {
                                                          try {
                                                              byte[] data = tuple.getT1();
                                                              ServletOutputStream stream = tuple.getT2();

                                                              stream.write(data);
                                                              stream.flush();
                                                          } catch (IOException error) {
                                                              error.printStackTrace();
                                                          }
                                                      }, 256);

        asyncContext.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                log.info("complete non-blocking-reactive-multiple");

                cancellation.dispose();
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                log.info("timeout non-blocking-reactive-multiple");
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                log.info("error non-blocking-reactive-multiple");
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // ignore
            }

        });

        log.info("out non-blocking-reactive-multiple");
    }


    Flux<ServletOutputStream> create(final ServletOutputStream out) {
        return Flux.from(subscriber -> {
            final AtomicBoolean dispose = new AtomicBoolean(false);

            out.setWriteListener(new WriteListener() {
                @Override
                public void onWritePossible() throws IOException {
                    do {
                        subscriber.onNext(out);
                    } while(!dispose.get() && out.isReady());
                }
                @Override
                public void onError(Throwable t) {
                    subscriber.onError(t);
                }
            });

            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    // ignore
                }
                @Override
                public void cancel() {
                    dispose.set(true);
                }
            });
        });
    }

}
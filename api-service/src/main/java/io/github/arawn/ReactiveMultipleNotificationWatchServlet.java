package io.github.arawn;

import io.github.arawn.notification.ReactiveMultipleNotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Disposable;
import reactor.core.Exceptions;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/notification/watch/reactive-multiple", asyncSupported = true)
public class ReactiveMultipleNotificationWatchServlet extends HttpServlet {

    final Log log = LogFactory.getLog("ReactiveMultiple");

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in reactive-multiple");

        AwkwardChaosMonkey.STOP.set(true);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

        ServletOutputStream outputStream = response.getOutputStream();

        AsyncContext asyncContext = request.startAsync();

        ReactiveMultipleNotificationStream notificationStream = new ReactiveMultipleNotificationStream();
        Disposable disposable = notificationStream.watch()
                                                  .map(notification -> {
                                                      String content = "event: " + notification.getEvent() + "\n" +
                                                                       "data: " + notification.getData() + "\n\n";

                                                      return content.getBytes();
                                                  })
                                                  .doOnNext(content -> {
                                                      try {
                                                          outputStream.write(content);
                                                          outputStream.flush();
                                                      } catch (IOException error) {
                                                          throw new DataWriteException(error);
                                                      }
                                                  })
                                                  .doOnError(throwable -> asyncContext.complete())
                                                  .doOnComplete(asyncContext::complete)
                                                  .doOnCancel(asyncContext::complete)
                                                  .subscribe(1);

        asyncContext.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                log.info("complete reactive-multiple");

                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // ignore
            }

        });

        log.info("out reactive-multiple");
    }


    class DataWriteException extends RuntimeException {
        public DataWriteException(Throwable cause) {
            super(cause);
        }
    }

}
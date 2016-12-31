package io.github.arawn;

import io.github.arawn.notification.ReactiveMultipleNotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Cancellation;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/notification/watch/reactive-multiple", asyncSupported = true)
public class ReactiveMultipleNotificationWatchServlet extends HttpServlet {

    final Log log = LogFactory.getLog("ReactiveMultiple");

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in reactive-multiple");

        AwkwardChaosMonkey.STOP.set(true);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

        PrintWriter writer = response.getWriter();

        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(Integer.MAX_VALUE);

        ReactiveMultipleNotificationStream notificationStream = new ReactiveMultipleNotificationStream();
        Cancellation cancellation = notificationStream.watch()
                                                      .doOnError(throwable -> {
                                                          log.error("notification stream error!", throwable);
                                                          asyncContext.complete();
                                                      })
                                                      .doOnComplete(asyncContext::complete)
                                                      .subscribe(notification -> {
                                                          writer.write("event: " + notification.getEvent() + "\n");
                                                          writer.write("data: " + notification.getData() + "\n\n");
                                                          writer.flush();
                                                      }, 256);

        asyncContext.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                log.info("complete reactive-multiple");

                cancellation.dispose();
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                log.info("timeout reactive-multiple");
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                log.info("error reactive-multiple");
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // ignore
            }

        });

        log.info("out reactive-multiple");
    }

}
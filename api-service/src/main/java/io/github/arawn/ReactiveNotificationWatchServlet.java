package io.github.arawn;

import io.github.arawn.notification.ReactiveNotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author ykpark@woowahan.com
 */
@WebServlet(urlPatterns = "/notification/watch/reactive", asyncSupported = true)
public class ReactiveNotificationWatchServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log("in notification/watch/reactive");

        AwkwardChaosMonkey.STOP.set(false);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

        PrintWriter writer = response.getWriter();

        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(60 * 1000);
        asyncContext.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                log("complete async");
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                log("timeout async");
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                log("error async");
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // ignore
            }

        });

        ReactiveNotificationStream notificationStream = new ReactiveNotificationStream();
        notificationStream.watch()
                          .doOnError(error -> asyncContext.complete())
                          .subscribe(notification -> {
                              writer.write("data: " + notification.getData() + "\n\n");
                              writer.flush();
                          }, 1);

        log("out notification/watch/reactive");
    }

}

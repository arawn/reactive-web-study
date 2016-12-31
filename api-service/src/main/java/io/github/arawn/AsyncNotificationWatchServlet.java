package io.github.arawn;

import io.github.arawn.notification.AsyncNotificationStream;
import io.github.arawn.notification.Notification;
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
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

/**
 * @author ykpark@woowahan.com
 */
@WebServlet(urlPatterns = "/notification/watch/async", asyncSupported = true)
public class AsyncNotificationWatchServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log("in notification/watch/async");

        AwkwardChaosMonkey.STOP.set(false);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

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

        Observer observer = (Observable observable, Object notification) -> {
            if (Objects.isNull(notification)) {
                asyncContext.complete();
            }

            try {
                PrintWriter writer = asyncContext.getResponse().getWriter();

                writer.write("event: " + ((Notification) notification).getEvent() + "\n\n");
                writer.write("data: " + ((Notification) notification).getData() + "\n\n");
                writer.flush();
            } catch (IOException e) {
                asyncContext.complete();
            }
        };

        AsyncNotificationStream notificationStream = new AsyncNotificationStream();
        notificationStream.addObserver(observer);
        notificationStream.watch();

        log("out notification/watch/async");
    }

}

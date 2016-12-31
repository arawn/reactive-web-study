package io.github.arawn;

import io.github.arawn.notification.AsyncMultipleNotificationStream;
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
@WebServlet(urlPatterns = "/notification/watch/async-multiple", asyncSupported = true)
public class AsyncMultipleNotificationWatchServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log("in notification/watch/async-multiple");

        AwkwardChaosMonkey.STOP.set(true);

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
                synchronizedLineByLineWrite(writer, (Notification) notification);
            } catch (IOException e) {
                asyncContext.complete();
            }
        };

        AsyncMultipleNotificationStream notificationStream = new AsyncMultipleNotificationStream();
        notificationStream.addObserver(observer);
        notificationStream.watch();

        log("out notification/watch/async-multiple");
    }

    private void noSynchronizedWrite(PrintWriter writer, Notification notification) {
        writer.write("event: " + notification.getEvent() + "\n");
        writer.write("data: " + notification.getData() + "\n\n");
        writer.flush();
    }

    private void synchronizedLineByLineWrite(PrintWriter writer, Notification notification) {
        synchronized (writer) {
            writer.write("event: " + notification.getEvent() + "\n");
        }
        synchronized (writer) {
            writer.write("data: " + notification.getData() + "\n\n");
        }
        synchronized (writer) {
            writer.flush();
        }
    }

    private void synchronizedWrite(PrintWriter writer, Notification notification) {
        synchronized (writer) {
            writer.write("event: " + ((Notification) notification).getEvent() + "\n");
            writer.write("data: " + ((Notification) notification).getData() + "\n\n");
            writer.flush();
        }
    }

}

package io.github.arawn.servlet;

import io.github.arawn.notification.Notification;
import io.github.arawn.notification.NotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@WebServlet(urlPatterns = "/notification/stream/async", asyncSupported = true)
public class AsyncNotificationStreamServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(AsyncNotificationStreamServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.enable(3, 10);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/async");

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");


        AtomicBoolean asyncDone = new AtomicBoolean(false);

        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout((new Random().nextInt(5) + 5) * 1000);
        asyncContext.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                log.info("complete notification/stream/async");
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                log.error("timeout notification/stream/async");
                asyncDone.set(true);
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                log.error("error notification/stream/async - " + event.getThrowable().getMessage());
                asyncDone.set(true);
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // ignore
            }

        });
        asyncContext.start(() -> {
            try {
                NotificationStream notificationStream = new NotificationStream(obtainUsername(request));
                Iterator<Notification> notifies = notificationStream.feedNotifies();

                while (notifies.hasNext() && !asyncDone.get()) {
                    Notification notification = notifies.next();
                    log.info("revised notification/stream/async : " + notification.getEvent());

                    ServletOutputStream outputStream = asyncContext.getResponse().getOutputStream();
                    outputStream.write(("event: " + notification.getEvent() + "\n").getBytes());
                    outputStream.write(("data: " + notification.getData() + "\n\n").getBytes());
                    outputStream.flush();
                }
            } catch (Exception error) {
                log.error("error notification/stream/async - " + error.getMessage());
            } finally {
                asyncContext.complete();
            }
        });

        log.info("out notification/stream/async");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }

}

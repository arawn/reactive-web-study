package io.github.arawn.servlet;

import io.github.arawn.notification.Notification;
import io.github.arawn.notification.NotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;

@WebServlet("/notification/stream")
public class NotificationStreamServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(NotificationStreamServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.enable(5, 10);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream");

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

        try {
            NotificationStream notificationStream = new NotificationStream(obtainUsername(request));
            Iterator<Notification> notifies = notificationStream.feedNotifies();

            while (notifies.hasNext()) {
                Notification notification = notifies.next();
                log.info("revised notification/stream : " + notification.getEvent());

                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(("event: " + notification.getEvent() + "\n").getBytes());
                outputStream.write(("data: " + notification.getData() + "\n\n").getBytes());
                outputStream.flush();
            }
        } catch (Exception error) {
            log.error("error notification/stream : " + error.getMessage());
        }

        log.info("out notification/stream");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }

}

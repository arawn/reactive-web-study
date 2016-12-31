package io.github.arawn;

import io.github.arawn.notification.Notification;
import io.github.arawn.notification.NotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;

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
@WebServlet("/notification/watch")
public class NotificationWatchServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log("in notification/watch");

        AwkwardChaosMonkey.STOP.set(false);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

        NotificationStream notificationStream = new NotificationStream();
        PrintWriter writer = response.getWriter();

        while (notificationStream.hasNext()) {
            Notification notification = notificationStream.next();

            writer.write("data: " + notification.getData() + "\n\n");
            writer.flush();
        }

        log("out notification/watch");
    }

}

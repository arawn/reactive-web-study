package io.github.arawn.servlet;

import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String notificationPath = "/notification/stream";

        String mode = StringUtils.trimAllWhitespace(request.getParameter("mode"));
        if (StringUtils.hasText(mode)) {
            notificationPath += "/" + mode;
        }

        String html = "<!DOCTYPE html>" +
                      "<html lang=en'>" +
                      "<head>" +
                      "    <meta charset='UTF-8'>" +
                      "    <script style='text/javascript'>" +
                      "        var eventSource = new EventSource('" + notificationPath + "');" +
                      "        eventSource.onopen = function (event) {" +
                      "            document.body.innerHTML = '<h1>Notification Panel (연결 중)</h1>';" +
                      "        };" +
                      "        eventSource.onerror = function (event) {" +
                      "            document.body.innerHTML = '<h1>Notification Panel (대기 중)</h1>';" +
                      "        };" +
                      "        eventSource.onmessage = function (event) {" +
                      "            document.body.innerHTML += '<h3>unknown - ' + event.data + '</h3><br/>';" +
                      "        };" +
                      "        eventSource.addEventListener('feed-notify', function(event) {" +
                      "            document.body.innerHTML += 'feed-notify - ' + event.data + '<br/>';" +
                      "        }, false);" +
                      "        eventSource.addEventListener('friend-request-notify', function(event) {" +
                      "            document.body.innerHTML += 'friend-request-notify - ' + event.data + '<br/>';" +
                      "        }, false);" +
                      "    </script>" +
                      "</head>" +
                      "<body>" +
                      "<h1>Notification Panel</h1>" +
                      "</body>" +
                      "</html>";

        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");
        response.getWriter().write(html);
    }

}

package io.github.arawn.servlet;

import io.github.arawn.notification.Notification;
import io.github.arawn.notification.NotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import io.github.arawn.servlet.ReactiveNotificationStreamServlet.AsyncContextEventPublisher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Random;

@WebServlet(urlPatterns = "/notification/stream/reactor-multiple", asyncSupported = true)
public class ReactorMultipleNotificationStreamServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(ReactorMultipleNotificationStreamServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.enable(9, 30);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/reactor-multiple");

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");
        response.getOutputStream().flush();


        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout((new Random().nextInt(15) + 15) * 1000);

        NotificationStream notificationStream = new NotificationStream(obtainUsername(request));

        Flux<Notification> feedNotifies = Flux
                .fromStream(notificationStream.feedNotifyStream())
                .publishOn(Schedulers.parallel(), 1)
                .map(Notification::of);

        Flux<Notification> friendRecommendationNotifies = Flux
                .fromStream(notificationStream.friendRecommendationNotifyStream())
                .publishOn(Schedulers.parallel(), 1)
                .map(Notification::of);

        Flux<byte[]> asyncEvents = Flux.from(new AsyncContextEventPublisher(asyncContext))
                                       .map(asyncEvent -> new byte[0]);

        Flux.merge(feedNotifies, friendRecommendationNotifies)
            .map(notification -> {
                // log.info("next notification/stream/reactor-multiple : " + notification.getEvent());

                String content = "event: " + notification.getEvent() + "\n" +
                                 "data: " + notification.getData() + "\n\n";

                return content.getBytes();
            })
            .mergeWith(asyncEvents)
            .doOnNext(bytes -> {
                try {
                    ServletOutputStream outputStream = asyncContext.getResponse().getOutputStream();
                    outputStream.write(bytes);
                    outputStream.flush();
                } catch (IOException error) {
                    throw new DataWriteException(error);
                }
            })
            .doOnError(throwable -> {
                log.error("error notification/stream/reactor-multiple : " + throwable.getMessage());

                asyncContext.complete();
            })
            .doOnTerminate(() -> {
                log.info("complete notification/stream/reactor-multiple");
            })
            .subscribe();

        log.info("out notification/stream/reactor-multiple");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }

    class DataWriteException extends RuntimeException {
        public DataWriteException(Throwable cause) {
            super(cause.getMessage());
        }
    }

}

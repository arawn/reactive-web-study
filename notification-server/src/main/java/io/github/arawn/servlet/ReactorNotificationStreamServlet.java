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
import javax.servlet.AsyncEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Random;

@WebServlet(urlPatterns = "/notification/stream/reactive-reactor", asyncSupported = true)
public class ReactorNotificationStreamServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(ReactorNotificationStreamServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.enable(3, 10);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/reactive-reactor");

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");


        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout((new Random().nextInt(5) + 5) * 1000);

        NotificationStream notificationStream = new NotificationStream(obtainUsername(request));
        Flux<Notification> notificationFlux = Flux
                .fromStream(notificationStream.feedNotifyStream())
                .map(Notification::of)
                .doOnNext(notification -> {
                    log.info("next notification/stream/reactive-reactor : " + notification.getEvent());

                    try {
                        ServletOutputStream outputStream = asyncContext.getResponse().getOutputStream();
                        outputStream.write(("event: " + notification.getEvent() + "\n").getBytes());
                        outputStream.write(("data: " + notification.getData() + "\n\n").getBytes());
                        outputStream.flush();
                    } catch (IOException error) {
                        throw new DataWriteException(error);
                    }
                })
                .subscribeOn(Schedulers.parallel());

        Flux<AsyncEvent> asyncEventFlux = Flux.from(new AsyncContextEventPublisher(asyncContext));

        Flux.merge(1, false, notificationFlux, asyncEventFlux)
            .doOnError(throwable -> {
                log.error("error notification/stream/reactive-reactor : " + throwable.getMessage());

                asyncContext.complete();

            })
            .doOnTerminate(() -> {
                log.info("complete notification/stream/reactive-reactor");
            })
            .subscribe();

        log.info("out notification/stream/reactive/reactor");
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

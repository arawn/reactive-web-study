package io.github.arawn.servlet;

import io.github.arawn.notification.Notification;
import io.github.arawn.notification.NotificationStream;
import io.github.arawn.notification.ReactiveNotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet(urlPatterns = "/notification/stream/reactive", asyncSupported = true)
public class ReactiveNotificationStreamServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(ReactiveNotificationStreamServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.enable(3, 10);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/reactive");

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

        AsyncContext asyncContext = request.startAsync(request, response);
        asyncContext.setTimeout((new Random().nextInt(5) + 5) * 1000);

        AsyncContextEventPublisher asyncContextEventPublisher = new AsyncContextEventPublisher(asyncContext);

        NotificationStream notificationStream = new NotificationStream(obtainUsername(request));
        ReactiveNotificationStream reactiveNotificationStream  = new ReactiveNotificationStream(notificationStream);

        reactiveNotificationStream.merge(asyncContextEventPublisher)
                                  .subscribe(new NotificationStreamSubscriber(asyncContext));

        log.info("out notification/stream/reactive");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }



    static class AsyncContextEventPublisher implements Publisher<AsyncEvent>, AsyncListener {

        Subscriber<? super AsyncEvent> subscriber;

        public AsyncContextEventPublisher(AsyncContext asyncContext) {
            Objects.requireNonNull(asyncContext).addListener(this);
        }


        @Override
        public void subscribe(Subscriber<? super AsyncEvent> subscriber) {
            this.subscriber = subscriber;

            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    // ignore
                }
                @Override
                public void cancel() {
                    // ignore
                }
            });
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            subscriber.onComplete();
        }

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
            subscriber.onError(new TimeoutException("timeout"));
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
            subscriber.onError(event.getThrowable());
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
            // ignore
        }

    }

    static class NotificationStreamSubscriber implements Subscriber<Notification> {

        AsyncContext asyncContext;
        Subscription subscription;

        Integer request = 1;
        AtomicInteger requestCounter = new AtomicInteger(request);


        NotificationStreamSubscriber(AsyncContext asyncContext) {
            this.asyncContext = Objects.requireNonNull(asyncContext);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            log.info("subscribe notification/stream/reactive");

            this.subscription = subscription;
            this.subscription.request(request);
        }

        @Override
        public void onNext(Notification notification) {
            log.info("next notification/stream/reactive : " + notification.getEvent());

            try {
                ServletOutputStream outputStream = asyncContext.getResponse().getOutputStream();
                outputStream.write(("event: " + notification.getEvent() + "\n").getBytes());
                outputStream.write(("data: " + notification.getData() + "\n\n").getBytes());
                outputStream.flush();
            } catch (IOException error) {
                throw new DataWriteException(error);
            }

            if (requestCounter.decrementAndGet() == 0) {
                requestCounter.set(request);
                subscription.request(request);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("error notification/stream/reactive : " + throwable.getMessage());

            subscription.cancel();
            asyncContext.complete();
        }

        @Override
        public void onComplete() {
            log.info("complete notification/stream/reactive");
        }

    }

    static class DataWriteException extends RuntimeException {
        DataWriteException(Throwable cause) {
            super(cause.getMessage());
        }
    }

}

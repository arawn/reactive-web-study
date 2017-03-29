package io.github.arawn.servlet;

import io.github.arawn.notification.Notification;
import io.github.arawn.notification.NotificationStream;
import io.github.arawn.notification.ReactiveMultipleNotificationStream;
import io.github.arawn.notification.ReactiveNotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;
import io.github.arawn.service.ServiceProcessing;
import io.github.arawn.servlet.ReactiveNotificationStreamServlet.AsyncContextEventPublisher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.util.StringUtils;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

@WebServlet(urlPatterns = "/notification/stream/reactive-multiple", asyncSupported = true)
public class ReactiveMultipleNotificationStreamServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(ReactiveMultipleNotificationStreamServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.enable(9, 30);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/reactive-multiple");

        FeedService feedService = new FeedService(ServiceProcessing.MEDIUM);
        FriendService friendService = new FriendService(ServiceProcessing.VERYSLOW);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");
        response.getOutputStream().flush();

        AsyncContext asyncContext = request.startAsync(request, response);
        asyncContext.setTimeout((new Random().nextInt(15) + 15) * 1000);

        AsyncContextEventPublisher asyncContextEventPublisher = new AsyncContextEventPublisher(asyncContext);

        NotificationStream notificationStream = new NotificationStream(feedService, friendService, obtainUsername(request));
        ReactiveNotificationStream feedNotificationStream  = new ReactiveNotificationStream(notificationStream.feedNotifies());
        ReactiveNotificationStream friendNotificationStream  = new ReactiveNotificationStream(notificationStream.friendRecommendationNotifies());

        ReactiveMultipleNotificationStream multipleNotificationStream = new ReactiveMultipleNotificationStream(feedNotificationStream, friendNotificationStream);
        multipleNotificationStream.merge(asyncContextEventPublisher)
                                  .subscribe(new MultipleNotificationStreamSubscriber(asyncContext));

        log.info("out notification/stream/reactive-multiple");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }


    static class MultipleNotificationStreamSubscriber implements Subscriber<Notification> {

        AsyncContext asyncContext;
        Set<Subscription> subscriptions = new HashSet<>();

        MultipleNotificationStreamSubscriber(AsyncContext asyncContext) {
            this.asyncContext = Objects.requireNonNull(asyncContext);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            log.info("subscribe notification/stream/reactive-multiple");

            subscriptions.add(subscription);
            subscription.request(1);
        }

        @Override
        public void onNext(Notification notification) {
            log.info("next notification/stream/reactive-multiple : " + notification.getEvent());

            try {
                ServletOutputStream outputStream = asyncContext.getResponse().getOutputStream();
                outputStream.write(("event: " + notification.getEvent() + "\n").getBytes());
                outputStream.write(("data: " + notification.getData() + "\n\n").getBytes());
                outputStream.flush();
            } catch (IOException error) {
                throw new ReactiveNotificationStreamServlet.DataWriteException(error);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("error notification/stream/reactive-multiple : " + throwable.getMessage());

            subscriptions.forEach(Subscription::cancel);
            asyncContext.complete();
        }

        @Override
        public void onComplete() {
            log.info("complete notification/stream/reactive-multiple");
        }

    }

}

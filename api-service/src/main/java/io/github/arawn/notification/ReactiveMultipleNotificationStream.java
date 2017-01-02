package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import io.github.arawn.service.FeedService.FeedNotify;
import io.github.arawn.service.FriendService;
import io.github.arawn.service.FriendService.FriendRequestNotify;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.*;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class ReactiveMultipleNotificationStream {

    final Log log = LogFactory.getLog(ReactiveMultipleNotificationStream.class);
    final FeedService feedService = new FeedService();
    final FriendService friendService = new FriendService();

    public Flux<Notification> watch() {

        Flux<Notification> feedNotifies = Flux.fromStream(Stream.generate(feedService::getFeedNotify))
                                              .map(Notification::of);

        Flux<Notification> friendRequestNotifies = Flux.fromStream(Stream.generate(friendService::getFriendRequestNotify))
                                                       .map(Notification::of);

        return new FluxProcessor<Notification, Notification>() {

            final AtomicBoolean subscribed = new AtomicBoolean(false);
            final Set<Subscription> subscriptions = new HashSet<>();
            final Set<Subscriber<? super Notification>> subscribers = new HashSet<>();

            @Override
            public void subscribe(Subscriber<? super Notification> subscriber) {
                super.subscribe(subscriber);

                if (subscribed.compareAndSet(false, true)) {
                    feedNotifies.subscribe(this);
                    friendRequestNotifies.subscribe(this);
                }

                synchronized (this) {
                    subscribers.add(subscriber);
                }

                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        subscriptions.forEach(subscription -> Schedulers.parallel().schedule(() -> subscription.request(n)));
                    }
                    @Override
                    public void cancel() {
                        subscriptions.forEach(Subscription::cancel);
                    }
                });
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                subscriptions.add(subscription);
            }

            @Override
            public void onNext(Notification notification) {
                subscribers.forEach(subscriber -> subscriber.onNext(notification));
            }

            @Override
            public void onError(Throwable throwable) {
//                System.out.println("throwable = [" + throwable + "]");
//                throwable.printStackTrace(System.out);

                subscribers.forEach(subscriber -> subscriber.onError(throwable));
            }

            @Override
            public void onComplete() {
                subscribers.forEach(Subscriber::onComplete);
            }

        };
    }

}
package io.github.arawn.notification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.extensions.PublisherX;

import javax.servlet.AsyncEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import static java.lang.Runtime.getRuntime;

public class ReactiveNotificationStream implements Publisher<Notification>, PublisherX<Notification> {

    final static Log log = LogFactory.getLog(ReactiveNotificationStream.class);
    final static ExecutorService executor = Executors.newFixedThreadPool(getRuntime().availableProcessors(), new ThreadFactory() {
        final AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "reactive-" + counter.getAndIncrement());
        }
    });


    Iterator<Notification> notifies;
    List<Subscriber<? super Notification>> subscribers = new CopyOnWriteArrayList<>();


    public ReactiveNotificationStream(NotificationStream stream) {
        this.notifies = Objects.requireNonNull(stream.feedNotifies());
    }

    public ReactiveNotificationStream(Iterator<Notification> notifies) {
        this.notifies = Objects.requireNonNull(notifies);
    }

    @Override
    public void subscribe(Subscriber<? super Notification> subscriber) {
        if (subscribers.add(subscriber)) {
            subscriber.onSubscribe(new ReactiveNotificationStreamSubscription());
        }
    }

    public Publisher<Notification> merge(Publisher<AsyncEvent> asyncEventPublisher) {
        asyncEventPublisher.subscribe(new Subscriber<AsyncEvent>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                // ignore
            }

            @Override
            public void onNext(AsyncEvent asyncEvent) {
                // ignore
            }

            @Override
            public void onError(Throwable error) {
                subscribers.forEach(s -> s.onError(error));
            }

            @Override
            public void onComplete() {
                subscribers.forEach(Subscriber::onComplete);
            }

        });

        return this;
    }

    class ReactiveNotificationStreamSubscription implements Subscription {

        boolean cancel = false;

        @Override
        public void request(long n) {
            if(cancel) {
                return;
            }

            executor.submit(() -> {
                try {
                    LongStream.rangeClosed(1, n).forEach(value -> {
                        if (cancel) {
                            return;
                        }

                        Notification notification = notifies.next();

                        if (cancel) {
                            return;
                        }

                        subscribers.forEach(s -> s.onNext(notification));
                    });
                } catch (Exception error) {
                    subscribers.forEach(s -> s.onError(error));
                }
            });
        }

        @Override
        public void cancel() {
            cancel = true;
        }

    }

}
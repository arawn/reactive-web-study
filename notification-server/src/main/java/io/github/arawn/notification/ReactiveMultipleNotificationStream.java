package io.github.arawn.notification;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.servlet.AsyncEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ReactiveMultipleNotificationStream implements Publisher<Notification> {

    final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        final AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "multiple-" + counter.getAndIncrement());
            thread.setDaemon(true);

            return thread;
        }
    });


    AtomicBoolean subscribed = new AtomicBoolean(false);

    Subscriber<? super Notification> subscriber;
    Set<Processor<Notification, Notification>> processors = new HashSet<>();

    public ReactiveMultipleNotificationStream(ReactiveNotificationStream...notificationStreams) {
        Arrays.asList(notificationStreams).forEach(stream -> {
            NotificationProcessor processor = new NotificationProcessor();
            stream.subscribe(processor);

            this.processors.add(processor);
        });
    }

    @Override
    public void subscribe(Subscriber<? super Notification> subscriber) {
        if (subscribed.compareAndSet(false, true)) {
            this.subscriber = subscriber;
            this.processors.forEach(processor -> processor.subscribe(subscriber));
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
                executor.schedule(() -> subscriber.onError(error), 10, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onComplete() {
                executor.schedule(subscriber::onComplete, 10, TimeUnit.MILLISECONDS);
            }

        });

        return this;
    }


    static class NotificationProcessor implements Processor<Notification, Notification> {

        Subscriber<? super Notification> subscriber;
        Subscription subscription;

        AtomicLong request = new AtomicLong(0);
        AtomicLong requestCounter = new AtomicLong(0);

        @Override
        public void subscribe(Subscriber<? super Notification> subscriber) {
            this.subscriber = subscriber;

            subscriber.onSubscribe(new Subscription() {

                @Override
                public void request(long n) {
                    request.set(n);
                    requestCounter.set(n);

                    subscription.request(n);
                }

                @Override
                public void cancel() {
                    subscription.cancel();
                }

            });
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onNext(Notification notification) {
            executor.schedule(() -> {
                try {
                    subscriber.onNext(notification);

                    if (requestCounter.decrementAndGet() == 0) {
                        requestCounter.set(request.longValue());
                        subscription.request(request.longValue());
                    }
                } catch (Exception error) {
                    subscriber.onError(error);
                }
            }, 20, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onError(Throwable error) {
            executor.schedule(() -> subscriber.onError(error), 10, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onComplete() {
            executor.schedule(subscriber::onComplete, 10, TimeUnit.MILLISECONDS);
        }

    }

}
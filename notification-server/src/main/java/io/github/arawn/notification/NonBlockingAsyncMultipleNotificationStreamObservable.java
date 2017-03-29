package io.github.arawn.notification;

import java.util.Observable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public class NonBlockingAsyncMultipleNotificationStreamObservable extends Observable {

    final static ScheduledExecutorService serviceExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        final AtomicInteger threadCounter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "service-" + threadCounter.getAndIncrement());
        }
    });
    final static ScheduledExecutorService notifyExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        final AtomicInteger threadCounter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "notify-" + threadCounter.getAndIncrement());
        }
    });

    NotificationPublisher feed;
    NotificationPublisher friend;

    public NonBlockingAsyncMultipleNotificationStreamObservable(NotificationStream stream) {
        requireNonNull(stream);

        this.feed = new NotificationPublisher(stream::feedNotifyAsync);
        this.friend = new NotificationPublisher(stream::friendRecommendationNotifyAsync);
    }

    public void subscribe() {
        serviceExecutor.execute(feed);
        serviceExecutor.execute(friend);
    }

    public void unsubscribe() {
        if (nonNull(feed)) feed.cancel();
        if (nonNull(friend)) friend.cancel();

        deleteObservers();
    }


    class NotificationPublisher implements Runnable {

        final Supplier<CompletableFuture<Notification>> notifies;
        final AtomicBoolean running = new AtomicBoolean(true);

        NotificationPublisher(Supplier<CompletableFuture<Notification>> notifies) {
            this.notifies = notifies;
        }

        @Override
        public void run() {
            if (countObservers() > 0) {
                notifies.get().thenAcceptAsync(event -> {
                    if (running.get()) {
                        setChanged();
                        notifyObservers(event);
                    }

                    schedule(20);
                }, notifyExecutor).exceptionally(throwable -> {
                    cancel();
                    setChanged();
                    notifyObservers(throwable);

                    return null;
                });
            } else {
                schedule(50);
            }
        }

        void schedule(long millis) {
            if (running.get()) {
                serviceExecutor.schedule(this, millis, TimeUnit.MILLISECONDS);
            }
        }

        void cancel() {
            running.set(false);
        }

    }

}
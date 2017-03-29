package io.github.arawn.notification;

import java.util.Iterator;
import java.util.Observable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

public class AsyncMultipleNotificationStreamObservable extends Observable {

    final static ScheduledExecutorService serviceExecutor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
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

    NotificationPublisher feedPublisher;
    NotificationPublisher friendPublisher;

    public AsyncMultipleNotificationStreamObservable(NotificationStream stream) {
        this.feedPublisher = new NotificationPublisher(requireNonNull(stream).feedNotifies());
        this.friendPublisher = new NotificationPublisher(requireNonNull(stream).friendRecommendationNotifies());
    }

    public void subscribe() {
        serviceExecutor.execute(feedPublisher);
        serviceExecutor.execute(friendPublisher);
    }

    public void unsubscribe() {
        feedPublisher.cancel();
        friendPublisher.cancel();

        deleteObservers();
    }


    class NotificationPublisher implements Runnable {

        final Iterator<Notification> notifies;
        final AtomicBoolean running = new AtomicBoolean(true);

        NotificationPublisher(Iterator<Notification> notifies) {
            this.notifies = notifies;
        }

        @Override
        public void run() {
            if (countObservers() > 0) {
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                completableFuture.thenAcceptAsync(event -> {
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

                try {
                    completableFuture.complete(notifies.next());
                } catch (Exception error) {
                    completableFuture.completeExceptionally(error);
                }
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
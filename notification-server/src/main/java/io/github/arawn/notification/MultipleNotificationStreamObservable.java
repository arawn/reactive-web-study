package io.github.arawn.notification;

import java.util.Iterator;
import java.util.Observable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

public class MultipleNotificationStreamObservable extends Observable {

    final static ExecutorService executor = Executors.newFixedThreadPool(3, new ThreadFactory() {
        final AtomicInteger threadCounter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "observable-" + threadCounter.getAndIncrement());
        }
    });

    BlockingQueue<Object> notifyQueue = new LinkedBlockingQueue<>();
    Future<?> notifyFuture;

    NotificationPublisher feedPublisher;
    NotificationPublisher friendPublisher;

    SequentialNotifyObserversPublisher sequentialFeed;
    SequentialNotifyObserversPublisher sequentialFriend;

    public MultipleNotificationStreamObservable(NotificationStream stream) {
        this.feedPublisher = new NotificationPublisher(requireNonNull(stream).feedNotifies());
        this.friendPublisher = new NotificationPublisher(requireNonNull(stream).friendRecommendationNotifies());

        this.sequentialFeed = new SequentialNotifyObserversPublisher(requireNonNull(stream).feedNotifies());
        this.sequentialFriend = new SequentialNotifyObserversPublisher(requireNonNull(stream).friendRecommendationNotifies());
    }

    public void subscribe() {
        subscribe(false);
    }

    public void subscribe(boolean mutualExclusion) {
        if (mutualExclusion) {
            executor.execute(sequentialFeed);
            executor.execute(sequentialFriend);

            notifyFuture = executor.submit(() -> {
                boolean running = true;
                while (running) {
                    if (countObservers() > 0) {
                        try {


                            Object event = notifyQueue.take();

                            setChanged();
                            notifyObservers(event);
                        } catch (InterruptedException e) {
                            running = false;
                        }
                    }

                    if (Thread.interrupted()) {
                        running = false;
                    }

                    Thread.yield();
                }
            });
        } else {
            executor.execute(feedPublisher);
            executor.execute(friendPublisher);

            notifyFuture = new NoOpFuture();
        }
    }

    public void unsubscribe() {
        feedPublisher.cancel();
        friendPublisher.cancel();

        sequentialFeed.cancel();
        sequentialFriend.cancel();

        notifyFuture.cancel(true);

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
            while (running.get()) {
                if (countObservers() > 0) {
                    try {
                        Notification notification = notifies.next();

                        setChanged();
                        notifyObservers(notification);
                    } catch (Exception error) {
                        cancel();

                        setChanged();
                        notifyObservers(error);
                    }
                }

                if (Thread.interrupted()) {
                    cancel();
                }

                Thread.yield();
            }
        }

        void cancel() {
            running.set(false);
        }

    }

    class SequentialNotifyObserversPublisher implements Runnable {

        final Iterator<Notification> notifies;
        final AtomicBoolean running = new AtomicBoolean(true);

        SequentialNotifyObserversPublisher(Iterator<Notification> notifies) {
            this.notifies = notifies;
        }

        @Override
        public void run() {
            while (running.get()) {
                if (countObservers() > 0) {
                    try {
                        notifyQueue.put(notifies.next());
                    } catch (Exception error) {
                        cancel();

                        try {
                            notifyQueue.put(error);
                        } catch (InterruptedException ignore) { }
                    }
                }

                if (Thread.interrupted()) {
                    cancel();
                }
            }
        }

        void cancel() {
            running.set(false);
        }

    }

    class NoOpFuture implements Future<Void> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

    }

}
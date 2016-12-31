package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;
import io.github.arawn.service.ServiceOperationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Observable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AsyncMultipleNotificationStream extends Observable {

    final Log log = LogFactory.getLog(AsyncMultipleNotificationStream.class);

    final FeedService feedService = new FeedService();
    final Worker feedWorker = new Worker(() -> Notification.of(feedService.getFeedNotify()));
    final FriendService friendService = new FriendService();
    final Worker friendWorker = new Worker(() -> Notification.of(friendService.getFriendRequestNotify()));

    public void watch() {
        if (!feedWorker.isAlive()) {
            feedWorker.start();
        }
        if (!friendWorker.isAlive()) {
            friendWorker.start();
        }
    }

    public void unwatch() {
        feedWorker.interrupt();
        friendWorker.interrupt();
    }


    public class Worker extends Thread {

        final Supplier<Notification> supplier;

        public Worker(Supplier<Notification> supplier) {
            this.supplier = supplier;
        }

        @Override
        public void run() {
            setName("async-worker");

            AtomicBoolean running = new AtomicBoolean(true);
            while (running.get()) {
                if (countObservers() == 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) { }
                } else if(isInterrupted()) {
                    running.set(false);
                } else {
                    try {
                        Notification notification = supplier.get();
                        // log.info("revised notification: " + notification);

                        setChanged();
                        notifyObservers(notification);
                    } catch (ServiceOperationException e) {
                        // running.set(false);
                        // log.error("server error, close center!", e);
                    }
                }
            }

            setChanged();
            notifyObservers(null);
        }

    }

}
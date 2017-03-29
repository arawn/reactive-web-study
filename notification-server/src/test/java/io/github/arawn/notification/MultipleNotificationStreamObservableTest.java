package io.github.arawn.notification;

import io.github.arawn.service.AwkwardChaosMonkey;
import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;
import io.github.arawn.service.ServiceProcessing;
import io.github.arawn.service.support.HttpClientFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class MultipleNotificationStreamObservableTest {

    @BeforeClass
    public static void before() {
        new HttpClientFactory().contextInitialized(null);
        AwkwardChaosMonkey.disable();
    }

    @AfterClass
    public static void after() {
        new HttpClientFactory().contextDestroyed(null);
    }

    @Test
    public void dataRace() throws InterruptedException {
        OutputStream outputStream = System.out;

        FeedService feedService = new FeedService(ServiceProcessing.NO);
        FriendService friendService = new FriendService(ServiceProcessing.NO);

        NotificationStream notificationStream = new NotificationStream(feedService, friendService, "anonymous");
        MultipleNotificationStreamObservable notificationStreamObservable = new MultipleNotificationStreamObservable(notificationStream);
        notificationStreamObservable.addObserver((observable, event) -> {
            if (event instanceof Throwable) {
                ((Throwable) event).printStackTrace();
            } else {
                try {
                    outputStream.write(("event: " + ((Notification) event).getEvent() + "\n").getBytes());
                    outputStream.write(("data: " + ((Notification) event).getData() + "\n\n").getBytes());
                    outputStream.flush();
                } catch (IOException error) {
                    notificationStreamObservable.unsubscribe();

                    throw new RuntimeException(error);
                }
            }
        });
        notificationStreamObservable.subscribe();

        Thread.sleep(5 * 1000);

        notificationStreamObservable.unsubscribe();


        MultipleNotificationStreamObservable.executor.shutdown();
        MultipleNotificationStreamObservable.executor.awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    public void synchronizedToNoDataRace() throws InterruptedException {
        OutputStream outputStream = System.out;

        FeedService feedService = new FeedService(ServiceProcessing.NO);
        FriendService friendService = new FriendService(ServiceProcessing.NO);

        NotificationStream notificationStream = new NotificationStream(feedService, friendService, "anonymous");
        MultipleNotificationStreamObservable notificationStreamObservable = new MultipleNotificationStreamObservable(notificationStream);
        notificationStreamObservable.addObserver((observable, event) -> {
            if (event instanceof Throwable) {
                ((Throwable) event).printStackTrace();
            } else {
                synchronized (this) {
                    try {
                        outputStream.write(("event: " + ((Notification) event).getEvent() + "\n").getBytes());
                        outputStream.write(("data: " + ((Notification) event).getData() + "\n\n").getBytes());
                        outputStream.flush();
                    } catch (IOException error) {
                        notificationStreamObservable.unsubscribe();

                        throw new RuntimeException(error);
                    }
                }
            }
        });
        notificationStreamObservable.subscribe();

        Thread.sleep(5 * 1000);

        notificationStreamObservable.unsubscribe();


        MultipleNotificationStreamObservable.executor.shutdown();
        MultipleNotificationStreamObservable.executor.awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    public void reentrantLockToNoDataRace() throws InterruptedException {
        ReentrantLock reentrantLock = new ReentrantLock();
        OutputStream outputStream = System.out;

        FeedService feedService = new FeedService(ServiceProcessing.NO);
        FriendService friendService = new FriendService(ServiceProcessing.NO);

        NotificationStream notificationStream = new NotificationStream(feedService, friendService, "anonymous");
        MultipleNotificationStreamObservable notificationStreamObservable = new MultipleNotificationStreamObservable(notificationStream);
        notificationStreamObservable.addObserver((observable, event) -> {
            if (event instanceof Throwable) {
                ((Throwable) event).printStackTrace();
            } else {
                reentrantLock.lock();

                try {
                    outputStream.write(("event: " + ((Notification) event).getEvent() + "\n").getBytes());
                    outputStream.write(("data: " + ((Notification) event).getData() + "\n\n").getBytes());
                    outputStream.flush();
                } catch (IOException error) {
                    notificationStreamObservable.unsubscribe();

                    throw new RuntimeException(error);
                }

                reentrantLock.unlock();
            }
        });
        notificationStreamObservable.subscribe();

        Thread.sleep(5 * 1000);

        notificationStreamObservable.unsubscribe();


        MultipleNotificationStreamObservable.executor.shutdown();
        MultipleNotificationStreamObservable.executor.awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    public void sequentialToNoDataRace() throws InterruptedException {
        OutputStream outputStream = System.out;

        FeedService feedService = new FeedService(ServiceProcessing.NO);
        FriendService friendService = new FriendService(ServiceProcessing.NO);

        NotificationStream notificationStream = new NotificationStream(feedService, friendService, "anonymous");
        MultipleNotificationStreamObservable notificationStreamObservable = new MultipleNotificationStreamObservable(notificationStream);
        notificationStreamObservable.addObserver((observable, event) -> {
            if (event instanceof Throwable) {
                ((Throwable) event).printStackTrace();
            } else {
                try {
                    outputStream.write(("event: " + ((Notification) event).getEvent() + "\n").getBytes());
                    outputStream.write(("data: " + ((Notification) event).getData() + "\n\n").getBytes());
                    outputStream.flush();
                } catch (IOException error) {
                    notificationStreamObservable.unsubscribe();

                    throw new RuntimeException(error);
                }
            }
        });
        notificationStreamObservable.subscribe(true);

        Thread.sleep(5 * 1000);

        notificationStreamObservable.unsubscribe();


        MultipleNotificationStreamObservable.executor.shutdown();
        MultipleNotificationStreamObservable.executor.awaitTermination(3, TimeUnit.SECONDS);
    }

}
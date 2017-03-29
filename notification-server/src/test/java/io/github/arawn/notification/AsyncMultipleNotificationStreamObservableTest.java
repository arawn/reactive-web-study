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

import static org.junit.Assert.*;

public class AsyncMultipleNotificationStreamObservableTest {

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
    public void subscribe() throws InterruptedException {
        OutputStream outputStream = System.out;

        FeedService feedService = new FeedService(ServiceProcessing.NO);
        FriendService friendService = new FriendService(ServiceProcessing.NO);

        NotificationStream notificationStream = new NotificationStream(feedService, friendService, "anonymous");
        AsyncMultipleNotificationStreamObservable notificationStreamObservable = new AsyncMultipleNotificationStreamObservable(notificationStream);
        notificationStreamObservable.addObserver((observable, event) -> {
            if (event instanceof Throwable) {
                System.out.println(((Throwable) event).getMessage());
                notificationStreamObservable.unsubscribe();
            } else {
                try {
                    outputStream.write(("event: " + ((Notification) event).getEvent() + "\n").getBytes());
                    outputStream.write(("data: " + ((Notification) event).getData() + "\n\n").getBytes());
                    outputStream.flush();
                } catch (IOException error) {
                    throw new RuntimeException(error);
                }
            }
        });
        notificationStreamObservable.subscribe();

        Thread.sleep(60 * 1000);

        notificationStreamObservable.unsubscribe();


        AsyncMultipleNotificationStreamObservable.serviceExecutor.shutdown();
        AsyncMultipleNotificationStreamObservable.serviceExecutor.awaitTermination(3, TimeUnit.SECONDS);
    }


}
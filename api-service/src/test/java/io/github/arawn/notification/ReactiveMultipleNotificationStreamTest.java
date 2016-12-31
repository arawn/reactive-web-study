package io.github.arawn.notification;

import io.github.arawn.service.AwkwardChaosMonkey;
import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.reactivestreams.Processor;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.WorkQueueProcessor;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static org.junit.Assert.*;

public class ReactiveMultipleNotificationStreamTest {

    final Log log = LogFactory.getLog(ReactiveMultipleNotificationStreamTest.class);
    final FeedService feedService = new FeedService();
    final FriendService friendService = new FriendService();

    @Test
    public void watch() throws InterruptedException {
        AwkwardChaosMonkey.STOP.set(true);


        DirectProcessor<Notification> processor = DirectProcessor.create();

        Flux<Notification> feedMono = Mono.fromCallable(feedService::getFeedNotify)
                                          .map(Notification::of)
                                          .publishOn(Schedulers.newParallel("reactive-worker"))
                                          .repeat(10);

        Flux<Notification> friendMono = Mono.fromCallable(friendService::getFriendRequestNotify)
                                            .map(Notification::of)
                                            .publishOn(Schedulers.newParallel("reactive-worker"))
                                            .repeat(10);



//        feedMono.log().subscribe(processor);
//        friendMono.log().subscribe(processor);

        Flux<Notification> notificationFlux = processor.log()
                .doOnSubscribe(subscription -> {
                    log.info("1 subscription = " + subscription);

                    feedMono.subscribeOn(Schedulers.single()).subscribe(processor);
                    friendMono.subscribeOn(Schedulers.single()).subscribe(processor);

                    log.info("2 subscription = " + subscription);
                });

        notificationFlux.doOnComplete(() -> log.info("complate")).subscribe(notification -> {
            log.info("notification 1 = " + notification.getEvent());
        }, 256);

        notificationFlux.subscribeOn(Schedulers.single()).doOnComplete(() -> log.info("complate")).subscribe(notification -> {
            log.info("notification 2 = " + notification.getEvent());
        }, 256);


//        processor
//            .log()
//            .subscribeOn(Schedulers.parallel())
//            .doOnRequest(request -> {
//                System.out.println("request = " + request);
//
//            })
//            .doOnSubscribe(subscription -> {
//                System.out.println("subscription = " + subscription);
//            })
//            .subscribe(notification -> {
//                System.out.println(notification.event + " : " + notification.getData());
//            }, 3);


//        ReactiveMultipleNotificationStream stream = new ReactiveMultipleNotificationStream();
//        stream.watch()
//              .subscribe(notification -> System.out.println(notification.getEvent()), 256);

        Thread.sleep(Long.MAX_VALUE);
    }

}
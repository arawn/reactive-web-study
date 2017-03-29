package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.*;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class ReactiveMultipleNotificationStream2Test {

    final Log log = LogFactory.getLog(ReactiveMultipleNotificationStream2Test.class);
    final FeedService feedService = new FeedService();
    final FriendService friendService = new FriendService();

    @Test
    public void watch() throws InterruptedException {
//        AwkwardChaosMonkey.STOP.set(false);

        Scheduler.Worker ioWorker = Schedulers.newSingle("io-worker").createWorker();

        AtomicInteger counter = new AtomicInteger(0);

        ReactiveMultipleNotificationStream2 stream = new ReactiveMultipleNotificationStream2();

        FluxProcessor<byte[], OutputStream> writerProcessor = new FluxProcessor<byte[], OutputStream>() {

            Subscription subscription;
            Subscriber<? super OutputStream> subscriber;

            @Override
            public void onSubscribe(Subscription subscription) {
                if (Operators.validate(this.subscription, subscription)) {
                    this.subscription = subscription;
                }
            }

            @Override
            public void onNext(byte[] bytes) {
                log.info("write onNext: " + counter.get());

                if (counter.incrementAndGet() > 4) {
                    counter.set(0);
                    log.info("write!!!");
                    subscriber.onNext(null);
                } else {
                    ioWorker.schedule(() -> onNext(bytes));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                subscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }

            @Override
            public void subscribe(Subscriber<? super OutputStream> subscriber) {
                this.subscriber = subscriber;

                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        Schedulers.parallel().schedule(() -> subscription.request(1));
                    }
                    @Override
                    public void cancel() {
                        subscription.cancel();
                    }
                });
            }

        };

        FluxProcessor<OutputStream, Void> flushProcessor = new FluxProcessor<OutputStream, Void>() {

            Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                if (Operators.validate(this.subscription, subscription)) {
                    this.subscription = subscription;
                    this.subscription.request(1);
                }
            }

            @Override
            public void onNext(OutputStream outputStream) {
                log.info("flush onNext: " + counter.get());

                if (counter.incrementAndGet()> 4) {
                    counter.set(0);
                    log.info("flush");
                    Schedulers.parallel().schedule(() -> subscription.request(1));
                } else {
                    ioWorker.schedule(() -> onNext(outputStream));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("오류다아아아아아~~~~");
            }

            @Override
            public void onComplete() {

            }

        };

        stream.watch().log().map(notification -> notification.getEvent().getBytes()).subscribe(writerProcessor);
        writerProcessor.log().subscribe(flushProcessor);




//        Disposable subscribe = stream.subscribe().log()
//                .subscribe(notification -> {
//            log.info("1::::::" + notification.getEvent());
//        }, 1);




//        Disposable subscribe1 = stream.subscribe().log().subscribe(notification -> {
//            log.info("2::::::" + notification.getEvent());
//        }, 1);
//
//        Disposable subscribe2 = stream.subscribe().log().subscribe(notification -> {
//            log.info("3::::::" + notification.getEvent());
//        }, 1);


//        Disposable subscribe = subscribe.subscribeOn(Schedulers.single()).log().subscribe(notification -> {
//            log.info(notification.getEvent());
//        }, 1);


//        Flux<byte[]> notifications = stream.subscribe().map(notification -> notification.getData().getBytes()).log();
//
//
//
////        DirectProcessor<Notification> waitProcessor = DirectProcessor.create();
////        DirectProcessor<Notification> writeProcessor = DirectProcessor.create();
//
////        waitProcessor.doOnSubscribe(subscription -> {
////            subscribe.subscribe(waitProcessor);
////        }).doOnNext(notification -> {
////            if (counter.incrementAndGet() > 5) {
////                System.out.println("ready!");
////            } else {
////                waitProcessor.onNext(notification);
////            }
////        }).subscribe(1);
//
//        Processor<byte[], Notification> waitProcessor = new FluxProcessor<byte[], Notification>() {
//
//            Subscription subscription;
//
//            @Override
//            public void onSubscribe(Subscription subscription) {
//                this.subscription = subscription;
//                subscription.request(1);
//            }
//
//            @Override
//            public void onNext(byte[] notification) {
//                System.out.println("notification = [" + notification + "]");
//
//                subscription.request(1);
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                System.out.println("t = [" + t + "]");
//            }
//
//            @Override
//            public void onComplete() {
//                System.out.println("ReactiveMultipleNotificationStream2Test.onComplete");
//            }
//
//            @Override
//            public void subscribe(Subscriber<? super Notification> subscriber) {
//
//            }
//
//        };
//
//        notifications.subscribe(waitProcessor);
//
//        waitProcessor.onComplete();

        Thread.sleep(Integer.MAX_VALUE);
    }

}
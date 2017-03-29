package io.github.arawn.notification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.Objects;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationStreamObservable extends Observable {

    final static Log log = LogFactory.getLog(NotificationStreamObservable.class);
    final static ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
        final AtomicInteger threadCounter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "observable-" + threadCounter.getAndIncrement());
        }
    });

    Iterator<Notification> feedNotifies;
    Future<?> feedFuture;

    public NotificationStreamObservable(NotificationStream stream) {
        this.feedNotifies = Objects.requireNonNull(stream).feedNotifies();
    }

    public void subscribe() {
        feedFuture = executorService.submit(() -> {
            boolean running = true;
            while (running) {
                if (countObservers() > 0) {
                    try {
                        Notification notification = feedNotifies.next();

                        setChanged();
                        notifyObservers(notification);
                    } catch (Exception error) {
                        running = false;

                        setChanged();
                        notifyObservers(error);
                    }
                }

                if (Thread.interrupted()) {
                    running = false;
                }

                Thread.yield();
            }
        });
    }

    public void unsubscribe() {
        feedFuture.cancel(true);
        deleteObservers();
    }


//    Thread 를 직접 사용하는 버전, 저수준 API를 직접 사용하것 보다는 자바가 제공하는 concurrent 을 사용하는게 좋다
//
//    Thread feedThread;
//
//    public void newThreadSubscribe() {
//        if (Objects.isNull(feedThread) || !feedThread.isAlive()) {
//            feedThread = new Thread(new Worker(() -> Notification.of(feedService.getFeedNotify())));
//            feedThread.setName("observable");
//            feedThread.start();
//        }
//    }
//
//    public void newThreadUnsubscribe() {
//        if (feedThread.isAlive()) {
//            feedThread.interrupt();
//        }
//        deleteObservers();
//    }

}
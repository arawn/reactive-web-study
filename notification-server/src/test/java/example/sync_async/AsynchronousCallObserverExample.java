package example.sync_async;

import io.github.arawn.notification.Notification;

import java.util.List;
import java.util.concurrent.*;

public class AsynchronousCallObserverExample {

    public static void main(String[] args) throws InterruptedException {

        String username = "guest";

        NotificationStreamObservable observable = new NotificationStreamObservable(username);
        observable.register(notification -> {

            // 데이터 사용

        });
        observable.subscribe();

        Thread.sleep(10 * 1000);

        observable.unsubscribe();


        observable.notifyApi.executorService.shutdownNow();

        observable.executorService.shutdown();
        observable.executorService.awaitTermination(3, TimeUnit.SECONDS);
    }


    static class NotificationStreamObservable {

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<?> future = new FutureTask<>(() -> {}, null);

        NotifyApi notifyApi = new NotifyApi();

        String username;
        List<NotificationStreamObserver> observers = new CopyOnWriteArrayList<>();


        NotificationStreamObservable(String username) {
            this.username = username;
        }

        void register(NotificationStreamObserver observer) {
            observers.add(observer);
        }

        void unregister(NotificationStreamObserver observer) {
            observers.remove(observer);
        }


        void subscribe() {
            future = executorService.submit(() -> {
                boolean running = true;
                while (running) {
                    Notification feedNotify = Notification.of(notifyApi.getFeedNotify(username));
                    observers.forEach(observer -> observer.onNotification(feedNotify));

                    if (Thread.interrupted()) {
                        running = false;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException error) {
                        running = false;
                    }
                }
            });
        }

        void unsubscribe() {
            future.cancel(true);
            observers.clear();
        }

    }

    interface NotificationStreamObserver {

        void onNotification(Notification notification);

    }

}

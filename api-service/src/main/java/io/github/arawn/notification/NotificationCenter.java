package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Observable;
import java.util.concurrent.CompletableFuture;

/**
 * @author ykpark@woowahan.com
 */
public class NotificationCenter extends Observable {

    final Log log = LogFactory.getLog(NotificationCenter.class);

    final FeedService feedService = new FeedService();

    public NotificationCenter() {
        CompletableFuture.runAsync(() -> {
            Thread.currentThread().setName("NotificationCenter");

            log.info("Started NotificationCenter");

//            while (!Thread.currentThread().isInterrupted()) {
//                if (!notificationBoxes.isEmpty()) {
//                    notificationBoxes.entrySet()
//                            .stream()
//                            .filter(entry -> !entry.getValue().isOpen())
//                            .forEach(entry -> notificationBoxes.remove(entry.getKey()));
//
//                    Notification notification = Notification.of(feedService.getFeedNotify());
//                    notificationBoxes.values()
//                            .forEach(box -> box.push(notification));
//
//                    log.info("push notification to " + notificationBoxes.size() + " boxes");
//                } else {
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        break;
//                    }
//                }
//
//            }

            log.info("Ended NotificationCenter");
        }).thenRunAsync(this::deleteObservers);
    }
//
//    public void register(AsyncNotificationStream notificationBox) {
//        notificationBoxes.putIfAbsent(notificationBox.session, notificationBox);
//    }




}

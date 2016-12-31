package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import io.github.arawn.service.ServiceOperationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ykpark@woowahan.com
 */
public class NotificationStream implements Iterator<Notification> {

    final Log log = LogFactory.getLog(NotificationCenter.class);

    final FeedService feedService = new FeedService();
    final AtomicBoolean next = new AtomicBoolean(true);

    @Override
    public boolean hasNext() {
        return next.get();
    }

    @Override
    public Notification next() {
        if (!hasNext()) {
            return null;
        }

        try {
            Notification notification = Notification.of(feedService.getFeedNotify());
            log.info("revised notification: " + notification);

            return notification;
        } catch (ServiceOperationException e) {
            next.set(false);
            log.error("server error, close center!", e);

            return null;
        }
    }

}
package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author ykpark@woowahan.com
 */
public class ReactiveNotificationStream {

    final Log log = LogFactory.getLog(ReactiveNotificationStream.class);
    final FeedService feedService = new FeedService();

    public Flux<Notification> watch() {
        return Mono.fromCallable(feedService::getFeedNotify)
                   .map(Notification::of)
                   .doOnNext(notification -> {
                       log.info("revised notification: " + notification);
                   })
                   .log()
                   .repeat()
                   .subscribeOn(Schedulers.newSingle("reactive-worker"));
    }

}
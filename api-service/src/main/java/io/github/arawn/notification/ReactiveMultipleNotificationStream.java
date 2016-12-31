package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class ReactiveMultipleNotificationStream {

    final Log log = LogFactory.getLog(ReactiveMultipleNotificationStream.class);
    final FeedService feedService = new FeedService();
    final FriendService friendService = new FriendService();

    public Flux<Notification> watch() {
        Flux<Notification> feedFlux = Mono.fromCallable(feedService::getFeedNotify)
                                          .map(Notification::of)
                                          .publishOn(Schedulers.parallel())
                                          .repeat(10);

        Flux<Notification> friendFlux = Mono.fromCallable(friendService::getFriendRequestNotify)
                                            .map(Notification::of)
                                            .publishOn(Schedulers.parallel())
                                            .repeat(10);

        DirectProcessor<Notification> processor = DirectProcessor.create();

        return processor.doOnSubscribe(subscription -> {
                            feedFlux.subscribeOn(Schedulers.single()).subscribe(processor);
                            friendFlux.subscribeOn(Schedulers.single()).subscribe(processor);
                        });
    }

}
package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class NotificationStream {

    FeedService feedService = new FeedService();
    FriendService friendService = new FriendService();

    String username;

    public NotificationStream(String username) {
        this.username = Objects.requireNonNull(username);
    }

    public NotificationStream(FeedService feedService, FriendService friendService, String username) {
        this.feedService = feedService;
        this.friendService = friendService;

        this.username = username;
    }


    public Stream<FeedService.FeedNotify> feedNotifyStream() {
        return Stream.generate(() -> feedService.getFeedNotify(username));
    }

    public CompletableFuture<Notification> feedNotifyAsync() {
        return feedService.getFeedNotifyAsync(username).thenApply(Notification::of);
    }

    public Iterator<Notification> feedNotifies() {
        return feedNotifyStream().map(Notification::of).iterator();
    }

    public Stream<FriendService.FriendRecommendationNotify> friendRecommendationNotifyStream() {
        return Stream.generate(() -> friendService.getRecommendationNotify(username));
    }

    public CompletableFuture<Notification> friendRecommendationNotifyAsync() {
        return friendService.getRecommendationNotifyAsync(username).thenApply(Notification::of);
    }

    public Iterator<Notification> friendRecommendationNotifies() {
        return friendRecommendationNotifyStream().map(Notification::of).iterator();
    }

}
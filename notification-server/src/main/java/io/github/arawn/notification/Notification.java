package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;

import java.util.Objects;
import java.util.stream.Stream;

public class Notification {

    final String event;
    final String data;

    public Notification(String event, String data) {
        this.event = Objects.requireNonNull(event);
        this.data = Objects.requireNonNull(data);
    }

    public Stream<String> getStream() {
        return null;
    }

    public String getEvent() {
        return event;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "event='" + event + '\'' +
                ", data='" + data + '\'' +
                '}';
    }

    public static Notification of(FeedService.FeedNotify feedNotify) {
        if (Objects.isNull(feedNotify)) {
            return null;
        }

        return new Notification("feed-notify", "New Feed: " + feedNotify.getContent());
    }

    public static Notification of(FriendService.FriendRecommendationNotify friendRecommendationNotify) {
        if (Objects.isNull(friendRecommendationNotify)) {
            return null;
        }

        return new Notification("friend-request-notify", "Friend Request: " + friendRecommendationNotify.getName());
    }

    public static Notification empty() {
        return new Notification("empty", "");
    }

}

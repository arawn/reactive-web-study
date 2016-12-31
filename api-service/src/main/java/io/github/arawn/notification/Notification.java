package io.github.arawn.notification;

import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;

import java.util.Objects;

public class Notification {

    final String event;
    final String data;

    public Notification(String event, String data) {
        this.event = Objects.requireNonNull(event);
        this.data = Objects.requireNonNull(data);
    }

    public String getEvent() {
        return event;
    }

    public String getData() {
        return data;
    }


    public static Notification of(FeedService.FeedNotify feedNotify) {
        if (Objects.isNull(feedNotify)) {
            return null;
        }

        return new Notification("feed-notify", "New Feed: " + feedNotify.getContent());
    }

    public static Notification of(FriendService.FriendRequestNotify friendRequestNotify) {
        if (Objects.isNull(friendRequestNotify)) {
            return null;
        }

        return new Notification("friend-request-notify", "Friend Request: " + friendRequestNotify.getUsername());
    }

}

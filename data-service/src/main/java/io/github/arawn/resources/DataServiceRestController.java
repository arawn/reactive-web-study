package io.github.arawn.resources;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataServiceRestController {

    private FeedService feedService;
    private FriendService friendService;

    public DataServiceRestController(FeedService feedService, FriendService friendService) {
        this.feedService = feedService;
        this.friendService = friendService;
    }

    @GetMapping("/feed-notify")
    public FeedService.FeedNotify feedNotify() {
        return feedService.getFeedNotify();
    }

    @GetMapping("/friend-request-notify")
    public FriendService.FriendRequestNotify friendRequestNotify() {
        return friendService.getFriendRequestNotify();
    }

}

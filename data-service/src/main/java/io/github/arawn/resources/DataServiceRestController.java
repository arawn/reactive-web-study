package io.github.arawn.resources;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
public class DataServiceRestController {

    private UserService userService;
    private FeedService feedService;
    private FriendService friendService;

    public DataServiceRestController(UserService userService, FeedService feedService, FriendService friendService) {
        this.userService = userService;
        this.feedService = feedService;
        this.friendService = friendService;
    }

    @GetMapping("/user/{username}")
    public Mono<UserService.User> user(@PathVariable String username) {
        return userService.getUser(username);
    }

    @GetMapping("/user/{username}/feed")
    public Mono<FeedService.Feed> feed(@PathVariable String username, @RequestParam Optional<ServiceProcessing> processing) {
        return feedService.getFeeds(username, processing);
    }

    @GetMapping("/user/{username}/feed/count")
    public Mono<Long> feedCount(@PathVariable String username, @RequestParam Optional<ServiceProcessing> processing) {
        return feedService.getFeedCount(username, processing);
    }

    @GetMapping("/user/{username}/friend-recommendation")
    public Mono<FriendService.FriendRecommendation> friendRecommendationNotify(@PathVariable String username, @RequestParam Optional<ServiceProcessing> processing) {
        return friendService.getFriendRecommendation(username, processing);
    }

    @GetMapping("/user/{username}/friend/count")
    public Mono<Long> friendCount(@PathVariable String username, @RequestParam Optional<ServiceProcessing> processing) {
        return friendService.getFriendCount(username, processing);
    }

}

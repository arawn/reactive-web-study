package example.sync_async;

import io.github.arawn.service.FeedService.FeedNotify;
import io.github.arawn.service.FriendService.FriendRecommendationNotify;

public class SynchronousCallExample {

    public static void main(String[] args) {


        NotifyApi notifyApi = new NotifyApi();
        String username = "guest";


        FeedNotify feedNotify = notifyApi.getFeedNotify(username);

        // feedNotify 사용


        FriendRecommendationNotify friendNotify = notifyApi.getFriendRecommendationNotify(username);

        // friendNotify 사용

    }

}

package example.sync_async;

import io.github.arawn.service.FeedService.FeedNotify;
import io.github.arawn.service.FriendService.FriendRecommendationNotify;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AsynchronousCallByThreadExample {

    static Log LOG = LogFactory.getLog(AsynchronousCallByThreadExample.class);

    public static void main(String[] args) throws Exception {

        NotifyApi notifyApi = new NotifyApi();
        String username = "guest";


        Thread feedThread = new Thread(new Runnable() {

            @Override
            public void run() {

                FeedNotify feedNotify = notifyApi.getFeedNotify(username);
                LOG.info(feedNotify);

            }

        });
        feedThread.start();


        Thread friendThread = new Thread(() -> {

            FriendRecommendationNotify friendNotify = notifyApi.getFriendRecommendationNotify(username);
            LOG.info(friendNotify);

        });
        friendThread.start();

    }

}

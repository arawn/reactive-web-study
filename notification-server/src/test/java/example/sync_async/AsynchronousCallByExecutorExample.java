package example.sync_async;

import io.github.arawn.service.FeedService.FeedNotify;
import io.github.arawn.service.FriendService.FriendRecommendationNotify;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsynchronousCallByExecutorExample {

    static Log LOG = LogFactory.getLog(AsynchronousCallByExecutorExample.class);

    public static void main(String[] args) throws Exception {

        NotifyApi notifyApi = new NotifyApi();
        String username = "guest";


        Executor executor = Executors.newFixedThreadPool(4);


        executor.execute(new Runnable() {
            @Override
            public void run() {
                FeedNotify feedNotify = notifyApi.getFeedNotify(username);
                LOG.info(feedNotify);
            }
        });

        executor.execute(() -> {
            FriendRecommendationNotify friendNotify = notifyApi.getFriendRecommendationNotify(username);
            LOG.info(friendNotify);
        });

    }

}

package example.sync_async;

import io.github.arawn.service.FeedService.FeedNotify;
import io.github.arawn.service.FriendService.FriendRecommendationNotify;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AsynchronousCallFutureExample {

    static Log LOG = LogFactory.getLog(AsynchronousCallFutureExample.class);

    public static void main(String[] args) throws Exception {

        NotifyApi notifyApi = new NotifyApi();
        String username = "guest";


        Future<FeedNotify> feedFuture = notifyApi.getFeedNotifyForFuture(username);
        Future<FriendRecommendationNotify> friendFuture = notifyApi.getFriendRecommendationNotifyForFuture(username);

        for (;;) {
            if (feedFuture.isDone()) {
                FeedNotify feedNotify = feedFuture.get();

                // feedNotify 사용

                break;
            }

            // 필요에 따라 취소 가능
            // feedFuture.cancel(true);

            LOG.info("FeedNotify 데이터를 기다리는 중 입니다.");
            Thread.sleep(100);
        }


        FriendRecommendationNotify friendNotify = friendFuture.get(5, TimeUnit.SECONDS);

        // friendNotify 사용


        notifyApi.executorService.shutdownNow();

    }

}

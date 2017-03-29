package example.sync_async;

import io.github.arawn.service.FeedService.FeedNotify;
import io.github.arawn.service.FriendService.FriendRecommendationNotify;

import java.util.concurrent.TimeUnit;

public class AsynchronousCallCallbacksExample {

    public static void main(String[] args) throws Exception {

        NotifyApi notifyApi = new NotifyApi();
        String username = "guest";


        notifyApi.getFeedNotify(username, new NotifyApi.CompletionHandler<FeedNotify>() {

            @Override
            public void onSuccess(FeedNotify result) {
                // 데이터 사용
            }

            @Override
            public void onFailure(Throwable ex) {
                // 오류 처리
            }
        });


        notifyApi.getFriendRecommendationNotify(username, new NotifyApi.CompletionHandler<FriendRecommendationNotify>() {
            @Override
            public void onSuccess(FriendRecommendationNotify result) {
            }
            @Override
            public void onFailure(Throwable ex) {
            }
        });


        Thread.sleep(1000);
        notifyApi.executorService.shutdown();
        notifyApi.executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

}

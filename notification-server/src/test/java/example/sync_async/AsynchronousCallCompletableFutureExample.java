package example.sync_async;

import io.github.arawn.notification.Notification;
import io.github.arawn.service.FriendService.FriendRecommendationNotify;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AsynchronousCallCompletableFutureExample {

    static Log LOG = LogFactory.getLog(AsynchronousCallCompletableFutureExample.class);

    public static void main(String[] args) throws Exception {

        /*
        CompletableFuture.supplyAsync(() -> {

            // 첫번째 비동기 작업

            return "async task";
        }).thenApply(result -> {

            // 두번째 비동기 작업

            return result.length();
        }).exceptionally(error -> {

            // 비동기 작업 중 오류가 발생시 처리

            return Integer.MIN_VALUE;
        }).thenAccept(length -> {

            // 비동기 작업 결과 소비

            LOG.info("Length : " + length);
        });
        */


        NotifyApi notifyApi = new NotifyApi();
        String username = "elton";


        notifyApi.getFeedNotifyForCF(username).thenAccept(feedNotify -> {
            LOG.info(feedNotify);
        });

        notifyApi.getFriendRecommendationNotifyForCF(username).thenAccept(friendNotify -> {
            LOG.info(friendNotify);
        });


        // 2개의 비동기 작업을 합성해서 처리
        username = "florence";
        notifyApi.getUserForCF(username)
                 .thenCompose(notifyApi::getFeedNotifyForCF)
                 .thenAccept(notification -> {
                     // 데이터 사용
                 }).join();


        notifyApi.executorService.shutdown();
        notifyApi.executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

}

package example.sync_async;

import io.github.arawn.service.FeedService.FeedNotify;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class NotifyApiTest {

    static Log LOG = LogFactory.getLog(NotifyApiTest.class);

    public static void main(String[] args) throws Exception {

        NotifyApi notifyApi = new NotifyApi();
        String username = "elton";


        notifyApi.getUser(username, new NotifyApi.CompletionHandler<NotifyApi.User>() {

            @Override
            public void onSuccess(NotifyApi.User user) {

                notifyApi.getFeedNotify(user, new NotifyApi.CompletionHandler<FeedNotify>() {

                    @Override
                    public void onSuccess(FeedNotify feedNotify) {
                        // 데이터 사용
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        // 오류 처리
                    }

                });

            }

            @Override
            public void onFailure(Throwable error) {
                // 오류 처리
            }

        });


        Future<NotifyApi.User> userFuture = notifyApi.getUserForFuture(username);
        for (;;) {
            if (userFuture.isDone()) {
                NotifyApi.User user = userFuture.get();

                FeedNotify feedNotify = notifyApi.getFeedNotifyForFuture(user).get(1, TimeUnit.SECONDS);

                // 데이터 사용

                break;
            }

            LOG.info("User 데이터를 기다리는 중 입니다.");
            Thread.sleep(100);
        }


        Thread.sleep(1000);
        notifyApi.executorService.shutdown();
        notifyApi.executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

}

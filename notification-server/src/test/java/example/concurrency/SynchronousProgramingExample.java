package example.concurrency;

import example.sync_async.NotifyApi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author ykpark@woowahan.com
 */
public class SynchronousProgramingExample {

    public static void main(String[] args) throws InterruptedException {

//        printUserInfo("fay");           // 일감 1
//        printUserInfo("murphy");        // 일감 2
//        printUserInfo("nichole");       // 일감 3


        ExecutorService executorService = Executors.newFixedThreadPool(3);

        executorService.execute(() -> printUserInfo("fay"));        // 일감 1
        executorService.execute(() -> printUserInfo("murphy"));     // 일감 2
        executorService.execute(() -> printUserInfo("nichole"));    // 일감 3

        executorService.execute(() -> printUserInfo("phillip"));    // 일감 4
        executorService.execute(() -> printUserInfo("adrienne"));   // 일감 5
        executorService.execute(() -> printUserInfo("nita"));       // 일감 6


        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);

    }

    static NotifyApi notifyApi = new NotifyApi();

    static void printUserInfo(String username) {
        NotifyApi.User user = notifyApi.getUser(username);
        Long feedCount = notifyApi.getFeedCount(username);
        Long friendCount = notifyApi.getFriendCount(username);

        UserInfo userInfo = new UserInfo(user.getName(), feedCount, friendCount);

        System.out.println("User Info");
        System.out.println("name = " + userInfo.getName());
        System.out.println("feedCount = " + userInfo.getFeedCount());
        System.out.println("friendCount = " + userInfo.getFriendCount());
    }


    public static class UserInfo {

        final String name;
        final Long feedCount;
        final Long friendCount;

        public UserInfo(String name, Long feedCount, Long friendCount) {
            this.name = name;
            this.feedCount = feedCount;
            this.friendCount = friendCount;
        }

        public String getName() {
            return name;
        }

        public Long getFeedCount() {
            return feedCount;
        }

        public Long getFriendCount() {
            return friendCount;
        }

    }

}

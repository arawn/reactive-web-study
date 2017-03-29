package example.concurrency;

import example.sync_async.NotifyApi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsynchronousProgramingExample {

    public static void main(String[] args) throws Exception {

        printUserInfo("fay");        // 일감 1
        printUserInfo("murphy");     // 일감 2
        printUserInfo("nichole");    // 일감 3

        printUserInfo("phillip");    // 일감 4
        printUserInfo("adrienne");   // 일감 5
        printUserInfo("nita");       // 일감 6

    }


    static ExecutorService executorService = Executors.newFixedThreadPool(4);
    static NotifyApi notifyApi = new NotifyApi();

    static void printUserInfo(String username) {
        CompletableFuture.supplyAsync(() -> notifyApi.getUser(username), executorService)
                         .thenCompose(user -> {
                             CompletableFuture<Long> feedCountCF = CompletableFuture.supplyAsync(
                                     () -> notifyApi.getFeedCount(username), executorService);

                             CompletableFuture<Long> friendCountCF = CompletableFuture.supplyAsync(
                                     () -> notifyApi.getFriendCount(username), executorService);

                             return feedCountCF.thenCombineAsync(friendCountCF, (feedCount, friendCount) -> {
                                 return new SynchronousProgramingExample.UserInfo(user.getName(), feedCount, friendCount);
                             }, executorService);
                         })
                         .thenAccept(userInfo -> {
                             System.out.println("User Info");
                             System.out.println("name = " + userInfo.getName());
                             System.out.println("feedCount = " + userInfo.getFeedCount());
                             System.out.println("friendCount = " + userInfo.getFriendCount());
                         });
    }


}

package example;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * @author ykpark@woowahan.com
 */
public class SyncAsyncExample {

    static ExecutorService executorService = Executors.newCachedThreadPool();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        SyncAsyncExample example = new SyncAsyncExample();

//        for(int count = 1; count<=5; count++) {
//            System.out.println("getDeveloperProfile() - " + count);
//            example.getDeveloperProfile("arawn");
//            System.out.println();
//        }
//
//        for(int count = 1; count<=5; count++) {
//            System.out.println("getDeveloperProfileAsync() - " + count);
//            example.getDeveloperProfileAsync("arawn");
//            System.out.println();
//        }

        Future<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep((long) (Math.random() * 300 + 100));
            } catch (InterruptedException ignore) {
            }

            System.out.println("done future");

            return UUID.randomUUID().toString();
        }, executorService);

        boolean isDone = false;
        while (!isDone) {
            System.out.println("future done: " + future.isDone());

            if (future.isDone()) {
                isDone = true;
                System.out.println("future get: " + future.get());
            }

            Thread.sleep(100);
        }

        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep((long) (Math.random() * 300 + 100));
            } catch (InterruptedException ignore) {
            }

            System.out.println("done completableFuture");

            return UUID.randomUUID().toString();
        }, executorService);
        completableFuture.thenAccept(value -> System.out.println("completableFuture callback: " + value));

        executorService.shutdown();
    }


    public DeveloperProfile getDeveloperProfile(String developerId) {
        DeveloperService developerService = new DeveloperService();
        GithubService githubService = new GithubService();

        System.out.println("1. call getRepositories()");
        List<GithubRepository> repositories = githubService.getRepositories(developerId);
        System.out.println("3. call getDeveloper()");
        Developer developer = developerService.getDeveloper(developerId);

        return new DeveloperProfile(developer, repositories);
    }

    public DeveloperProfile getDeveloperProfileAsync(String developerId) throws ExecutionException, InterruptedException {
        DeveloperService developerService = new DeveloperService();
        GithubService githubService = new GithubService();

        System.out.println("1. call getRepositoriesAsync()");
        Future<List<GithubRepository>> repositoriesFuture = githubService.getRepositoriesAsync(developerId);
        System.out.println("3. call getDeveloper()");
        Developer developer = developerService.getDeveloper(developerId);

        List<GithubRepository> repositories = repositoriesFuture.get();

        return new DeveloperProfile(developer, repositories);
    }


    class DeveloperProfile {
        Developer developer;
        List<GithubRepository> repositories;

        DeveloperProfile(Developer developer, List<GithubRepository> repositories) {
            this.developer = developer;
            this.repositories = repositories;
        }
    }

    class DeveloperService {
        Developer getDeveloper(String developerId) {
            try {
                Thread.sleep((long) (Math.random() * 300 + 100));
            } catch (InterruptedException ignore) {
            }

            System.out.println("4. return getDeveloper()");

            return new Developer();
        }
    }

    class Developer {
        String id;
        String name;
    }

    class GithubService {
        List<GithubRepository> getRepositories(String developerId) {
            try {
                Thread.sleep((long) (Math.random() * 300 + 100));
            } catch (InterruptedException ignore) {
            }

            System.out.println("2. return getRepositories()");

            return Collections.emptyList();
        }

        CompletableFuture<List<GithubRepository>> getRepositoriesAsync(String developerId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 500 + 100));
                } catch (InterruptedException ignore) {
                }

                System.out.println("2. return getRepositoriesAsync()");

                return Collections.emptyList();
            }, executorService);
        }

    }

    class GithubRepository {
        String id;
        String number;
    }

}

package example.sync_async;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.arawn.service.FeedService.FeedNotify;
import io.github.arawn.service.FriendService.FriendRecommendationNotify;
import io.github.arawn.service.ServiceOperationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.nio.charset.Charset.forName;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NotifyApi {

    static Log LOG = LogFactory.getLog(NotifyApi.class);

    final String USER_URL = "http://localhost:9000/user/%s?processing=MEDIUM";
    final String FEED_COUNT_URL = "http://localhost:9000/user/%s/feed/count?processing=MEDIUM";
    final String FEED_NOTIFY_URL = "http://localhost:9000/user/%s/feed";
    final String FRIEND_COUNT_URL = "http://localhost:9000/user/%s/friend/count?processing=MEDIUM";
    final String FRIEND_NOTIFY_URL = "http://localhost:9000/user/%s/friend-recommendation";

    final ExecutorService executorService = Executors.newCachedThreadPool();
    final ObjectMapper objectMapper = new ObjectMapper();

    public Long getFeedCount(String username) {
        LOG.info("Feed Count 데이터 얻기 메소드가 호출되었습니다. [username: " + username + "]");

        try {
            URL url = new URL(String.format(FEED_COUNT_URL, username));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(30000);

            Long feedCount = Long.valueOf(read(urlConnection.getInputStream()));

            LOG.info("Feed Count 데이터가 반환됩니다. [username: " + username + "]");

            return feedCount;
        } catch (IOException error) {
            throw new ServiceOperationException(error);
        }
    }

    public FeedNotify getFeedNotify(String username) {
        LOG.info("FeedNotify 데이터 얻기 메소드가 호출되었습니다. [username: " + username + "]");

        try {
            URL url = new URL(String.format(FEED_NOTIFY_URL, username));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(30000);

            FeedNotify feedNotify = objectMapper.readValue(urlConnection.getInputStream(), FeedNotify.class);

            LOG.info("FeedNotify 데이터가 반환됩니다. [username: " + username + "]");

            return feedNotify;
        } catch (IOException error) {
            throw new ServiceOperationException(error);
        }
    }

    public Future<FeedNotify> getFeedNotifyForFuture(String username) {
        return executorService.submit(new Callable<FeedNotify>() {
            @Override
            public FeedNotify call() throws Exception {
                return getFeedNotify(username);
            }
        });
    }

    public Future<FeedNotify> getFeedNotifyForFuture(User user) {
        return executorService.submit(new Callable<FeedNotify>() {
            @Override
            public FeedNotify call() throws Exception {
                return getFeedNotify(user.getUsername());
            }
        });
    }

    public CompletableFuture<FeedNotify> getFeedNotifyForCF(String username) {
        return CompletableFuture.supplyAsync(new Supplier<FeedNotify>() {
            @Override
            public FeedNotify get() {
                return getFeedNotify(username);
            }
        }, executorService);
    }

    public CompletableFuture<FeedNotify> getFeedNotifyForCF(User user) {
        return CompletableFuture.supplyAsync(new Supplier<FeedNotify>() {
            @Override
            public FeedNotify get() {
                return getFeedNotify(user.getUsername());
            }
        }, executorService);
    }

    public void getFeedNotify(String username, CompletionHandler<FeedNotify> completionHandler) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    completionHandler.onSuccess(getFeedNotify(username));
                } catch (Exception error) {
                    completionHandler.onFailure(error);
                }
            }
        });
    }

    public void getFeedNotify(User user, CompletionHandler<FeedNotify> completionHandler) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    completionHandler.onSuccess(getFeedNotify(user.getUsername()));
                } catch (Exception error) {
                    completionHandler.onFailure(error);
                }
            }
        });
    }

    public Long getFriendCount(String username) {
        LOG.info("Friend Count 데이터 얻기 메소드가 호출되었습니다. [username: " + username + "]");

        try {
            URL url = new URL(String.format(FRIEND_COUNT_URL, username));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(30000);

            Long friendCount = Long.valueOf(read(urlConnection.getInputStream()));

            LOG.info("Friend Count 데이터가 반환됩니다. [username: " + username + "]");

            return friendCount;
        } catch (IOException error) {
            throw new ServiceOperationException(error);
        }
    }

    public FriendRecommendationNotify getFriendRecommendationNotify(String username) {
        LOG.info("FriendRecommendationNotify 데이터 얻기 메소드가 호출되었습니다. [username: " + username + "]");

        try {
            URL url = new URL(String.format(FRIEND_NOTIFY_URL, username));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(30000);

            FriendRecommendationNotify notify = objectMapper.readValue(urlConnection.getInputStream(), FriendRecommendationNotify.class);

            LOG.info("FriendRecommendationNotify 데이터가 반환됩니다. [username: " + username + "]");

            return notify;
        } catch (IOException error) {
            throw new ServiceOperationException(error);
        }
    }

    public Future<FriendRecommendationNotify> getFriendRecommendationNotifyForFuture(String username) {
        return executorService.submit(() -> getFriendRecommendationNotify(username));
    }

    public CompletableFuture<FriendRecommendationNotify> getFriendRecommendationNotifyForCF(String username) {
        return CompletableFuture.supplyAsync(() -> getFriendRecommendationNotify(username), executorService);
    }

    public void getFriendRecommendationNotify(String username, CompletionHandler<FriendRecommendationNotify> completionHandler) {
        executorService.execute(() -> {
            try {
                completionHandler.onSuccess(getFriendRecommendationNotify(username));
            } catch (Exception error) {
                completionHandler.onFailure(error);
            }
        });
    }



    interface CompletionHandler<R> extends SuccessCallback<R>, FailureCallback {

    }

    interface SuccessCallback<R> {
        void onSuccess(R result);
    }

    interface FailureCallback {
        void onFailure(Throwable ex);
    }


    public static class User {

        String username;
        String name;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public User getUser(String username) {
        LOG.info("User 데이터 얻기 메소드가 호출되었습니다. [username: " + username + "]");

        try {
            URL url = new URL(String.format(USER_URL, username));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(30000);

            User user = objectMapper.readValue(urlConnection.getInputStream(), User.class);

            LOG.info("User 데이터가 반환됩니다. [username: " + username + "]");

            return user;
        } catch (IOException error) {
            throw new ServiceOperationException(error);
        }
    }

    public Future<User> getUserForFuture(String username) {
        return executorService.submit(() -> getUser(username));
    }

    public CompletableFuture<User> getUserForCF(String username) {
        return CompletableFuture.supplyAsync(() -> getUser(username), executorService);
    }

    public void getUser(String username, CompletionHandler<User> completionHandler) {
        executorService.execute(() -> {
            try {
                completionHandler.onSuccess(getUser(username));
            } catch (Exception error) {
                completionHandler.onFailure(error);
            }
        });
    }


    public String read(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, forName(UTF_8.name())))) {
            int range;
            while ((range = reader.read()) != -1) {
                content.append((char) range);
            }
        }
        return content.toString();
    }

}

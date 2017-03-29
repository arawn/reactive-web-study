package io.github.arawn.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.arawn.service.FeedService.FeedNotify;
import io.github.arawn.service.ServiceOperationException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.stream.Stream;

public class NotificationStreamDisassemble {

    public static void main(String[] args) {

        FeedService feedService = new FeedService();

        String username = "guest";
        Iterator<Notification> feedNotifies =
                Stream.generate(() -> feedService.getFeedNotify(username))
                      .map(Notification::of)
                      .limit(5)
                      .iterator();


        while (feedNotifies.hasNext()) {
            Notification next = feedNotifies.next();

            System.out.println(next);
        }

    }


    static class FeedService {

        final String FEED_NOTIFY_URL = "http://localhost:9000/user/%s/feed";
        final ObjectMapper MAPPER = new ObjectMapper();

        FeedNotify getFeedNotify(String username) {
            try {
                URL url = new URL(String.format(FEED_NOTIFY_URL, username));

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setConnectTimeout(3000);
                urlConnection.setReadTimeout(3000);

                return MAPPER.readValue(urlConnection.getInputStream(), FeedNotify.class);
            } catch (IOException error) {
                throw new ServiceOperationException(error);
            }
        }

    }

}

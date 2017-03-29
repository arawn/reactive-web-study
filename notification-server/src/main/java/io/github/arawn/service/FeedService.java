package io.github.arawn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.arawn.service.support.HttpClientFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FeedService {

    static final Log log = LogFactory.getLog(FeedService.class);
    static final String url = "http://localhost:9000/user/%s/feed?processing=%s";

    final AwkwardChaosMonkey chaosMonkey = new AwkwardChaosMonkey();

    final HttpClient httpClient;
    final HttpAsyncClient httpAsyncClient;
    final ObjectMapper objectMapper = new ObjectMapper();

    final ServiceProcessing processing;

    public FeedService() {
        this(ServiceProcessing.MEDIUM);
    }

    public FeedService(ServiceProcessing processing) {
        this.httpClient = Objects.requireNonNull(HttpClientFactory.getHttpClient());
        this.httpAsyncClient = Objects.requireNonNull(HttpClientFactory.getHttpAsyncClient());

        this.processing = Objects.requireNonNull(processing);
    }

    public FeedNotify getFeedNotify(String username) {
        if (chaosMonkey.doItNow()) {
            throw new ServiceOperationException();
        }

        try {
            HttpGet request = new HttpGet(String.format(url, username, processing.name()));
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new ServiceOperationException();
            }

            return objectMapper.readValue(response.getEntity().getContent(), FeedNotify.class);
        } catch (IOException error) {
            throw new ServiceOperationException(error);
        }
    }

    public CompletableFuture<FeedNotify> getFeedNotifyAsync(String username) throws ServiceOperationException {
        if (chaosMonkey.doItNow()) {
            throw new ServiceOperationException("카오스 몽키가 동작했습니다.");
        }

        CompletableFuture<FeedNotify> completableFuture = new CompletableFuture<>();

        HttpGet request = new HttpGet(String.format(url, username, processing.name()));
        httpAsyncClient.execute(request, new FutureCallback<HttpResponse>() {

            @Override
            public void completed(HttpResponse response) {
                try {
                    int statusCode = response.getStatusLine().getStatusCode();
                    InputStream content = response.getEntity().getContent();

                    if (statusCode != HttpStatus.SC_OK) {
                        completableFuture.completeExceptionally(new ServiceOperationException());
                    }

                    completableFuture.complete(objectMapper.readValue(content, FeedNotify.class));
                } catch (IOException error) {
                    completableFuture.completeExceptionally(new ServiceOperationException(error));
                }
            }

            @Override
            public void failed(Exception error) {
                completableFuture.completeExceptionally(error);
            }

            @Override
            public void cancelled() {
                completableFuture.cancel(true);
            }

        });

        return completableFuture;
    }

    public Publisher<FeedNotify> getFeedNotifyReactive(String username) throws ServiceOperationException {
        return Flux.from(new Publisher<FeedNotify>() {

            volatile boolean cancel = false;

            @Override
            public void subscribe(Subscriber<? super FeedNotify> subscriber) {

                subscriber.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        for (int count=0; count<n; count++) {
                            if (cancel) {
                                return;
                            }

                            try {
                                getFeedNotifyAsync(username).whenComplete((feedNotify, throwable) -> {
                                    if (Objects.nonNull(throwable)) {
                                        subscriber.onError(throwable);
                                    } else {
                                        subscriber.onNext(feedNotify);
                                    }
                                });
                            } catch (Exception error) {
                                subscriber.onError(error);
                            }
                        }
                    }

                    @Override
                    public void cancel() {
                        cancel = true;
                    }

                });
            }

        });
    }


    public static class FeedNotify {

        String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return String.format("새로운 피드(Feed)가 등록되었습니다. [%s]", content);
        }

    }

}

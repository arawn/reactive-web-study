package io.github.arawn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.arawn.service.support.HttpClientFactory;
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
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FriendService {

    static final String url = "http://localhost:9000/user/%s/friend-recommendation?processing=%s";

    final AwkwardChaosMonkey chaosMonkey = new AwkwardChaosMonkey();

    final HttpClient httpClient;
    final HttpAsyncClient httpAsyncClient;
    final ObjectMapper objectMapper = new ObjectMapper();

    final ServiceProcessing processing;

    public FriendService() {
        this(ServiceProcessing.SLOW);
    }

    public FriendService(ServiceProcessing processing) {
        this.httpClient = Objects.requireNonNull(HttpClientFactory.getHttpClient());
        this.httpAsyncClient = Objects.requireNonNull(HttpClientFactory.getHttpAsyncClient());

        this.processing = Objects.requireNonNull(processing);
    }

    public FriendRecommendationNotify getRecommendationNotify(String username) throws ServiceOperationException {
        if (chaosMonkey.doItNow()) {
            throw new ServiceOperationException();
        }

        try {
            HttpGet request = new HttpGet(String.format(url, username, processing.name()));
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new ServiceOperationException();
            }

            return objectMapper.readValue(response.getEntity().getContent(), FriendRecommendationNotify.class);
        } catch (IOException error) {
            throw new ServiceOperationException(error);
        }
    }

    public CompletableFuture<FriendRecommendationNotify> getRecommendationNotifyAsync(String username) throws ServiceOperationException {
        if (chaosMonkey.doItNow()) {
            throw new ServiceOperationException("카오스 몽키가 동작했습니다.");
        }

        CompletableFuture<FriendRecommendationNotify> completableFuture = new CompletableFuture<>();

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

                    completableFuture.complete(objectMapper.readValue(content, FriendRecommendationNotify.class));
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

    public Publisher<FriendRecommendationNotify> getRecommendationNotifyReactive(String username) throws ServiceOperationException {
        return Flux.from(new Publisher<FriendRecommendationNotify>() {

            volatile boolean cancel = false;

            @Override
            public void subscribe(Subscriber<? super FriendRecommendationNotify> subscriber) {

                subscriber.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        for (int count=0; count<n; count++) {
                            if (cancel) {
                                return;
                            }

                            try {
                                getRecommendationNotifyAsync(username).whenComplete((feedNotify, throwable) -> {
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


    public static class FriendRecommendationNotify {

        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }


        @Override
        public String toString() {
            return name + "님을 친구로 추천드립니다!";
        }

    }

}

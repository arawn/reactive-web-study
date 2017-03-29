package io.github.arawn.service;

import io.github.arawn.service.support.HttpClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StopWatch;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FriendServiceTest {

    FriendService friendService;

    @Before
    public void before() {
        AwkwardChaosMonkey.disable();

        new HttpClientFactory().contextInitialized(null);
        friendService = new FriendService(ServiceProcessing.FIX);
    }

    @After
    public void after() {
        new HttpClientFactory().contextDestroyed(null);
    }

    @Test
    public void getRecommendationNotify() throws Exception {
        StopWatch stopWatch = new StopWatch("recommendationNotify");
        stopWatch.start();

        IntStream.rangeClosed(1, 10)
                 .mapToObj(value -> friendService.getRecommendationNotify("user-" + value))
                 .forEach(System.out::println);

        stopWatch.stop();
        System.out.println();
        System.out.println(stopWatch.prettyPrint());
    }

    @Test
    public void getRecommendationNotifyAsync() throws Exception {
        StopWatch stopWatch = new StopWatch("recommendationNotifyAsync");
        stopWatch.start();

        IntStream.rangeClosed(1, 10)
                 .mapToObj(value -> friendService.getRecommendationNotifyAsync("user-" + value))
                 .collect(Collectors.toList())
                 .stream()
                 .map(CompletableFuture::join)
                 .forEach(System.out::println);

        stopWatch.stop();
        System.out.println();
        System.out.println(stopWatch.prettyPrint());
    }


}
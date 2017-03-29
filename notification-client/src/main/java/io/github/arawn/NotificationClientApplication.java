package io.github.arawn;

import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SpringBootApplication
public class NotificationClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationClientApplication.class, args);
    }


    final Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
    final WebTarget target = client.target("http://localhost:8080/notification/stream/non-blocking-async-concurrency");

    @Bean
    public CommandLineRunner runner() {
        return args -> {
            List<CompletableFuture<EventCountDown>> clients = new ArrayList<>();
            for (int count = 0; count <= 1000; count++) {
                clients.add(createEventCountDown(count, target, 10));
            }

            List<EventCountDown> countDowns = clients.stream().map(CompletableFuture::join).collect(Collectors.toList());
            LongSummaryStatistics statistics = countDowns.stream().mapToLong(it -> it.countTime).summaryStatistics();

            System.out.println();
            countDowns.forEach(System.out::println);

            System.out.println();
            System.out.println(String.format("최소 작업시간: %d", statistics.getMin()));
            System.out.println(String.format("최대 작업시간: %d", statistics.getMax()));
            System.out.println(String.format("평균 작업시간: %f", statistics.getAverage()));
        };
    }

    private CompletableFuture<EventCountDown> createEventCountDown(int number, WebTarget target, int count) {
        CompletableFuture<EventCountDown> completableFuture = new CompletableFuture<>();

        EventSource eventSource = EventSource.target(target).build();
        eventSource.register(new EventCountDown(String.format("es-%d", number), completableFuture, eventSource, count));
        eventSource.open();

        return completableFuture;
    }

    class EventCountDown implements EventListener {

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String name;
        CompletableFuture<EventCountDown> completableFuture;
        EventSource eventSource;
        AtomicInteger counter;

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime;
        long countTime;

        EventCountDown(String name, CompletableFuture<EventCountDown> completableFuture, EventSource eventSource, int count) {
            this.name = name;
            this.completableFuture = completableFuture;
            this.eventSource = eventSource;
            this.counter = new AtomicInteger(count);
        }

        @Override
        public void onEvent(InboundEvent inboundEvent) {
            if (counter.decrementAndGet() == 0) {
                eventSource.close();
                endTime = LocalDateTime.now();
                countTime = startTime.until(endTime, ChronoUnit.SECONDS);

                completableFuture.complete(this);
            }
        }

        @Override
        public String toString() {
            return String.format("[%s] 시작시간: %s, 종료시간: %s, 작업시간: %d", name, formatter.format(startTime), formatter.format(endTime), countTime);
        }

    }

}
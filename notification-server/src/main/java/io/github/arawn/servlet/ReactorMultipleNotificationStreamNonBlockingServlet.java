package io.github.arawn.servlet;

import io.github.arawn.notification.Notification;
import io.github.arawn.service.AwkwardChaosMonkey;
import io.github.arawn.service.FeedService;
import io.github.arawn.service.FriendService;
import io.github.arawn.servlet.ReactiveNotificationStreamServlet.AsyncContextEventPublisher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Schedulers;
import reactor.core.scheduler.TimedScheduler;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@WebServlet(urlPatterns = "/notification/stream/reactor-multiple-nonblocking", asyncSupported = true)
public class ReactorMultipleNotificationStreamNonBlockingServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(ReactorMultipleNotificationStreamNonBlockingServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.enable(3, 10);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/reactor-multiple-nonblocking");
        
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");
        response.getOutputStream().flush();

        FeedService feedService = new FeedService();
        FriendService friendService = new FriendService();


        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout((new Random().nextInt(5) + 5) * 1000);

        Flux<Notification> feedNotifies = Flux
                .from(feedService.getFeedNotifyReactive(obtainUsername(request)))
                .publishOn(Schedulers.parallel(), 1)
                .map(Notification::of);

        Flux<Notification> friendRecommendationNotifies = Flux
                .from(friendService.getRecommendationNotifyReactive(obtainUsername(request)))
                .publishOn(Schedulers.parallel(), 1)
                .map(Notification::of);

        Flux<byte[]> asyncEvents = Flux.from(new AsyncContextEventPublisher(asyncContext))
                                       .map(asyncEvent -> new byte[0]);

        Flux<byte[]> notifies = Flux
                .merge(feedNotifies, friendRecommendationNotifies)
                .map(notification -> {
                    //log.info("next notification/stream/reactor-multiple-nonblocking : " + notification.getEvent());

                    String content = "event: " + notification.getEvent() + "\n" +
                                     "data: " + notification.getData() + "\n\n";

                    return content.getBytes();
                })
                .mergeWith(asyncEvents)
                .doOnError(throwable -> {
                    log.error("error notification/stream/reactor-multiple-nonblocking : " + throwable.getMessage());

                })
                .doOnTerminate(() -> {
                    log.info("complete notification/stream/reactor-multiple-nonblocking");
                });

        ServletOutputStreamFlushProcessor flushProcessor = new ServletOutputStreamFlushProcessor();
        response.getOutputStream().setWriteListener(new WriteListener() {
            @Override
            public void onWritePossible() throws IOException {
                // ignore
            }

            @Override
            public void onError(Throwable throwable) {
                flushProcessor.onError(throwable);
            }
        });
        ServletOutputStreamWriteProcessor writeProcessor = new ServletOutputStreamWriteProcessor(notifies, asyncContext);
        writeProcessor.subscribe(flushProcessor);

        log.info("out notification/stream/reactor-multiple-nonblocking");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }


    final static TimedScheduler ioScheduler = Schedulers.newTimer("write-io", true);
    final static class ServletOutputStreamWriteProcessor extends FluxProcessor<byte[], ServletOutputStream> {

        final Flux<byte[]> publisher;
        final AsyncContext asyncContext;
        final ServletOutputStream outputStream;

        Subscription subscription;
        Subscriber<? super ServletOutputStream> subscriber;

        public ServletOutputStreamWriteProcessor(Flux<byte[]> publisher, AsyncContext asyncContext) throws IOException {
            this.publisher = publisher;
            this.asyncContext = asyncContext;
            this.outputStream = asyncContext.getResponse().getOutputStream();

            this.publisher.publishOn(ioScheduler).subscribe(this);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (Operators.validate(this.subscription, subscription)) {
                this.subscription = subscription;
            }
        }

        @Override
        public void onNext(byte[] bytes) {
            if (outputStream.isReady()) {
                try {
                    outputStream.write(bytes);
                    ioScheduler.schedule(() -> subscriber.onNext(outputStream), 10, TimeUnit.MILLISECONDS);
                } catch (IOException error) {
                    subscriber.onError(error);
                }
            } else {
                ioScheduler.schedule(() -> onNext(bytes), 10, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            subscriber.onComplete();
            asyncContext.complete();
        }

        @Override
        public void subscribe(Subscriber<? super ServletOutputStream> subscriber) {
            this.subscriber = subscriber;

            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    subscription.request(1);
                }
                @Override
                public void cancel() {
                    subscription.cancel();
                    asyncContext.complete();
                }
            });
        }

    }

    final static class ServletOutputStreamFlushProcessor extends FluxProcessor<ServletOutputStream, Void> {

        Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            if (Operators.validate(this.subscription, subscription)) {
                this.subscription = subscription;
                this.subscription.request(1);
            }
        }

        @Override
        public void onNext(ServletOutputStream outputStream) {
            if (outputStream.isReady()) {
                try {
                    outputStream.flush();
                    subscription.request(1);
                } catch (IOException error) {
                    onError(error);
                }
            } else {
                ioScheduler.schedule(() -> onNext(outputStream), 10, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            subscription.cancel();
        }

        @Override
        public void onComplete() {

        }

    }

    class DataWriteException extends RuntimeException {
        public DataWriteException(Throwable cause) {
            super(cause.getMessage());
        }
    }

}

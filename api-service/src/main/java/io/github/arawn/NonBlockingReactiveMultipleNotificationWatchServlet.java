package io.github.arawn;

import io.github.arawn.notification.ReactiveMultipleNotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Schedulers;
import reactor.core.scheduler.TimedScheduler;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@WebServlet(urlPatterns = "/notification/watch/non-blocking-reactive-multiple", asyncSupported = true)
public class NonBlockingReactiveMultipleNotificationWatchServlet extends HttpServlet {

    final static Log log = LogFactory.getLog("NonBlockingReactiveMultiple");

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in non-blocking-reactive-multiple");

        AwkwardChaosMonkey.STOP.set(true);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

        AsyncContext asyncContext = request.startAsync();

        // TODO doOnNext, doOnError, doOnComplate 순서 중요 왜???

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

        asyncContext.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                log.info("complete non-blocking-reactive-multiple");
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                flushProcessor.onError(event.getThrowable());
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                flushProcessor.onError(event.getThrowable());
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // ignore
            }
        });

        ReactiveMultipleNotificationStream notificationStream = new ReactiveMultipleNotificationStream();
        new ServletOutputStreamWriteProcessor(notificationStream.watch().map(notification -> {
            String content = "event: " + notification.getEvent() + "\n" +
                             "data: " + notification.getData() + "\n\n";

            return content.getBytes();
        }), asyncContext).take(50).subscribe(flushProcessor);

        this.log.info("out non-blocking-reactive-multiple");
    }


    final static TimedScheduler ioScheduler = Schedulers.newTimer("io-worker", true);
    final static class ServletOutputStreamWriteProcessor extends FluxProcessor<byte[], ServletOutputStream> {

        final Publisher<byte[]> publisher;
        final AsyncContext asyncContext;
        final ServletOutputStream outputStream;

        Subscription subscription;
        Subscriber<? super ServletOutputStream> subscriber;

        public ServletOutputStreamWriteProcessor(Publisher<byte[]> publisher, AsyncContext asyncContext) throws IOException {
            this.publisher = publisher;
            this.asyncContext = asyncContext;
            this.outputStream = asyncContext.getResponse().getOutputStream();

            this.publisher.subscribe(this);
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




    //        Disposable disposable = notificationStream.watch()
//                                                .map(notification -> {
//                                                    String content = "event: " + notification.getEvent() + "\n" +
//                                                            "data: " + notification.getData() + "\n\n";
//
//                                                    return content.getBytes();
//                                                })
//                                                .takeWhile(bytes -> {
//                                                    while (!outputStream.isReady()) {
//                                                    }
//                                                    return true;
//                                                }).doOnNext(bytes -> {
//                                                    System.out.println("write process");
//
//                                                    while (!outputStream.isReady()) {
//                                                    }
//
//                                                    try {
//                                                        outputStream.write(bytes);
//                                                    } catch (IOException error) {
//                                                        System.out.println("write process onNext: " + bytes);
//                                                        error.printStackTrace();
//                                                    }
//                                                }).takeWhile(bytes -> {
//                                                    while (!outputStream.isReady()) {
//                                                    }
//                                                    return true;
//                                                }).doOnNext(bytes -> {
//                                                    System.out.println("flush process");
//                                                    try {
//                                                        outputStream.flush();
//                                                    } catch (IOException error) {
//                                                        System.out.println("flush process onNext: " + bytes);
//                                                        error.printStackTrace();
//                                                    }
//                                                }).doOnError(throwable -> {
//                                                    log.error("notification stream error!", throwable);
//                                                    throwable.printStackTrace();
//                                                }).doOnComplete(asyncContext::complete).log().subscribe();
//
//        Flux<byte[]> dataFlux = notificationStream.watch()
//                                                  .map(notification -> {
//                                                      String content = "event: " + notification.getEvent() + "\n" +
//                                                                       "data: " + notification.getData() + "\n\n";
//
//                                                      return content.getBytes();
//                                                  }).doOnError(throwable -> {
//                                                      throwable.printStackTrace();
//                                                      asyncContext.complete();
//                                                  }).log();
//        DirectProcessor<byte[]> writeProcess = DirectProcessor.create();
//        writeProcess.doOnSubscribe(subscription -> {
//            System.out.println("write subscribe");
//
//            dataFlux.subscribe(writeProcess);
//        }).doOnNext(bytes -> {
//            System.out.println("write process");
//
//            if (outputStream.isReady()) {
//                try {
//                    outputStream.write(bytes);
//                } catch (IOException error) {
//                    writeProcess.onError(error);
//                }
//            } else {
//                writeProcess.onNext(bytes);
//            }
//        });
//
//        DirectProcessor<byte[]> flushProcess = DirectProcessor.create();
//        flushProcess.doOnSubscribe(subscription -> {
//            System.out.println("flush subscribe");
//
//            writeProcess.subscribe(flushProcess);
//        }).doOnNext(bytes -> {
//            System.out.println("flush process");
//
//            if (outputStream.isReady()) {
//                try {
//                    outputStream.flush();
//                } catch (IOException error) {
//                    flushProcess.onError(error);
//                }
//            } else {
//                flushProcess.onNext(bytes);
//            }
//        });
//
//        flushProcess.subscribe(bytes -> {
//            System.out.println("NonBlockingReactiveMultipleNotificationWatchServlet.service");
//        });
//
//        asyncContext.addListener(new AsyncListener() {
//
//            @Override
//            public void onComplete(AsyncEvent event) throws IOException {
//                log.info("complete non-blocking-reactive-multiple");
//
//
//            }
//
//            @Override
//            public void onTimeout(AsyncEvent event) throws IOException {
//                log.info("timeout non-blocking-reactive-multiple");
//            }
//
//            @Override
//            public void onError(AsyncEvent event) throws IOException {
//                log.info("error non-blocking-reactive-multiple");
//            }
//
//            @Override
//            public void onStartAsync(AsyncEvent event) throws IOException {
//                // ignore
//            }
//
//        });
//    Flux<ServletOutputStream> create(final ServletOutputStream out) {
//        return Flux.from(subscriber -> {
//            final AtomicBoolean dispose = new AtomicBoolean(false);
//
//            class ServletOutputStreamReadyWatchman implements Runnable {
//                @Override
//                public void run() {
//
//                    if (out.isReady()) {
//                        subscriber.onNext(out);
//                    }
//
//                    if (!dispose.get()) {
//                        Schedulers.single().schedule(this);
//                    }
//                }
//            }
//
//            out.setWriteListener(new WriteListener() {
//                @Override
//                public void onWritePossible() throws IOException {
//                    do {
//                        subscriber.onNext(out);
//                        Thread.yield();
//                    } while(!dispose.get() && out.isReady());
//                }
//                @Override
//                public void onError(Throwable t) {
//                    subscriber.onError(t);
//                }
//            });
//
//            subscriber.onSubscribe(new Subscription() {
//                @Override
//                public void request(long n) {
//                    // ignore
//                }
//                @Override
//                public void cancel() {
//                    dispose.set(true);
//                }
//            });
//        });
//    }

}
package io.github.arawn.servlet;

import io.github.arawn.notification.NonBlockingAsyncMultipleNotificationStreamObservable;
import io.github.arawn.notification.Notification;
import io.github.arawn.notification.NotificationStream;
import io.github.arawn.service.AwkwardChaosMonkey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet(urlPatterns = "/notification/stream/non-blocking-async-concurrency", asyncSupported = true)
public class NonBlockingAsyncObserverMultipleNotificationStreamServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(NonBlockingAsyncObserverMultipleNotificationStreamServlet.class);
    final static ScheduledExecutorService ioExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        final AtomicInteger threadCounter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "io-" + threadCounter.getAndIncrement());
        }
    });

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.disable();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/non-blocking-async-concurrency");

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");

        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.flush();

        AsyncContext asyncContext = request.startAsync(request, response);
        asyncContext.setTimeout(60 * 1000);
//        asyncContext.setTimeout((new Random().nextInt(5) + 5) * 1000);


        NotificationStream notificationStream = new NotificationStream(obtainUsername(request));
        NonBlockingAsyncMultipleNotificationStreamObservable streamObservable = new NonBlockingAsyncMultipleNotificationStreamObservable(notificationStream);

        NonBlockingAsyncMultipleNotificationStreamObserver streamObserver = new NonBlockingAsyncMultipleNotificationStreamObserver(streamObservable, asyncContext);
        streamObservable.addObserver(streamObserver);
        asyncContext.addListener(streamObserver);
        outputStream.setWriteListener(streamObserver);

        streamObservable.subscribe();

        log.info("out notification/stream/non-blocking-async-concurrency");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }


    class AsyncMultipleNotificationStreamObserver implements Observer, AsyncListener {

        final NonBlockingAsyncMultipleNotificationStreamObservable streamObservable;
        final AsyncContext asyncContext;
        final ServletOutputStream outputStream;

        public AsyncMultipleNotificationStreamObserver(NonBlockingAsyncMultipleNotificationStreamObservable streamObservable, AsyncContext asyncContext) throws IOException {
            this.streamObservable = Objects.requireNonNull(streamObservable);
            this.asyncContext = Objects.requireNonNull(asyncContext);
            this.outputStream = asyncContext.getResponse().getOutputStream();
        }

        @Override
        public void update(Observable observable, Object event) {
            if (event instanceof Notification) {
                handler((Notification) event);
            }

            if (event instanceof Throwable) {
                handlerError((Throwable) event);
            }
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            log.info("complete notification/stream/non-blocking-async-concurrency");
        }

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
            handlerError(new TimeoutException("timeout"));
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
            handlerError(event.getThrowable());
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
            // ignore
        }

        private void handler(Notification notification) {
            // log.info("revised notification/stream/non-blocking-async-concurrency : " + notification.getEvent());

            String content = "event: " + notification.getEvent() + "\n" +
                             "data: " + notification.getData() + "\n\n";

            try {
                outputStream.write(content.getBytes());
                outputStream.flush();
            } catch (IOException error) {
                throw new DataWriteException(error);
            }
        }

        private void handlerError(Throwable error) {
            log.error("error notification/stream/non-blocking-async-concurrency : " + error.getMessage());

            streamObservable.unsubscribe();
            asyncContext.complete();
        }

    }

    class NonBlockingAsyncMultipleNotificationStreamObserver implements Observer, AsyncListener, WriteListener {

        final NonBlockingAsyncMultipleNotificationStreamObservable streamObservable;
        final AsyncContext asyncContext;
        final ServletOutputStream outputStream;

        public NonBlockingAsyncMultipleNotificationStreamObserver(NonBlockingAsyncMultipleNotificationStreamObservable streamObservable, AsyncContext asyncContext) throws IOException {
            this.streamObservable = Objects.requireNonNull(streamObservable);
            this.asyncContext = Objects.requireNonNull(asyncContext);
            this.outputStream = asyncContext.getResponse().getOutputStream();
        }

        @Override
        public void update(Observable observable, Object event) {
            if (event instanceof Notification) {
                handler((Notification) event);
            }

            if (event instanceof Throwable) {
                handlerError((Throwable) event);
            }
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            log.info("complete notification/stream/non-blocking-async-concurrency");
        }

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
            handlerError(new TimeoutException("timeout"));
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
            handlerError(event.getThrowable());
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
            // ignore
        }

        @Override
        public void onWritePossible() throws IOException {

        }

        @Override
        public void onError(Throwable throwable) {
            handlerError(throwable);
        }

        private void handler(Notification notification) {
            // log.info("revised notification/stream/non-blocking-async-concurrency : " + notification.getEvent());

            String content = "event: " + notification.getEvent() + "\n" +
                             "data: " + notification.getData() + "\n\n";

            ioExecutor.schedule(() -> write(content.getBytes()), 10, TimeUnit.MILLISECONDS);
        }

        private void handlerError(Throwable error) {
            log.error("error notification/stream/non-blocking-async-concurrency : " + error.getMessage());

            streamObservable.unsubscribe();
            asyncContext.complete();
        }

        private void write(byte[] data) {
            if (outputStream.isReady()) {
                try {
                    outputStream.write(data);

                    ioExecutor.schedule(this::flush, 10, TimeUnit.MILLISECONDS);
                } catch (IOException error) {
                    handlerError(error);
                }
            } else {
                ioExecutor.schedule(() -> write(data), 10, TimeUnit.MILLISECONDS);
            }
        }

        private void flush() {
            if (outputStream.isReady()) {
                try {
                    outputStream.flush();
                } catch (IOException error) {
                    handlerError(error);
                }
            } else {
                ioExecutor.schedule(this::flush, 10, TimeUnit.MILLISECONDS);
            }
        }

    }

    class DataWriteException extends RuntimeException {
        DataWriteException(Throwable cause) {
            super(cause);
        }
    }

}

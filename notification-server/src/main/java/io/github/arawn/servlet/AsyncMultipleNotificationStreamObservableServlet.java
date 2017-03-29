package io.github.arawn.servlet;

import io.github.arawn.notification.AsyncMultipleNotificationStreamObservable;
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
import java.util.concurrent.TimeoutException;

@WebServlet(urlPatterns = "/notification/stream/async-concurrency", asyncSupported = true)
public class AsyncMultipleNotificationStreamObservableServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(AsyncMultipleNotificationStreamObservableServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.disable();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/async-concurrency");

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");
        response.getOutputStream().flush();


        AsyncContext asyncContext = request.startAsync(request, response);
        // asyncContext.setTimeout((new Random().nextInt(10) + 5) * 1000);

        NotificationStream notificationStream = new NotificationStream(obtainUsername(request));
        AsyncMultipleNotificationStreamObservable streamObservable = new AsyncMultipleNotificationStreamObservable(notificationStream);

        AsyncMultipleNotificationStreamObserver streamObserver = new AsyncMultipleNotificationStreamObserver(streamObservable, asyncContext);
        streamObservable.addObserver(streamObserver);
        asyncContext.addListener(streamObserver);

        streamObservable.subscribe();

        log.info("out notification/stream/async-concurrency");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }


    class AsyncMultipleNotificationStreamObserver implements Observer, AsyncListener {

        final AsyncMultipleNotificationStreamObservable streamObservable;
        final AsyncContext asyncContext;

        public AsyncMultipleNotificationStreamObserver(AsyncMultipleNotificationStreamObservable streamObservable, AsyncContext asyncContext) {
            this.streamObservable = Objects.requireNonNull(streamObservable);
            this.asyncContext = Objects.requireNonNull(asyncContext);
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
            log.info("complete notification/stream/async-concurrency");
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
            try {
                log.info("revised notification/stream/async-concurrency : " + notification.getEvent());

                ServletOutputStream outputStream = asyncContext.getResponse().getOutputStream();
                outputStream.write(("event: " + notification.getEvent() + "\n").getBytes());
                outputStream.write(("data: " + notification.getData() + "\n\n").getBytes());
                outputStream.flush();
            } catch (IOException error) {
                throw new DataWriteException(error);
            }
        }

        private void handlerError(Throwable error) {
            log.error("error notification/stream/async-concurrency : " + error.getMessage());

            streamObservable.unsubscribe();
            asyncContext.complete();
        }

    }

    class DataWriteException extends RuntimeException {
        DataWriteException(Throwable cause) {
            super(cause);
        }
    }

}

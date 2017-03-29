package io.github.arawn.servlet;

import io.github.arawn.notification.MultipleNotificationStreamObservable;
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
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

@WebServlet(urlPatterns = "/notification/stream/concurrency", asyncSupported = true)
public class MultipleNotificationStreamObservableServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(MultipleNotificationStreamObservableServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.disable();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/concurrency");

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");
        response.getOutputStream().flush();


        AsyncContext asyncContext = request.startAsync(request, response);
        asyncContext.setTimeout((new Random().nextInt(10) + 5) * 1000);

        NotificationStream notificationStream = new NotificationStream(obtainUsername(request));
        MultipleNotificationStreamObservable streamObservable = new MultipleNotificationStreamObservable(notificationStream);

        MultipleNotificationStreamObserver streamObserver = new MultipleNotificationStreamObserver(streamObservable, asyncContext);
        streamObservable.addObserver(streamObserver);
        asyncContext.addListener(streamObserver);

        streamObservable.subscribe(true);

        log.info("out notification/stream/concurrency");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }


    class MultipleNotificationStreamObserver implements Observer, AsyncListener {

        final MultipleNotificationStreamObservable streamObservable;
        final AsyncContext asyncContext;

        final ReentrantLock reentrantLock = new ReentrantLock();

        public MultipleNotificationStreamObserver(MultipleNotificationStreamObservable streamObservable, AsyncContext asyncContext) {
            this.streamObservable = Objects.requireNonNull(streamObservable);
            this.asyncContext = Objects.requireNonNull(asyncContext);
        }

        @Override
        public void update(Observable observable, Object event) {
//            synchronized (this) {
//                if (event instanceof Notification) {
//                    handler((Notification) event);
//                }
//
//                if (event instanceof Throwable) {
//                    handlerError((Throwable) event);
//                }
//            }
//
//
//            reentrantLock.lock();
//
//            if (event instanceof Notification) {
//                handler((Notification) event);
//            }
//
//            if (event instanceof Throwable) {
//                handlerError((Throwable) event);
//            }
//
//            reentrantLock.unlock();


            if (event instanceof Notification) {
                handler((Notification) event);
            }

            if (event instanceof Throwable) {
                handlerError((Throwable) event);
            }
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            log.info("complete notification/stream/concurrency");
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
                log.info("revised notification/stream/concurrency : " + notification.getEvent());

                ServletOutputStream outputStream = asyncContext.getResponse().getOutputStream();
                outputStream.write(("event: " + notification.getEvent() + "\n").getBytes());
                outputStream.write(("data: " + notification.getData() + "\n\n").getBytes());
                outputStream.flush();
            } catch (IOException error) {
                throw new DataWriteException(error);
            }
        }

        private void handlerError(Throwable error) {
            log.error("error notification/stream/concurrency : " + error.getMessage());

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

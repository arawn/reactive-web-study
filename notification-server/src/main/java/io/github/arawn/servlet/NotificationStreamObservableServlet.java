package io.github.arawn.servlet;

import io.github.arawn.notification.Notification;
import io.github.arawn.notification.NotificationStream;
import io.github.arawn.notification.NotificationStreamObservable;
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

@WebServlet(urlPatterns = "/notification/stream/observer", asyncSupported = true)
public class NotificationStreamObservableServlet extends HttpServlet {

    final static Log log = LogFactory.getLog(NotificationStreamObservableServlet.class);

    @Override
    public void init() throws ServletException {
        AwkwardChaosMonkey.enable(3, 10);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("in notification/stream/observer");

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/event-stream");
        response.getOutputStream().flush();


        AsyncContext asyncContext = request.startAsync(request, response);
        asyncContext.setTimeout((new Random().nextInt(5) + 5) * 1000);

        NotificationStream notificationStream = new NotificationStream(obtainUsername(request));
        NotificationStreamObservable streamObservable = new NotificationStreamObservable(notificationStream);

        NotificationStreamObserver streamObserver = new NotificationStreamObserver(asyncContext);
        streamObservable.addObserver(streamObserver);
        asyncContext.addListener(streamObserver);

        streamObservable.subscribe();

        log.info("out notification/stream/observer");
    }

    String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        return "anonymous";
    }


    class NotificationStreamObserver implements Observer, AsyncListener {

        final AsyncContext asyncContext;

        public NotificationStreamObserver(AsyncContext asyncContext) {
            this.asyncContext = Objects.requireNonNull(asyncContext);
        }

        @Override
        public void update(Observable observable, Object event) {
            if (event instanceof Notification) {
                handler((Notification) event);
            }

            // 옵저버 내부에서 예외가 발생했을 때 해당 예외를 보내겠다고 약속했다
            if (event instanceof Throwable) {
                handlerError(observable, (Throwable) event);
            }
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            log.info("complete notification/stream/observer");
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
                log.info("revised notification/stream/observer : " + notification.getEvent());


                ServletOutputStream outputStream = asyncContext.getResponse().getOutputStream();
                outputStream.write(("event: " + notification.getEvent() + "\n").getBytes());
                outputStream.write(("data: " + notification.getData() + "\n\n").getBytes());
                outputStream.flush();
            } catch (IOException error) {
                throw new DataWriteException(error);
            }
        }

        private void handlerError(Observable observable, Throwable error) {
            ((NotificationStreamObservable) observable).unsubscribe();

            handlerError(error);
        }

        private void handlerError(Throwable error) {
            log.error("error notification/stream/observer : " + error.getMessage());

            asyncContext.complete();
        }

    }

    class DataWriteException extends RuntimeException {
        DataWriteException(Throwable cause) {
            super(cause);
        }
    }

}

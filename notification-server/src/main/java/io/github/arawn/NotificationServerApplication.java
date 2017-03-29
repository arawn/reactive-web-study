package io.github.arawn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@ServletComponentScan
public class NotificationServerApplication {

    final static Log LOG = LogFactory.getLog(NotificationServerApplication.class);

    public static void main(String[] args) throws Exception {
        try {
            URL url = new URL("http://localhost:9000/user/guest/feed/count?processing=FAST");

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(30000);
            urlConnection.getContent();

            SpringApplication.run(NotificationServerApplication.class, args);
        } catch (IOException error) {
            LOG.error("data-service 를 먼저 실행시켜주세요.");
        }
    }

    @Bean
    public UndertowServletWebServerFactory undertowServletWebServerFactory() {
        UndertowServletWebServerFactory servletContainerFactory = new UndertowServletWebServerFactory();

        /*
        servletContainerFactory.addDeploymentInfoCustomizers(deploymentInfo -> {
            deploymentInfo.setAsyncExecutor(Executors.newFixedThreadPool(10, new ThreadFactory() {
                final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable runnable) {
                    return new Thread(runnable, "ASYNC task-" + counter.getAndIncrement());
                }
            }));
        });
        */

        return servletContainerFactory;
    }

}
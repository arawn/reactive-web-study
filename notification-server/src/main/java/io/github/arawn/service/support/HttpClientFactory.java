package io.github.arawn.service.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;

@WebListener
public class HttpClientFactory implements ServletContextListener {

    final static Log log = LogFactory.getLog(HttpClientFactory.class);

    static CloseableHttpClient httpClient;
    static CloseableHttpAsyncClient httpAsyncClient;

    public static CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public static CloseableHttpAsyncClient getHttpAsyncClient() {
        return httpAsyncClient;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("contextInitialized");

        httpClient = HttpClients.custom()
                                .setMaxConnTotal(10)
                                .setMaxConnPerRoute(20)
                                .build();

        httpAsyncClient = HttpAsyncClients.custom()
                                          .setMaxConnTotal(Integer.MAX_VALUE)
                                          .setMaxConnPerRoute(Integer.MAX_VALUE)
                                          .setDefaultIOReactorConfig(IOReactorConfig.custom()
                                                                                    .setIoThreadCount(1)
                                                                                    .build())
                                          .build();
        httpAsyncClient.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("contextDestroyed");

        try {
            httpClient.close();
        } catch (IOException error) {
            throw new HttpClientErrorException(error);
        }

        try {
            httpAsyncClient.close();
        } catch (IOException error) {
            throw new HttpClientErrorException(error);
        }
    }


    class HttpClientErrorException extends RuntimeException {

        public HttpClientErrorException(Throwable cause) {
            super(cause);
        }

    }

}

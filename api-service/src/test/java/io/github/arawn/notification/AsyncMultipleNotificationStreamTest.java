package io.github.arawn.notification;

import org.junit.Test;

import java.io.*;
import java.util.Observable;
import java.util.Observer;

import static org.junit.Assert.*;

/**
 * @author ykpark@woowahan.com
 */
public class AsyncMultipleNotificationStreamTest {

    @Test
    public void watch() throws Exception {
        Writer writer = new OutputStreamWriter(System.out);

        AsyncMultipleNotificationStream stream = new AsyncMultipleNotificationStream();
        stream.addObserver((observable, data) -> {
            try {
                Notification notification = (Notification) data;

                writer.write("event: " + data + "\n");
                writer.write("data: " + data + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        stream.watch();

        Thread.sleep(60000);
    }

}
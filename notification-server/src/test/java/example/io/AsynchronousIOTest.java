package example.io;

import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AsynchronousIOTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "async");
            }
        });


        StopWatch stopWatch = new StopWatch("Asynchronous I/O");
        stopWatch.start();

        List<CompletableFuture<String>> futures = IntStream.rangeClosed(1, 10)
                .mapToObj(number -> {
                    return getForString(asynchronousChannelGroup, new InetSocketAddress("localhost", 9000), "GET /user/guest/friend-recommendation?processing=FIX HTTP/1.1\n\n".getBytes());
                })
                .collect(Collectors.toList());

        futures.stream().map(CompletableFuture::join).forEach(System.out::println);

        stopWatch.stop();
        System.out.println();
        System.out.println(stopWatch.prettyPrint());

        asynchronousChannelGroup.shutdownNow();
    }


    static CompletableFuture<String> getForString(AsynchronousChannelGroup asynchronousChannelGroup, InetSocketAddress socketAddress, byte[] requestBytes) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        AsynchronousSocketChannel asynchronousSocketChannel;
        try {
            asynchronousSocketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        } catch (IOException error) {
            throw new RuntimeException(error);
        }

        asynchronousSocketChannel.connect(socketAddress, null, new CompletionHandler<Void, Void>() {

            @Override
            public void completed(Void v, Void attachment) {
                ByteBuffer writeBuffer = ByteBuffer.wrap(requestBytes);
                asynchronousSocketChannel.write(writeBuffer, null, new CompletionHandler<Integer, Void>() {

                    @Override
                    public void completed(Integer length, Void attachment) {
                        ByteBuffer readBuffer = ByteBuffer.allocate(4096);
                        asynchronousSocketChannel.read(readBuffer, null, new CompletionHandler<Integer, Void>() {

                            @Override
                            public void completed(Integer length, Void attachment) {
                                try {
                                    readBuffer.flip();

                                    CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
                                    CharBuffer charBuffer = decoder.decode(readBuffer);
                                    BufferedReader bufferedReader = new BufferedReader(new CharArrayReader(charBuffer.array()));

                                    StringBuilder response = new StringBuilder();
                                    String line = bufferedReader.readLine();
                                    while (Objects.nonNull(line)) {
                                        response.append(line).append("\n");
                                        line = bufferedReader.readLine();

                                        // body 메시지는 버린다
                                        if ("".equals(line)) {
                                            while (Objects.nonNull(line)) {
                                                line = bufferedReader.readLine();
                                            }
                                        }
                                    }

                                    completableFuture.complete(response.toString());
                                } catch (IOException error) {
                                    completableFuture.completeExceptionally(error);
                                }
                            }

                            @Override
                            public void failed(Throwable error, Void attachment) {
                                completableFuture.completeExceptionally(error);
                            }

                        });
                    }

                    @Override
                    public void failed(Throwable error, Void attachment) {
                        completableFuture.completeExceptionally(error);
                    }

                });

            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                completableFuture.completeExceptionally(exc);
            }

        });

        return completableFuture;
    }

}

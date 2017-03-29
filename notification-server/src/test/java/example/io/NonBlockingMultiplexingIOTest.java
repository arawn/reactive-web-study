package example.io;

import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

public class NonBlockingMultiplexingIOTest {

    public static void main(String[] args) throws Exception {
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

        ByteBuffer writeBuffer = ByteBuffer.allocateDirect(4096);
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(4096);


        StopWatch stopWatch = new StopWatch("Non-blocking I/O");
        stopWatch.start();


        Selector selector = Selector.open();

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        socketChannel.connect(new InetSocketAddress("www.naver.com", 80));
        socketChannel.register(selector, SelectionKey.OP_CONNECT);

        while (socketChannel.isOpen()) {
            if (selector.select() > 0) {
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    if (selectionKey.isConnectable()) {
                        SocketChannel channel = (SocketChannel) selectionKey.channel();
                        if (channel.isConnectionPending()) {
                            if (channel.finishConnect()) {
                                selectionKey.interestOps(SelectionKey.OP_WRITE);
                            } else {
                                selectionKey.cancel();
                            }
                        }
                    } else if (selectionKey.isWritable()) {
                        SocketChannel channel = (SocketChannel) selectionKey.channel();

                        writeBuffer.put("GET / HTTP/1.0".getBytes());
                        writeBuffer.put("\n\n".getBytes());
                        writeBuffer.flip();

                        channel.write(writeBuffer);

                        writeBuffer.compact();

                        selectionKey.interestOps(SelectionKey.OP_READ);
                    } else if (selectionKey.isReadable()) {
                        SocketChannel channel = (SocketChannel) selectionKey.channel();

                        channel.read(readBuffer);
                        readBuffer.flip();

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

                        channel.close();

                        System.out.println(response.toString());
                        System.out.println();
                    }

                    iterator.remove();
                }
            }
        }

        stopWatch.stop();
        System.out.println();
        System.out.println(stopWatch.prettyPrint());
    }

    public static void test(String[] args) throws Exception {
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

        ByteBuffer writeBuffer = ByteBuffer.allocateDirect(4096);
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(4096);


        StopWatch stopWatch = new StopWatch("Non-blocking I/O");
        stopWatch.start();

        Selector selector = Selector.open();
        List<SocketChannel> channels = new ArrayList<>();

        for (int number=1; number<=10; number++) {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            socketChannel.connect(new InetSocketAddress("localhost", 9000));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            channels.add(socketChannel);
        }

        boolean running = true;
        while (running) {
            if (selector.select() > 0) {
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    if (selectionKey.isConnectable()) {
                        SocketChannel channel = (SocketChannel) selectionKey.channel();
                        if (channel.isConnectionPending()) {
                            if (channel.finishConnect()) {
                                selectionKey.interestOps(SelectionKey.OP_WRITE);
                            } else {
                                selectionKey.cancel();
                            }
                        }
                    } else if (selectionKey.isWritable()) {
                        SocketChannel channel = (SocketChannel) selectionKey.channel();

                        writeBuffer.put("GET /user/guest/friend-recommendation?processing=FIX HTTP/1.0".getBytes());
                        writeBuffer.put("\n\n".getBytes());
                        writeBuffer.flip();

                        channel.write(writeBuffer);

                        writeBuffer.compact();

                        selectionKey.interestOps(SelectionKey.OP_READ);
                    } else if (selectionKey.isReadable()) {
                        SocketChannel channel = (SocketChannel) selectionKey.channel();

                        channel.read(readBuffer);
                        readBuffer.flip();

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

                        channel.close();
                        channels.remove(channel);

                        System.out.println(response.toString());
                        System.out.println();
                    }

                    iterator.remove();
                }
            }

            running = channels.size() > 0;
        }

        stopWatch.stop();
        System.out.println();
        System.out.println(stopWatch.prettyPrint());
    }

}

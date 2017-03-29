package example.io;

import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

public class NonBlockingIOTest {

    public static void main(String[] args) throws Exception {
        copy(args);

//        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
//
//        ByteBuffer writeBuffer = ByteBuffer.allocateDirect(4096);
//        ByteBuffer readBuffer = ByteBuffer.allocateDirect(4096);
//
//
//        StopWatch stopWatch = new StopWatch("Non-blocking I/O");
//        stopWatch.start();
//
//        List<SocketHandler> socketHandlers = new ArrayList<>();
//
//        for (int number=1; number<=1; number++) {
//            SocketChannel channel = SocketChannel.open();
//            channel.configureBlocking(false);
//
//            channel.connect(new InetSocketAddress("localhost", 9000));
//
//            socketHandlers.add(new SocketHandler(channel));
//        }
//
//        boolean running = true;
//        while (running) {
//            List<SocketHandler> closeSocketHandlers = new ArrayList<>();
//            for(SocketHandler socketHandler : socketHandlers) {
//                if (socketHandler.channel.isConnectionPending()) {
//                    if (!socketHandler.channel.finishConnect()) {
//                        throw new RuntimeException("connect error!");
//                    }
//
//                    System.out.println("접속 완료");
//                    socketHandler.next = Action.SEND;
//                }
//
//                // 접속 완료 상태니 데이터를 보내자
//                if (socketHandler.channel.isConnected()) {
//
//                    if (socketHandler.next == Action.SEND) {
//                        System.out.println("전송");
//
//                        writeBuffer.put("GET /user/guest/friend-recommendation?processing=FIX HTTP/1.0".getBytes());
//                        writeBuffer.put("\n\n".getBytes());
//                        writeBuffer.flip();
//
//                        if (socketHandler.channel.write(writeBuffer) > 0) {
//                            socketHandler.next = Action.RECV;
//                        }
//
//                        writeBuffer.compact();
//
//                    } else if (socketHandler.next == Action.RECV) {
//
//                        System.out.println("수신");
//
//                        if (socketHandler.channel.read(readBuffer) > 0) {
//                            readBuffer.flip();
//
//                            CharBuffer charBuffer = decoder.decode(readBuffer);
//                            BufferedReader bufferedReader = new BufferedReader(new CharArrayReader(charBuffer.array()));
//
//                            StringBuilder response = new StringBuilder();
//                            String line = bufferedReader.readLine();
//                            while (Objects.nonNull(line)) {
//                                response.append(line).append("\n");
//                                line = bufferedReader.readLine();
//
//                                // body 메시지는 버린다
//                                if ("".equals(line)) {
//                                    while (Objects.nonNull(line)) {
//                                        line = bufferedReader.readLine();
//                                    }
//                                }
//                            }
//
//                            readBuffer.compact();
//
//                            socketHandler.channel.close();
//                            closeSocketHandlers.add(socketHandler);
//
//                            System.out.println(response.toString());
//                            System.out.println();
//                        }
//                    }
//                }
//            }
//
//            socketHandlers.removeAll(closeSocketHandlers);
//            running = socketHandlers.size() > 0;
//
//            Thread.sleep(10);
//        }
//
//        stopWatch.stop();
//        System.out.println();
//        System.out.println(stopWatch.prettyPrint());
    }


    static class SocketHandler {

        SocketChannel channel;
        Action next;

        SocketHandler(SocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SocketHandler that = (SocketHandler) o;

            return channel.equals(that.channel);

        }

        @Override
        public int hashCode() {
            return channel.hashCode();
        }

    }

    enum Action {
        SEND, RECV, CLOSE;
    }



    public static void copy(String[] args) throws Exception {
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

        ByteBuffer writeBuffer = ByteBuffer.allocateDirect(4096);
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(4096);

        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress("www.naver.com", 80));

        SocketHandler socketHandler = new SocketHandler(channel);

        while (socketHandler.channel.isOpen()) {
            if (socketHandler.channel.isConnectionPending()) {
                if (!socketHandler.channel.finishConnect()) {
                    throw new RuntimeException("접속 실패!");
                }
                socketHandler.next = Action.SEND;
            }

            if (socketHandler.next == Action.SEND) {
                writeBuffer.put("GET / HTTP/1.0".getBytes());
                writeBuffer.put("\n\n".getBytes());
                writeBuffer.flip();

                if (socketHandler.channel.write(writeBuffer) > 0) {
                    socketHandler.next = Action.RECV;
                }
                // WindowsSelectorImpl
                // PollSelectorImpl
                // KQueueSelectorImpl
                writeBuffer.compact();
            } else if (socketHandler.next == Action.RECV) {
                if (socketHandler.channel.read(readBuffer) > 0) {
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

                    readBuffer.compact();

                    socketHandler.next = Action.CLOSE;

                    System.out.println(response.toString());
                    System.out.println();
                }
            } else if (socketHandler.next == Action.CLOSE) {
                socketHandler.channel.close();
                socketHandler.next = null;
            }

            Thread.sleep(10);
        }
    }

}

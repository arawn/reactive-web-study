package example.io;

import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public class BlockingIOTest {

    public static void main(String[] args) throws Exception {
        StopWatch stopWatch = new StopWatch("Blocking I/O");
        stopWatch.start();

        newVersion();

        stopWatch.stop();
        System.out.println();
        System.out.println(stopWatch.prettyPrint());
    }

    static void oldVersion() throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("localhost", 9000));

        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintStream output = new PrintStream(socket.getOutputStream());

        output.println("GET /user/guest/friend-recommendation?processing=FIX HTTP/1.0");
        output.println();

        StringBuilder response = new StringBuilder();
        String line = input.readLine();
        while (Objects.nonNull(line)) {
            response.append(line).append("\n");
            line = input.readLine();

            // body 메시지는 버린다
            if ("".equals(line)) {
                while (Objects.nonNull(line)) {
                    line = input.readLine();
                }
            }
        }

        input.close();
        output.close();

        System.out.println(response.toString());
        System.out.println();
    }

    static void newVersion() throws Exception {

        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(true);

        Socket socket = channel.socket();
        socket.connect(new InetSocketAddress("www.naver.com", 80));

        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintStream output = new PrintStream(socket.getOutputStream());

        output.println("GET / HTTP/1.0");
        output.println();

        StringBuilder response = new StringBuilder();
        String line = input.readLine();
        while (Objects.nonNull(line)) {
            response.append(line).append("\n");
            line = input.readLine();

            // body 메시지는 버린다
            if ("".equals(line)) {
                while (Objects.nonNull(line)) {
                    line = input.readLine();
                }
            }
        }

        input.close();
        output.close();

        System.out.println(response.toString());
        System.out.println();

    }

}

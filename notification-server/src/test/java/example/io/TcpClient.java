package example.io;

/**
 * @author ykpark@woowahan.com
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * A simple NIO TCP client
 * Assumptions:
 * - the client should always be connected,
 *   once it gets disconnected it reconnects
 * - the exception thrown by onRead means protocol error
 *   so client disconnects and reconnects
 * - the incoming flow is higher than outgoing, so
 *   direct channel write method is not implemented
 *
 * @author Vladimir Lysyy (mail@bobah.net)
 *
 */
public abstract class TcpClient implements Runnable {
    protected static final Logger LOG = Logger.getLogger(TcpClient.class);
    private static final long INITIAL_RECONNECT_INTERVAL = 500; // 500 ms.
    private static final long MAXIMUM_RECONNECT_INTERVAL = 30000; // 30 sec.
    private static final int READ_BUFFER_SIZE = 0x100000;
    private static final int WRITE_BUFFER_SIZE = 0x100000;

    private long reconnectInterval = INITIAL_RECONNECT_INTERVAL;

    private ByteBuffer readBuf = ByteBuffer.allocateDirect(READ_BUFFER_SIZE); // 1Mb
    private ByteBuffer writeBuf = ByteBuffer.allocateDirect(WRITE_BUFFER_SIZE); // 1Mb

    private final Thread thread = new Thread(this);
    private SocketAddress address;

    private Selector selector;
    private SocketChannel channel;

    private final AtomicBoolean connected = new AtomicBoolean(false);

    private AtomicLong bytesOut = new AtomicLong(0L);
    private AtomicLong bytesIn = new AtomicLong(0L);

    public TcpClient() {

    }

    @PostConstruct
    public void init() {
        assert address != null: "server address missing";
    }

    public void start() throws IOException {
        LOG.info("starting event loop");
        thread.start();
    }

    public void join() throws InterruptedException {
        if (Thread.currentThread().getId() != thread.getId()) thread.join();
    }

    public void stop() throws IOException, InterruptedException {
        LOG.info("stopping event loop");
        thread.interrupt();
        selector.wakeup();
    }

    public boolean isConnected() {
        return connected.get();
    }

    /**
     * @param buffer data to send, the buffer should be flipped (ready for read)
     * @throws InterruptedException
     * @throws IOException
     */
    public void send(ByteBuffer buffer) throws InterruptedException, IOException {
        if (!connected.get()) throw new IOException("not connected");
        synchronized (writeBuf) {
            // try direct write of what's in the buffer to free up space
            if (writeBuf.remaining() < buffer.remaining()) {
                writeBuf.flip();
                int bytesOp = 0, bytesTotal = 0;
                while (writeBuf.hasRemaining() && (bytesOp = channel.write(writeBuf)) > 0) bytesTotal += bytesOp;
                writeBuf.compact();
            }

            // if didn't help, wait till some space appears
            if (Thread.currentThread().getId() != thread.getId()) {
                while (writeBuf.remaining() < buffer.remaining()) writeBuf.wait();
            }
            else {
                if (writeBuf.remaining() < buffer.remaining()) throw new IOException("send buffer full"); // TODO: add reallocation or buffers chain
            }
            writeBuf.put(buffer);

            // try direct write to decrease the latency
            writeBuf.flip();
            int bytesOp = 0, bytesTotal = 0;
            while (writeBuf.hasRemaining() && (bytesOp = channel.write(writeBuf)) > 0) bytesTotal += bytesOp;
            writeBuf.compact();

            if (writeBuf.hasRemaining()) {
                SelectionKey key = channel.keyFor(selector);
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                selector.wakeup();
            }
        }
    }

    protected abstract void onRead(ByteBuffer buf) throws Exception;

    protected abstract void onConnected() throws Exception;

    protected abstract void onDisconnected();

    private void configureChannel(SocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        channel.socket().setSendBufferSize(0x100000); // 1Mb
        channel.socket().setReceiveBufferSize(0x100000); // 1Mb
        channel.socket().setKeepAlive(true);
        channel.socket().setReuseAddress(true);
        channel.socket().setSoLinger(false, 0);
        channel.socket().setSoTimeout(0);
        channel.socket().setTcpNoDelay(true);
    }

    @Override
    public void run() {
        LOG.info("event loop running");
        try {
            while(! Thread.interrupted()) { // reconnection loop
                try {
                    selector = Selector.open();
                    channel = SocketChannel.open();
                    configureChannel(channel);

                    channel.connect(address);
                    channel.register(selector, SelectionKey.OP_CONNECT);

                    while(!thread.isInterrupted() && channel.isOpen()) { // events multiplexing loop
                        if (selector.select() > 0) processSelectedKeys(selector.selectedKeys());
                    }
                } catch (Exception e) {
                    LOG.error("exception", e);
                } finally {
                    connected.set(false);
                    onDisconnected();
                    writeBuf.clear();
                    readBuf.clear();
                    if (channel != null) channel.close();
                    if (selector != null) selector.close();
                    LOG.info("connection closed");
                }

                try {
                    Thread.sleep(reconnectInterval);
                    if (reconnectInterval < MAXIMUM_RECONNECT_INTERVAL) reconnectInterval *= 2;
                    LOG.info("reconnecting to " + address);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error("unrecoverable error", e);
        }

        LOG.info("event loop terminated");
    }

    private void processSelectedKeys(Set keys) throws Exception {
        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            SelectionKey key = (SelectionKey) itr.next();
            if (key.isReadable()) processRead(key);
            if (key.isWritable()) processWrite(key);
            if (key.isConnectable()) processConnect(key);
            if (key.isAcceptable()) ;
            itr.remove();
        }
    }

    private void processConnect(SelectionKey key) throws Exception {
        SocketChannel ch = (SocketChannel) key.channel();
        if (ch.finishConnect()) {
            LOG.info("connected to " + address);
            key.interestOps(key.interestOps() ^ SelectionKey.OP_CONNECT);
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            reconnectInterval = INITIAL_RECONNECT_INTERVAL;
            connected.set(true);
            onConnected();
        }
    }

    private void processRead(SelectionKey key) throws Exception {
        ReadableByteChannel ch = (ReadableByteChannel)key.channel();

        int bytesOp = 0, bytesTotal = 0;
        while (readBuf.hasRemaining() && (bytesOp = ch.read(readBuf)) > 0) bytesTotal += bytesOp;

        if (bytesTotal > 0) {
            readBuf.flip();
            onRead(readBuf);
            readBuf.compact();
        }
        else if (bytesOp == -1) {
            LOG.info("peer closed read channel");
            ch.close();
        }

        bytesIn.addAndGet(bytesTotal);
    }

    private void processWrite(SelectionKey key) throws IOException {
        WritableByteChannel ch = (WritableByteChannel)key.channel();
        synchronized (writeBuf) {
            writeBuf.flip();

            int bytesOp = 0, bytesTotal = 0;
            while (writeBuf.hasRemaining() && (bytesOp = ch.write(writeBuf)) > 0) bytesTotal += bytesOp;

            bytesOut.addAndGet(bytesTotal);

            if (writeBuf.remaining() == 0) {
                key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
            }

            if (bytesTotal > 0) writeBuf.notify();
            else if (bytesOp == -1) {
                LOG.info("peer closed write channel");
                ch.close();
            }

            writeBuf.compact();
        }
    }

    public SocketAddress getAddress() {
        return address;
    }

    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    public long getBytesOut() {
        return bytesOut.get();
    }

    public long getBytesIn() {
        return bytesIn.get();
    }


    /**
     * can be used for testing
     */
    public static void main(String[] args) throws Exception {
        // BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d{yyyyMMdd-HH:mm:ss} %-10t %-5p %-20C{1} - %m%n")));

        Logger.getRootLogger().setLevel(Level.INFO);
        final TcpClient client = new TcpClient() {
            @Override protected void onRead(ByteBuffer buf) throws Exception { buf.position(buf.limit()); }
            @Override protected void onDisconnected() { }
            @Override protected void onConnected() throws Exception { }
        };

        client.setAddress(new InetSocketAddress("127.0.0.1", 20001));
        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                LOG.info("out bytes: " + client.bytesOut.get());
                LOG.info("in bytes:  " + client.bytesIn.get());
            }
        }, 5000, 5000);

        while(!client.isConnected()) Thread.sleep(500);

        LOG.info("starting server flood");
        ByteBuffer buf = ByteBuffer.allocate(65535);
        Random rnd = new Random();
        while (true) {
            short len = (short) rnd.nextInt(Short.MAX_VALUE - 2);
            byte[] bytes = new byte[len];
            rnd.nextBytes(bytes);
            buf.putShort((short)len);
            buf.put(bytes);
            buf.flip();
            try {
                client.send(buf);
            } catch (Exception e) {
                LOG.error("exception: " + e.getMessage());
                while (!client.isConnected()) Thread.sleep(1000);
            }
            buf.clear();
            Thread.sleep(10);
        }
    }

}
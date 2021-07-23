package com.stan.netty.reactor.single.pool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: stan
 * @Date: 2021/07/22
 * @Description: Handler + 业务线程池
 */
public class HandlerPooled implements Runnable {

    private static final int PROCESSING = 3;
    private static final int READING = 0;
    private static final int SENDING = 1;
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private final SocketChannel socketChannel;
    final SelectionKey sk;
    int state = READING;


    public HandlerPooled(Selector selector, SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        sk = this.socketChannel.register(selector, SelectionKey.OP_READ);
        sk.attach(this);
        sk.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            if (state == READING) {
                read();
            } else if (state == SENDING) {
                write();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void read() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socketChannel.read(buffer);
        buffer.flip();
        if (readComplete(buffer)) {
            POOL.execute(new Processor(buffer));
            state = SENDING;
            sk.interestOps(SelectionKey.OP_WRITE);
        }

    }


    private void write() throws IOException {
        byte[] msg = "hello server msg".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        socketChannel.write(buffer);
        if (writeComplete(buffer)) {
            sk.cancel();
        }
    }


    private boolean readComplete(ByteBuffer buffer) {
        try {
            return !(socketChannel.read(buffer) > 0 && buffer.position() >= buffer.capacity());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    private boolean writeComplete(ByteBuffer buffer) {
        try {
            return !(socketChannel.write(buffer) < 1 && buffer.hasRemaining());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    private class Processor implements Runnable {

        private final ByteBuffer byteBuffer;

        public Processor(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }

        @Override
        public void run() {
            process(byteBuffer);
        }

        private synchronized void process(ByteBuffer buffer) {
            byte[] dst = new byte[buffer.remaining()];
            buffer.get(dst);
            System.out.printf("服务端收到信息: %s \n", new String(dst, StandardCharsets.UTF_8));
        }

    }
}

package com.stan.netty.reactor.multi;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * @Author: stan
 * @Date: 2021/07/24
 * @Description: 业务Handler
 */
public class Handler implements Runnable {

    private final SocketChannel channel;
    private final SubReactorThread subReactorThread;
    private ByteBuffer buffer;

    public Handler(SocketChannel channel, ByteBuffer buffer, SubReactorThread subReactorThread) {
        this.channel = channel;
        this.buffer = buffer;
        this.subReactorThread = subReactorThread;
    }

    @Override
    public void run() {
        buffer.flip();
        System.out.println("业务Handler解码中...");
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        System.out.printf("业务Handler %s 处理收到的消息: %s \n", Thread.currentThread().getName(),
                new String(bytes, StandardCharsets.UTF_8));
        buffer = null;

        String msg = "A message from " + Thread.currentThread().getName();
        ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
        System.out.println("业务Handler编码中...");
        subReactorThread.register(new NioTask(channel, SelectionKey.OP_WRITE, byteBuffer));
    }
}

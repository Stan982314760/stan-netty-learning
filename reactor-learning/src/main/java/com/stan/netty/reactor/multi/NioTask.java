package com.stan.netty.reactor.multi;

import lombok.Data;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @Author: stan
 * @Date: 2021/07/24
 * @Description: NioTask read/write
 */
@Data
public class NioTask {

    private final SocketChannel socketChannel;
    private final int op;
    private final ByteBuffer byteBuffer;

    public NioTask(SocketChannel socketChannel, int op) {
        this(socketChannel, op, null);
    }

    public NioTask(SocketChannel socketChannel, int op, ByteBuffer byteBuffer) {
        this.socketChannel = socketChannel;
        this.op = op;
        this.byteBuffer = byteBuffer;
    }


}

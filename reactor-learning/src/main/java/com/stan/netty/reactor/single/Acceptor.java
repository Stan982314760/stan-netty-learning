package com.stan.netty.reactor.single;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @Author: stan
 * @Date: 2021/07/21
 * @Description: Acceptor
 */
public class Acceptor implements Runnable {

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;


    public Acceptor(ServerSocketChannel serverSocketChannel, Selector selector) {
        this.serverSocketChannel = serverSocketChannel;
        this.selector = selector;
    }


    @Override
    public void run() {
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            if (socketChannel != null) {
                new Handler(selector, socketChannel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

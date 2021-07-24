package com.stan.netty.reactor.multi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: stan
 * @Date: 2021/07/23
 * @Description: MainReactor
 */
public class MainReactor implements Runnable {

    private static final int SUB_REACTOR_NUM = 3;
    private static final AtomicInteger poolCounter = new AtomicInteger(1);
    private static final ExecutorService MAIN_POOL = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r);
        thread.setName("mainReactor-" + poolCounter.getAndIncrement());
        return thread;
    });
    private final SubReactorGroup subReactorGroup = new SubReactorGroup(SUB_REACTOR_NUM);
    private final Selector selector;


    public MainReactor(int port) throws IOException {
        ServerSocketChannel socketChannel = ServerSocketChannel.open();
        socketChannel.bind(new InetSocketAddress(port));
        socketChannel.configureBlocking(false);
        this.selector = Selector.open();
        SelectionKey sk = socketChannel.register(selector, SelectionKey.OP_ACCEPT);
        sk.attach(new Acceptor(sk));
    }


    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                selector.select(1000);
                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    SelectionKey selectionKey = it.next();
                    it.remove();
                    dispatch(selectionKey);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatch(SelectionKey selectionKey) {
        Object attachment = selectionKey.attachment();
        if (attachment instanceof Runnable) {
            MAIN_POOL.execute((Runnable) attachment);
        }
    }


    private class Acceptor implements Runnable {
        final SelectionKey sk;

        Acceptor(SelectionKey sk) {
            this.sk = sk;
        }

        @Override
        public void run() {
            try {
                ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
                SocketChannel socketChannel = ssc.accept();
                if (socketChannel != null) {
                    socketChannel.configureBlocking(false);
                    subReactorGroup.dispatch(socketChannel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}

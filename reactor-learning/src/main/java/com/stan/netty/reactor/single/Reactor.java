package com.stan.netty.reactor.single;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author: stan
 * @Date: 2021/07/22
 * @Description: Reactor
 */
public class Reactor implements Runnable {

    private final Selector selector;


    public Reactor(int port) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress(port));
        this.selector = Selector.open();
        SelectionKey sk = ssc.register(this.selector, SelectionKey.OP_ACCEPT);
        sk.attach(new Acceptor(ssc, selector));
    }


    @Override
    public void run() {
       try {
            while (!Thread.interrupted()) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (Iterator<SelectionKey> it = selectionKeys.iterator(); it.hasNext(); ) {
                    SelectionKey key = it.next();
                    it.remove();
                    dispatch(key); // dispatch
                }
            }
       } catch (IOException e) {
            e.printStackTrace();
       }
    }

    private void dispatch(SelectionKey key) {
        Object attachment = key.attachment();
        if (attachment instanceof Runnable) {
            Runnable task = (Runnable) attachment;
            task.run();
        }
    }
}

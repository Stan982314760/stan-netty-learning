package com.stan.netty.reactor.multi;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: stan
 * @Date: 2021/07/23
 * @Description: SubReactorGroup
 */
public class SubReactorGroup {

    private static final AtomicInteger poolCounter = new AtomicInteger(1);
    private static final ExecutorService BIZ_POOL = Executors.newFixedThreadPool(5, r -> {
        Thread thread = new Thread(r);
        thread.setName("bizPool-" + poolCounter.getAndIncrement());
        return thread;
    });
    private static AtomicInteger counter = new AtomicInteger(0);
    private final int num;
    private final SubReactorThread[] subReactors;

    public SubReactorGroup(int num) throws IOException {
        this.num = num;
        subReactors = new SubReactorThread[num];
        for (int i = 0; i < num; i++) {
            SubReactorThread subReactorThread = new SubReactorThread(BIZ_POOL);
            subReactors[i] = subReactorThread;
            subReactorThread.start();
        }
    }

    private SubReactorThread next() {
        return subReactors[counter.getAndIncrement() % num];
    }


    public void dispatch(SocketChannel socketChannel) {
        next().register(new NioTask(socketChannel, SelectionKey.OP_READ));
    }


}

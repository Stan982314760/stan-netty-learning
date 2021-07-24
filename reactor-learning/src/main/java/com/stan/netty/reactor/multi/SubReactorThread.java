package com.stan.netty.reactor.multi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: stan
 * @Date: 2021/07/23
 * @Description: SubReactor
 */
public class SubReactorThread extends Thread {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private final Lock mainLock;
    private final Selector selector;
    private final List<NioTask> taskList;
    private final ExecutorService bizPool;


    public SubReactorThread(ExecutorService bizPool) throws IOException {
        super("subReactorThread-" + COUNTER.getAndIncrement());
        this.selector = Selector.open();
        this.taskList = new LinkedList<>();
        this.mainLock = new ReentrantLock();
        this.bizPool = bizPool;
    }


    @SuppressWarnings("all")
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                selector.select(1000);
                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    SelectionKey sk = it.next();
                    it.remove();
                    if (sk.isReadable()) {
                        System.out.printf("%s 开始读取客户端消息...\n", Thread.currentThread().getName());
                        SocketChannel channel = (SocketChannel) sk.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int read = channel.read(buffer);
                        // TODO 没读完怎么办？？
                        if (read > 0) {
                            dispatch(channel, buffer);
                        }
                    } else if (sk.isWritable()) {
                        SocketChannel channel = (SocketChannel) sk.channel();
                        Object attachment = sk.attachment();
                        if (attachment instanceof ByteBuffer) {
                            System.out.printf("%s 开始向客户端发送消息...\n", Thread.currentThread().getName());
                            ByteBuffer buffer = (ByteBuffer) attachment;
                            channel.write(buffer);
                            channel.register(selector, SelectionKey.OP_READ);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            if (!taskList.isEmpty()) {
                mainLock.lock();
                try {
                    for (Iterator<NioTask> it = taskList.iterator(); it.hasNext(); ) {
                        NioTask nioTask = it.next();
                        it.remove();
                        SocketChannel socketChannel = nioTask.getSocketChannel();
                        if (nioTask.getByteBuffer() == null) {
                            socketChannel.register(selector, nioTask.getOp());
                        } else {
                            ByteBuffer byteBuffer = nioTask.getByteBuffer();
                            byteBuffer.flip();
                            System.out.printf("%s 开始向客户端发送消息...\n", Thread.currentThread().getName());
                            int write = socketChannel.write(byteBuffer);
                            if (write < 1 && byteBuffer.hasRemaining()) {
                                socketChannel.register(selector, nioTask.getOp(), byteBuffer);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mainLock.unlock();
                }
            }

        }
    }


    private void dispatch(SocketChannel channel, ByteBuffer buffer) {
        bizPool.execute(new Handler(channel, buffer, this));
    }


    public void register(NioTask nioTask) {
        if (nioTask != null) {
            mainLock.lock();
            try {
                taskList.add(nioTask);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mainLock.unlock();
            }
        }
    }
}

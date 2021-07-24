package com.stan.netty.reactor.multi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
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
    private static final int DEFAULT_BUFFER_SIZE = 1024;
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
                        read(sk);
                    } else if (sk.isWritable()) {
                        write(sk);
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
                        if (nioTask.getOp() == SelectionKey.OP_READ) {
                            socketChannel.register(selector, SelectionKey.OP_READ);
                        } else if (nioTask.getOp() == SelectionKey.OP_WRITE) {
                            socketChannel.register(selector, SelectionKey.OP_WRITE, nioTask.getByteBuffer());
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

    private void read(SelectionKey sk) throws IOException {
        System.out.printf("%s 开始读取客户端消息...\n", Thread.currentThread().getName());
        SocketChannel channel = (SocketChannel) sk.channel();
        final ByteBuffer temp = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        if (channel.read(temp) > 0) {
            if (temp.position() < temp.capacity()) {
                dispatch(channel, temp);
            } else {
                final List<byte[]> dstList = new ArrayList<>();
                for (; ; ) {
                    temp.flip();
                    byte[] dst = new byte[temp.remaining()];
                    temp.get(dst);
                    temp.clear();
                    dstList.add(dst);

                    int read = channel.read(temp);
                    if(read < 1)
                        break;
                }

                if (dstList.size() > 0) {
                    int capacity = dstList.stream().mapToInt(arr -> arr.length).sum();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(capacity);
                    for (byte[] bytes : dstList) {
                        byteBuffer.put(bytes);
                    }

                    dispatch(channel, byteBuffer);
                }
            }
        }
    }

    private void write(SelectionKey sk) throws IOException {
        SocketChannel channel = (SocketChannel) sk.channel();
        Object attachment = sk.attachment();
        if (attachment instanceof ByteBuffer) {
            System.out.printf("%s 开始向客户端发送消息...\n", Thread.currentThread().getName());
            ByteBuffer buffer = (ByteBuffer) attachment;
            int write = channel.write(buffer);
            if (write < 1 && buffer.hasRemaining()) { // write not finished
                channel.register(selector, SelectionKey.OP_WRITE);
            } else {
                sk.attach(null);
                channel.register(selector, SelectionKey.OP_READ);
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

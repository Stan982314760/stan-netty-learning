package com.stan.netty.reactor.single;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author: stan
 * @Date: 2021/07/22
 * @Description: Client
 */
public class Client implements Runnable {

    private final Selector selector;


    public Client(String ip, int port) throws IOException {
        SocketChannel clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(false);
        selector = Selector.open();
        SelectionKey sk = clientChannel.register(selector, SelectionKey.OP_CONNECT);
        clientChannel.connect(new InetSocketAddress(ip, port));
        sk.attach(this);
    }


    public static void main(String[] args) throws IOException {

      new Thread(new Client("127.0.0.1", 9527), "client").start();

    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (Iterator<SelectionKey> it = selectionKeys.iterator(); it.hasNext(); ) {
                    SelectionKey sk = it.next();
                    it.remove();
                    dispatch(sk);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dispatch(SelectionKey sk) {
        SocketChannel channel = (SocketChannel) sk.channel();
        try {
            if (sk.isConnectable()) {
                if (channel.isConnectionPending()) {
                    channel.finishConnect();
                    System.out.println("客户端成功连接到了服务端");
                    sk.interestOps(SelectionKey.OP_WRITE);
                }
            } else if (sk.isWritable()) {
                byte[] msg = "hello client".getBytes(StandardCharsets.UTF_8);
                ByteBuffer buffer = ByteBuffer.wrap(msg);
                channel.write(buffer);
                sk.interestOps(SelectionKey.OP_READ);
            } else if (sk.isReadable()) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int read = channel.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    byte[] dst = new byte[buffer.remaining()];
                    buffer.get(dst);
                    System.out.printf("客户端收到信息: %s \n", new String(dst, StandardCharsets.UTF_8));
                    sk.cancel();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

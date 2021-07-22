package com.stan.netty.reactor.single;

import java.io.IOException;

/**
 * @Author: stan
 * @Date: 2021/07/22
 * @Description: Server
 */
public class Server {
    public static void main(String[] args) throws IOException {

       new Thread(new Reactor(9527),"reactor").start();

    }
}

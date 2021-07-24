package com.stan.netty.reactor.multi;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: stan
 * @Date: 2021/07/23
 * @Description: Server
 */
public class Server {

    private static final ExecutorService MAIN_REACTOR_POOL = Executors.newFixedThreadPool(3);


    public static void main(String[] args) throws IOException {
        new Thread(new MainReactor(9527)).start();
    }

}

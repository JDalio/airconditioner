package edu.bupt.airconditioner;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class Test {

    public static void testChannelMode(boolean isBloking) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(isBloking);
        ssc.bind(new InetSocketAddress(80));
        SocketChannel sc = null;

        if (!isBloking) {
            while ((sc = ssc.accept()) == null) { // Will not be blocked here
                // if no connection, return null immediately
                Thread.sleep(1000);
                System.out.println("Try to accept again");
            }
        } else {
            System.out.println("Waiting to connect...");
            sc = ssc.accept(); // If there is no connection, blocking here
        }


        System.out.println("Accept connection from:" + sc.getRemoteAddress());

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (sc.read(buffer) != -1) {
            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            System.out.println(new String(bytes));
            buffer.clear();
        }
        sc.close();
        ssc.close();
    }

    public static void main(String[] args) throws Exception {
        String a ="a a b c d";
        String[] arr =a.split(" ");
        for(String s : arr){
            System.out.println(s);
        }
    }
}
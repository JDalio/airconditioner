package edu.bupt.airconditioner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


public class Server {
    private Selector selector;
    private ServerSocketChannel ssc;
    private SocketChannel sc;

    private volatile boolean stop;

    // message processor
    private Message msg;

    // test configuration
    private String testAddr="localhost";
    private int testPort = 9000;

    private String kg="123456";

    public Server(int port) throws Exception {
        selector = Selector.open();
        sc = SocketChannel.open();
        sc.configureBlocking(false);

        ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.socket().bind(new InetSocketAddress(port), 1024);
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        msg=new Message(kg,"s");
        System.out.println("TIME SERVER IS LISTENING!!!");
    }

    public void handleInput(SelectionKey key) throws Exception {
        if (key.isValid()) {

            // test connection establish
            if(key.isConnectable()){
                SocketChannel sc = (SocketChannel)key.channel();
                if(sc.finishConnect()) {
                    sc.register(selector,SelectionKey.OP_READ);
                    msg.request(key);
                } else {
                    throw new RuntimeException("Connect Fail");
                }
            }

            // client connection establish
            if (key.isAcceptable()) {
                //通过ServerSocketChannel的accept()操作接收客户端的请求并创立SocketChannel连接，相当于完成TCP三次握手操作
                ServerSocketChannel schannel = (ServerSocketChannel) key.channel();
                SocketChannel accept = schannel.accept();
                accept.configureBlocking(false);
                accept.register(selector, SelectionKey.OP_READ);
            }

            //receive message from test or clients
            if (key.isReadable()) {
                msg.read(key);

                if(msg.get("e")==0) {
                    System.out.println("Connect to test");
                    msg.put("i",1);
                    msg.response(key);
                }










            }

        }
    }

    public void stop(){
        this.stop=true;
    }

    public void run() throws Exception {
        sc.connect(new InetSocketAddress(testAddr,testPort));
        sc.register(selector,SelectionKey.OP_CONNECT);

        while (!stop) {
            //selector每一秒被唤醒一次
            selector.select(1000);
            //还回就绪状态的chanel的selectedKeys
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            SelectionKey key = null;
            while (iterator.hasNext()) {
                key = iterator.next();
                iterator.remove();
                try {
                    handleInput(key);
                } catch (Exception e) {
                    if (key != null) {
                        key.cancel();
                    }
                }
            }
        }
        if (selector != null) {
            selector.close();
        }
    }

    public static void main(String[] args) throws Exception{
        Server server=new Server(8080);
        server.run();
    }
}
package edu.bupt.airconditioner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

public class Client {
    // server port and host
    private int port;
    private String host;

    private Selector selector;
    private SocketChannel channel;
    private SocketChannel testsc;
    private volatile boolean stop;

    // message processor
    private Message msg;

    // test configuration
    private String testAddr="localhost";
    private int testPort = 9000;

    private int roomNum=1;
    private String kg="123456";

    //home configuration
    private int it;
    private int tt;
    private int w;
    private int tc;
    private double ts;
    // state monitor
    private TimerTask monitor;

    public Client(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
        selector = Selector.open();
        channel = SocketChannel.open();
        channel.configureBlocking(false);
        testsc=SocketChannel.open();
        testsc.configureBlocking(false);
        msg=new Message(kg,""+roomNum);
    }



    public void handleInput(SelectionKey key) throws Exception {
        if (key.isValid()) {
            SocketChannel sc = (SocketChannel) key.channel();

            // connect to test
            if (key.isConnectable()) {

                if (sc.finishConnect()) {
                    sc.register(selector, SelectionKey.OP_READ);
                    msg.request(key);
                } else {
                    throw new RuntimeException("Connect Fail");
                }
            }

            // receive data from server or test
            if (key.isReadable()) {
                msg.read(key);

                // connect to server
                if(msg.get("e")==0){
                    System.out.println("Connect to test");
                }

                // test init the room
                else if(msg.get("w") >0){
                    this.it=msg.get("it");
                    this.tt=msg.get("tt");
                    this.w=msg.get("w");
                    this.tc=msg.get("tc");
                    this.ts=((double)msg.get("ts"))/1000;
                    // launch new thread to report state Periodically


                }

                // stop the client
                else if(msg.get("w")==0){
                    this.w=0;
                    this.stop();
                }

            }
        }
    }

    public void run() throws Exception {
        doConnect();
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

    private void doConnect() throws IOException {
        testsc.connect(new InetSocketAddress(testAddr,testPort));
        testsc.register(selector,SelectionKey.OP_CONNECT);

        channel.connect(new InetSocketAddress(host, port));
        channel.register(selector, SelectionKey.OP_CONNECT);
    }

    private void stop(){
        this.stop=true;
    }
    public static void main(String[] args) throws Exception {
        Client handler = new Client("localhost", 8080);
        handler.run();
    }


}
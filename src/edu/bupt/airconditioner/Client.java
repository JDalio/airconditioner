package edu.bupt.airconditioner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
    private String testAddr = "localhost";
    private int testPort = 9000;

    private static int roomNum = 1;

    private String kg = "BQD64Y";

    //home configuration
    private int it;
    private int tt;
    private int tc;
    private long ts;
    private volatile int w;
    private int duration;
    private int modeHot = 1; // 1制热, -1制冷
    private Timer timer = new Timer();

    public Client(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
        selector = Selector.open();
        channel = SocketChannel.open();
        channel.configureBlocking(false);
        testsc = SocketChannel.open();
        testsc.configureBlocking(false);
        msg = new Message(kg, "" + roomNum);
    }

    public void handleInput(SelectionKey key) throws Exception {
        if (key.isValid()) {
            SocketChannel sc = (SocketChannel) key.channel();

            // connect to test
            if (key.isConnectable()) {

                if (sc.finishConnect()) {
                    sc.register(selector, SelectionKey.OP_READ);
                    System.out.println("Connect to Server/Test");
                    msg.request(key);
                } else {
                    throw new RuntimeException("Connect Fail");
                }
            }

            // receive message from server or test
            if (key.isReadable()) {
                msg.read(key);

                // Pass Test Validation
                if (msg.get("e") == 0) {
                    System.out.println("Pass Validation");
                }

                // test init the room
                else if (msg.get("w") > 0) {
                    this.it = msg.get("it");
                    this.tt = msg.get("tt");

                    if (tt < 18 || tt > 28) {
                        throw new RuntimeException("Target Temperature Invalid");
                    }

                    if (tt < it) {
                        modeHot = -1;
                    }
                    this.w = msg.get("w");
                    if (w == 3) {
                        duration = 1;
                    } else if (w == 1) {
                        duration = 3;
                    } else {
                        duration = 2;
                    }
                    this.tc = msg.get("tc");
                    this.ts = msg.get("ts");

                    // launch new thread to report state Periodically
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            System.out.println(">>>>>> Client tc:" + tc);
                            int temp = it + modeHot * (tc - 1) / duration;

                            if (modeHot == 1) {
                                if (temp > tt) {
                                    System.out.println("Have Reached Target Temp: " + tt + " Current tc: " + tc);
                                    temp = tt;
                                }
                            } else {
                                if (temp < tt) {
                                    System.out.println("Have Reached Target Temp: " + tt + " Current tc: " + tc);
                                    temp = tt;
                                }
                            }

                            if (w > 0) {
                                if ((tc - 1) % duration == 0 || temp == tt) {
                                    msg.put("r", roomNum);
                                    msg.put("tc", tc);
                                    msg.put("t", temp);
                                    try {
                                        System.out.println("thread send");
                                        msg.send(channel);
                                    } catch (Exception e) {
                                        System.out.println("Monitor 2 Server w > 0:" + e);
                                    }
                                }
                                tc++;
                            }
                        }
                    }, 0, this.ts);
                }

                // stop the client
                else if (msg.get("w") == 0 && this.w != 0) {
                    this.w = 0;
                    timer.cancel();
                    msg.put("r", roomNum);
                    msg.put("tc", msg.get("tc"));
                    msg.put("w", 0);
                    try {
                        System.out.println("main send");
                        msg.send(channel);
                    } catch (Exception e) {
                        System.out.println("Monitor 2 Server w = 0:" + e);

                    }
                }

            }
        } else {
            stop();
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
        testsc.connect(new InetSocketAddress(testAddr, testPort));
        testsc.register(selector, SelectionKey.OP_CONNECT);

        channel.connect(new InetSocketAddress(host, port));
        channel.register(selector, SelectionKey.OP_CONNECT);
    }

    private void stop() {
        this.stop = true;
    }

    public static void main(String[] args) throws Exception {
        roomNum = Integer.valueOf(args[0]);
        Client handler = new Client("localhost", 8080);
        handler.run();
    }
}
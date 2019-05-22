package edu.bupt.airconditioner;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class Server {
    private Selector selector;
    private ServerSocketChannel ssc;
    private SocketChannel sc;

    private volatile boolean stop;

    // message processor
    private Message msg;

    // test configuration
    private String testAddr = "localhost";
    private int testPort = 9000;

    //      bill detail
    //      Map<roomNum,int[PrevTc, Pay]    >
    private Map<Integer, int[]> bill;

    private String kg = "123456";

    public Server(int port) throws Exception {
        selector = Selector.open();
        sc = SocketChannel.open();
        sc.configureBlocking(false);

        ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.socket().bind(new InetSocketAddress(port), 1024);
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        bill = new HashMap<>();

        msg = new Message(kg, "s");
        System.out.println("TIME SERVER IS LISTENING!!!");
    }

    public void handleInput(SelectionKey key) throws Exception {
        if (key.isValid()) {

            // test connection establish
            if (key.isConnectable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                if (sc.finishConnect()) {
                    sc.register(selector, SelectionKey.OP_READ);
                    msg.request(key);
                } else {
                    throw new RuntimeException("Connect Fail");
                }
            }

            // client connection establish
            if (key.isAcceptable()) {
                ServerSocketChannel schannel = (ServerSocketChannel) key.channel();
                SocketChannel accept = schannel.accept();
                accept.configureBlocking(false);
                accept.register(selector, SelectionKey.OP_READ);
            }

            //receive message from test or clients
            if (key.isReadable()) {
                msg.read(key);

                // connect to test and start test
                if (msg.get("e") == 0) {
                    System.out.println("Connect to test");
                    msg.put("i", 1);
                    msg.response(key);
                }

                //client terminate
                else if (msg.get("w") == 0) {
                    int roomNum = msg.get("r");

                    // send room state to server
                    msg.put("r", roomNum);
                    msg.put("tc", msg.get("tc"));
                    msg.put("w", 0);
                    msg.send(sc);

                }

                //receive from clients
                else if (msg.get("r") >= 0) {
                    int roomNum = msg.get("r");
                    int t = msg.get("t");
                    // send to test
                    msg.put("r", roomNum);
                    msg.put("tc", msg.get("tc"));
                    msg.put("t", t);
                    msg.send(sc);

                    // refresh bill
                    int[] cur = new int[2];
                    if (bill.containsKey(roomNum)) {
                        int[] prev = bill.get(roomNum);
                        cur[0] = t;
                        cur[1] = prev[1] + prev[0] - t;
                    } else {
                        cur[0] = t;
                        cur[1] = 0;
                    }
                    bill.put(roomNum, cur);

                }

                //receive from test to print bill
                else if (msg.get("b") == 1) {
                    int tc = msg.get("tc");
                    String result = "";
                    for (Integer roomNum : bill.keySet()) {
                        result += "r=" + roomNum + " tc=" + tc + " b=" + bill.get(roomNum)[1] + "\n";
                    }
                    msg.send(sc, result);
                }
            }

        }
    }

    public void stop() {
        this.stop = true;
    }

    public void run() throws Exception {
        sc.connect(new InetSocketAddress(testAddr, testPort));
        sc.register(selector, SelectionKey.OP_CONNECT);

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

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.run();
    }
}
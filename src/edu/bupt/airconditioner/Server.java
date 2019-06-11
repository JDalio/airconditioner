package edu.bupt.airconditioner;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;


public class Server {
    private Selector selector;
    private ServerSocketChannel ssc;
    private SocketChannel sc;

    private volatile boolean stop;

    private int validCondNums = 0;

    // message processor
    private Message msg;

    // test configuration
    private String testAddr = "192.169.2.10";
    private int testPort = 9000;

    //      bill detail
    //      Map<roomNum,int[TargetTemp, PrevTemp, Pay]>
    private Map<Integer, int[]> bill;

    private String kg = "ETWHC5";

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
                    System.out.println("Connect to Test");
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
                System.out.println("Client connect and registered");
            }


            //receive message from test or clients
            if (key.isReadable()) {
                msg.read(key);

                // Pass Test Validation
                if (msg.get("e") == 0) {
                    System.out.println("Pass Validation");
                }

                // When 4 Room login, Start Test
                else if (msg.get("k") == 200) {
                    System.out.println("Client " + msg.get("r") + " login to Test");
                    validCondNums++;
                    if (validCondNums == 4) {
                        msg.put("i", 1);
                        msg.response(sc);
                        System.out.println("Start Test");
                        validCondNums = -1;
                    }
                }

                //client terminate
                else if (msg.get("w") == 0) {
                    int roomNum = msg.get("r");
                    int tc = msg.get("tc");
                    // send room state to server
                    msg.put("r", roomNum);
                    msg.put("tc", tc);
                    msg.put("w", 0);
                    msg.send(sc);


//                    int[] roomBill = bill.get(roomNum);
//                    System.out.println("###### RoomNum:" + roomNum + " W=0 bill0:" + roomBill[0] + " bill1:" + roomBill[1] + " Now pay: " + roomBill[2]);
//                    roomBill[2] += Math.abs(roomBill[1] - roomBill[0]);
//                    bill.put(roomNum, roomBill);
                    int[] roomBill = bill.get(roomNum);
                    System.out.println("###### RoomNum:" + roomNum + " W=0 bill0:" + roomBill[0] + " bill1:" + roomBill[1] + " Now pay: " + roomBill[2]);
                    roomBill[1] += roomBill[0] * (tc + roomBill[2] + 1);
                    bill.put(roomNum, roomBill);
                }

                //receive from clients
                else if (msg.get("r") > 0) {
                    int roomNum = msg.get("r");
                    int t = msg.get("t");
                    int tc = msg.get("tc");

                    msg.put("r", roomNum);
                    msg.put("tc", tc);
                    msg.put("t", t);
                    msg.send(sc);

                    // refresh bill
                    //init target temperature of all rooms
                    if (!bill.containsKey(roomNum)) {
                        int[] init = new int[3];
                        init[0] = t;
                        init[1] = t;
                        bill.put(roomNum, init);
                    }
//                    else {
//                        int[] roomBill = bill.get(roomNum);
//                        if (t != roomBill[1]) {
//                            if (Math.abs(roomBill[1] - t) != 1) {
//                                System.out.println("Bill Count error");
//                                throw new RuntimeException("Bill Count error");
//                            }
//                            roomBill[1] = t;
//                            roomBill[2]++;
//                        } else {
//                            roomBill[2] += Math.abs(roomBill[1] - roomBill[0]);
//                        }
//                        bill.put(roomNum, roomBill);
//                    }
                    else {
                        int[] roomBill = bill.get(roomNum);
                        if (t != roomBill[1] && roomBill[2] >= 0) {
                            if (Math.abs(roomBill[1] - t) != 1) {
                                System.out.println("Bill Count error");
                                throw new RuntimeException("Bill Count error");
                            }
                            roomBill[1] = t;
                            roomBill[2]++;
                        } else {
                            if (roomBill[2] >= 0) {
                                int pay = roomBill[2];
                                // 0 存每次的步长
                                roomBill[0] = Math.abs(roomBill[1] - roomBill[0]);
                                // 2 存当前的tc
                                roomBill[2] = -1 * tc;
                                // 1 当前的钱
                                roomBill[1]=pay;
                                System.out.println("###### RoomNum:"+roomNum+" Save bill0:"+roomBill[0]+" bill1:"+roomBill[1]+" Now pay: "+roomBill[2]);
                            }
                        }
                        bill.put(roomNum, roomBill);
                    }
                }

                //receive from test to print bill
                else if (msg.get("b") != -1) {
                    List<LinkedHashMap<String, Integer>> orders = msg.getBillOrders();
                    for (LinkedHashMap<String, Integer> order : orders) {
                        int tc = order.get("tc");
                        int b = order.get("b");

                        msg.put("r", b);
                        msg.put("tc", tc);
                        msg.put("b", bill.get(b)[2]);
                        msg.send(sc);
                    }
                }
            }

        } else {
            stop();
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
        Server server = new Server(5555);
        server.run();
    }
}
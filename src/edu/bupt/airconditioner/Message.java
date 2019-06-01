package edu.bupt.airconditioner;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class Message {
    private List<LinkedHashMap<String, Integer>> billOrders;
    private LinkedHashMap<String, Integer> recv;
    private LinkedHashMap<String, Integer> resp;

    private String kg;
    private String role;

    public Message(String kg, String role) {
        this.recv = new LinkedHashMap<>();
        this.resp = new LinkedHashMap<>();
        this.kg = kg;
        this.role = role;
    }

    public List<LinkedHashMap<String, Integer>> getBillOrders() {
        return billOrders;
    }

    public void read(SelectionKey key) throws Exception {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int size = sc.read(buffer);
        if (size > 0) {
            buffer.flip();
            byte[] arr = new byte[buffer.remaining()];
            buffer.get(arr);
            String body = new String(arr, "UTF-8").trim();
            if (body.contains("b")) {
                String[] billArr = body.split("\n");
                this.billOrders = new ArrayList<>();
                for (String order : billArr) {
                    this.recv = new LinkedHashMap<>();
                    this.parse(order);
                    billOrders.add(this.recv);
                }
            } else {
                this.parse(body);
            }
            System.out.println(">>>>>READ MESSAGE:" + body);

        } else if (size < 0) {
            key.cancel();
            sc.close();
        }
    }

    public void response(SocketChannel sc) throws Exception {
        String res = this.toString();
        send(sc, res);
    }

    public void send(SocketChannel sc) throws Exception {
        System.out.println(">>>>>>Send Message:" + this.toString());
        send(sc, this.toString());
    }

    public void send(SocketChannel sc, String res) throws Exception {
        if (res != null && res.trim().length() > 0) {
            byte[] bytes = res.getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            sc.write(buffer);
        }
        resp.clear();
    }

    public void request(SelectionKey key) throws Exception {
        SocketChannel sc = (SocketChannel) key.channel();
        String res = id();
        send(sc, res);
    }

    public String id() {
        return "k=" + kg + " r=" + role + "\n";
    }

    public void parse(String order) {
        recv.clear();

        String[] pairs = order.split(" ");
        for (String pair : pairs) {
            String[] entity = pair.split("=");

            if (entity[0].equals("k")) {
                recv.put("k", 200);
                String[] arr = pairs[1].split("=");
                recv.put("r", Integer.valueOf(arr[1]));
                break;
            } else {
                if (entity[0].equals("ts")) {
                    if (entity[1].contains(".")) {
                        Float tick = Float.valueOf(entity[1]) * 1000;
                        recv.put(entity[0],
                                tick.intValue());
                    } else {
                        recv.put(entity[0],
                                Integer.valueOf(entity[1]) * 1000);
                    }
                } else {
                    recv.put(entity[0],
                            Integer.valueOf(entity[1]));
                }
            }

        }
        System.out.println(">>>>>PARSE MESSAGE:" + recv.toString());
    }

    public Integer get(String key) {
        if (key.trim().equals("ts")) {
            return recv.getOrDefault(key, -1);
        }
        return recv.getOrDefault(key, -1);
    }

    public void put(String key, Integer value) {
        if (!resp.containsKey(key)) {
            resp.put(key, value);
        }
    }

    @Override
    public String toString() {
        return resp.toString().replaceAll("[{},]", "") + "\n";
    }
}

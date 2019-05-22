package edu.bupt.airconditioner;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class Message {
    private Map<String, Integer> recv;
    private Map<String, Integer> resp;

    private String kg;
    private String role;

    public Message(String kg, String role) {
        this.recv = new HashMap<>();
        this.resp = new HashMap<>();
        this.kg = kg;
        this.role = role;
    }

    public void read(SelectionKey key) throws Exception {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int size = sc.read(buffer);
        if (size > 0) {
            buffer.flip();
            byte[] arr = new byte[buffer.remaining()];
            buffer.get(arr);
            String body = new String(arr, "UTF-8");
            this.parse(body);
        } else if (size < 0) {
            key.cancel();
            sc.close();
        }
    }

    public void response(SelectionKey key) throws Exception {
        SocketChannel sc = (SocketChannel) key.channel();
        String res = this.toString();
        send(sc, res);
    }

    public void send(SocketChannel sc) throws Exception {
        send(sc, this.toString());
    }

    public void send(SocketChannel sc, String res) throws Exception {
        if (res != null && res.trim().length() > 0) {
            byte[] bytes = res.getBytes();
            ByteBuffer buffe = ByteBuffer.allocate(bytes.length);
            buffe.put(bytes);
            buffe.flip();
            sc.write(buffe);
        }
        resp.clear();
    }

    public void request(SelectionKey key) throws Exception {
        SocketChannel sc = (SocketChannel) key.channel();
        String res = id();
        send(sc, res);
    }

    public String id() {
        return "k=" + kg + " r=" + role;
    }

    public void parse(String order) {
        recv.clear();
        String[] pairs = order.split(" ");
        for (String pair : pairs) {
            String[] entity = pair.split("=");
            if (entity[1].contains(".")) {
                Float tick = Float.valueOf(entity[1]) * 10000;
                recv.put(entity[0],
                        tick.intValue());
            } else {
                recv.put(entity[0],
                        Integer.valueOf(entity[1]));
            }
        }
    }

    public Integer get(String key) {
        if (key.trim().equals("ts")) {
            return recv.getOrDefault(key, -1) * 1000;
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
        return resp.toString().replaceAll("[{},]", "");
    }
}

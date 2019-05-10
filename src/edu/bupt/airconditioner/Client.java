package edu.bupt.airconditioner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    //    private static String host="127.0.0.1";
    private static int port;

    public static void main(String[] args) throws Exception {
        port = 8080;
        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;

        socket = new Socket("127.0.0.1", port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        out.println("QUERY");
        System.out.println("Send order 2 server succeed");
        String resp = in.readLine();
        System.out.println("Now is: " + resp);
    }
}

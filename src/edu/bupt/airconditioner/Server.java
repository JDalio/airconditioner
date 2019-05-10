package edu.bupt.airconditioner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class Server {
//    private static String host;
    private static int port;

    public static void main(String[] args) throws Exception {
        port = 8080;
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server start at port: " + port);
        while (true) {
            final Socket socket = server.accept();
            new Thread(() -> {
                BufferedReader in = null;
                PrintWriter out = null;

                try {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);
                    String currentTime = null;
                    String body = null;
                    while (true) {
                        body = in.readLine();
                        if (body == null) {
                            break;
                        }
                        System.out.println("Server Receive Order: " + body);
                        currentTime = "QUERY".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                        out.println(currentTime);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Server IO Fatal Failure");
                }
            }).start();
        }
    }
}

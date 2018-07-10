// REST-ful cloud server collecting data

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ServerApp {

    private static String serverAddress;

    static {
        try {
            serverAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static final int serverPort = 2302;

    public static void main(String[] args) {
        HttpServer server = null;
        Scanner keyIn = new Scanner(System.in);

        // Boot
        System.out.println("- Setting up server...");
        try {
            server = HttpServerFactory.create("http://" + serverAddress + ":" + serverPort + "/");
        } catch (IOException e) {
            System.err.println("- ERROR: Couldn't set up server!");
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("- Starting server...");
        server.start();

        // Running
        System.out.println("- Server running on: http://"+ serverAddress +":"+ serverPort);
        System.out.println("- Press ENTER to stop server...");

        // Shutdown
        keyIn.nextLine();
        System.out.println("- Stopping server...");
        server.stop(0);
        System.out.println("- Server stopped!");
        System.exit(0);
    }

}

package Client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    public final static String SERVER_IP = "127.0.0.1";
    public final static int SERVER_PORT = 8686;
    public int clientPort; //port cua chinh no
    public Socket socket; // socket cua chinh no
    public int port1; //port cua client 1
    public int port2; //port cua client 2
    public Socket socket1; // socket ket noi den client 1
    public  Socket socket2; // socket ket noi den client 2
    public  ServerSocket serverSocket;
    public String filePath;

    ArrayList<Socket> listSocket = new ArrayList<>();

    public void rcfile() {
        try {

            InputStream input = socket.getInputStream();
            DataInputStream clientData = new DataInputStream(input);
            port1 = clientData.readInt();
            port2 = clientData.readInt();
            String file = clientData.readUTF();
            String dir = file.substring(0, 1);
            filePath = "src/Client/" + dir + "/" + file;
            OutputStream output = new FileOutputStream(filePath);
            long size = clientData.readLong();
            int bytes;
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytes = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytes);
                size -= bytes;
            }

            output.close();
            input.close();
            System.out.println("Port 1 da duoc nhan: " + port1);
            System.out.println("Port 2 da duoc nhan: " + port2);
            System.out.println(file+" da duoc gui tu server");
        } catch (IOException ex) {
            System.out.println("Error: "+ex);
        }

    }

    public void sendPort(){
        OutputStream osPort = null;
        try {
            osPort = socket.getOutputStream();
            DataOutputStream dosPort = new DataOutputStream(osPort);
            dosPort.writeInt(clientPort);
            System.out.println("Send port to server: " + clientPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createServerPeer(){
        try {
            serverSocket = new ServerSocket(clientPort);
            System.out.println("Binding to port " + clientPort + ", please wait  ...");
            System.out.println("Server started: " + serverSocket);
            System.out.println("Waiting for a client ...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createConnectToClients(){
        try{
            do{
                socket1 = new Socket(SERVER_IP, port1);
                System.out.println("Connect: " + socket1);
            }while(!socket1.isConnected());

            do{
                socket2 = new Socket(SERVER_IP, port2);
                System.out.println("Connect: "+ socket2);
            }while(!socket2.isConnected());

        }catch (Exception e){
            System.out.println("Error connect to clients");
            System.out.println(e.getMessage());
        }

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            Client client = new Client();
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter port client: ");
            client.clientPort = sc.nextInt();
            client.socket = new Socket(SERVER_IP, SERVER_PORT); // Connect to server
            System.out.println("Connected: " + client.socket);
            client.sendPort();
            client.rcfile();
            client.createServerPeer();
            client.createConnectToClients();
            //dir save
            String dir = client.filePath.substring(11, 12);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            //rc file from clients;
            System.out.println("bat dau nhan file");

            for(int i = 0; i < 2; i++){
                Socket socket = client.serverSocket.accept();
                ReadFileFromClients readFileFromClient = new ReadFileFromClients(socket, dir);
                executor.execute(readFileFromClient);
            }
            //send file to clients
            SendFileToClients sendFileToClient1 = new SendFileToClients(client.socket1, client.filePath);
            sendFileToClient1.start();

            SendFileToClients sendFileToClient2 = new SendFileToClients(client.socket2, client.filePath);
            sendFileToClient2.start();


        } catch (IOException ie) {
            System.out.println("Can't connect to server");
        } finally {

        }
    }
}
class SendFileToClients extends Thread{
    private Socket socket;
    private String fileName;

    public SendFileToClients(Socket socket, String fileName) {
        this.socket = socket;
        this.fileName = fileName;
    }
    public void sendFile() {
        try {
            File file = new File(fileName);
            byte[] mybytearray = new byte[(int) file.length()];
            FileInputStream finstream = new FileInputStream(file);
            BufferedInputStream bufinstream = new BufferedInputStream(finstream);
            DataInputStream dinstream = new DataInputStream(bufinstream);
            dinstream.readFully(mybytearray, 0, mybytearray.length);
            OutputStream os = socket.getOutputStream();
            DataOutputStream doutstream = new DataOutputStream(os);
            doutstream.writeUTF(file.getName());
            doutstream.writeLong(mybytearray.length);
            doutstream.write(mybytearray, 0, mybytearray.length);
            doutstream.flush();
            System.out.println(fileName+" send to client " + socket);

        } catch (Exception e) {
            System.err.println("Khong co file");
            System.out.println(e.getMessage());
        }
    }
    @Override
    public void run() {
        System.out.println("Processing: " + socket);
        try {
            sendFile();
        } catch (Exception e) {
            System.err.println("Send file error!: " + e);
        }
        System.out.println("Complete processing: " + socket);
    }

}
class ReadFileFromClients extends Thread {
    private Socket socket;
    private String dir;
    public ReadFileFromClients(Socket socket, String dir) {
        this.socket = socket;
        this.dir = dir;
    }

    public void rcfile() {
        try {
            System.out.println("bat dau nhan nha");
            InputStream input = socket.getInputStream();
            DataInputStream clientData = new DataInputStream(input);
            String file = clientData.readUTF();
            //  String dir = file.substring(0, 1);
            String filePath = "src/Client/" + dir + "/" + file;
            OutputStream output = new FileOutputStream(filePath);
            long size = clientData.readLong();
            int bytes;
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytes = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytes);
                size -= bytes;
            }
            System.out.println(file+" da duoc gui tu client" + socket);

            //output.close();
            //input.close();
            //socket.close();

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }

    }

    @Override

    public void run() {
        System.out.println("Processing: " + socket);
        try {
            rcfile();
        } catch (Exception e) {
            System.err.println(" rcfile error!: " + e);
        }
        System.out.println("Complete processing: " + socket);
    }

}
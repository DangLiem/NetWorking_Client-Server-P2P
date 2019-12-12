package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class Server {
    public static final int NUM_OF_THREAD = 3;
    public final static int SERVER_PORT = 8686;
    public static ArrayList<Socket> listSocket;
    public static HashMap<Integer, Socket> listClient;


    public static void main(String[] args) throws IOException {

        // tao thread pool, su dung method newFixedThreadPool trong Executors de gioi han thread la 3
        ExecutorService executor = Executors.newFixedThreadPool(NUM_OF_THREAD);
        ServerSocket serverSocket = null;
        //Server.listSocket = new ArrayList<>();
        Server.listClient = new HashMap<>();
        try {
            System.out.println("Binding to port " + SERVER_PORT + ", please wait  ...");
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started: " + serverSocket);
            System.out.println("Waiting for a client ...");
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("Client accepted: " + socket);
                    //get port from client
                    InputStream input = socket.getInputStream();
                    DataInputStream dos = new DataInputStream(input);
                    Integer portClient = dos.readInt();
                    System.out.println("Port from Client: " + portClient);
                    listClient.put(portClient, socket);
                    //listSocket.add(socket);
//                    if(listSocket.size() == 3)
//                        break;
                    if(listClient.size() == 3)
                        break;
                } catch (IOException e) {
                    System.err.println(" Connection Error: " + e);
                }
            }

        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
        try {
            if (listClient.size() == 3){

                String filePath = "src/Server/SharedFolder/";
                Integer fileOut = 1;
                Integer[] ports = new Integer[2];
//                for (Socket socketItem : listSocket) {
//                    String out = Integer.toString(fileOut);
//                    String fileName = filePath + out + ".txt";
//                    WorkerThread hander = new WorkerThread(socketItem, fileName);
//                    executor.execute(hander);
//                    fileOut++;
//                }
                for(Map.Entry m:listClient.entrySet()){
//                    System.out.println(m.getKey());
//                    System.out.println(m.getValue());
//                    System.out.println("-----------------------------");
                    String out = Integer.toString(fileOut);
                    String fileName = filePath + out + ".jpg";

                    // cac port con lai
                    Iterator<Integer> itr = listClient.keySet().iterator();
                    int index = 0;
                    while(itr.hasNext()){
                        Integer port = itr.next();
                        if(port != m.getKey()){
                            ports[index] = port;
                            index++;
                        }
                    }
                    WorkerThread hander = new WorkerThread((Socket) m.getValue(), fileName, ports);
                    executor.execute(hander);
                    fileOut++;
                }

            }

        } catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println("Error send file to clients");
        }

    }
}

class WorkerThread extends Thread {
    private Socket socket;
    private String fileName;
    private Integer[] ports = new Integer[2]; // danh sach cac port con lai

    public WorkerThread(Socket socket, String fileName, Integer[] p) {
        this.socket = socket;
        this.fileName = fileName;
        for(int i = 0; i < p.length; i++) this.ports[i] = p[i];
    }

    public void run() {
        System.out.println("Processing: " + socket);
        try {
            sendFile();
        } catch (Exception e) {
            System.err.println("Send file error!: " + e);
        }
        System.out.println("Complete processing: " + socket);
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
            //gui lan luot cac port
            for(int i = 0; i < 2; i++){
                doutstream.writeInt(ports[i]);
            }
            doutstream.writeUTF(file.getName());
            doutstream.writeLong(mybytearray.length);
            doutstream.write(mybytearray, 0, mybytearray.length);
            doutstream.flush();
            System.out.println(fileName+" send to client ");
        } catch (Exception e) {
            System.err.println("Khong co file");
        }
    }
}


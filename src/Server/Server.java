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
            System.out.println("Gán port cho Server Socket " + SERVER_PORT + ", vui lòng đợi ...");
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server đã khởi động: " + serverSocket);
            System.out.println("Đợi kết nối từ client...");
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("Chấp nhận kết nối với client: " + socket);
                    //get port from client
                    InputStream input = socket.getInputStream();
                    DataInputStream dos = new DataInputStream(input);
                    Integer portClient = dos.readInt();
                    System.out.println("Port từ Client: " + portClient);
                    listClient.put(portClient, socket);
                    //listSocket.add(socket);
//                    if(listSocket.size() == 3)
//                        break;
                    if(listClient.size() == 3)
                        break;
                } catch (IOException e) {
                    System.err.println("Kết nỗi  " + e);
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
                for(Map.Entry m:listClient.entrySet()){
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
            System.out.println("Lỗi gửi file đến các client");
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
        System.out.println("Đang xử lý: " + socket);
        try {
            sendFile();
        } catch (Exception e) {
            System.err.println("Gửi file tới client lỗi!: " + e);
        }
        System.out.println("Gửi file hoàn thành: " + socket);
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
            System.out.println(fileName+" đã gửi tới client " + socket);
        } catch (Exception e) {
            System.err.println("Không có file " + fileName);
        }
    }
}


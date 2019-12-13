package Client;

import org.apache.commons.io.IOUtils;

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
    public String ip1; //ip cua client 1
    public String ip2; //ip cua client 2
    public Socket socket1; // socket ket noi den client 1
    public  Socket socket2; // socket ket noi den client 2
    public  ServerSocket serverSocket;
    public String filePath;
    public String fileUpdateOut;

    public static int checkCountRcFileFromClients = 0;
    ArrayList<Socket> listSocket = new ArrayList<>();

    public void rcfile() {
        try {

            InputStream input = socket.getInputStream();
            DataInputStream clientData = new DataInputStream(input);
            port1 = clientData.readInt();
            port2 = clientData.readInt();
            ip1 = clientData.readUTF();
            ip2 = clientData.readUTF();
            fileUpdateOut = clientData.readUTF();
            String file = clientData.readUTF();
            //String dir = file.substring(0, 1);
            filePath = "src/Client/"+file;
            OutputStream output = new FileOutputStream(filePath);
            long size = clientData.readLong();
            int bytes;
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytes = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytes);
                size -= bytes;
            }

            output.close();
            //input.close();
            System.out.println("Port1 đã được nhận: " + port1);
            System.out.println("Port2 đã được nhận: " + port2);
            System.out.println(file+" đã được nhận từ server");
        } catch (IOException ex) {
            System.out.println("Lỗi nhận file: "+ex);
        }

    }

    public void sendPort(){
        OutputStream osPort = null;
        try {
            osPort = socket.getOutputStream();
            DataOutputStream dosPort = new DataOutputStream(osPort);
            dosPort.writeInt(clientPort);
            System.out.println("Gửi port đến server: " + clientPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createServerPeer(){
        try {
            serverSocket = new ServerSocket(clientPort);
            System.out.println("Gán port cho Server Socket: " + clientPort + ", vui lòng đợi...");
            System.out.println("Server đã được khởi động: " + serverSocket);
            System.out.println("Đợi kết nối từ các client...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createConnectToClients(){
        try{
            socket1 = new Socket(ip1, port1);
            System.out.println("đã kết nối tới: " + socket1);
            socket2 = new Socket(ip2, port2);
            System.out.println("đã kết nối tới: "+ socket2);

        }catch (Exception e){
            System.out.println("Lỗi kết nối tới các client " + e);
        }

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            Client client = new Client();

            Scanner sc = new Scanner(System.in);
            System.out.println("Nhập port để tạo Server Socket: ");
            client.clientPort = sc.nextInt();
            client.createServerPeer();
            client.socket = new Socket(SERVER_IP, SERVER_PORT); // Connect to server
            System.out.println("Đã kết nối tới: " + client.socket);
            client.sendPort();
            client.rcfile();

            client.createConnectToClients();
            //dir save
            String dir = client.filePath.substring(11, 12);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            //rc file from clients;

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

            MergerFile mergerFile = new MergerFile(client.socket, client.fileUpdateOut);
            mergerFile.start();

        } catch (IOException ie) {
            System.out.println("Không thể kết nối tới server");
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
            System.out.println(fileName+" gửi tới client: " + socket);

        } catch (Exception e) {
            System.err.println("Lỗi gửi file tới client");
            System.out.println(e.getMessage());
        }
    }
    @Override
    public void run() {
        System.out.println("Đang tiến hành gửi file tới client: " + socket);
        try {
            sendFile();
        } catch (Exception e) {
            System.err.println("Gửi file tới client lỗi!: " + e);
        }
        System.out.println("Gửi file tới client hoàn thành: " + socket);
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
            InputStream input = socket.getInputStream();
            DataInputStream clientData = new DataInputStream(input);
            String file = clientData.readUTF();
            // dir = file.substring(0, 1);
            String filePath = "src/Client/" + file;
            //  String filePath = "src/Client/" +  file;

            OutputStream output = new FileOutputStream(filePath);
            long size = clientData.readLong();
            int bytes;
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytes = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytes);
                size -= bytes;
            }

            System.out.println(file+" đã được nhận từ client: " + socket);

            //output.close();
            //input.close();
            //socket.close();

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }

    }

    @Override

    public void run() {
        System.out.println("Bắt đầu nhận file từ client: " + socket);
        try {
            rcfile();
        } catch (Exception e) {
            System.err.println("Lỗi nhận file!: " + e);
        }
        System.out.println("Hoàn thành nhận file: " + socket);
        Client.checkCountRcFileFromClients++;
        System.out.println(Client.checkCountRcFileFromClients);
    }

}

class IOCopier {
    public static void joinFiles(File destination, File[] sources)
            throws IOException {
        OutputStream output = null;
        try {
            output = createAppendableStream(destination);
            for (File source : sources) {
                appendFile(output, source);
            }
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    private static BufferedOutputStream createAppendableStream(File destination)
            throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(destination, true));
    }

    private static void appendFile(OutputStream output, File source)
            throws IOException {
        InputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(source));
            IOUtils.copy(input, output);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }
}

class MergerFile extends Thread{
    private String result;
    private Socket socket;
    public MergerFile(Socket socket, String result) {
        this.result = result;
        this.socket = socket;
    }

    public static void mergeFile(String file1, String file2, String file3, String result) throws IOException{
        IOCopier.joinFiles(new File(result), new File[] {
                new File(file1), new File(file2), new File(file3)});
        File f1 = new File(file1);
        boolean b = f1.delete();
        File f2 = new File(file2);
        boolean c = f2.delete();
        File f3 = new File(file3);
        boolean d = f3.delete();
    }
    public void sendNotification(){
        OutputStream send = null;
        try {
            send = socket.getOutputStream();
            DataOutputStream ok = new DataOutputStream(send);
            ok.writeUTF("done");
            System.out.println("Gửi xác nhận cập nhật file thành công tới server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
//        File file1 = new File("src/Client/1");
//        File file2 = new File("src/Client/2");
//        File file3 = new File("src/Client/3");
        while(true){
            System.out.println("Test cout: " + Client.checkCountRcFileFromClients);
            if(Client.checkCountRcFileFromClients == 2){
                try {
                    mergeFile("src/Client/1", "src/Client/2", "src/Client/3", "src/Client/" + result);
                    System.out.println("Hợp file thành công");
                    sendNotification();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

    }
}
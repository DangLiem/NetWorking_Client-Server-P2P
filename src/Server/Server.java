package Server;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class Server {
    public static final int NUM_OF_THREAD = 3;
    public final static int SERVER_PORT = 8686;
    public static final int SIZE_BUFF = 4096;
    public static final int NUM_SPLIT = 3;
    public static final String PATH = "src/Server/SharedFolder/";
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
                // bat dau gui file
               // String filePath = "src/Server/SharedFolder/";
                System.out.println("Nhập file cần cập nhật: ");
                Scanner sc = new Scanner(System.in);
                String fileUpdate = sc.nextLine();
                splitFile(PATH + fileUpdate);
                sc.close();

                Integer fileOut = 1;
                Integer[] ports = new Integer[2];
                for(Map.Entry m:listClient.entrySet()){
                    String out = Integer.toString(fileOut);
                    String fileName = PATH + out;

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
    public static void splitFile(String fileName) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(fileName, "r");
        long numSplits = NUM_SPLIT;
        long sourceSize = raf.length();
        long bytesPerSplit = sourceSize/numSplits;
        long remainingBytes = sourceSize % numSplits;
        int maxReadBufferSize = SIZE_BUFF;
        for(int destIx=1; destIx <= numSplits; destIx++) {
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(PATH+destIx));
            if(bytesPerSplit > maxReadBufferSize) {
                long numReads = bytesPerSplit/maxReadBufferSize;
                long numRemainingRead = bytesPerSplit % maxReadBufferSize;
                for(int i=0; i<numReads; i++) {
                    readWrite(raf, bw, maxReadBufferSize);
                }
                if(numRemainingRead > 0) {
                    readWrite(raf, bw, numRemainingRead);
                }
            }else {
                readWrite(raf, bw, bytesPerSplit);
            }
            bw.close();
        }
        if(remainingBytes > 0) {
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(PATH+(numSplits+1)));
            readWrite(raf, bw, remainingBytes);
            bw.close();
            IOCopier.joinFiles(new File(PATH+"5"), new File[] {
                    new File(PATH+"3"), new File(PATH+"4")});
            File f1 = new File(PATH+"3");
            boolean b = f1.delete();
           // System.out.println("Delete file 1 remaining success.");
            File f2 = new File(PATH+"4");
            boolean c = f2.delete();
          //  System.out.println("Delete file 2 remaining success.");
            File f3 = new File(PATH+"5");
            File f4 = new File(PATH+"3");
            boolean d = f3.renameTo(f4);
            f3.delete();
        }
        raf.close();
    }
    static void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
        byte[] buf = new byte[(int) numBytes];
        int val = raf.read(buf);
        if(val != -1) {
            bw.write(buf);
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


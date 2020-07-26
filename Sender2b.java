import java.io.*;
import java.lang.Math;
import java.util.concurrent.*;
import java.net.*;
import java.util.Timer;

public class Sender2b {

    static int sendBase = 0;
    static Frame[] window;
    static double[] log;
    static int windowSize;
    static int timeOut;
    static int port;
    static InetAddress host;
    static DatagramSocket socket;
    static int lastPktLength;
    static int numOfPkts;
    static byte[] data;
    static double startTime = (double) System.currentTimeMillis();
    static double endTime = (double) System.currentTimeMillis();

    public static void main(String[] args) {

        try {
            // Read arguments
            String remoteHost = args[0];
            port = Integer.parseInt(args[1]);
            String fileName = args[2];
            timeOut = Integer.parseInt(args[3]);
            windowSize = Integer.parseInt(args[4]);

            // Read file    
            RandomAccessFile file = new RandomAccessFile(fileName, "r");
            data = new byte[(int)file.length()];
            file.readFully(data);
            file.close();

            // Calculate how many packets are needed to transfer the file
            numOfPkts = (int) Math.ceil((double) data.length / Packet.payloadLength());
            // Calculate the payload of the last packet
            lastPktLength = data.length - (numOfPkts - 1) * Packet.payloadLength();
    
            // Initialize socket and set timeout
            socket = new DatagramSocket();
            host = InetAddress.getByName(remoteHost);

            int maxRetry = 10;
            int lostPackets = 0;

            // Setup the window which consists of Frames
            if (numOfPkts >= windowSize) {
                window = new Frame[windowSize];
                for (int i = 0; i < windowSize; i++) {
                    window[i] = new Frame (i, Status.READY, (double) System.currentTimeMillis());
                }
            }
            else {
                window = new Frame[numOfPkts];
                for (int i = 0; i < numOfPkts; i++) {
                    window[i] = new Frame (i, Status.READY, (double) System.currentTimeMillis());
                }
            }

            // Listen to incoming ACKs in a seperate thread
            Receiver receiver = new Receiver(socket, windowSize, window);
            receiver.start();

            // Start the transmission
            boolean transmission = true;
            while (transmission) {

                // Slide the window if possible
                slideWindow();
                // Update the window in the other thread
                receiver.setWindow(window);

                // If the first packet in the window is empty, then we are done
                if (window[0].getStatus() == Status.EMPTY) {
                    socket.close();
                    transmission = false;
                    receiver.killThread();
                    System.out.println((data.length / 1024) / ((endTime - startTime) / 1000));
                }

                // Send a load of packets
                for (int i = 0; i < windowSize; i++) {

                    // Send packets that are ready or they timed out
                    if (window[i].getStatus() == Status.READY || (window[i].getStatus() == Status.UNACKED && 
                            (double) System.currentTimeMillis() - window[i].getTime() > timeOut)) {

                        Packet pkt = new Packet();
                        // Sending the packets normally
                        if (window[i].getSeqNum() != numOfPkts - 1) {
                            pkt.setEoF(false);
                            pkt.setSeqNum(window[i].getSeqNum());
                            for (int j = 0; j < Packet.payloadLength(); j++) {
                                pkt.setPayload(j, data[Packet.payloadLength() * window[i].getSeqNum() + j]);
                            }
                            DatagramPacket datagram = new DatagramPacket(pkt.toByteArray(),
                                Packet.length(), host, port);
                            try {
                                socket.send(datagram);
                            }
                            catch (Exception e) {}
                        }
        
                        // Sending the last packet
                        else {
                            endTime = (double) System.currentTimeMillis();
                            pkt.setEoF(true);
                            pkt.setSeqNum(window[i].getSeqNum());
                            for (int j = 0; j < lastPktLength; j++) {
                                pkt.setPayload(j, data[Packet.payloadLength() * i + j]);
                            }
                            DatagramPacket datagram = new DatagramPacket(pkt.toByteArray(),
                                Packet.headerLength() + lastPktLength, host, port);
                            try {
                                socket.send(datagram);
                            }
                            catch (Exception e){}
                        }
                        // Update the packet status and time
                        window[i].setStatus(Status.UNACKED);
                        window[i].setTime((double) System.currentTimeMillis());
                    }
                }
            }

        }
        catch (IllegalArgumentException e) {
            System.out.println("Invalid arguments");
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        catch (UnknownHostException e) {
            System.out.println("Invalid host");
        }
        catch (Exception e) {
            System.out.println("An error occured during transmission");

        }

    }

    // Slide the window 
    static synchronized void slideWindow() {
        int slideAmount = 0;
        // Calculate the number of shifts needed
        try {
            while (window[slideAmount].getStatus() == Status.ACKED) {
                slideAmount++;
            }
        }
        catch (Exception e){}

        // Slide the window frames if needed
        if (slideAmount > 0) {
            Frame[] newWindow = new Frame[windowSize];
            // Copy the elements
            for (int i = slideAmount; i < windowSize; i++) {
                newWindow[i - slideAmount] = window[i];
            }
            // Pad it with "ready" or "empty" packets
            int j = 0;
            for (int i = windowSize - slideAmount; i < windowSize; i++) {
                if (sendBase + windowSize + j < numOfPkts) {
                    newWindow[i] = new Frame(sendBase + windowSize + j,
                    Status.READY, (double) System.currentTimeMillis());
                }
                // If the new packet were out of range flag them as empty
                else {
                    newWindow[i] = new Frame(sendBase + windowSize + j,
                    Status.EMPTY, (double) System.currentTimeMillis());
                }
                j++;
            }

            
            // Update the sendBase and window
            window = newWindow;
            sendBase += slideAmount;
        }
    }
}


// Listens to incoming ACKs and updates the window accordingly
class Receiver extends Thread {

    boolean exit = false;
    DatagramSocket socket;
    int windowSize;
    Frame[] window;

    public Receiver(DatagramSocket socket, int windowSize, Frame[] window) {
        this.socket = socket;
        this.windowSize = windowSize;
        this.window = window;
    }

    public void setWindow (Frame[] window) {
        this.window = window;
    }

    @Override
    public void run(){
        synchronized(window) {
            byte[] buf = new byte[Ack.length()];
            DatagramPacket dp = new DatagramPacket(buf, Ack.length());
            while (!exit) {
                try {
                    socket.receive(dp);
                    Ack ack = new Ack(dp.getData());
                    // Update the given window frame's status
                    for (int i = 0; i < windowSize; i++) {
                        if (window[i].getSeqNum() == ack.getSeqNum()) {
                            window[i].setStatus(Status.ACKED);
                        }
                    }
                }
                catch (IOException e) {}
            }
        }
    }

    public void killThread() {
        exit = true;
    }

}

class Frame {
    private int seqNum;
    private Status status;
    private double time;

    public Frame (int seqNum, Status status, double time) {
        this.seqNum = seqNum;
        this.status = status;
        this.time = time;
    }

    public int getSeqNum() {
        return seqNum;
    }
    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }

    public double getTime() {
        return time;
    }
    public void setTime(double time) {
        this.time = time;
    }
}

enum Status {
    ACKED, UNACKED, READY, EMPTY
}
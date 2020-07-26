import java.io.*;
import java.lang.Math;
import java.util.concurrent.*;
import java.net.*;
import java.util.Timer;

public class Sender2a {

    public static void main(String[] args) {

        try {

            // Read arguments
            String remoteHost = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            int timeOut = Integer.parseInt(args[3]);
            int windowSize = Integer.parseInt(args[4]);

            // Read file    
            RandomAccessFile file = new RandomAccessFile(fileName, "r");
            byte[] data = new byte[(int)file.length()];
            file.readFully(data);
            file.close();

            // Calculate how many packets are needed to transfer the file
            int numOfPkts = (int) Math.ceil((double) data.length / Packet.payloadLength());
            // Calculate the payload of the last packet
            int lastPktLength = data.length - (numOfPkts - 1) * Packet.payloadLength();
    
            // Initialize socket and set timeout
            DatagramSocket socket = new DatagramSocket();
            InetAddress host = InetAddress.getByName(remoteHost);

            // Keep track of the transmission time
            double startTime = (double) System.currentTimeMillis();
            double endTime = (double) System.currentTimeMillis();

            /* In my implementation sendBase must be synced between threads
			   Because synchronized() only works with references, I used an array of length 1 instead of int*/
            int[] sendBase = new int[1];
            sendBase[0] = 0;
            int nextSeqNum = 0;
            int maxRetry[] = new int[1];
            maxRetry[0] = 10;
            int lostPackets = 0;

            // The range of the for loop
            int start = 0;
            int end = windowSize;

            // This logs the time 
            double timer = (double) System.currentTimeMillis();

            // Start a new thread which listens to the incoming ACKs
            Receiver receiver = new Receiver(sendBase, maxRetry, numOfPkts, socket);
            receiver.start();

            // Start the transmission
            boolean transmission = true;
            while (transmission) {

                // If we time out we need to set the variables to resend the whole window
                if ((double) System.currentTimeMillis() - timer > timeOut) {
                    // Reset the nextSeqNum
                    nextSeqNum = sendBase[0];
                    // Send packets starting from the sendBase
                    start = sendBase[0];
                    // Till the end of the window
                    end = sendBase[0] + windowSize;
                }
                // Otherwise set the variables to send the next packet
                else {
                    // Send packets starting at nextSeqNum
                    start = nextSeqNum;
                    // As long as it is in the window
                    end = sendBase[0] + windowSize;
                }

                // Send a load of packets
                for (int i = start; i < end && i < numOfPkts; i++) {

                    Packet pkt = new Packet();

                    // Sending the packets normally
                    if (i != numOfPkts - 1) {
                        pkt.setEoF(false);
                        pkt.setSeqNum(i);
                        for (int j = 0; j < Packet.payloadLength(); j++) {
                            pkt.setPayload(j, data[Packet.payloadLength() * i + j]);
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
                        pkt.setSeqNum(i);
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
                    nextSeqNum++;
                    timer = (double) System.currentTimeMillis();
                }

                // If the send base reaches the end of the window or we exceed the max number of retries, then we close the connection, print the throughput and exit
                if (sendBase[0] == numOfPkts || maxRetry[0] == 0) {
                    socket.close();
                    transmission = false;
                    receiver.killThread();
                    System.out.println((data.length / 1024) / ((endTime - startTime) / 1000));
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
}


// Updates the sendBase so it always starts at the lowest unacked index
class Receiver extends Thread {

    boolean exit = false;
    DatagramSocket socket;
    int[] sendBase;
    int[] maxRetry;
    int numOfPkts;

    public Receiver(int[] sendBase, int[] maxRetry, int numOfPkts, DatagramSocket socket) {
        this.socket = socket;
        this.sendBase = sendBase;
        this.maxRetry = maxRetry;
        this.numOfPkts = numOfPkts;
    }

    @Override
    public void run() {
        synchronized(this) {
            byte[] buf = new byte[Ack.length()];
            DatagramPacket dp = new DatagramPacket(buf, Ack.length());
            while (!exit){
                try {
                    socket.receive(dp);
                    Ack ack = new Ack(dp.getData());
                    if (ack.getSeqNum() >= sendBase[0]) {
                        sendBase[0] = ack.getSeqNum() + 1;
                    }
                    else if (sendBase[0] == numOfPkts - 1) {
                        maxRetry[0]--;
                    }
                }
                catch (IOException e) {
                    
                }
            }
        }
    }

    public void killThread() {
        exit = true;
    }
}
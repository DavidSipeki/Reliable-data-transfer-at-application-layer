import java.io.*;
import java.lang.Math;
import java.util.concurrent.TimeUnit;
import java.net.*;

public class Sender1b {

    public static void main(String[] args) {

        try {
            // Read arguments
            String remoteHost = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            int timeOut = Integer.parseInt(args[3]);

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
            socket.setSoTimeout(timeOut);
            InetAddress host = InetAddress.getByName(remoteHost);

            // Keep track of the transmission time
            double startTime = (double) System.currentTimeMillis();
            double endTime = (double) System.currentTimeMillis();

            // Set max number of retransmission attempt for the last packet
            int maxRetry = 10;
            // Initialize starting index
            int index = 0;
            // Initialize the number of packets lost
            int lostPackets = 0;
            boolean transmission = true;

            while(transmission) {

                Packet pkt = new Packet();
        
                // Sending the packets normally
                if (index != numOfPkts - 1) {
                    pkt.setEoF(false);
                    pkt.setSeqNum(index % 2);
                    for (int j = 0; j < Packet.payloadLength(); j++) {
                        pkt.setPayload(j, data[Packet.payloadLength() * index + j]);
                    }
                    DatagramPacket datagram = new DatagramPacket(pkt.toByteArray(),
                        Packet.length(), host, port);
                    socket.send(datagram);
                }
        
                // Sending the last packet
                else {
                    endTime = (double) System.currentTimeMillis();
                    pkt.setEoF(true);
                    pkt.setSeqNum(index % 2);
                    for (int j = 0; j < lastPktLength; j++) {
                        pkt.setPayload(j, data[Packet.payloadLength() * index + j]);
                    }
                    DatagramPacket datagram = new DatagramPacket(pkt.toByteArray(),
                        Packet.headerLength() + lastPktLength, host, port);
                    socket.send(datagram);
                }


                // After sending a packet we need to wait for an ACK
                byte[] buf = new byte[Ack.length()];
                DatagramPacket dp = new DatagramPacket(buf, Ack.length());
                try {
                    socket.receive(dp);
                    Ack ack = new Ack(dp.getData());
                    
                    // If we are waiting for the last ACK
                    if (index == numOfPkts - 1 ) {
                        // If ACK is correct we are done
                        if (ack.getSeqNum() == (index % 2)) {
                            transmission = false;
                            socket.close();
                            System.out.println(lostPackets + " " + (data.length / 1024) / ((endTime - startTime) / 1000));
                        }
                        // Resend the last packet and decrement the remaining number of attempts
                        else if (maxRetry != 0) {
                            maxRetry -= 1;
                        }
                        // If the retransmission limit is reached we give up and finish
                        else {
                            socket.close();
                            transmission = false;
                        }
                    
                    }

                    // Waiting for ACK normally
                    else {
                        // If the server acknowledges the packet, then send the next one.
                        if (ack.getSeqNum() == (index % 2)) {
                            index++;
                        }
                        // If the server sends ACK for the previous packet then resend the current one.
                        else {
                            throw new SocketTimeoutException();
                        }
                    }
                }
                // In case of a timeot we need to resend the current packet.
                catch (SocketTimeoutException e){
                    lostPackets++;
                }
            }
        }
        catch (IllegalArgumentException e) {
            System.out.println("Invalid arguments");
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        catch (Exception e) {
            System.out.println("An error occured during transmission");
        }

    }
}

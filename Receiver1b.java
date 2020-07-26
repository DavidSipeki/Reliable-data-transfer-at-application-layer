import java.io.*;
import java.net.*;

public class Receiver1b {
    
    public static void main(String[] args) {

        try {
            // Read arguments
            int port = Integer.parseInt(args[0]);
            String fileName = args[1];

            DatagramSocket socket = new DatagramSocket(port); 
            byte[] buf = new byte[Packet.length()];
            FileOutputStream fos = new FileOutputStream(new File(fileName));
                    
            int expectedSeqNum = 0;
            boolean transmission = true;

            while (transmission) {

                DatagramPacket dp = new DatagramPacket(buf, Packet.length()); 
                socket.receive(dp);
                Packet pkt = new Packet(dp.getData());

                // If ACK matches
                if (pkt.getSeqNum() == expectedSeqNum) {
                    // Write payload into file
                    int payloadLength = dp.getLength() - Packet.headerLength();
                    fos.write(pkt.getPayload(payloadLength));

                    // Respond with the sequence number of the received packet
                    Ack ack = new Ack();
                    ack.setSeqNum(expectedSeqNum);
                    DatagramPacket datagram = new DatagramPacket(ack.toByteArray(),
                        Ack.length(), dp.getAddress(), dp.getPort());
                    socket.send(datagram);
                    // Update the expected sequence number
                    expectedSeqNum = (expectedSeqNum + 1) % 2;
                    
                    // If this was the last packet, close connection and file and finish
                    if (pkt.getEoF()) {
                        fos.flush();
                        fos.close();
                        socket.close();
                        transmission = false;
                    }
                }

                // If ACK differs
                else {
                    Ack ack = new Ack();
                    ack.setSeqNum((expectedSeqNum + 1) % 2);
                    DatagramPacket datagram = new DatagramPacket(ack.toByteArray(),
                        Ack.length(), dp.getAddress(), dp.getPort());
                    socket.send(datagram);
                }
            }
        }
        catch (IllegalArgumentException e) {
            System.out.println("Invalid arguments");
        }
        catch (Exception e) {
            System.out.println("An error occured during transmission");
        }
    }
}

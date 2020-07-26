import java.io.*;
import java.lang.Math;
import java.util.concurrent.TimeUnit;
import java.net.*;

public class Sender1a {
    
    public static void main(String[] args) {

        try {
            String remoteHost = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            // Read from file
            RandomAccessFile file = new RandomAccessFile(fileName, "r");
            byte[] data = new byte[(int)file.length()];
            file.readFully(data);
            file.close();

            // Calculate how many packets are needed to transfer the file
            int numOfPkts = (int) Math.ceil((double) data.length / Packet.payloadLength());
            // Calculate the payload of the last packet
            int lastPktLength = data.length - (numOfPkts - 1) * Packet.payloadLength();

            DatagramSocket socket = new DatagramSocket();
            InetAddress host = InetAddress.getByName(remoteHost);
    
            // Construct and send the packets
            for (int i = 0; i < numOfPkts; i++) {
    
                Packet pkt = new Packet();
        
                // Sending the packets normally
                if (i != numOfPkts - 1) {
                    pkt.setEoF(false);
                    for (int j = 0; j < Packet.payloadLength(); j++) {
                        pkt.setPayload(j, data[Packet.payloadLength() * i + j]);
                    }
                    DatagramPacket datagram = new DatagramPacket(pkt.toByteArray(),
                        Packet.length(), host, port);
                    socket.send(datagram);
                }
        
                // Sending the last packet
                else {
                    pkt.setEoF(true);
                    for (int j = 0; j < lastPktLength; j++) {
                        pkt.setPayload(j, data[Packet.payloadLength() * i + j]);
                    }
                    DatagramPacket datagram = new DatagramPacket(pkt.toByteArray(),
                        Packet.headerLength() + lastPktLength, host, port);
                    socket.send(datagram);
                    socket.close();
                }
        
                TimeUnit.MILLISECONDS.sleep(150);
            }
        }
        catch (IllegalArgumentException e) {
            System.out.println("Invalid arguments");
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        catch(IOException e) {
            System.out.println("IO Exception");
        }
        catch(Exception e) {
            System.out.println("An error occured during transmission");
        }

    }
    
}

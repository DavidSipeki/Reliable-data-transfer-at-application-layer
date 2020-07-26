import java.io.*;
import java.net.*;

public class Receiver1a {
    
    public static void main(String[] args) {

        try {
            int port = Integer.parseInt(args[0]);
            String fileName = args[1];
            DatagramSocket socket = new DatagramSocket(port); 
            byte[] buf = new byte[Packet.length()];
            FileOutputStream fos = new FileOutputStream(new File(fileName));
                    
            boolean transmission = true;
            while (transmission) {
                DatagramPacket dp = new DatagramPacket(buf, Packet.length()); 
                socket.receive(dp);
                Packet pkt = new Packet(dp.getData());
                // Get the total length of the packet and substract the length of the header
                int payloadLength = dp.getLength() - Packet.headerLength();
                // Write the payload bytes into file
                fos.write(pkt.getPayload(payloadLength));
                // If the End of File flag is true then close the file, the connection and break the loop
                if (pkt.getEoF()) {
                    fos.flush();
                    fos.close();
                    socket.close();
                    transmission = false;
                }
            }
        }

        catch (FileNotFoundException e) {
            System.out.println("Problem with the file name");
        }
        catch (SocketException e) {
            System.out.println("Invalid port number");
        }
        catch (IllegalArgumentException e) {
            System.out.println("Invalid arguments");
        }
        catch(Exception e) {
            System.out.println("An error occured during transmission");
        }
    }
}

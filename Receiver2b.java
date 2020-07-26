import java.io.*;
import java.net.*;

public class Receiver2b {

    public static int windowSize;
    public static Data[] buffer;
    
    public static void main(String[] args) {

        try {
            // Read arguments
            int port = Integer.parseInt(args[0]);
            String fileName = args[1];
            windowSize = Integer.parseInt(args[2]);
            buffer = new Data[windowSize];

            DatagramSocket socket = new DatagramSocket(port); 
            byte[] buf = new byte[Packet.length()];
            FileOutputStream fos = new FileOutputStream(new File(fileName));
                    
            int expectedSeqNum = 0;
            boolean transmission = true;

            int lastPktSeqNum = -1;

            while (transmission) {

                try {

                    DatagramPacket dp = new DatagramPacket(buf, Packet.length()); 
                    socket.receive(dp);
                    Packet pkt = new Packet(dp.getData());
                    int payloadLength = dp.getLength() - Packet.headerLength();

                    if (pkt.getEoF()) {
                        lastPktSeqNum = pkt.getSeqNum();
                    }

                    // If ACK matches
                    if (pkt.getSeqNum() == expectedSeqNum) {
    
                        // Buffer the data
                        buffer[0] = new Data(pkt.getPayload(payloadLength));
                        // Write everything in the buffer to file
                        writeBufferToFile(fos);
                        // Update the buffer
                        int shiftAmount = getShiftAmount();
                        shiftBuffer(shiftAmount);
    
                        // Respond with the sequence number of the received packet
                        Ack ack = new Ack();
                        ack.setSeqNum(expectedSeqNum);
                        DatagramPacket datagram = new DatagramPacket(ack.toByteArray(),
                            Ack.length(), dp.getAddress(), dp.getPort());
                        socket.send(datagram);
    
                        // Update the expected sequence number
                        expectedSeqNum += shiftAmount;
                    }

                    // If ACK differs, but it can be buffered
                    else if (pkt.getSeqNum() < expectedSeqNum + windowSize 
                        && pkt.getSeqNum() > expectedSeqNum) {
                            // Buffer data
                            buffer [pkt.getSeqNum() - expectedSeqNum] = new Data(pkt.getPayload(payloadLength));
                            // Send ACK
                            Ack ack = new Ack();
                            ack.setSeqNum(pkt.getSeqNum());
                            DatagramPacket datagram = new DatagramPacket(ack.toByteArray(),
                                Ack.length(), dp.getAddress(), dp.getPort());
                            socket.send(datagram);
                    }

                    // If the seq num is in negative window size range we have to send an ACK
                    else if (pkt.getSeqNum() >= expectedSeqNum - windowSize) {
                        new Data(pkt.getPayload(payloadLength));
                        Ack ack = new Ack();
                        ack.setSeqNum(pkt.getSeqNum());
                        DatagramPacket datagram = new DatagramPacket(ack.toByteArray(),
                            Ack.length(), dp.getAddress(), dp.getPort());
                        socket.send(datagram);
                    }

                    // Set a timeout after the last packet has arrived, because we might have more incoming data
                    if (expectedSeqNum == (lastPktSeqNum + 1)) {
                        socket.setSoTimeout(3000);
                    }
                }
                // After timing out, close everything
                catch (SocketTimeoutException e) {
                    fos.flush();
                    fos.close();
                    socket.close();
                    transmission = false;
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

    // Write everything in the buffer array to file
    public static void writeBufferToFile(FileOutputStream fos) {
        for(int i = 0; i < windowSize; i++) {
            try {
                fos.write(buffer[i].getData());
            }
            catch (NullPointerException e) {
                break;
            }
            catch (IOException e) {
                
            }
        }
    }

    // Shift the buffer to the left by shiftAmount
    public static void shiftBuffer(int shiftAmount) {
        for (int i = shiftAmount; i < windowSize; i++) {
            buffer[i - shiftAmount] = buffer[i];
        }

        for (int i = windowSize - shiftAmount; i < windowSize; i++) {
            buffer[i] = null;
        }
    }

    // Calculates how much we can left-shift the buffer
    public static int getShiftAmount() {
        int shiftAmount = 0;
        for(int i = 0; i < windowSize; i++) {
            if (buffer[i] != null) {
                shiftAmount++;
            }
            else {
                break;
            }
        }

        return shiftAmount;
    }
}

// Data is just a byte array
class Data {
    private byte[] data;

    public Data(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
public class Packet {

    private static final int HEADERLENGTH = 5;
    private static final int PAYLOADLENGTH = 1024;

    private byte[] pkt;

    // Constructors
    public Packet() {
        pkt = new byte[HEADERLENGTH + PAYLOADLENGTH];
    }

    public Packet(byte[] pkt) {
        this.pkt = pkt;
    }

    // Static functions
    public static int length() {
        return (HEADERLENGTH + PAYLOADLENGTH);
    }

    public static int payloadLength() {
        return PAYLOADLENGTH;
    }

    public static int headerLength() {
        return HEADERLENGTH;
    }

    // Convert an integer to a 2 byte sequence number
    public void setSeqNum(int i) {
        pkt[2] = (byte) ((i >> 8) & 0xFF);
        pkt[3] = (byte) (i & 0xFF);
    }

    // Converts the sequence number consisting of 2 bytes to an int
    public int getSeqNum() {
        int seqNum = Integer.parseInt(String.format("%8s", Integer.toBinaryString(pkt[2]&0xFF)).replace(' ', '0') +
            String.format("%8s", Integer.toBinaryString(pkt[3] & 0xFF)).replace(' ', '0'), 2);

        return seqNum;
    }

    // Sets the EoF byte to 0 or 1
    public void setEoF(boolean endOfFile) {
        if (!endOfFile) {
            pkt[4] = 0;
        }
        else {
            pkt[4] = 1;
        }
    }

    // Returns true/ false based on the EoF flag
    public boolean getEoF() {
        if (pkt[4] == 0) {
            return false;
        }
        else {
            return true;
        }
    }

    // Returns the first n payload bytes
    public byte[] getPayload(int n) {
        byte[] payload = new byte[n];
        for (int i = 0; i < n; i++) {
            payload[i] = pkt[HEADERLENGTH + i];
        }

        return payload;
    }

    // Sets the payload byte at the given index
    public void setPayload(int index, byte data) {
        pkt[index + HEADERLENGTH] = data;
    }

    public byte[] toByteArray() {
        return pkt;
    }

}

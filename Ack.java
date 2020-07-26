public class Ack {

    private static final int HEADERLENGTH = 4;

    private byte[] ack;

    // Constructors
    public Ack() {
        ack = new byte[HEADERLENGTH];
    }

    public Ack(byte[] ack) {
        this.ack = ack;
    }

    public static int length() {
        return (HEADERLENGTH);
    }

    // Convert an integer to a 2 byte sequence number
    public void setSeqNum(int i) {
        ack[2] = (byte) ((i >> 8) & 0xFF);
        ack[3] = (byte) (i & 0xFF);
    }

    // Converts the sequence number consisting of 2 bytes to an int
    public int getSeqNum() {
        int seqNum = Integer.parseInt(String.format("%8s", Integer.toBinaryString(ack[2]&0xFF)).replace(' ', '0') +
            String.format("%8s", Integer.toBinaryString(ack[3] & 0xFF)).replace(' ', '0'), 2);

        return seqNum;
    }

    public byte[] toByteArray() {
        return ack;
    }
}

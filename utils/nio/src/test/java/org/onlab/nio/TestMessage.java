package org.onlab.nio;

/**
 * Fixed-length message.
 */
public class TestMessage extends AbstractMessage {

    private final byte[] data;

    /**
     * Creates a new message with the specified length.
     *
     * @param length message length
     */
    public TestMessage(int length) {
        this.length = length;
        data = new byte[length];
    }

    /**
     * Creates a new message with the specified data.
     *
     * @param data message data
     */
    TestMessage(byte[] data) {
        this.length = data.length;
        this.data = data;
    }

    /**
     * Gets the backing byte array data.
     *
     * @return backing byte array
     */
    public byte[] data() {
        return data;
    }

}

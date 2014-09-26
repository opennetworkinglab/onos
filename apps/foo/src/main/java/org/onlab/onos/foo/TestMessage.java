package org.onlab.onos.foo;

import org.onlab.nio.AbstractMessage;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Test message for measuring rate and round-trip latency.
 */
public class TestMessage extends AbstractMessage {

    private final byte[] padding;

    private final long requestorTime;
    private final long responderTime;

    /**
     * Creates a new message with the specified data.
     *
     * @param requestorTime requester time
     * @param responderTime responder time
     * @param padding       message padding
     */
    TestMessage(int length, long requestorTime, long responderTime, byte[] padding) {
        this.length = length;
        this.requestorTime = requestorTime;
        this.responderTime = responderTime;
        this.padding = checkNotNull(padding, "Padding cannot be null");
    }

    public long requestorTime() {
        return requestorTime;
    }

    public long responderTime() {
        return responderTime;
    }

    public byte[] padding() {
        return padding;
    }

}

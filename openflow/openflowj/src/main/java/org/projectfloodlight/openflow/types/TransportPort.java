package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Ints;

/**
 * Represents L4 (Transport Layer) port (TCP, UDP, etc.)
 *
 * @author Yotam Harchol (yotam.harchol@bigswitch.com)
 */
public class TransportPort implements OFValueType<TransportPort> {

    static final int LENGTH = 2;
    static final int MAX_PORT = 0xFFFF;
    static final int MIN_PORT = 0;

    private final static int NONE_VAL = 0;
    public final static TransportPort NONE = new TransportPort(NONE_VAL);

    public static final TransportPort NO_MASK = new TransportPort(0xFFFFFFFF);
    public static final TransportPort FULL_MASK = TransportPort.of(0x0);

    private final int port;

    private TransportPort(int port) {
        this.port = port;
    }

    public static TransportPort of(int port) {
        if(port == NONE_VAL)
            return NONE;
        else if (port == NO_MASK.port)
            return NO_MASK;
        else if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Illegal transport layer port number: " + port);
        }
        return new TransportPort(port);
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TransportPort))
            return false;
        TransportPort other = (TransportPort)obj;
        if (other.port != this.port)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 59;
        int result = 1;
        result = prime * result + port;
        return result;
    }

    @Override
    public String toString() {
        return Integer.toString(port);
    }

    public void write2Bytes(ChannelBuffer c) {
        c.writeShort(this.port);
    }

    public static TransportPort read2Bytes(ChannelBuffer c) throws OFParseError {
        return TransportPort.of((c.readUnsignedShort() & 0x0FFFF));
    }

    @Override
    public TransportPort applyMask(TransportPort mask) {
        return TransportPort.of(this.port & mask.port);
    }

    @Override
    public int compareTo(TransportPort o) {
        return Ints.compare(port,  o.port);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort((short) port);
    }

}

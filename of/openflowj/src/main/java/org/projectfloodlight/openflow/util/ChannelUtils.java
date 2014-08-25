package org.projectfloodlight.openflow.util;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.Writeable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Collection of helper functions for reading and writing into ChannelBuffers
 *
 * @author capveg
 */

public class ChannelUtils {
    private static final Logger logger = LoggerFactory.getLogger(ChannelUtils.class);
    public static String readFixedLengthString(ChannelBuffer bb, int length) {
        byte[] dst = new byte[length];
        bb.readBytes(dst, 0, length);
        int validLength = 0;
        for (validLength = 0; validLength < length; validLength++) {
            if (dst[validLength] == 0)
                break;
        }
        return new String(dst, 0, validLength, Charsets.US_ASCII);
    }

    public static void writeFixedLengthString(ChannelBuffer bb, String string,
            int length) {
        int l = string.length();
        if (l > length) {
            throw new IllegalArgumentException("Error writing string: length="
                    + l + " > max Length=" + length);
        }
        bb.writeBytes(string.getBytes(Charsets.US_ASCII));
        if (l < length) {
            bb.writeZero(length - l);
        }
    }

    static public byte[] readBytes(final ChannelBuffer bb, final int length) {
        byte byteArray[] = new byte[length];
        bb.readBytes(byteArray);
        return byteArray;
    }

    static public void writeBytes(final ChannelBuffer bb,
            final byte byteArray[]) {
        bb.writeBytes(byteArray);
    }

    public static <T> List<T> readList(ChannelBuffer bb, int length, OFMessageReader<T> reader) throws OFParseError {
        int end = bb.readerIndex() + length;
        Builder<T> builder = ImmutableList.<T>builder();
        if(logger.isTraceEnabled())
            logger.trace("readList(length={}, reader={})", length, reader.getClass());
        while(bb.readerIndex() < end) {
            T read = reader.readFrom(bb);
            if(logger.isTraceEnabled())
                logger.trace("readList: read={}, left={}", read, end - bb.readerIndex());
            builder.add(read);
        }
        if(bb.readerIndex() != end) {
            throw new IllegalStateException("Overread length: length="+length + " overread by "+ (bb.readerIndex() - end) + " reader: "+reader);
        }
        return builder.build();
    }

    public static void writeList(ChannelBuffer bb, List<? extends Writeable> writeables) {
        for(Writeable w: writeables)
            w.writeTo(bb);
    }
}

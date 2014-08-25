package org.projectfloodlight.openflow.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFVersion;

public class OFErrorCauseDataTest {
    @Test
    public void testEmpty() {
        OFErrorCauseData emptyCause = OFErrorCauseData.of(new byte[] {}, OFVersion.OF_13);
        assertThat(emptyCause.getData(), equalTo(new byte[] {}));
        assertThat(emptyCause.getParsedMessage().isPresent(), equalTo(false));
        assertThat(emptyCause.toString(), Matchers.containsString("unparsed"));
    }

    @Test
    public void testTooShort() {
        OFErrorCauseData emptyCause = OFErrorCauseData.of(new byte[] {0x1, 0x2}, OFVersion.OF_13);
        assertThat(emptyCause.getData(), equalTo(new byte[] {0x1, 0x2}));
        assertThat(emptyCause.getParsedMessage().isPresent(), equalTo(false));
        assertThat(emptyCause.toString(), Matchers.containsString("unparsed"));
        assertThat(emptyCause.toString(), Matchers.containsString("01 02"));
    }

    byte[] truncatedFlowAddd = new byte[] {
            0x04, 0x0e, // version, type
            0x00, (byte) 0x80, // length
            0x12, 0x34, 0x56, 0x78, // xid
            (byte) 0xfe, (byte) 0xdc , (byte) 0xba, (byte) 0x98, 0x76, 0x54, 0x32, 0x10, // cookie
            (byte) 0xff, 0x00, (byte) 0xff, 0x00, (byte) 0xff, 0x00, (byte) 0xff, 0x00, // cookie_mask
            0x03 // table_id
            // rest truncated
    };

    @Test
    public void testTruncated() {
        OFErrorCauseData emptyCause = OFErrorCauseData.of(truncatedFlowAddd, OFVersion.OF_13);
        assertThat(emptyCause.getData(), equalTo(truncatedFlowAddd));
        assertThat(emptyCause.getParsedMessage().isPresent(), equalTo(false));
        assertThat(emptyCause.toString(), Matchers.containsString("unparsed"));
        assertThat(emptyCause.toString(), Matchers.containsString("04 0e 00 80"));
    }

    @Test
    public void testFlowAdd() {
        OFFlowAdd flowAdd = OFFactories.getFactory(OFVersion.OF_13).buildFlowAdd()
        .setXid(0x12345678)
        .setCookie(U64.parseHex("FEDCBA9876543210"))
        .setCookieMask(U64.parseHex("FF00FF00FF00FF00"))
        .setTableId(TableId.of(3))
        .setIdleTimeout(5)
        .setHardTimeout(10)
        .setPriority(6000)
        .build();

        ChannelBuffer bb = ChannelBuffers.dynamicBuffer();
        flowAdd.writeTo(bb);
        byte[] flowAddBytes = new byte[bb.readableBytes()];
        bb.readBytes(flowAddBytes);

        OFErrorCauseData emptyCause = OFErrorCauseData.of(flowAddBytes, OFVersion.OF_13);
        assertThat(emptyCause.getData(), equalTo(flowAddBytes));
        assertThat(emptyCause.getParsedMessage().isPresent(), equalTo(true));
        assertThat(emptyCause.toString(), Matchers.containsString("OFFlowAdd"));
        assertThat(emptyCause.toString(), Matchers.containsString("idleTimeout=5"));
    }
}

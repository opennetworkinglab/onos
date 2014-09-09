package org.onlab.onos.net.link;

import org.junit.Test;
import org.onlab.onos.event.AbstractEventTest;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.provider.ProviderId;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Tests of the device event.
 */
public class LinkEventTest extends AbstractEventTest {

    private Link createLink() {
        return new DefaultLink(new ProviderId("foo"),
                               new ConnectPoint(deviceId("of:foo"), portNumber(1)),
                               new ConnectPoint(deviceId("of:bar"), portNumber(2)),
                               Link.Type.INDIRECT);
    }

    @Test
    public void withTime() {
        Link link = createLink();
        LinkEvent event = new LinkEvent(LinkEvent.Type.LINK_ADDED, link, 123L);
        validateEvent(event, LinkEvent.Type.LINK_ADDED, link, 123L);
    }

    @Test
    public void withoutTime() {
        Link link = createLink();
        long before = System.currentTimeMillis();
        LinkEvent event = new LinkEvent(LinkEvent.Type.LINK_ADDED, link);
        long after = System.currentTimeMillis();
        validateEvent(event, LinkEvent.Type.LINK_ADDED, link, before, after);
    }

}

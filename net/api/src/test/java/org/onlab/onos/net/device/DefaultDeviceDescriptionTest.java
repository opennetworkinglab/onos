package org.onlab.onos.net.device;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.net.Device.Type.SWITCH;

/**
 * Test of the default device description.
 */
public class DefaultDeviceDescriptionTest {

    private static final URI DURI = URI.create("of:foo");
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW = "3.9.1";
    private static final String SN = "43311-12345";


    @Test
    public void basics() {
        DeviceDescription device =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN);
        assertEquals("incorrect uri", DURI, device.deviceURI());
        assertEquals("incorrect type", SWITCH, device.type());
        assertEquals("incorrect manufacturer", MFR, device.manufacturer());
        assertEquals("incorrect hw", HW, device.hwVersion());
        assertEquals("incorrect sw", SW, device.swVersion());
        assertEquals("incorrect serial", SN, device.serialNumber());
        assertTrue("incorrect toString", device.toString().contains("uri=of:foo"));
    }

}

package org.onosproject.incubator.net.resource.label;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;

import com.google.common.testing.EqualsTester;

/**
 * Tests of default label resource.
 */
public class DefaultLabelResourceTest extends AbstractEventTest {

    @Test
    public void testEquality() {
        String deviceId1 = "of:001";
        String deviceId2 = "of:002";
        long labelResourceId1 = 100;
        long labelResourceId2 = 200;
        DefaultLabelResource h1 = new DefaultLabelResource(deviceId1,
                                                           labelResourceId1);
        DefaultLabelResource h2 = new DefaultLabelResource(deviceId1,
                                                           labelResourceId1);
        DefaultLabelResource h3 = new DefaultLabelResource(deviceId2,
                                                           labelResourceId2);
        DefaultLabelResource h4 = new DefaultLabelResource(deviceId2,
                                                           labelResourceId2);

        new EqualsTester().addEqualityGroup(h1, h2).addEqualityGroup(h3, h4)
                .testEquals();
    }
}

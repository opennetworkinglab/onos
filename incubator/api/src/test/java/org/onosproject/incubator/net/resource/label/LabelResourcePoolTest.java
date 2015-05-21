package org.onosproject.incubator.net.resource.label;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;

import com.google.common.testing.EqualsTester;

/**
 * Tests of the label resource pool.
 */
public class LabelResourcePoolTest extends AbstractEventTest {

    @Test
    public void testEquality() {
        LabelResourcePool h1 = new LabelResourcePool("of:001", 0, 100);
        LabelResourcePool h2 = new LabelResourcePool("of:001", 0, 100);
        LabelResourcePool h3 = new LabelResourcePool("of:002", 0, 100);
        LabelResourcePool h4 = new LabelResourcePool("of:002", 0, 100);
        new EqualsTester().addEqualityGroup(h1, h2).addEqualityGroup(h3, h4)
                .testEquals();
    }

}

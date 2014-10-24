package org.onlab.onos.net.intent;

import org.junit.Test;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.TestApplicationId;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.onos.net.NetTestTools.hid;

/**
 * Unit tests for the HostToHostIntent class.
 */
public class TestHostToHostIntent {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    private HostToHostIntent makeHostToHost(HostId one, HostId two) {
        return new HostToHostIntent(APPID, one, two, selector, treatment);
    }

    /**
     * Tests the equals() method where two HostToHostIntents have references
     * to the same hosts. These should compare equal.
     */
    @Test
    public void testSameEquals() {

        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(one, two);

        assertThat(i1, is(equalTo(i2)));
    }

    /**
     * Tests the equals() method where two HostToHostIntents have references
     * to different Hosts. These should compare not equal.
     */
    @Test
    public void testSameEquals2() {
        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(two, one);

        assertThat(i1, is(equalTo(i2)));
    }

    /**
     * Tests that the hashCode() values for two equivalent HostToHostIntent
     * objects are the same.
     */
    @Test
    public void testHashCodeEquals() {
        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(one, two);

        assertThat(i1.hashCode(), is(equalTo(i2.hashCode())));
    }

    /**
     * Tests that the hashCode() values for two distinct LinkCollectionIntent
     * objects are different.
     */
    @Test
    public void testHashCodeEquals2() {
        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(two, one);

        assertThat(i1.hashCode(), is(equalTo(i2.hashCode())));
    }

    /**
     * Tests that the hashCode() values for two distinct LinkCollectionIntent
     * objects are different.
     */
    @Test
    public void testHashCodeDifferent() {
        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostId three = hid("00:00:00:00:00:32/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(one, three);

        assertThat(i1.hashCode(), is(not(equalTo(i2.hashCode()))));
    }

    /**
     * Checks that the HostToHostIntent class is immutable.
     */
    @Test
    public void checkImmutability() {
        assertThatClassIsImmutable(HostToHostIntent.class);
    }
}

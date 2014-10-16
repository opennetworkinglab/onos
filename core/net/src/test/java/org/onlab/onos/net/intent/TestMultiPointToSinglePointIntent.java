package org.onlab.onos.net.intent;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.onlab.onos.net.NetTestTools.connectPoint;

/**
 * Unit tests for the MultiPointToSinglePointIntent class.
 */
public class TestMultiPointToSinglePointIntent {

    private ConnectPoint point1 = connectPoint("dev1", 1);
    private ConnectPoint point2 = connectPoint("dev2", 1);
    private ConnectPoint point3 = connectPoint("dev3", 1);

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    Set<ConnectPoint> ingress1;
    Set<ConnectPoint> ingress2;

    /**
     * Creates a MultiPointToSinglePointIntent object.
     *
     * @param id identifier to use for the new intent
     * @param ingress set of ingress points
     * @param egress egress point
     * @return MultiPointToSinglePoint intent
     */
    private MultiPointToSinglePointIntent makeIntent(long id,
                       Set<ConnectPoint> ingress,
                       ConnectPoint egress) {
        return new MultiPointToSinglePointIntent(new IntentId(id),
                                                 selector,
                                                 treatment,
                                                 ingress,
                                                 egress);
    }

    /**
     * Initializes the ingress sets.
     */
    @Before
    public void setup() {
        ingress1 = new HashSet<>();
        ingress2 = new HashSet<>();
    }

    /**
     * Tests the equals() method where two MultiPointToSinglePoint have references
     * to the same Links in different orders. These should compare equal.
     */
    @Test
    public void testSameEquals() {

        Set<ConnectPoint> ingress1 = new HashSet<>();
        ingress1.add(point2);
        ingress1.add(point3);

        Set<ConnectPoint> ingress2 = new HashSet<>();
        ingress2.add(point3);
        ingress2.add(point2);

        Intent i1 = makeIntent(12, ingress1, point1);
        Intent i2 = makeIntent(12, ingress2, point1);

        assertThat(i1, is(equalTo(i2)));
    }

    /**
     * Tests the equals() method where two MultiPointToSinglePoint have references
     * to different Links. These should compare not equal.
     */
    @Test
    public void testLinksDifferentEquals() {
        ingress1.add(point3);

        ingress2.add(point3);
        ingress2.add(point2);

        Intent i1 = makeIntent(12, ingress1, point1);
        Intent i2 = makeIntent(12, ingress2, point1);

        assertThat(i1, is(not(equalTo(i2))));
    }

    /**
     * Tests the equals() method where two MultiPointToSinglePoint have different
     * ids. These should compare not equal.
     */
    @Test
    public void testBaseDifferentEquals() {
        ingress1.add(point3);
        ingress2.add(point3);

        Intent i1 = makeIntent(12, ingress1, point1);
        Intent i2 = makeIntent(11, ingress2, point1);

        assertThat(i1, is(not(equalTo(i2))));
    }

    /**
     * Tests that the hashCode() values for two equivalent MultiPointToSinglePoint
     * objects are the same.
     */
    @Test
    public void testHashCodeEquals() {
        ingress1.add(point2);
        ingress1.add(point3);

        ingress2.add(point3);
        ingress2.add(point2);

        Intent i1 = makeIntent(12, ingress1, point1);
        Intent i2 = makeIntent(12, ingress2, point1);

        assertThat(i1.hashCode(), is(equalTo(i2.hashCode())));
    }

    /**
     * Tests that the hashCode() values for two distinct MultiPointToSinglePoint
     * objects are different.
     */
    @Test
    public void testHashCodeDifferent() {
        ingress1.add(point2);

        ingress2.add(point3);
        ingress2.add(point2);

        Intent i1 = makeIntent(12, ingress1, point1);
        Intent i2 = makeIntent(12, ingress2, point1);


        assertThat(i1.hashCode(), is(not(equalTo(i2.hashCode()))));
    }

    /**
     * Checks that the MultiPointToSinglePointIntent class is immutable.
     */
    @Test
    public void checkImmutability() {
        ImmutableClassChecker.
                assertThatClassIsImmutable(MultiPointToSinglePointIntent.class);
    }
}

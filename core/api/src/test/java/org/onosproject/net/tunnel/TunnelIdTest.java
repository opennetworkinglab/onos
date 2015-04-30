package org.onosproject.net.tunnel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for tunnel id class.
 */
public class TunnelIdTest {

    final TunnelId tunnelId1 = TunnelId.valueOf(1);
    final TunnelId sameAstunnelId1 = TunnelId.valueOf(1);
    final TunnelId tunnelId2 = TunnelId.valueOf(2);

    /**
     * Checks that the TunnelId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(TunnelId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(tunnelId1, sameAstunnelId1)
                .addEqualityGroup(tunnelId2)
                .testEquals();
    }

    /**
     * Checks the construction of a FlowId object.
     */
    @Test
    public void testConstruction() {
        final long tunnelIdValue = 7777L;
        final TunnelId tunnelId = TunnelId.valueOf(tunnelIdValue);
        assertThat(tunnelId, is(notNullValue()));
        assertThat(tunnelId.id(), is(tunnelIdValue));
    }
}

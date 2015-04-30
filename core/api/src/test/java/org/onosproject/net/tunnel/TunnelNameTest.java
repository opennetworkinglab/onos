package org.onosproject.net.tunnel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for tunnel name class.
 */
public class TunnelNameTest {
    final TunnelName name1 = TunnelName.tunnelName("name1");
    final TunnelName sameAsName1 = TunnelName.tunnelName("name1");
    final TunnelName name2 = TunnelName.tunnelName("name2");

    /**
     * Checks that the TunnelName class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(TunnelName.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(name1, sameAsName1)
                .addEqualityGroup(name2).testEquals();
    }

    /**
     * Checks the construction of a OpenFlowGroupId object.
     */
    @Test
    public void testConstruction() {
        final String nameValue = "name3";
        final TunnelName name = TunnelName.tunnelName(nameValue);
        assertThat(name, is(notNullValue()));
        assertThat(name.value(), is(nameValue));
    }

}

package org.onosproject.net.config.basics;

import java.time.Duration;

import org.junit.Test;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.config.ConfigApplyDelegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for the BasicLinkConfig class.
 */

public class BasicLinkConfigTest {
    private static final long BANDWIDTH = 11;
    private static final double METRIC = 3.0;
    private static final Duration LATENCY =  Duration.ofNanos(5555);

    /**
     * Tests construction, setters and getters of a BasicLinkConfig object.
     */
    @Test
    public void testConstruction() {
        BasicLinkConfig config = new BasicLinkConfig();
        ConfigApplyDelegate delegate = configApply -> { };
        ObjectMapper mapper = new ObjectMapper();
        LinkKey linkKey = LinkKey.linkKey(
                NetTestTools.connectPoint("device1", 1),
                NetTestTools.connectPoint("device2", 2));

        config.init(linkKey, "KEY", JsonNodeFactory.instance.objectNode(), mapper, delegate);


        config.bandwidth(BANDWIDTH)
              .isDurable(FALSE)
              .metric(METRIC)
              .type(Link.Type.DIRECT)
              .latency(LATENCY);

        assertThat(config.bandwidth(), is(BANDWIDTH));
        assertThat(config.isDurable(), is(FALSE));
        assertThat(config.metric(), is(METRIC));
        assertThat(config.type(), is(Link.Type.DIRECT));
        assertThat(config.latency(), is(LATENCY));
        assertThat(config.isValid(), is(true));
    }
}

package org.onlab.onos.net.intent.impl;

import org.hamcrest.Matchers;
//import org.junit.Test;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.TestApplicationId;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentTestsMocks;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.intent.PointToPointIntent;

import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onlab.onos.net.NetTestTools.connectPoint;
import static org.onlab.onos.net.intent.LinksHaveEntryWithSourceDestinationPairMatcher.linksHasPath;

/**
 * Unit tests for the HostToHost intent compiler.
 */
public class TestPointToPointIntentCompiler {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    /**
     * Creates a PointToPoint intent based on ingress and egress device Ids.
     *
     * @param ingressIdString string for id of ingress device
     * @param egressIdString  string for id of egress device
     * @return PointToPointIntent for the two devices
     */
    private PointToPointIntent makeIntent(String ingressIdString,
                                          String egressIdString) {
        return new PointToPointIntent(APPID, selector, treatment,
                                      connectPoint(ingressIdString, 1),
                                      connectPoint(egressIdString, 1));
    }

    /**
     * Creates a compiler for HostToHost intents.
     *
     * @param hops string array describing the path hops to use when compiling
     * @return HostToHost intent compiler
     */
    private PointToPointIntentCompiler makeCompiler(String[] hops) {
        PointToPointIntentCompiler compiler =
                new PointToPointIntentCompiler();
        compiler.pathService = new IntentTestsMocks.MockPathService(hops);
        return compiler;
    }


    /**
     * Tests a pair of devices in an 8 hop path, forward direction.
     */
//    @Test
    public void testForwardPathCompilation() {

        PointToPointIntent intent = makeIntent("d1", "d8");
        assertThat(intent, is(notNullValue()));

        String[] hops = {"d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8"};
        PointToPointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent forwardResultIntent = result.get(0);
        assertThat(forwardResultIntent instanceof PathIntent, is(true));

        if (forwardResultIntent instanceof PathIntent) {
            PathIntent forwardPathIntent = (PathIntent) forwardResultIntent;
            // 7 links for the hops, plus one default lnk on ingress and egress
            assertThat(forwardPathIntent.path().links(), hasSize(hops.length + 1));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d1", "d2"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d2", "d3"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d3", "d4"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d4", "d5"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d5", "d6"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d6", "d7"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d7", "d8"));
        }
    }

    /**
     * Tests a pair of devices in an 8 hop path, forward direction.
     */
//    @Test
    public void testReversePathCompilation() {

        PointToPointIntent intent = makeIntent("d8", "d1");
        assertThat(intent, is(notNullValue()));

        String[] hops = {"d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8"};
        PointToPointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent reverseResultIntent = result.get(0);
        assertThat(reverseResultIntent instanceof PathIntent, is(true));

        if (reverseResultIntent instanceof PathIntent) {
            PathIntent reversePathIntent = (PathIntent) reverseResultIntent;
            assertThat(reversePathIntent.path().links(), hasSize(hops.length + 1));
            assertThat(reversePathIntent.path().links(), linksHasPath("d2", "d1"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d3", "d2"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d4", "d3"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d5", "d4"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d6", "d5"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d7", "d6"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d8", "d7"));
        }
    }
}

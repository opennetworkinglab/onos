package org.onlab.onos.net.intent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.instructions.Instruction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestLinkCollectionIntent {

    private static class MockSelector implements TrafficSelector {
        @Override
        public Set<Criterion> criteria() {
            return new HashSet<Criterion>();
        }
    }

    private static class MockTreatment implements TrafficTreatment {
        @Override
        public List<Instruction> instructions() {
            return new ArrayList<>();
        }
    }

    @Test
    public void testComparison() {
        TrafficSelector selector = new MockSelector();
        TrafficTreatment treatment = new MockTreatment();
        Set<Link> links = new HashSet<>();
        LinkCollectionIntent i1 = new LinkCollectionIntent(new IntentId(12),
                selector, treatment, links);
        LinkCollectionIntent i2 = new LinkCollectionIntent(new IntentId(12),
                selector, treatment, links);

        assertThat(i1.equals(i2), is(true));
    }

}

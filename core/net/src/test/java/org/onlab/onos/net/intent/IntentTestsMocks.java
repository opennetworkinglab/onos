package org.onlab.onos.net.intent;

import static org.onlab.onos.net.NetTestTools.createPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.criteria.Criterion.Type;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.PathService;

/**
 * Common mocks used by the intent framework tests.
 */
public class IntentTestsMocks {
    /**
     * Mock traffic selector class used for satisfying API requirements.
     */
    public static class MockSelector implements TrafficSelector {
        @Override
        public Set<Criterion> criteria() {
            return new HashSet<>();
        }

        @Override
        public Criterion getCriterion(Type type) {
            return null;
        }
    }

    /**
     * Mock traffic treatment class used for satisfying API requirements.
     */
    public static class MockTreatment implements TrafficTreatment {
        @Override
        public List<Instruction> instructions() {
            return new ArrayList<>();
        }
    }

    /**
     * Mock path service for creating paths within the test.
     */
    public static class MockPathService implements PathService {

        final String[] pathHops;
        final String[] reversePathHops;

        /**
         * Constructor that provides a set of hops to mock.
         *
         * @param pathHops path hops to mock
         */
        public MockPathService(String[] pathHops) {
            this.pathHops = pathHops;
            String[] reversed = pathHops.clone();
            Collections.reverse(Arrays.asList(reversed));
            reversePathHops = reversed;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst) {
            Set<Path> result = new HashSet<>();

            String[] allHops = new String[pathHops.length];

            if (src.toString().endsWith(pathHops[0])) {
                System.arraycopy(pathHops, 0, allHops, 0, pathHops.length);
            } else {
                System.arraycopy(reversePathHops, 0, allHops, 0, pathHops.length);
            }

            result.add(createPath(allHops));
            return result;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight) {
            return getPaths(src, dst);
        }
    }
}

package org.onlab.onos.net.flow;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.onlab.onos.net.flow.criteria.Criterion;
import org.slf4j.Logger;

public final class DefaultTrafficSelector implements TrafficSelector {

    private final List<Criterion> selector;

    private DefaultTrafficSelector(List<Criterion> selector) {
        this.selector = Collections.unmodifiableList(selector);
    }

    @Override
    public List<Criterion> criteria() {
        return selector;
    }

    public static class Builder implements TrafficSelector.Builder {

        private final Logger log = getLogger(getClass());

        private final List<Criterion> selector = new LinkedList<>();

        @Override
        public TrafficSelector.Builder add(Criterion criterion) {
            selector.add(criterion);
            return this;
        }

        @Override
        public TrafficSelector build() {
            return new DefaultTrafficSelector(selector);
        }

    }

}

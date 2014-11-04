/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of end-station to end-station bidirectional connectivity.
 */
public final class HostToHostIntent extends ConnectivityIntent {

    private final HostId one;
    private final HostId two;

    /**
     * Creates a new host-to-host intent with the supplied host pair.
     *
     * @param appId     application identifier
     * @param one       first host
     * @param two       second host
     * @param selector  action
     * @param treatment ingress port
     * @throws NullPointerException if {@code one} or {@code two} is null.
     */
    public HostToHostIntent(ApplicationId appId, HostId one, HostId two,
                            TrafficSelector selector,
                            TrafficTreatment treatment) {
        this(appId, one, two, selector, treatment, null);
    }

    /**
     * Creates a new host-to-host intent with the supplied host pair.
     *
     * @param appId       application identifier
     * @param one         first host
     * @param two         second host
     * @param selector    action
     * @param treatment   ingress port
     * @param constraints optional prioritized list of path selection constraints
     * @throws NullPointerException if {@code one} or {@code two} is null.
     */
    public HostToHostIntent(ApplicationId appId, HostId one, HostId two,
                            TrafficSelector selector,
                            TrafficTreatment treatment,
                            List<Constraint> constraints) {
        super(id(HostToHostIntent.class, min(one, two), max(one, two),
                 selector, treatment, constraints),
              appId, null, selector, treatment, constraints);
        this.one = checkNotNull(one);
        this.two = checkNotNull(two);

    }

    private static HostId min(HostId one, HostId two) {
        return one.hashCode() < two.hashCode() ? one : two;
    }

    private static HostId max(HostId one, HostId two) {
        return one.hashCode() >= two.hashCode() ? one : two;
    }

    /**
     * Returns identifier of the first host.
     *
     * @return first host identifier
     */
    public HostId one() {
        return one;
    }

    /**
     * Returns identifier of the second host.
     *
     * @return second host identifier
     */
    public HostId two() {
        return two;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("one", one)
                .add("two", two)
                .toString();
    }

}

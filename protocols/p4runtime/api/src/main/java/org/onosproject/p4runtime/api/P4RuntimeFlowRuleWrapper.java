/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.p4runtime.api;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.pi.runtime.PiTableEntry;

/**
 * A wrapper for a ONOS flow rule installed on a P4Runtime device.
 */
@Beta
public final class P4RuntimeFlowRuleWrapper {

    private final FlowRule rule;
    private final PiTableEntry piTableEntry;
    private final long installedOnMillis;

    /**
     * Creates a new flow rule wrapper.
     *
     * @param rule              a flow rule
     * @param piTableEntry      PI table entry
     * @param installedOnMillis the time (in milliseconds, since January 1, 1970 UTC) when the flow rule was installed
     *                          on the device
     */
    public P4RuntimeFlowRuleWrapper(FlowRule rule, PiTableEntry piTableEntry, long installedOnMillis) {
        this.rule = rule;
        this.piTableEntry = piTableEntry;
        this.installedOnMillis = installedOnMillis;
    }

    /**
     * Returns the flow rule contained by this wrapper.
     *
     * @return a flow rule
     */
    public FlowRule rule() {
        return rule;
    }

    /**
     * Returns the PI table entry defined by this wrapper.
     *
     * @return table entry
     */
    public PiTableEntry piTableEntry() {
        return piTableEntry;
    }

    /**
     * Return the number of seconds since when this flow rule was installed on the device.
     *
     * @return an integer value
     */
    public long lifeInSeconds() {
        return (System.currentTimeMillis() - installedOnMillis) / 1000;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rule, installedOnMillis);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4RuntimeFlowRuleWrapper other = (P4RuntimeFlowRuleWrapper) obj;
        return Objects.equal(this.rule, other.rule)
                && Objects.equal(this.installedOnMillis, other.installedOnMillis);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("rule", rule)
                .add("installedOnMillis", installedOnMillis)
                .toString();
    }
}

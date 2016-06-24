/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.api.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.flow.FlowRule;

/**
 * A wrapper for a ONOS flow rule installed on a BMv2 device.
 */
@Beta
public final class Bmv2FlowRuleWrapper {

    private final FlowRule rule;
    private final long entryId;
    private final long installedOnMillis;

    /**
     * Creates a new flow rule wrapper.
     *
     * @param rule              a flow rule
     * @param entryId           a BMv2 table entry ID
     * @param installedOnMillis the time (in milliseconds, since January 1, 1970 UTC) when the flow rule was installed
     *                          on the device
     */
    public Bmv2FlowRuleWrapper(FlowRule rule, long entryId, long installedOnMillis) {
        this.rule = rule;
        this.entryId = entryId;
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
     * Return the number of seconds since when this flow rule was installed on the device.
     *
     * @return an integer value
     */
    public long lifeInSeconds() {
        return (System.currentTimeMillis() - installedOnMillis) / 1000;
    }

    /**
     * Returns the the time (in milliseconds, since January 1, 1970 UTC) when the flow rule was installed on
     * the device.
     *
     * @return a long value
     */
    public long installedOnMillis() {
        return installedOnMillis;
    }

    /**
     * Returns the BMv2 entry ID of this flow rule.
     *
     * @return a long value
     */
    public long entryId() {
        return entryId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rule, entryId, installedOnMillis);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2FlowRuleWrapper other = (Bmv2FlowRuleWrapper) obj;
        return Objects.equal(this.rule, other.rule)
                && Objects.equal(this.entryId, other.entryId)
                && Objects.equal(this.installedOnMillis, other.installedOnMillis);
    }

    @Override
    public String toString() {
        return installedOnMillis + "-" + rule.hashCode();
    }
}

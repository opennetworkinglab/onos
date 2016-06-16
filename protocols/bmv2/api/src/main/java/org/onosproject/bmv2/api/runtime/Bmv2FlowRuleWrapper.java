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

import java.util.Date;

/**
 * A wrapper for a ONOS flow rule installed on a BMv2 device.
 */
@Beta
public final class Bmv2FlowRuleWrapper {

    private final FlowRule rule;
    private final long entryId;
    private final Date creationDate;

    /**
     * Creates a new flow rule wrapper.
     *
     * @param rule         a flow rule
     * @param entryId      a BMv2 table entry ID
     * @param creationDate the creation date of the flow rule
     */
    public Bmv2FlowRuleWrapper(FlowRule rule, long entryId, Date creationDate) {
        this.rule = rule;
        this.entryId = entryId;
        this.creationDate = new Date();
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
     * Return the seconds since when this flow rule was installed on the device.
     *
     * @return an integer value
     */
    public long lifeInSeconds() {
        return (new Date().getTime() - creationDate.getTime()) / 1000;
    }

    /**
     * Returns the creation date of this flow rule.
     *
     * @return a date
     */
    public Date creationDate() {
        return creationDate;
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
        return Objects.hashCode(rule, entryId, creationDate);
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
                && Objects.equal(this.creationDate, other.creationDate);
    }

    @Override
    public String toString() {
        return creationDate + "-" + rule.hashCode();
    }
}

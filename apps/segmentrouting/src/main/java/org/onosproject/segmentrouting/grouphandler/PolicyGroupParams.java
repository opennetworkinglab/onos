/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.segmentrouting.grouphandler;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

import org.onosproject.net.PortNumber;

/**
 * Representation of parameters used to create policy based groups.
 */
public class PolicyGroupParams {
    private final List<PortNumber> ports;
    private final List<Integer> labelStack;

    /**
     * Constructor.
     *
     * @param labelStack mpls label stack to be applied on the ports
     * @param ports ports to be part of the policy group
     */
    public PolicyGroupParams(List<Integer> labelStack,
                             List<PortNumber> ports) {
        this.ports = checkNotNull(ports);
        this.labelStack = checkNotNull(labelStack);
    }

    /**
     * Returns the ports associated with the policy group params.
     *
     * @return list of port numbers
     */
    public List<PortNumber> getPorts() {
        return ports;
    }

    /**
     * Returns the label stack associated with the policy group params.
     *
     * @return list of integers
     */
    public List<Integer> getLabelStack() {
        return labelStack;
    }

    @Override
    public int hashCode() {
        int result = 17;
        int combinedHash = 0;
        for (PortNumber port:ports) {
            combinedHash = combinedHash + port.hashCode();
        }
        combinedHash = combinedHash + Objects.hash(labelStack);
        result = 31 * result + combinedHash;

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PolicyGroupParams) {
            PolicyGroupParams that = (PolicyGroupParams) obj;
            boolean result = this.labelStack.equals(that.labelStack);
            result = result &&
                    this.ports.containsAll(that.ports) &&
                    that.ports.containsAll(this.ports);
            return result;
        }

        return false;
    }
}

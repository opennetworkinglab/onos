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
package org.onosproject.net.flow;

import com.google.common.base.Objects;

/**
 * Representation of a Flow ID.
 */
public final class FlowId {

    private final long flowid;

    private FlowId(long id) {
        this.flowid = id;
    }

    public static FlowId valueOf(long id) {
        return new FlowId(id);
    }

    public long value() {
        return flowid;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass()  == this.getClass()) {
            FlowId that = (FlowId) obj;
            return Objects.equal(this.flowid, that.flowid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.flowid);
    }
}

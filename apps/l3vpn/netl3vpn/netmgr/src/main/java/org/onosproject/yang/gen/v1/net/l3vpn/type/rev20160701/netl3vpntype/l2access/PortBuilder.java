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

package org.onosproject.yang.gen.v1.net.l3vpn.type.rev20160701.netl3vpntype.l2access;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * Represents the builder implementation of port.
 */
public class PortBuilder implements Port.PortBuilder {

    private String ltpId;

    @Override
    public String ltpId() {
        return ltpId;
    }

    @Override
    public PortBuilder ltpId(String ltpId) {
        this.ltpId = ltpId;
        return this;
    }

    @Override
    public Port build() {
        return new PortImpl(this);
    }

    /**
     * Creates an instance of portBuilder.
     */
    public PortBuilder() {
    }


    /**
     * Represents the implementation of port.
     */
    public final class PortImpl implements Port {

        private String ltpId;

        @Override
        public String ltpId() {
            return ltpId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ltpId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PortImpl) {
                PortImpl other = (PortImpl) obj;
                return
                     Objects.equals(ltpId, other.ltpId);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("ltpId", ltpId)
                .toString();
        }

        /**
         * Creates an instance of portImpl.
         *
         * @param builderObject builder object of port
         */
        public PortImpl(PortBuilder builderObject) {
            this.ltpId = builderObject.ltpId();
        }
    }
}
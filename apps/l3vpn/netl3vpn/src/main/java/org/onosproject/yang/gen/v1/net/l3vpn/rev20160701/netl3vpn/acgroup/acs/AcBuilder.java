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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.ac.L2Access;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.ac.L3Access;

/**
 * Represents the builder implementation of ac.
 */
public class AcBuilder implements Ac.AcBuilder {

    private String id;
    private String neId;
    private L2Access l2Access;
    private L3Access l3Access;

    @Override
    public String id() {
        return id;
    }

    @Override
    public String neId() {
        return neId;
    }

    @Override
    public L2Access l2Access() {
        return l2Access;
    }

    @Override
    public L3Access l3Access() {
        return l3Access;
    }

    @Override
    public AcBuilder id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public AcBuilder neId(String neId) {
        this.neId = neId;
        return this;
    }

    @Override
    public AcBuilder l2Access(L2Access l2Access) {
        this.l2Access = l2Access;
        return this;
    }

    @Override
    public AcBuilder l3Access(L3Access l3Access) {
        this.l3Access = l3Access;
        return this;
    }

    @Override
    public Ac build() {
        return new AcImpl(this);
    }

    /**
     * Creates an instance of acBuilder.
     */
    public AcBuilder() {
    }


    /**
     * Represents the implementation of ac.
     */
    public final class AcImpl implements Ac {

        private String id;
        private String neId;
        private L2Access l2Access;
        private L3Access l3Access;

        @Override
        public String id() {
            return id;
        }

        @Override
        public String neId() {
            return neId;
        }

        @Override
        public L2Access l2Access() {
            return l2Access;
        }

        @Override
        public L3Access l3Access() {
            return l3Access;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, neId, l2Access, l3Access);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof AcImpl) {
                AcImpl other = (AcImpl) obj;
                return
                     Objects.equals(id, other.id) &&
                     Objects.equals(neId, other.neId) &&
                     Objects.equals(l2Access, other.l2Access) &&
                     Objects.equals(l3Access, other.l3Access);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .add("neId", neId)
                .add("l2Access", l2Access)
                .add("l3Access", l3Access)
                .toString();
        }

        /**
         * Creates an instance of acImpl.
         *
         * @param builderObject builder object of ac
         */
        public AcImpl(AcBuilder builderObject) {
            this.id = builderObject.id();
            this.neId = builderObject.neId();
            this.l2Access = builderObject.l2Access();
            this.l3Access = builderObject.l3Access();
        }
    }
}
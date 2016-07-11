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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf
            .ImportRoutes;
import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.BgpcommPrefixType;

/**
 * Represents the builder implementation of bgpVrfAf.
 */
public class BgpVrfAfBuilder implements BgpVrfAf.BgpVrfAfBuilder {

    private BgpcommPrefixType afType;
    private ImportRoutes importRoutes;

    @Override
    public BgpcommPrefixType afType() {
        return afType;
    }

    @Override
    public ImportRoutes importRoutes() {
        return importRoutes;
    }

    @Override
    public BgpVrfAfBuilder afType(BgpcommPrefixType afType) {
        this.afType = afType;
        return this;
    }

    @Override
    public BgpVrfAfBuilder importRoutes(ImportRoutes importRoutes) {
        this.importRoutes = importRoutes;
        return this;
    }

    @Override
    public BgpVrfAf build() {
        return new BgpVrfAfImpl(this);
    }

    /**
     * Creates an instance of bgpVrfAfBuilder.
     */
    public BgpVrfAfBuilder() {
    }


    /**
     * Represents the implementation of bgpVrfAf.
     */
    public final class BgpVrfAfImpl implements BgpVrfAf {

        private BgpcommPrefixType afType;
        private ImportRoutes importRoutes;

        @Override
        public BgpcommPrefixType afType() {
            return afType;
        }

        @Override
        public ImportRoutes importRoutes() {
            return importRoutes;
        }

        @Override
        public int hashCode() {
            return Objects.hash(afType, importRoutes);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof BgpVrfAfImpl) {
                BgpVrfAfImpl other = (BgpVrfAfImpl) obj;
                return
                     Objects.equals(afType, other.afType) &&
                     Objects.equals(importRoutes, other.importRoutes);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("afType", afType)
                .add("importRoutes", importRoutes)
                .toString();
        }

        /**
         * Creates an instance of bgpVrfAfImpl.
         *
         * @param builderObject builder object of bgpVrfAf
         */
        public BgpVrfAfImpl(BgpVrfAfBuilder builderObject) {
            this.afType = builderObject.afType();
            this.importRoutes = builderObject.importRoutes();
        }
    }
}
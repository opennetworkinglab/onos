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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.BgpVrf;

/**
 * Represents the builder implementation of bgpVrfs.
 */
public class BgpVrfsBuilder implements BgpVrfs.BgpVrfsBuilder {

    private List<BgpVrf> bgpVrf;

    @Override
    public List<BgpVrf> bgpVrf() {
        return bgpVrf;
    }

    @Override
    public BgpVrfsBuilder bgpVrf(List<BgpVrf> bgpVrf) {
        this.bgpVrf = bgpVrf;
        return this;
    }

    @Override
    public BgpVrfs build() {
        return new BgpVrfsImpl(this);
    }

    /**
     * Creates an instance of bgpVrfsBuilder.
     */
    public BgpVrfsBuilder() {
    }


    /**
     * Represents the implementation of bgpVrfs.
     */
    public final class BgpVrfsImpl implements BgpVrfs {

        private List<BgpVrf> bgpVrf;

        @Override
        public List<BgpVrf> bgpVrf() {
            return bgpVrf;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bgpVrf);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof BgpVrfsImpl) {
                BgpVrfsImpl other = (BgpVrfsImpl) obj;
                return
                     Objects.equals(bgpVrf, other.bgpVrf);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("bgpVrf", bgpVrf)
                .toString();
        }

        /**
         * Creates an instance of bgpVrfsImpl.
         *
         * @param builderObject builder object of bgpVrfs
         */
        public BgpVrfsImpl(BgpVrfsBuilder builderObject) {
            this.bgpVrf = builderObject.bgpVrf();
        }
    }
}
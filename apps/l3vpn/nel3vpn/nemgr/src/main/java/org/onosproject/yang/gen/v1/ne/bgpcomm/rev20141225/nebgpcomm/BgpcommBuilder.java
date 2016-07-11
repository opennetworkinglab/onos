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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.BgpVrfs;

/**
 * Represents the builder implementation of bgpcomm.
 */
public class BgpcommBuilder implements Bgpcomm.BgpcommBuilder {

    private BgpVrfs bgpVrfs;

    @Override
    public BgpVrfs bgpVrfs() {
        return bgpVrfs;
    }

    @Override
    public BgpcommBuilder bgpVrfs(BgpVrfs bgpVrfs) {
        this.bgpVrfs = bgpVrfs;
        return this;
    }

    @Override
    public Bgpcomm build() {
        return new BgpcommImpl(this);
    }

    /**
     * Creates an instance of bgpcommBuilder.
     */
    public BgpcommBuilder() {
    }


    /**
     * Represents the implementation of bgpcomm.
     */
    public final class BgpcommImpl implements Bgpcomm {

        private BgpVrfs bgpVrfs;

        @Override
        public BgpVrfs bgpVrfs() {
            return bgpVrfs;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bgpVrfs);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof BgpcommImpl) {
                BgpcommImpl other = (BgpcommImpl) obj;
                return
                     Objects.equals(bgpVrfs, other.bgpVrfs);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("bgpVrfs", bgpVrfs)
                .toString();
        }

        /**
         * Creates an instance of bgpcommImpl.
         *
         * @param builderObject builder object of bgpcomm
         */
        public BgpcommImpl(BgpcommBuilder builderObject) {
            this.bgpVrfs = builderObject.bgpVrfs();
        }
    }
}
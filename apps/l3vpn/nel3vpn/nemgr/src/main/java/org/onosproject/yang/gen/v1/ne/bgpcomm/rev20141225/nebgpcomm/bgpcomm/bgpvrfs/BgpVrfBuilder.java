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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.BgpVrfAfs;

/**
 * Represents the builder implementation of bgpVrf.
 */
public class BgpVrfBuilder implements BgpVrf.BgpVrfBuilder {

    private String vrfName;
    private BgpVrfAfs bgpVrfAfs;

    @Override
    public String vrfName() {
        return vrfName;
    }

    @Override
    public BgpVrfAfs bgpVrfAfs() {
        return bgpVrfAfs;
    }

    @Override
    public BgpVrfBuilder vrfName(String vrfName) {
        this.vrfName = vrfName;
        return this;
    }

    @Override
    public BgpVrfBuilder bgpVrfAfs(BgpVrfAfs bgpVrfAfs) {
        this.bgpVrfAfs = bgpVrfAfs;
        return this;
    }

    @Override
    public BgpVrf build() {
        return new BgpVrfImpl(this);
    }

    /**
     * Creates an instance of bgpVrfBuilder.
     */
    public BgpVrfBuilder() {
    }


    /**
     * Represents the implementation of bgpVrf.
     */
    public final class BgpVrfImpl implements BgpVrf {

        private String vrfName;
        private BgpVrfAfs bgpVrfAfs;

        @Override
        public String vrfName() {
            return vrfName;
        }

        @Override
        public BgpVrfAfs bgpVrfAfs() {
            return bgpVrfAfs;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vrfName, bgpVrfAfs);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof BgpVrfImpl) {
                BgpVrfImpl other = (BgpVrfImpl) obj;
                return
                     Objects.equals(vrfName, other.vrfName) &&
                     Objects.equals(bgpVrfAfs, other.bgpVrfAfs);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("vrfName", vrfName)
                .add("bgpVrfAfs", bgpVrfAfs)
                .toString();
        }

        /**
         * Creates an instance of bgpVrfImpl.
         *
         * @param builderObject builder object of bgpVrf
         */
        public BgpVrfImpl(BgpVrfBuilder builderObject) {
            this.vrfName = builderObject.vrfName();
            this.bgpVrfAfs = builderObject.bgpVrfAfs();
        }
    }
}
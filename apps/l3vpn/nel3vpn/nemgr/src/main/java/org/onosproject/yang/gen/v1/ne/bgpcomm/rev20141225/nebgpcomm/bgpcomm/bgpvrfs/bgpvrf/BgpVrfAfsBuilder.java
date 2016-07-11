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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.BgpVrfAf;

/**
 * Represents the builder implementation of bgpVrfAfs.
 */
public class BgpVrfAfsBuilder implements BgpVrfAfs.BgpVrfAfsBuilder {

    private List<BgpVrfAf> bgpVrfAf;

    @Override
    public List<BgpVrfAf> bgpVrfAf() {
        return bgpVrfAf;
    }

    @Override
    public BgpVrfAfsBuilder bgpVrfAf(List<BgpVrfAf> bgpVrfAf) {
        this.bgpVrfAf = bgpVrfAf;
        return this;
    }

    @Override
    public BgpVrfAfs build() {
        return new BgpVrfAfsImpl(this);
    }

    /**
     * Creates an instance of bgpVrfAfsBuilder.
     */
    public BgpVrfAfsBuilder() {
    }


    /**
     * Represents the implementation of bgpVrfAfs.
     */
    public final class BgpVrfAfsImpl implements BgpVrfAfs {

        private List<BgpVrfAf> bgpVrfAf;

        @Override
        public List<BgpVrfAf> bgpVrfAf() {
            return bgpVrfAf;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bgpVrfAf);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof BgpVrfAfsImpl) {
                BgpVrfAfsImpl other = (BgpVrfAfsImpl) obj;
                return
                     Objects.equals(bgpVrfAf, other.bgpVrfAf);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("bgpVrfAf", bgpVrfAf)
                .toString();
        }

        /**
         * Creates an instance of bgpVrfAfsImpl.
         *
         * @param builderObject builder object of bgpVrfAfs
         */
        public BgpVrfAfsImpl(BgpVrfAfsBuilder builderObject) {
            this.bgpVrfAf = builderObject.bgpVrfAf();
        }
    }
}
/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.utils.tapi;

import java.util.Objects;
import java.util.stream.Stream;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * TAPI NepRef pair class for representation of endpoints of single connection.
 */
public class TapiNepPair {

    private TapiNepRef left;
    private TapiNepRef right;

    public TapiNepPair() {
    }

    public static TapiNepPair create(TapiNepRef left, TapiNepRef right) {
        TapiNepPair self = new TapiNepPair();
        self.left = left;
        self.right = right;
        return self;
    }

    public TapiNepRef left() {
        return left;
    }

    public TapiNepRef right() {
        return right;
    }

    public TapiNepPair invert() {
        return TapiNepPair.create(right, left);
    }

    public boolean isSameNode() {
        return left.getTopologyId().equals(right.getTopologyId()) && left.getNodeId().equals(right.getNodeId());
    }

    public Stream<TapiNepRef> stream() {
        return Stream.of(left, right);
    }

    public String toString() {
        return toStringHelper(getClass())
                .add("left", left)
                .add("right", right)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TapiNepPair)) {
            return false;
        }
        TapiNepPair that = (TapiNepPair) o;
        return Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}

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
 * TAPI CepRef pair class for representation of endpoints of single connection.
 */
public final class TapiCepPair {

    private TapiCepRef left;
    private TapiCepRef right;

    private TapiCepPair() {
    }

    public static TapiCepPair create(TapiCepRef left, TapiCepRef right) {
        TapiCepPair self = new TapiCepPair();
        self.left = left;
        self.right = right;
        return self;
    }

    public TapiCepRef left() {
        return left;
    }

    public TapiCepRef right() {
        return right;
    }

    public TapiNepPair getTapiNepPair() {
        return TapiNepPair.create(left.getNepRef(), right.getNepRef());
    }

    public TapiCepPair invert() {
        return TapiCepPair.create(right, left);
    }

    public boolean isSameNode() {
        return left.getTopologyId().equals(right.getTopologyId()) && left.getNodeId().equals(right.getNodeId());
    }

    public Stream<TapiCepRef> stream() {
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
        if (!(o instanceof TapiCepPair)) {
            return false;
        }
        TapiCepPair that = (TapiCepPair) o;
        return Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}

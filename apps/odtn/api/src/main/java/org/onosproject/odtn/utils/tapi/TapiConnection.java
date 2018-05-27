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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static com.google.common.base.MoreObjects.toStringHelper;

public final class TapiConnection {

    private TapiCepPair ceps;
    private List<TapiConnection> lowerConnections = new ArrayList<>();

    private TapiConnection(TapiCepPair ceps) {
        this.ceps = ceps;
    }

    public static TapiConnection create(TapiCepPair ceps) {
        return new TapiConnection(ceps);
    }

    public static TapiConnection create(TapiCepRef left, TapiCepRef right) {
        TapiCepPair cepPair = TapiCepPair.create(left, right);
        return new TapiConnection(cepPair);
    }

    public TapiCepPair getCeps() {
        return ceps;
    }

    public List<TapiConnection> getLowerConnections() {
        return lowerConnections;
    }

    public TapiConnection addLowerConnection(TapiConnection lowerConnection) {
        this.lowerConnections.add(lowerConnection);
        return this;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("ceps", ceps)
                .add("lowerConnection", lowerConnections)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TapiConnection)) {
            return false;
        }
        TapiConnection that = (TapiConnection) o;
        return Objects.equals(ceps, that.ceps) &&
                Objects.equals(lowerConnections, that.lowerConnections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ceps, lowerConnections);
    }
}

/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.bgprouter;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onosproject.net.group.GroupKey;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jono on 2/16/15.
 */
public class NextHopGroupKey implements GroupKey {

    private final IpAddress address;

    public NextHopGroupKey(IpAddress address) {
        this.address = checkNotNull(address);
    }

    public IpAddress address() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NextHopGroupKey)) {
            return false;
        }

        NextHopGroupKey that = (NextHopGroupKey) o;

        return Objects.equals(this.address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("address", address)
                .toString();
    }
}

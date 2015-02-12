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
import org.onlab.packet.MacAddress;

import java.util.Objects;

/**
 * Created by jono on 2/12/15.
 */
public class NextHop {

    private final IpAddress ip;
    private final MacAddress mac;

    public NextHop(IpAddress ip, MacAddress mac) {
        this.ip = ip;
        this.mac = mac;
    }

    public IpAddress ip() {
        return ip;
    }

    public MacAddress mac() {
        return mac;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NextHop)) {
            return false;
        }

        NextHop that = (NextHop) o;

        return Objects.equals(this.ip, that.ip) &&
                Objects.equals(this.mac, that.mac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, mac);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ip", ip)
                .add("mac", mac)
                .toString();
    }
}

/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */
package org.onosproject.dhcprelay.store;


import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Objects;

public class DhcpRelayCounters {
    // common counters

    // IpV6 specific counters
    public static final String SOLICIT = "SOLICIT";
    public static final String ADVERTISE = "ADVERTISE";
    public static final String REQUEST = "REQUEST";
    public static final String CONFIRM = "CONFIRM";
    public static final String RENEW = "RENEW";
    public static final String REBIND = "REBIND";
    public static final String REPLY = "REPLY";
    public static final String RELEASE = "RELEASE";
    public static final String DECLINE = "DECLINE";
    public static final String RECONFIGURE = "RECONFIGURE";
    public static final String INFORMATION_REQUEST = "INFORMATION_REQUEST";
    public static final String RELAY_FORW = "RELAY_FORW";
    public static final String RELAY_REPL = "RELAY_REPL";

    public static final String NO_LINKLOCAL_GW = "No link-local in Gateway";
    public static final String NO_LINKLOCAL_FAIL = "No link-local in CLIENT_ID";
    public static final String NO_CLIENTID_FAIL = "No CLIENT_ID Found";
    public static final String SVR_CFG_FAIL = "Server Config Error";
    public static final String OPTION_MISSING_FAIL = "Expected Option missing";
    public static final String NO_MATCHING_INTF = "No matching Inteface";
    public static final String NO_CLIENT_INTF_MAC = "No client interface mac";
    public static final String NO_SERVER_INFO = "No Server info found";
    public static final String NO_SERVER_IP6ADDR = "No Server ip6 addr found";

    public static final String INVALID_PACKET = "Invalid Packet";

    public static final Set<String> SUPPORTED_COUNTERS =
            ImmutableSet.of(SOLICIT, ADVERTISE, REQUEST, CONFIRM, RENEW,
                    REBIND, REPLY, RELEASE, DECLINE, RECONFIGURE,
                    INFORMATION_REQUEST, RELAY_FORW, RELAY_REPL,
                    NO_LINKLOCAL_GW, NO_LINKLOCAL_FAIL, NO_CLIENTID_FAIL, SVR_CFG_FAIL, OPTION_MISSING_FAIL,
                    NO_MATCHING_INTF, NO_CLIENT_INTF_MAC, NO_SERVER_INFO, NO_SERVER_IP6ADDR,
                    INVALID_PACKET);

    // TODO Use AtomicInteger for the counters
    private Map<String, Integer> countersMap = new ConcurrentHashMap<>();
    public long lastUpdate;

    public void resetCounters() {
        countersMap.forEach((name, value) -> {
            countersMap.put(name, 0);
        });
    }

    public boolean incrementCounter(String name) {
        boolean counterValid = false;
        if (SUPPORTED_COUNTERS.contains(name)) {
            Integer counter = countersMap.get(name);
            if (counter != null) {
                counter = counter + 1;
                countersMap.put(name, counter);
            } else {
                // this is the first time
                countersMap.put(name, 1);
            }
            lastUpdate = System.currentTimeMillis();
            counterValid = true;
        }
        return counterValid;
    }

    public Map<String, Integer> getCounters() {
        return countersMap;
    }

    @Override
    public int hashCode() {
        return Objects.hash(countersMap, lastUpdate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DhcpRelayCounters)) {
            return false;
        }
        DhcpRelayCounters that = (DhcpRelayCounters) obj;
        return Objects.equals(countersMap, that.countersMap) &&
                Objects.equals(lastUpdate, that.lastUpdate);
    }
}

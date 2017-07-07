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
package org.onosproject.artemis.impl;

import com.google.common.collect.Sets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.artemis.impl.bgpspeakers.BgpSpeakers;
import org.onosproject.artemis.impl.bgpspeakers.QuaggaBgpSpeakers;
import org.onosproject.routing.bgp.BgpInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;

/**
 * Timertask class which detects and mitigates BGP hijacks.
 */
class Deaggregator extends java.util.TimerTask {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Set<ArtemisConfig.ArtemisPrefixes> prefixes = Sets.newHashSet();
    private Set<BgpSpeakers> bgpSpeakers = Sets.newHashSet();

    Deaggregator(BgpInfoService bgpInfoService) {
        super();
        // deaggregator must know the type of the connected BGP speakers and the BGP info.
        // for this example we only have one Quagga BGP speaker.
        bgpSpeakers.add(new QuaggaBgpSpeakers(bgpInfoService));
    }

    @Override
    public void run() {
        ArrayList<JSONObject> messagesArray = DataHandler.getInstance().getData();
//        log.info("Messages size: " + messagesArray.size());

        // Example of BGP Update message:
        // {
        //  "path":[65001, 65002, 65004], (origin being last)
        //  "prefix":"12.0.0.0/8",
        // }

        prefixes.forEach(prefix -> {
            IpPrefix monitoredPrefix = prefix.prefix();

            // for each update message in memory check for hijack
            for (JSONObject tmp : messagesArray) {
                IpPrefix receivedPrefix = null;
                try {
                    receivedPrefix = IpPrefix.valueOf(tmp.getString("prefix"));
                } catch (JSONException e) {
                    log.warn("JSONException: " + e.getMessage());
                    e.printStackTrace();
                }
                if (receivedPrefix == null) {
                    continue;
                }

                // check if the announced network address is inside our subnet
                if (monitoredPrefix.contains(receivedPrefix)) {
                    JSONArray path = null;
                    try {
                        path = tmp.getJSONArray("path");
                    } catch (JSONException e) {
                        log.warn("JSONException: " + e.getMessage());
                        e.printStackTrace();
                    }
                    if (path == null) {
                        continue;
                    }

                    int state = prefix.checkPath(path);
                    if (state >= 100) {
                        log.warn("BGP Hijack detected of type " +
                                (state - 100) + "\n" + tmp.toString());
                        DataHandler.Serializer.writeHijack(tmp);
                        // can only de-aggregate /23 subnets and higher
                        int cidr = receivedPrefix.prefixLength();
                        if (receivedPrefix.prefixLength() < 24) {
                            byte[] octets = receivedPrefix.address().toOctets();
                            int byteGroup = (cidr + 1) / 8,
                                    bitPos = 8 - (cidr + 1) % 8;

                            octets[byteGroup] = (byte) (octets[byteGroup] & ~(1 << bitPos));
                            String low = IpPrefix.valueOf(IpAddress.Version.INET, octets, cidr + 1).toString();
                            octets[byteGroup] = (byte) (octets[byteGroup] | (1 << bitPos));
                            String high = IpPrefix.valueOf(IpAddress.Version.INET, octets, cidr + 1).toString();

                            String[] prefixes = {low, high};
                            bgpSpeakers.forEach(bgpSpeakers -> bgpSpeakers.announceSubPrefixes(prefixes));
                        } else {
                            log.warn("Cannot announce smaller prefix than /24");
                        }
                    }
                }
            }
        });
    }

    public Set<ArtemisConfig.ArtemisPrefixes> getPrefixes() {
        return prefixes;
    }

    public void setPrefixes(Set<ArtemisConfig.ArtemisPrefixes> prefixes) {
        this.prefixes = prefixes;
    }
}
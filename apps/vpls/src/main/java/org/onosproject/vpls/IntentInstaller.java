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
package org.onosproject.vpls;

import com.google.common.collect.SetMultimap;

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.routing.IntentSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Synchronizes intents between the in-memory intent store and the
 * IntentService.
 */
public class IntentInstaller {
    private static final Logger log = LoggerFactory.getLogger(
            IntentInstaller.class);

    private static final int PRIORITY_OFFSET = 1000;

    private static final String PREFIX_BROADCAST = "brc";
    private static final String PREFIX_UNICAST = "uni";

    private final ApplicationId appId;
    private final IntentSynchronizationService intentSynchronizer;
    private final IntentService intentService;

    /**
     * Class constructor.
     *
     * @param appId              the Application ID
     * @param intentService      the intent service
     * @param intentSynchronizer the intent synchronizer service
     */
    public IntentInstaller(ApplicationId appId, IntentService intentService,
                           IntentSynchronizationService intentSynchronizer) {
        this.appId = appId;
        this.intentService = intentService;
        this.intentSynchronizer = intentSynchronizer;
    }

    /**
     * Formats the requests for creating and submit intents.
     * Single Points to Multi Point intents are created for all the configured
     * Connect Points. Multi Point to Single Point intents are created for
     * Connect Points configured that have hosts attached.
     *
     * @param confHostPresentCPoint A map of Connect Points with the eventual
     *                              MAC address of the host attached, by VLAN
     */
    protected void installIntents(SetMultimap<VlanId,
            Pair<ConnectPoint,
                    MacAddress>> confHostPresentCPoint) {
        List<Intent> intents = new ArrayList<>();

        confHostPresentCPoint.keySet()
                .stream()
                .filter(vlanId -> confHostPresentCPoint.get(vlanId) != null)
                .forEach(vlanId -> {
                    Set<Pair<ConnectPoint, MacAddress>> cPoints =
                            confHostPresentCPoint.get(vlanId);
                    cPoints.forEach(cPoint -> {
                        MacAddress mac = cPoint.getValue();
                        ConnectPoint src = cPoint.getKey();
                        Set<ConnectPoint> dsts = cPoints.stream()
                                .map(Pair::getKey)
                                .filter(cp -> !cp.equals(src))
                                .collect(Collectors.toSet());
                        Key brcKey = buildKey(PREFIX_BROADCAST, src, vlanId);

                        if (intentService.getIntent(brcKey) == null && dsts.size() > 0) {
                            intents.add(buildBrcIntent(brcKey, src, dsts, vlanId));
                        }

                        if (mac != null && countMacInCPoints(cPoints) > 1 &&
                                dsts.size() > 0) {
                            Key uniKey = buildKey(PREFIX_UNICAST, src, vlanId);
                            if (intentService.getIntent(uniKey) == null) {
                                MultiPointToSinglePointIntent uniIntent =
                                        buildUniIntent(uniKey,
                                                       dsts,
                                                       src,
                                                       vlanId,
                                                       mac);
                                intents.add(uniIntent);
                            }
                        }
                    });
                });
        submitIntents(intents);
    }

    /**
     * Requests to install the intents passed as argument to the Intent Service.
     *
     * @param intents intents to be submitted
     */
    private void submitIntents(Collection<Intent> intents) {
        log.debug("Submitting intents to the Intent Synchronizer");
        intents.forEach(intent -> {
            intentSynchronizer.submit(intent);
        });
    }

    /**
     * Builds a Single Point to Multi Point intent.
     *
     * @param src  The source Connect Point
     * @param dsts The destination Connect Points
     * @return Single Point to Multi Point intent generated.
     */
    private SinglePointToMultiPointIntent buildBrcIntent(Key key,
                                                         ConnectPoint src,
                                                         Set<ConnectPoint> dsts,
                                                         VlanId vlanId) {
        log.debug("Building p2mp intent from {}", src);

        SinglePointToMultiPointIntent intent;

        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST)
                .matchVlanId(vlanId);

        TrafficSelector selector = builder.build();

        intent = SinglePointToMultiPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(src)
                .egressPoints(dsts)
                .priority(PRIORITY_OFFSET)
                .build();
        return intent;
    }

    /**
     * Builds a Multi Point to Single Point intent.
     *
     * @param srcs The source Connect Points
     * @param dst  The destination Connect Point
     * @return Multi Point to Single Point intent generated.
     */
    private MultiPointToSinglePointIntent buildUniIntent(Key key,
                                                         Set<ConnectPoint> srcs,
                                                         ConnectPoint dst,
                                                         VlanId vlanId,
                                                         MacAddress mac) {
        log.debug("Building mp2p intent to {}", dst);

        MultiPointToSinglePointIntent intent;

        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthDst(mac)
                .matchVlanId(vlanId);

        TrafficSelector selector = builder.build();

        intent = MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoints(srcs)
                .egressPoint(dst)
                .priority(PRIORITY_OFFSET)
                .build();
        return intent;
    }

    /**
     * Builds an intent Key for either for a Single Point to Multi Point or
     * Multi Point to Single Point intent, based on a prefix that defines
     * the type of intent, the single connection point representing the source
     * or the destination and the vlan id representing the network.
     *
     * @param cPoint the source or destination connect point
     * @param vlanId the network vlan id
     * @param prefix prefix string
     * @return
     */
    private Key buildKey(String prefix, ConnectPoint cPoint, VlanId vlanId) {
        String keyString = new StringBuilder()
                .append(prefix)
                .append("-")
                .append(cPoint.deviceId())
                .append("-")
                .append(cPoint.port())
                .append("-")
                .append(vlanId)
                .toString();

        return Key.of(keyString, appId);
    }

    /**
     * Counts the number of mac addresses associated to a specific list of
     * ConnectPoint.
     *
     * @param cPoints Set of ConnectPoints, eventually bound to the MAC of the
     *                host attached
     * @return number of mac addresses found.
     */
    private int countMacInCPoints(Set<Pair<ConnectPoint, MacAddress>> cPoints) {
        return (int) cPoints.stream().filter(p -> p.getValue() != null).count();
    }

}

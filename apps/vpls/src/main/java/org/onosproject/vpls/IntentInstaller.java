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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.routing.IntentSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Synchronizes intents between the in-memory intent store and the
 * IntentService.
 */
public class IntentInstaller {
    private static final String SUBMIT =
            "Submitting intents to the Intent Synchronizer";
    private static final String WITHDRAW =
            "Withdrawing intents to the Intent Synchronizer";
    private static final String SP2MP =
            "Building sp2mp intent from {}";
    private static final String MP2SP =
            "Building mp2sp intent to {}";

    private static final Logger log = LoggerFactory.getLogger(
            IntentInstaller.class);

    private static final int PRIORITY_OFFSET = 1000;

    private static final Set<IntentState> WITHDRAWN_INTENT_STATES =
            ImmutableSet.of(IntentState.WITHDRAWN,
                            IntentState.WITHDRAW_REQ,
                            IntentState.WITHDRAWING);

    static final String PREFIX_BROADCAST = "brc";
    static final String PREFIX_UNICAST = "uni";
    static final String DASH = "-";

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
     * Requests to install the intents passed as argument to the Intent Service.
     *
     * @param intents intents to be submitted
     */
    protected void submitIntents(Collection<Intent> intents) {
        log.debug(SUBMIT);
        intents.forEach(intentSynchronizer::submit);
    }

    /**
     * Requests to withdraw the intents passed as argument to the Intent Service.
     *
     * @param intents intents to be withdraw
     */
    protected void withdrawIntents(Collection<Intent> intents) {
        log.debug(WITHDRAW);
        intents.forEach(intentSynchronizer::withdraw);
    }

    /**
     * Returns list of intents belongs to a VPLS.
     *
     * @param name required VPLS network name
     * @return list of intents belongs to a VPLS
     */
    protected List<Intent> getIntentsFromVpls(String name) {
        List<Intent> intents = Lists.newArrayList();

        intentService.getIntents().forEach(intent -> {
            if (intent.key().toString().startsWith(name)) {
                intents.add(intent);
            }
        });

        return intents;
    }

    /**
     * Builds a broadcast intent.
     *
     * @param key key to identify the intent
     * @param src the source connect point
     * @param dsts the destination connect points
     * @return the generated single-point to multi-point intent
     */
    protected SinglePointToMultiPointIntent buildBrcIntent(Key key,
                                                           FilteredConnectPoint src,
                                                           Set<FilteredConnectPoint> dsts) {
        log.debug(SP2MP, src);

        SinglePointToMultiPointIntent intent;

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST)
                .build();

        intent = SinglePointToMultiPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .filteredIngressPoint(src)
                .filteredEgressPoints(dsts)
                .priority(PRIORITY_OFFSET)
                .build();
        return intent;
    }

    /**
     * Builds a unicast intent.
     *
     * @param key key to identify the intent
     * @param srcs the source Connect Points
     * @param dst the destination Connect Point
     * @param host destination Host
     * @return the generated multi-point to single-point intent
     */
    protected MultiPointToSinglePointIntent buildUniIntent(Key key,
                                                           Set<FilteredConnectPoint> srcs,
                                                           FilteredConnectPoint dst,
                                                           Host host) {
        log.debug(MP2SP, dst);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(host.mac())
                .build();


        return MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .filteredIngressPoints(srcs)
                .filteredEgressPoint(dst)
                .priority(PRIORITY_OFFSET)
                .build();

    }

    /**
     * Builds an intent Key for either for a single-point to multi-point or
     * multi-point to single-point intent, based on a prefix that defines
     * the type of intent, the single connection point representing the source
     * or the destination and the VLAN identifier representing the network.
     *
     * @param prefix key prefix
     * @param cPoint connect point for single source/destination
     * @param networkName VPLS network name
     * @param hostMac source/destination mac address
     * @return key to identify the intent
     */
    protected Key buildKey(String prefix,
                           ConnectPoint cPoint,
                           String networkName,
                           MacAddress hostMac) {
        String keyString = networkName +
                DASH +
                prefix +
                DASH +
                cPoint.deviceId() +
                DASH +
                cPoint.port() +
                DASH +
                hostMac;

        return Key.of(keyString, appId);
    }

    /**
     * Returns true if the specified intent exists; false otherwise.
     *
     * @param intentKey intent key
     * @return true if the intent exists, false otherwise
     */
    protected boolean intentExists(Key intentKey) {
        if (intentService.getIntent(intentKey) == null) {
            return false;
        }

        // Intent does not exist if intent withdrawn
        IntentState currentIntentState = intentService.getIntentState(intentKey);
        if (WITHDRAWN_INTENT_STATES.contains(currentIntentState)) {
            return false;
        }

        return true;
    }
}

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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.routing.IntentSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.onosproject.net.EncapsulationType.*;

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
    static final String SEPARATOR = "-";

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
     * @param name the name of the VPLS
     * @return the list of intents belonging to a VPLS
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
     * @param encap the encapsulation type
     * @return the generated single-point to multi-point intent
     */
    protected SinglePointToMultiPointIntent buildBrcIntent(Key key,
                                                           FilteredConnectPoint src,
                                                           Set<FilteredConnectPoint> dsts,
                                                           EncapsulationType encap) {
        log.debug("Building broadcast intent {} for source {}", SP2MP, src);

        SinglePointToMultiPointIntent.Builder intentBuilder;

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST)
                .build();

        intentBuilder = SinglePointToMultiPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .filteredIngressPoint(src)
                .filteredEgressPoints(dsts)
                .priority(PRIORITY_OFFSET);

        encap(intentBuilder, encap);

        return intentBuilder.build();
    }

    /**
     * Builds a unicast intent.
     *
     * @param key key to identify the intent
     * @param srcs the source Connect Points
     * @param dst the destination Connect Point
     * @param host destination Host
     * @param encap the encapsulation type
     * @return the generated multi-point to single-point intent
     */
    protected MultiPointToSinglePointIntent buildUniIntent(Key key,
                                                           Set<FilteredConnectPoint> srcs,
                                                           FilteredConnectPoint dst,
                                                           Host host,
                                                           EncapsulationType encap) {
        log.debug("Building unicast intent {} for destination {}", MP2SP, dst);

        MultiPointToSinglePointIntent.Builder intentBuilder;

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(host.mac())
                .build();

        intentBuilder = MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .filteredIngressPoints(srcs)
                .filteredEgressPoint(dst)
                .priority(PRIORITY_OFFSET);

        encap(intentBuilder, encap);

        return intentBuilder.build();
    }

    /**
     * Builds an intent key either for single-point to multi-point or
     * multi-point to single-point intents, based on a prefix that defines
     * the type of intent, the single connect point representing the single
     * source or destination for that intent, the name of the VPLS the intent
     * belongs to, and the destination host MAC address the intent reaches.
     *
     * @param prefix the key prefix
     * @param cPoint the connect point identifying the source/destination
     * @param vplsName the name of the VPLS
     * @param hostMac the source/destination MAC address
     * @return the key to identify the intent
     */
    protected Key buildKey(String prefix,
                           ConnectPoint cPoint,
                           String vplsName,
                           MacAddress hostMac) {
        String keyString = vplsName +
                SEPARATOR +
                prefix +
                SEPARATOR +
                cPoint.deviceId() +
                SEPARATOR +
                cPoint.port() +
                SEPARATOR +
                hostMac;

        return Key.of(keyString, appId);
    }

    /**
     * Returns true if the specified intent exists; false otherwise.
     *
     * @param intentKey the intent key
     * @return true if the intent exists; false otherwise
     */
    protected boolean intentExists(Key intentKey) {
        if (intentService.getIntent(intentKey) == null) {
            return false;
        }

        // Intent does not exist if intent withdrawn
        IntentState currentIntentState = intentService.getIntentState(intentKey);
        return !WITHDRAWN_INTENT_STATES.contains(currentIntentState);

    }

    /**
     * Adds an encapsulation constraint to the builder given, if encap is not
     * equal to NONE.
     *
     * @param builder the intent builder
     * @param encap the encapsulation type
     */
    private static void encap(ConnectivityIntent.Builder builder,
                              EncapsulationType encap) {
        if (!encap.equals(NONE)) {
            builder.constraints(ImmutableList.of(
                    new EncapsulationConstraint(encap)));
        }
    }
}

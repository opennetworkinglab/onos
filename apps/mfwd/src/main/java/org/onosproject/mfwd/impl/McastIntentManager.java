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
package org.onosproject.mfwd.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

@Component(immediate = true)
@Service(value = org.onosproject.mfwd.impl.McastIntentManager.class)
public class McastIntentManager {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    private static McastIntentManager instance;

    public McastIntentManager() {
        instance = this;
    }

    /**
     * Active this component.
     */
    @Activate
    public void activate() { }

    /**
     * Deactivate this component.
     */
    @Deactivate
    public void deactivate() {
        withdrawAllIntents();
    }

    /**
     * Get instance of this intentManager.
     *
     * @return the instance of this intent manager.
     */
    public static McastIntentManager getInstance() {
        if (instance == null) {
            instance = new McastIntentManager();
        }
        return instance;
    }

    /**
     * Install the PointToMultipoint forwarding intent.
     *
     * @param mroute multicast route entry
     * @return the intent that has been set or null otherwise
     */
    public SinglePointToMultiPointIntent setIntent(McastRoute mroute) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        if (mroute.getIngressPoint() == null ||
                mroute.getEgressPoints().isEmpty()) {
            return null;
        }

        /*
         * Match the group AND source addresses.  We will also check ether type to
         * determine if we are doing ipv4 or ipv6.
         *
         * If we really wanted to be pendantic we could put in a
         * condition to make sure the ethernet MAC address was also
         * mcast.
         */
        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(mroute.getGaddr())
                .matchIPSrc(mroute.getSaddr());

        SinglePointToMultiPointIntent intent =
                SinglePointToMultiPointIntent.builder()
                        .appId(McastForwarding.getAppId())
                        .selector(selector.build())
                        .treatment(treatment)
                        .ingressPoint(mroute.getIngressPoint())
                        .egressPoints(mroute.getEgressPoints()).
                        build();

        intentService.submit(intent);
        return intent;
    }

    /**
     * Withdraw the intent represented by this route.
     *
     * @param mroute the mcast route whose intent we want to remove
     */
    public void withdrawIntent(McastRouteBase mroute) {
        Intent intent = intentService.getIntent(mroute.getIntentKey());
        intentService.withdraw(intent);
    }

    /**
     * Withdraw all intents.
     *
     * This will be called from the deactivate method so we don't leave
     * a mess behind us after we leave.
     */
    public void withdrawAllIntents() {
        for (Intent intent : intentService.getIntents()) {
            intentService.withdraw(intent);
        }
    }
}

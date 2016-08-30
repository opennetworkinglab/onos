/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.provider.ProviderId;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component(immediate = true)
public class SinglePointToMultiPointIntentCompiler
        extends ConnectivityIntentCompiler<SinglePointToMultiPointIntent> {

    // TODO: use off-the-shell core provider ID
    private static final ProviderId PID =
            new ProviderId("core", "org.onosproject.core", true);

    @Activate
    public void activate() {
        intentManager.registerCompiler(SinglePointToMultiPointIntent.class,
                                       this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(SinglePointToMultiPointIntent.class);
    }


    @Override
    public List<Intent> compile(SinglePointToMultiPointIntent intent,
                                List<Intent> installable) {
        Set<Link> links = new HashSet<>();

        for (ConnectPoint egressPoint : intent.egressPoints()) {
            if (egressPoint.deviceId().equals(intent.ingressPoint().deviceId())) {
                continue;
            }

            Path path = getPath(intent, intent.ingressPoint().deviceId(), egressPoint.deviceId());
            links.addAll(path.links());
        }

        Intent result = LinkCollectionIntent.builder()
                .appId(intent.appId())
                .key(intent.key())
                .selector(intent.selector())
                .treatment(intent.treatment())
                .links(links)
                .ingressPoints(ImmutableSet.of(intent.ingressPoint()))
                .egressPoints(intent.egressPoints())
                .priority(intent.priority())
                .applyTreatmentOnEgress(true)
                .constraints(intent.constraints())
                .egressTreatments(intent.egressTreatments())
                .build();

        return Collections.singletonList(result);
    }
}
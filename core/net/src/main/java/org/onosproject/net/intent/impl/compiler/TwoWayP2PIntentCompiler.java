/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.TwoWayP2PIntent;

import java.util.List;

/**
 * A intent compiler for {@link org.onosproject.net.intent.TwoWayP2PIntent}.
 */
@Component(immediate = true)
public class TwoWayP2PIntentCompiler
        extends ConnectivityIntentCompiler<TwoWayP2PIntent> {

    @Activate
    public void activate() {
        intentManager.registerCompiler(TwoWayP2PIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(TwoWayP2PIntent.class);
    }

    @Override
    public List<Intent> compile(TwoWayP2PIntent intent, List<Intent> installable) {
        return Lists.newArrayList(
                PointToPointIntent.builder()
                        .appId(intent.appId())
                        .key(intent.key())
                        .selector(intent.selector())
                        .treatment(intent.treatment())
                        .filteredIngressPoint(new FilteredConnectPoint(intent.one()))
                        .filteredEgressPoint(new FilteredConnectPoint(intent.two()))
                        .constraints(intent.constraints())
                        .priority(intent.priority())
                        .resourceGroup(intent.resourceGroup())
                        .build(),
                PointToPointIntent.builder()
                        .appId(intent.appId())
                        .key(intent.key())
                        .selector(intent.selector())
                        .treatment(intent.treatment())
                        .filteredIngressPoint(new FilteredConnectPoint(intent.two()))
                        .filteredEgressPoint(new FilteredConnectPoint(intent.one()))
                        .constraints(intent.constraints())
                        .priority(intent.priority())
                        .resourceGroup(intent.resourceGroup())
                        .build());
    }
}

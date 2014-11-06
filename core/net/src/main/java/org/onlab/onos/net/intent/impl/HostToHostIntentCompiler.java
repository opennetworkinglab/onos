/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.net.intent.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.PathIntent;

import java.util.Arrays;
import java.util.List;

import static org.onlab.onos.net.flow.DefaultTrafficSelector.builder;

/**
 * A intent compiler for {@link HostToHostIntent}.
 */
@Component(immediate = true)
public class HostToHostIntentCompiler
        extends ConnectivityIntentCompiler<HostToHostIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(HostToHostIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(HostToHostIntent.class);
    }

    @Override
    public List<Intent> compile(HostToHostIntent intent) {
        Path pathOne = getPath(intent, intent.one(), intent.two());
        Path pathTwo = getPath(intent, intent.two(), intent.one());

        Host one = hostService.getHost(intent.one());
        Host two = hostService.getHost(intent.two());

        return Arrays.asList(createPathIntent(pathOne, one, two, intent),
                             createPathIntent(pathTwo, two, one, intent));
    }

    // Creates a path intent from the specified path and original connectivity intent.
    private Intent createPathIntent(Path path, Host src, Host dst,
                                    HostToHostIntent intent) {
        TrafficSelector selector = builder(intent.selector())
                .matchEthSrc(src.mac()).matchEthDst(dst.mac()).build();
        return new PathIntent(intent.appId(), selector, intent.treatment(),
                              path, intent.constraints());
    }

}

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
import org.onlab.onos.net.DefaultEdgeLink;
import org.onlab.onos.net.DefaultPath;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * An intent compiler for {@link org.onlab.onos.net.intent.PointToPointIntent}.
 */
@Component(immediate = true)
public class PointToPointIntentCompiler
        extends ConnectivityIntentCompiler<PointToPointIntent> {

    // TODO: use off-the-shell core provider ID
    private static final ProviderId PID =
            new ProviderId("core", "org.onlab.onos.core", true);

    @Activate
    public void activate() {
        intentManager.registerCompiler(PointToPointIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(PointToPointIntent.class);
    }

    @Override
    public List<Intent> compile(PointToPointIntent intent) {
        Path path = getPath(intent, intent.ingressPoint().deviceId(),
                            intent.egressPoint().deviceId());

        List<Link> links = new ArrayList<>();
        links.add(DefaultEdgeLink.createEdgeLink(intent.ingressPoint(), true));
        links.addAll(path.links());
        links.add(DefaultEdgeLink.createEdgeLink(intent.egressPoint(), false));

        return asList(createPathIntent(new DefaultPath(PID, links, path.cost(),
                                                       path.annotations()), intent));
    }

    /**
     * Creates a path intent from the specified path and original
     * connectivity intent.
     *
     * @param path   path to create an intent for
     * @param intent original intent
     */
    private Intent createPathIntent(Path path,
                                    PointToPointIntent intent) {
        return new PathIntent(intent.appId(),
                              intent.selector(), intent.treatment(), path,
                              intent.constraints());
    }

}

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
package org.onosproject.net.intent.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.resource.LinkResourceAllocations;
import org.onosproject.net.topology.PathService;

import com.google.common.collect.Sets;

/**
 * An intent compiler for
 * {@link org.onosproject.net.intent.MultiPointToSinglePointIntent}.
 */
@Component(immediate = true)
public class MultiPointToSinglePointIntentCompiler
        implements IntentCompiler<MultiPointToSinglePointIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(MultiPointToSinglePointIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(PointToPointIntent.class);
    }

    @Override
    public List<Intent> compile(MultiPointToSinglePointIntent intent, List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        Map<DeviceId, Link> links = new HashMap<>();

        for (ConnectPoint ingressPoint : intent.ingressPoints()) {
            Path path = getPath(ingressPoint, intent.egressPoint());
            for (Link link : path.links()) {
                if (links.containsKey(link.src().deviceId())) {
                    // We've already reached the existing tree with the first
                    // part of this path. Don't add the remainder of the path
                    // in case it differs from the path we already have.
                    break;
                }

                links.put(link.src().deviceId(), link);
            }
        }

        Intent result = new LinkCollectionIntent(intent.appId(),
                                                 intent.selector(), intent.treatment(),
                                                 Sets.newHashSet(links.values()), intent.egressPoint());
        return Arrays.asList(result);
    }

    /**
     * Computes a path between two ConnectPoints.
     *
     * @param one start of the path
     * @param two end of the path
     * @return Path between the two
     * @throws org.onosproject.net.intent.impl.PathNotFoundException if a path cannot be found
     */
    private Path getPath(ConnectPoint one, ConnectPoint two) {
        Set<Path> paths = pathService.getPaths(one.deviceId(), two.deviceId());
        if (paths.isEmpty()) {
            throw new PathNotFoundException(one.elementId(), two.elementId());
        }
        // TODO: let's be more intelligent about this eventually
        return paths.iterator().next();
    }
}

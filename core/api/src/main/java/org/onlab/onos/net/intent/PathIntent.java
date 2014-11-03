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
package org.onlab.onos.net.intent;

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.resource.LinkResourceRequest;

/**
 * Abstraction of explicitly path specified connectivity intent.
 */
public class PathIntent extends ConnectivityIntent {

    private final Path path;
    private final List<LinkResourceRequest> resourceRequests;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param appId     application identifier
     * @param selector  traffic selector
     * @param treatment treatment
     * @param path      traversed links
     * @throws NullPointerException {@code path} is null
     */
    public PathIntent(ApplicationId appId, TrafficSelector selector,
                      TrafficTreatment treatment, Path path, LinkResourceRequest[] resourceRequests) {
        super(id(PathIntent.class, selector, treatment, path), appId,
              resources(path.links()), selector, treatment);
        this.path = path;
        this.resourceRequests = ImmutableList.copyOf(resourceRequests);
    }

    /**
     * Constructor for serializer.
     */
    protected PathIntent() {
        super();
        this.path = null;
        this.resourceRequests = ImmutableList.of();
    }

    /**
     * Returns the links which the traffic goes along.
     *
     * @return traversed links
     */
    public Path path() {
        return path;
    }

    @Override
    public boolean isInstallable() {
        return true;
    }

    // TODO: consider changing return type
    public LinkResourceRequest[] resourceRequests() {
        return resourceRequests.toArray(new LinkResourceRequest[resourceRequests.size()]);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("path", path)
                .toString();
    }

}

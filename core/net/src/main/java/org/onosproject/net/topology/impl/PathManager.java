/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.topology.impl;

import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.AbstractPathService;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.TOPOLOGY_READ;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Provides implementation of a path selection service atop the current
 * topology and host services.
 */
@Component(immediate = true, service = PathService.class)
public class PathManager extends AbstractPathService implements PathService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Activate
    public void activate() {
        // initialize AbstractPathService
        super.topologyService = this.topologyService;
        super.hostService = this.hostService;
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst) {
        checkPermission(TOPOLOGY_READ);

        return getPaths(src, dst, (LinkWeigher) null);
    }

    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeigher weigher) {
        checkPermission(TOPOLOGY_READ);
        return super.getPaths(src, dst, weigher);
    }

    @Override
    public Stream<Path> getKShortestPaths(ElementId src, ElementId dst,
                                          LinkWeigher weigher) {
        checkPermission(TOPOLOGY_READ);
        return super.getKShortestPaths(src, dst, weigher);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst) {
        checkPermission(TOPOLOGY_READ);
        return getDisjointPaths(src, dst, (LinkWeigher) null);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst, LinkWeigher weigher) {
        checkPermission(TOPOLOGY_READ);
        return super.getDisjointPaths(src, dst, weigher);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst,
                                              Map<Link, Object> riskProfile) {
        checkPermission(TOPOLOGY_READ);
        return getDisjointPaths(src, dst, (LinkWeigher) null, riskProfile);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst, LinkWeigher weigher,
                                              Map<Link, Object> riskProfile) {
        checkPermission(TOPOLOGY_READ);
        return super.getDisjointPaths(src, dst, weigher, riskProfile);
    }

}

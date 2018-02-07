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
package org.onosproject.net.topology;

import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;

import java.util.Map;
import java.util.Set;

/**
 * Test adapter for path service.
 */
public class PathServiceAdapter implements PathService {
    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst) {
        return null;
    }

    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst,
                              LinkWeigher weigher) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst,
                                              LinkWeigher weigher) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst,
                                              Map<Link, Object> riskProfile) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst,
                                              LinkWeigher weigher,
                                              Map<Link, Object> riskProfile) {
        return null;
    }
}

/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents Network Information Base (NIB) for links
 * and supports alternative functions to
 * {@link org.onosproject.net.link.LinkService} for offline data.
 */
public class LinkNib extends AbstractNib {

    // TODO with method optimization, store into subdivided structures at the first load
    private Set<Link> links;

    // use the singleton helper to create the instance
    protected LinkNib() {
    }

    /**
     * Sets a set of links.
     *
     * @param links link set
     */
    public void setLinks(Set<Link> links) {
        this.links = links;
    }

    /**
     * Returns the set of links.
     *
     * @return link set
     */
    public Set<Link> getLinks() {
        return ImmutableSet.copyOf(links);
    }

    /**
     * Returns set of all infrastructure links leading from the specified
     * connection point.
     *
     * @param connectPoint connection point
     * @return set of device egress links
     */
    public Set<Link> getEgressLinks(ConnectPoint connectPoint) {
        Set<Link> egressLinks = links.stream()
                .filter(link -> connectPoint.equals(link.src()))
                .collect(Collectors.toSet());
        return egressLinks != null ? ImmutableSet.copyOf(egressLinks) : ImmutableSet.of();
    }

    /**
     * Returns the singleton instance of links NIB.
     *
     * @return instance of links NIB
     */
    public static LinkNib getInstance() {
        return LinkNib.SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final LinkNib INSTANCE = new LinkNib();
    }

}

/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.pceweb;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.ui.topo.BiLink;
import org.onosproject.ui.topo.LinkHighlight;
import org.onosproject.ui.topo.LinkHighlight.Flavor;

import java.util.Set;

/**
 * Provides the link color highlight mechanism for given links.
 */
public class PceWebLink extends BiLink {

    private boolean primary;
    private boolean secondary;

    /**
     * Initialize the Link and key attributes.
     * @param key the link key identifier
     * @param link the link to be highlighted.
     */
    public PceWebLink(LinkKey key, Link link) {
        super(key, link);
    }

     /**
     * Highlight the color of given selected links.
     * @param selectedLinks the primary links
     * @param allLinks the secondary links
     */
    public void computeHilight(Set<Link> selectedLinks, Set<Link> allLinks) {
        primary = selectedLinks.contains(this.one()) ||
                (two() != null && selectedLinks.contains(two()));
        secondary = allLinks.contains(this.one()) ||
                (two() != null && allLinks.contains(two()));
    }

    @Override
    public LinkHighlight highlight(Enum<?> anEnum) {
        Flavor flavor = primary ? Flavor.PRIMARY_HIGHLIGHT :
                (secondary ? Flavor.SECONDARY_HIGHLIGHT : Flavor.NO_HIGHLIGHT);
        return new LinkHighlight(this.linkId(), flavor);
    }
}

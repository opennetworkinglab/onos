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
package org.onosproject.sfcweb;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.ui.topo.BiLink;
import org.onosproject.ui.topo.LinkHighlight;
import org.onosproject.ui.topo.LinkHighlight.Flavor;

/**
 * Bi-directional link capable of different highlights.
 */
public class SfcLink extends BiLink {

    private boolean primary = false;
    private boolean secondary = false;

    public SfcLink(LinkKey key, Link link) {
        super(key, link);
    }

    @Override
    public LinkHighlight highlight(Enum<?> anEnum) {
        Flavor flavor = primary ? Flavor.PRIMARY_HIGHLIGHT :
            (secondary ? Flavor.SECONDARY_HIGHLIGHT : Flavor.NO_HIGHLIGHT);
        return new LinkHighlight(this.linkId(), Flavor.PRIMARY_HIGHLIGHT);
    }
}

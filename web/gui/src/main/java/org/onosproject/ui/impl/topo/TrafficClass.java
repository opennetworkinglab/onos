/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo;

import org.onosproject.net.intent.Intent;
import org.onosproject.ui.topo.LinkHighlight;

/**
 * Auxiliary data carrier for assigning a highlight class to a set of
 * intents, for visualization in the topology view.
 */
@Deprecated
public class TrafficClass {

    private final LinkHighlight.Flavor flavor;
    private final Iterable<Intent> intents;
    private final boolean showTraffic;

    public TrafficClass(LinkHighlight.Flavor flavor, Iterable<Intent> intents) {
        this(flavor, intents, false);
    }

    public TrafficClass(LinkHighlight.Flavor flavor, Iterable<Intent> intents,
                        boolean showTraffic) {
        this.flavor = flavor;
        this.intents = intents;
        this.showTraffic = showTraffic;
    }

    public LinkHighlight.Flavor flavor() {
        return flavor;
    }

    public Iterable<Intent> intents() {
        return intents;
    }

    public boolean showTraffic() {
        return showTraffic;
    }
}

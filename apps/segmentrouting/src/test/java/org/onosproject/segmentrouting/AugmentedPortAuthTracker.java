/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.segmentrouting;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.ArrayList;
import java.util.List;

/**
 * An augmented implementation of {@link PortAuthTracker}, so that we can
 * instrument its behavior for unit test assertions.
 */
class AugmentedPortAuthTracker extends PortAuthTracker {

    // instrument blocking flow activity, so we can see when we get hits
    final List<ConnectPoint> installed = new ArrayList<>();
    final List<ConnectPoint> cleared = new ArrayList<>();


    void resetMetrics() {
        installed.clear();
        cleared.clear();
    }

    @Override
    void installBlockingFlow(DeviceId d, PortNumber p) {
        super.installBlockingFlow(d, p);
        installed.add(new ConnectPoint(d, p));
    }

    @Override
    void clearBlockingFlow(DeviceId d, PortNumber p) {
        super.clearBlockingFlow(d, p);
        cleared.add(new ConnectPoint(d, p));
    }
}

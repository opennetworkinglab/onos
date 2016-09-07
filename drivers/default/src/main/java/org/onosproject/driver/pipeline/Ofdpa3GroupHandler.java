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

package org.onosproject.driver.pipeline;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

/**
 * Group handler for OFDPA2 pipeline.
 */
public class Ofdpa3GroupHandler extends Ofdpa2GroupHandler {
    @Override
    protected GroupInfo createL2L3Chain(TrafficTreatment treatment, int nextId,
                                        ApplicationId appId, boolean mpls,
                                        TrafficSelector meta) {
        return createL2L3ChainInternal(treatment, nextId, appId, mpls, meta, false);
    }
}

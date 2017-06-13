/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.driver.optical.query;

import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;

import java.util.Set;

/**
 * C-band DWDM channel plan for 100 GHz fixed grid and centered around 193.1 GHz.
 *
 * This supports up to 40 optical channels.
 */
public class CBand100LambdaQuery extends CBandLambdaQuery {
    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        channelSpacing = ChannelSpacing.CHL_100GHZ;
        lambdaCount = 40;
        slotGranularity = 8;

        return super.queryLambdas(port);
    }
}

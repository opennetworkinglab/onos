/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.incubator.net.resource.label;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderService;

/**
 * Means for injecting label information into the core.
 */
@Beta
public interface LabelResourceProviderService extends ProviderService<LabelResourceProvider> {

    /**
     * Signals that a device label resource pool has been detected.
     * @param deviceId device identifier
     * @param beginLabel the begin label number of resource
     * @param endLabel the end label number of resource
     */
    void deviceLabelResourcePoolDetected(DeviceId deviceId,
                                         LabelResourceId beginLabel,
                                         LabelResourceId endLabel);

    /**
     * Signals that an label resource pool has been destroyed.
     * @param deviceId device identifier
     */
    void deviceLabelResourcePoolDestroyed(DeviceId deviceId);
}

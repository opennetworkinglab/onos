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
package org.onosproject.pcep.api;

import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;

/**
 * Configuration to specify device capabilities.
 */
public class DeviceCapability extends Config<DeviceId> {
    public static final String SRCAP = "srCapabaility";
    public static final String LABELSTACKCAP = "labelStackCapability";
    public static final String LOCALLABELCAP = "localLabelCapability";

    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * Gets the SR capability of the router.
     *
     * @return SR capability
     */
    public boolean srCap() {
        String srCap = get(SRCAP, null);
        return srCap != null ?
                Boolean.valueOf(srCap) :
                false;
    }

    /**
     * Gets the label stack capability of the router.
     *
     * @return label stack capability
     */
    public boolean labelStackCap() {
        String labelStackCap = get(LABELSTACKCAP, null);
        return labelStackCap != null ?
                              Boolean.valueOf(labelStackCap) :
                              false;
    }

    /**
     * Gets the local label capability of the router.
     *
     * @return local label capability
     */
    public boolean localLabelCap() {
        String localLabelCap = get(LOCALLABELCAP, null);
        return localLabelCap != null ?
                                      Boolean.valueOf(localLabelCap) :
                                      false;
    }

    /**
     * Sets the SR capability of the router.
     *
     * @param srCap SR capability of the router.
     * @return the capability configuration of the device.
     */
    public DeviceCapability setSrCap(boolean srCap) {
        return (DeviceCapability) setOrClear(SRCAP, srCap);
    }

    /**
     * Sets the label stack capability of the router.
     *
     * @param labelStackCap label stack capability of the router.
     * @return the capability configuration of the device.
     */
    public DeviceCapability setLabelStackCap(boolean labelStackCap) {
        return (DeviceCapability) setOrClear(LABELSTACKCAP, labelStackCap);
    }

    /**
     * Sets the local label capability of the router.
     *
     * @param localLabelCap local label capability of the router.
     * @return the capability configuration of the device.
     */
    public DeviceCapability setLocalLabelCap(boolean localLabelCap) {
        return (DeviceCapability) setOrClear(LOCALLABELCAP, localLabelCap);
    }
}


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

package org.onosproject.net.config.basics;

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;

import java.util.List;
import java.util.Set;

/**
 * Basic configuration for network regions.
 */
public final class BasicRegionConfig extends BasicElementConfig<RegionId> {

    private static final String TYPE = "type";
    private static final String DEVICES = "devices";

    @Override
    public boolean isValid() {
        return hasOnlyFields(ALLOWED, NAME, LATITUDE, LONGITUDE, UI_TYPE,
                RACK_ADDRESS, OWNER, TYPE, DEVICES);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add(NAME, name())
                .add(TYPE, type())
                .add(UI_TYPE, uiType())
                .add(LATITUDE, latitude())
                .add(LONGITUDE, longitude())
                .add(DEVICES, devices())
                .toString();
    }

    /**
     * Returns the region type.
     *
     * @return the region type
     */
    public Region.Type type() {
        String t = get(TYPE, null);
        return t == null ? null : regionTypeFor(t);
    }

    private Region.Type regionTypeFor(String t) {
        try {
            return Region.Type.valueOf(t.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    /**
     * Sets the region type.
     *
     * @param type the region type, or null to unset
     * @return the config of the region
     */
    public BasicRegionConfig type(Region.Type type) {
        String t = type == null ? null : type.name().toLowerCase();
        return (BasicRegionConfig) setOrClear(TYPE, t);
    }

    /**
     * Returns the identities of the devices in this region.
     *
     * @return list of device identifiers
     */
    public List<DeviceId> devices() {
        return object.has(DEVICES) ? getList(DEVICES, DeviceId::deviceId) : null;
    }

    /**
     * Sets the devices of this region.
     *
     * @param devices the device identifiers, or null to unset
     * @return the config of the region
     */
    public BasicRegionConfig devices(Set<DeviceId> devices) {
        return (BasicRegionConfig) setOrClear(DEVICES, devices);
    }
}

/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.security;

import com.google.common.annotations.Beta;

import java.security.BasicPermission;

/**
 * Implementation of API access permission.
 */
@Beta
public class AppPermission extends BasicPermission {

    public enum Type {
        APP_READ,
        APP_EVENT,
        APP_WRITE,
        CONFIG_READ,
        CONFIG_WRITE,
        CONFIG_EVENT,
        CLUSTER_READ,
        CLUSTER_WRITE,
        CLUSTER_EVENT,
        CODEC_READ,
        CODEC_WRITE,
        CLOCK_WRITE,
        DEVICE_KEY_EVENT,
        DEVICE_KEY_READ,
        DEVICE_KEY_WRITE,
        DEVICE_READ,
        DEVICE_EVENT,
        DRIVER_READ,
        DRIVER_WRITE,
        EVENT_READ,
        EVENT_WRITE,
        FLOWRULE_READ,
        FLOWRULE_WRITE,
        FLOWRULE_EVENT,
        GROUP_READ,
        GROUP_WRITE,
        GROUP_EVENT,
        HOST_READ,
        HOST_WRITE,
        HOST_EVENT,
        INTENT_READ,
        INTENT_WRITE,
        INTENT_EVENT,
        LINK_READ,
        LINK_WRITE,
        LINK_EVENT,
        MUTEX_WRITE,
        PACKET_READ,
        PACKET_WRITE,
        PACKET_EVENT,
        PERSISTENCE_WRITE,
        PARTITION_READ,
        PARTITION_EVENT,
        RESOURCE_READ,
        RESOURCE_WRITE,
        RESOURCE_EVENT,
        REGION_READ,
        STATISTIC_READ,
        STORAGE_WRITE,
        TOPOLOGY_READ,
        TOPOLOGY_EVENT,
        TUNNEL_READ,
        TUNNEL_WRITE,
        TUNNEL_EVENT,
        UI_READ,
        UI_WRITE,
        UPGRADE_READ,
        UPGRADE_WRITE,
        UPGRADE_EVENT,
        GLYPH_READ,
        GLYPH_WRITE
    }

    protected Type type;
    /**
     * Creates new application permission using the supplied data.
     * @param name permission name
     */
    public AppPermission(String name) {
        super(name.toUpperCase(), "");
        try {
            type = Type.valueOf(name);
        } catch (IllegalArgumentException e) {
            type = null;
        }
    }

    /**
     * Creates new application permission using the supplied data.
     * @param name permission name
     * @param actions permission action
     */
    public AppPermission(String name, String actions) {
        super(name.toUpperCase(), actions);
        try {
            type = Type.valueOf(name);
        } catch (IllegalArgumentException e) {
            type = null;
        }
    }

    /**
     * Crates new application permission using the supplied data.
     * @param type permission type
     */
    public AppPermission(Type type) {
        super(type.name(), "");
        this.type = type;
    }

    /**
     * Returns type of permission.
     * @return application permission type
     */
    public Type getType() {
        return this.type;
    }

}

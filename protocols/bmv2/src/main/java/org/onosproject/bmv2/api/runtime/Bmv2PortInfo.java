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

package org.onosproject.bmv2.api.runtime;

import com.google.common.base.MoreObjects;
import org.p4.bmv2.thrift.DevMgrPortInfo;

import java.util.Collections;
import java.util.Map;

/**
 * Bmv2 representation of a switch port information.
 */
public final class Bmv2PortInfo {

    private final DevMgrPortInfo portInfo;

    public Bmv2PortInfo(DevMgrPortInfo portInfo) {
        this.portInfo = portInfo;
    }

    public final String ifaceName() {
        return portInfo.getIface_name();
    }

    public final int portNumber() {
        return portInfo.getPort_num();
    }

    public final boolean isUp() {
        return portInfo.isIs_up();
    }

    public final Map<String, String> getExtraProperties() {
        return Collections.unmodifiableMap(portInfo.getExtra());
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(portInfo)
                .toString();
    }
}

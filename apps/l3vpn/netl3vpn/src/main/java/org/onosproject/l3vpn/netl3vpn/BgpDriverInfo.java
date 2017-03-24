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

package org.onosproject.l3vpn.netl3vpn;

/**
 * Representation of BGP configuration required for driver to process.
 */
public class BgpDriverInfo {

    /**
     * Model id level of the BGP information that needed to be added in store.
     */
    private BgpModelIdLevel modIdLevel;

    /**
     * Device id required for the creation of driver model object data.
     */
    private String devId;

    /**
     * Constructs BGP driver info.
     *
     * @param m model id level for BGP
     * @param d device id
     */
    public BgpDriverInfo(BgpModelIdLevel m, String d) {
        modIdLevel = m;
        devId = d;
    }

    /**
     * Returns the model id level of the BGP information to be added.
     *
     * @return model id level
     */
    public BgpModelIdLevel modIdLevel() {
        return modIdLevel;
    }

    /**
     * Returns the device id.
     *
     * @return device id
     */
    public String devId() {
        return devId;
    }
}

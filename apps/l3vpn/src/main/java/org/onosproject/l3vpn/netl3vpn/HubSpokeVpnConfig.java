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
 * Representation of the hub and spoke VPN configuration containing import and
 * export RTs.
 */
public class HubSpokeVpnConfig extends VpnConfig {

    /**
     * Hub import RT value.
     */
    private String hubImpRt;

    /**
     * Hub export RT value.
     */
    private String hubExpRt;

    /**
     * Spoke import RT value.
     */
    private String spokeImpRt;

    /**
     * Spoke export RT value.
     */
    private String spokeExpRt;

    /**
     * Creates hub and spoke VPN config.
     */
    public HubSpokeVpnConfig() {
    }

    /**
     * Returns hub import RT value.
     *
     * @return RT value
     */
    public String hubImpRt() {
        return hubImpRt;
    }

    /**
     * Sets hub import RT value.
     *
     * @param hubImpRt RT value
     */
    public void hubImpRt(String hubImpRt) {
        this.hubImpRt = hubImpRt;
    }

    /**
     * Returns hub export RT value.
     *
     * @return RT value
     */
    public String hubExpRt() {
        return hubExpRt;
    }

    /**
     * Sets hub export RT value.
     *
     * @param hubExpRt RT value
     */
    public void hubExpRt(String hubExpRt) {
        this.hubExpRt = hubExpRt;
    }

    /**
     * Returns spoke import RT value.
     *
     * @return RT value
     */
    public String spokeImpRt() {
        return spokeImpRt;
    }

    /**
     * Sets spoke import RT value.
     *
     * @param spokeImpRt RT value
     */
    public void spokeImpRt(String spokeImpRt) {
        this.spokeImpRt = spokeImpRt;
    }

    /**
     * Returns spoke export RT value.
     *
     * @return RT value
     */
    public String spokeExpRt() {
        return spokeExpRt;
    }

    /**
     * Sets spoke export RT value.
     *
     * @param spokeExpRt RT value
     */
    public void spokeExpRt(String spokeExpRt) {
        this.spokeExpRt = spokeExpRt;
    }
}

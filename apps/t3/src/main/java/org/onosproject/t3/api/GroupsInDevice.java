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

package org.onosproject.t3.api;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.group.Group;

import java.util.List;

/**
 * Class to represent the groups in a device for a given output and packet.
 */
//FIXME consider name change.
public class GroupsInDevice {

    private ConnectPoint output;
    private List<Group> groups;
    private TrafficSelector selector;

    /**
     * Saves the given groups for the output connect point and the selector.
     *
     * @param output   the output connect point
     * @param groups   the groups
     * @param selector the selector representing the final packet
     */
    public GroupsInDevice(ConnectPoint output, List<Group> groups, TrafficSelector selector) {

        this.output = output;
        this.groups = groups;
        this.selector = selector;
    }

    /**
     * Returns the output connect point.
     *
     * @return the connect point
     */
    public ConnectPoint getOutput() {
        return output;
    }

    /**
     * Returns the groups.
     *
     * @return groups.
     */
    public List<Group> getGroups() {
        return groups;
    }

    /**
     * Returns the final packet after traversing the network.
     *
     * @return the selector with packet info
     */
    public TrafficSelector getFinalPacket() {
        return selector;
    }

    /**
     * Updates the final packet.
     *
     * @param updatedPacket the updated final packet
     */
    public void setFinalPacket(TrafficSelector updatedPacket) {
        selector = updatedPacket;
    }

    @Override
    public String toString() {
        return "GroupsInDevice{" +
                "output=" + output +
                ", groups=" + groups +
                ", selector=" + selector +
                '}';
    }

}

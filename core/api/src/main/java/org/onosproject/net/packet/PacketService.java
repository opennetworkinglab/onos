/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.packet;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for intercepting data plane packets and for emitting synthetic
 * outbound packets.
 */
public interface PacketService {

    // TODO: ponder better ordering scheme that does not require absolute numbers

    /**
     * Adds the specified processor to the list of packet processors.
     * It will be added into the list in the order of priority. The higher
     * numbers will be processing the packets after the lower numbers.
     *
     * @param processor processor to be added
     * @param priority  priority in the reverse natural order
     * @throws java.lang.IllegalArgumentException if a processor with the
     *                                            given priority already exists
     */
    void addProcessor(PacketProcessor processor, int priority);

    // TODO allow processors to register for particular types of packets

    /**
     * Removes the specified processor from the processing pipeline.
     *
     * @param processor packet processor
     */
    void removeProcessor(PacketProcessor processor);

    /**
     * Adds the specified filter to the list of packet filters.
     * It will be added into the list in the order in which it is added.
     *
     * @param filter filter to be added
     */
    default void addFilter(PacketInFilter filter) {}


    /**
     * Removes the specified filter from the filters list.
     *
     * @param filter filter to be removed
     */
    default void removeFilter(PacketInFilter filter) {}

    /**
     * Returns priority bindings of all registered packet processor entries.
     *
     * @return list of existing packet processor entries
     */
    List<PacketProcessorEntry> getProcessors();

    /**
     * Requests that packets matching the given selector are punted from the
     * dataplane to the controller.
     *
     * @param selector the traffic selector used to match packets
     * @param priority the priority of the rule
     * @param appId    the application ID of the requester
     */
    void requestPackets(TrafficSelector selector, PacketPriority priority,
                        ApplicationId appId);


    /**
     * Requests that packets matching the given selector are punted from the
     * dataplane to the controller. If a deviceId is specified then the
     * packet request is only installed at the device represented by that
     * deviceId.
     *
     * @param selector the traffic selector used to match packets
     * @param priority the priority of the rule
     * @param appId    the application ID of the requester
     * @param deviceId an optional deviceId
     */
    void requestPackets(TrafficSelector selector, PacketPriority priority,
                        ApplicationId appId, Optional<DeviceId> deviceId);

    /**
     * Cancels previous packet requests for packets matching the given
     * selector to be punted from the dataplane to the controller.
     *
     * @param selector the traffic selector used to match packets
     * @param priority the priority of the rule
     * @param appId    the application ID of the requester
     */
    void cancelPackets(TrafficSelector selector, PacketPriority priority,
                       ApplicationId appId);

    /**
     * Cancels previous packet requests for packets matching the given
     * selector to be punted from the dataplane to the controller. If a
     * deviceId is specified then the packet request is only withdrawn from
     * the device represented by that deviceId.
     *
     * @param selector the traffic selector used to match packets
     * @param priority the priority of the rule
     * @param appId    the application ID of the requester
     * @param deviceId an optional deviceId
     */
    void cancelPackets(TrafficSelector selector, PacketPriority priority,
                       ApplicationId appId, Optional<DeviceId> deviceId);

    /**
     * Returns list of all existing requests ordered by priority.
     *
     * @return list of existing packet requests
     */
    List<PacketRequest> getRequests();

    /**
     * Emits the specified outbound packet onto the network.
     *
     * @param packet outbound packet
     */
    void emit(OutboundPacket packet);

    /**
     * Get the list of packet filters present in ONOS.
     *
     * @return List of packet filters
     */
    default List<PacketInFilter> getFilters() {
        return new ArrayList<>();
    }

    /**
     * Clear all packet filters in one shot.
     *
     */
    default void clearFilters() {}


}

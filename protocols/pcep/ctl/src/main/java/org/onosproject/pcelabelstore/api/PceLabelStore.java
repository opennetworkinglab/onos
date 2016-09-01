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
package org.onosproject.pcelabelstore.api;

import java.util.List;

import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import java.util.Map;

/**
 * Abstraction of an entity providing pool of available labels to devices, links and tunnels.
 */
public interface PceLabelStore {
    /**
     * Checks whether device id is present in global node label store.
     *
     * @param id device id
     * @return success of failure
     */
    boolean existsGlobalNodeLabel(DeviceId id);

    /**
     * Checks whether link is present in adjacency label store.
     *
     * @param link link between devices
     * @return success of failure
     */
    boolean existsAdjLabel(Link link);

    /**
     * Checks whether tunnel id is present in tunnel info store.
     *
     * @param tunnelId tunnel id
     * @return success of failure
     */
    boolean existsTunnelInfo(TunnelId tunnelId);

    /**
     * Retrieves the node label count.
     *
     * @return node label count
     */
    int getGlobalNodeLabelCount();

    /**
     * Retrieves the adjacency label count.
     *
     * @return adjacency label count
     */
    int getAdjLabelCount();

    /**
     * Retrieves the tunnel info count.
     *
     * @return tunnel info count
     */
    int getTunnelInfoCount();

    /**
     * Retrieves device id and label pairs collection from global node label store.
     *
     * @return collection of device id and label pairs
     */
    Map<DeviceId, LabelResourceId> getGlobalNodeLabels();

    /**
     * Retrieves link and label pairs collection from adjacency label store.
     *
     * @return collection of link and label pairs
     */
    Map<Link, LabelResourceId> getAdjLabels();

    /**
     * Retrieves tunnel id and pcecc tunnel info pairs collection from tunnel info store.
     *
     * @return collection of tunnel id and pcecc tunnel info pairs
     */
    Map<TunnelId, List<LspLocalLabelInfo>> getTunnelInfos();

    /**
     * Retrieves node label for specified device id.
     *
     * @param id device id
     * @return node label
     */
    LabelResourceId getGlobalNodeLabel(DeviceId id);

    /**
     * Retrieves adjacency label for specified link.
     *
     * @param link between devices
     * @return adjacency label
     */
    LabelResourceId getAdjLabel(Link link);

    /**
     * Retrieves local label info with tunnel consumer id from tunnel info store.
     *
     * @param tunnelId tunnel id
     * @return pcecc tunnel info
     */
    List<LspLocalLabelInfo> getTunnelInfo(TunnelId tunnelId);

    /**
     * Stores node label into global node label store.
     *
     * @param deviceId device id
     * @param labelId node label id
     */
    void addGlobalNodeLabel(DeviceId deviceId, LabelResourceId labelId);

    /**
     * Stores adjacency label into adjacency label store.
     *
     * @param link link between nodes
     * @param labelId link label id
     */
    void addAdjLabel(Link link, LabelResourceId labelId);

    /**
     * Stores local label info with tunnel consumer id into tunnel info store for specified tunnel id.
     *
     * @param tunnelId tunnel id
     * @param lspLocalLabelInfoList local label info
     */
    void addTunnelInfo(TunnelId tunnelId, List<LspLocalLabelInfo> lspLocalLabelInfoList);

    /**
     * Removes device label from global node label store for specified device id.
     *
     * @param id device id
     * @return success or failure
     */
    boolean removeGlobalNodeLabel(DeviceId id);

    /**
     * Removes adjacency label from adjacency label store for specified link information.
     *
     * @param link between nodes
     * @return success or failure
     */
    boolean removeAdjLabel(Link link);

    /**
     * Removes local label info with tunnel consumer id from tunnel info store for specified tunnel id.
     *
     * @param tunnelId tunnel id
     * @return success or failure
     */
    boolean removeTunnelInfo(TunnelId tunnelId);

    /**
     * Adds lsrid to device id mapping.
     *
     * @param lsrId lsrId of the device
     * @param deviceId device id
     * @return success or failure
     */
    boolean addLsrIdDevice(String lsrId, DeviceId deviceId);

    /**
     * Removes lsrid to device id mapping.
     *
     * @param lsrId lsrId of the device
     * @return success or failure
     */
    boolean removeLsrIdDevice(String lsrId);

    /**
     * Gets lsrid to device id mapping.
     *
     * @param lsrId lsrId of the device
     * @return device id of the lsrId
     */
    DeviceId getLsrIdDevice(String lsrId);

    /**
     * Adds lsrId of the PCC in form of device id for the PCC for which sync is pending due to non-availability of BGP.
     * device.
     *
     * @param lsrId LSR id of the PCC in form of device id
     * @return success or failure
     */
    public boolean addPccLsr(DeviceId lsrId);

    /**
     * Removes lsrId of the PCC in form of device id for the PCC for which pending sync is done.
     *
     * @param lsrId LSR id of the PCC in form of device id
     * @return success or failure
     */
    public boolean removePccLsr(DeviceId lsrId);

    /**
     * Gets lsrId of the PCC in form of device id.
     *
     * @param lsrId LSR id of the PCC in form of device id
     * @return success or failure
     */
    public boolean hasPccLsr(DeviceId lsrId);
}

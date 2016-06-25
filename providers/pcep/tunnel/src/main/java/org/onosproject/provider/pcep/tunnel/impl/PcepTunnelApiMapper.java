/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.pcep.tunnel.impl;

import java.util.HashMap;
import java.util.Map;

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entity to provide tunnel DB and mapping for request/response between CORE to PCEP
 * and PCEP to PCC.
 */
public class PcepTunnelApiMapper {
    protected static final Logger log = LoggerFactory.getLogger(PcepTunnelApiMapper.class);

    static final String PROVIDER_ID = "org.onosproject.provider.tunnel.pcep";
    // Map to store all the tunnel requests.
    private Map<Integer, PcepTunnelData> tunnelRequestQueue;
    //Map to store all core related tunnel requests.
    private Map<TunnelId, PcepTunnelData> coreTunnelRequestQueue;
    //Map to store all the created tunnels.
    private Map<Integer, PcepTunnelData> tunnelDB;
    // Map to store the tunnel ids, given by core and given by pcc.
    private Map<TunnelId, Integer> tunnelIdMap;

    TunnelProviderService tunnelApiMapperservice;

    /**
     * Default constructor.
     */
    public PcepTunnelApiMapper() {
        //TODO check if the map need to initialize
        tunnelRequestQueue = new HashMap<Integer, PcepTunnelData>();
        coreTunnelRequestQueue = new HashMap<TunnelId, PcepTunnelData>();
        tunnelDB = new HashMap<Integer, PcepTunnelData>();
        tunnelIdMap = new HashMap<TunnelId, Integer>();
    }

    /**
     * Add tunnels to tunnel Request queues.
     *
     * @param srpId srp id
     * @param pcepTunnelData pcep tunnel data
     */
    public void addToTunnelRequestQueue(int srpId, PcepTunnelData pcepTunnelData) {
        tunnelRequestQueue.put(new Integer(srpId), pcepTunnelData);
        log.debug("Tunnel Added to TunnelRequestQueue");
    }

    /**
     * Map between Tunnel ID and pcc provided Tunnel ID.
     *
     * @param pcepTunnelData pcep tunnel data
     */
    public void addToTunnelIdMap(PcepTunnelData pcepTunnelData) {
        int value = pcepTunnelData.statefulIpv4IndentifierTlv().getTunnelId() & 0xFFFF;
        tunnelIdMap.put(pcepTunnelData.tunnel().tunnelId(), (new Integer(value)));
        log.debug("Tunnel ID Added to tunnelIdMap");
    }

    /**
     * Add tunnels to core tunnel request queue.
     *
     * @param pcepTunnelData pcep tunnel data
     */
    public void addToCoreTunnelRequestQueue(PcepTunnelData pcepTunnelData) {
        coreTunnelRequestQueue.put(pcepTunnelData.tunnel().tunnelId(), pcepTunnelData);
        log.debug("Tunnel Added to CoreTunnelRequestQueue");
    }

    /**
     * Removes tunnels from the core tunnel request queue.
     *
     * @param tunnelId tunnel id
     */
    public void removeFromCoreTunnelRequestQueue(TunnelId tunnelId) {
        coreTunnelRequestQueue.remove(tunnelId);
        log.debug("Tunnnel create response sent to core and removed from CoreTunnelRequestQueue");
    }

    /**
     * Handle the report which comes after initiate message.
     *
     * @param srpId srp id
     * @param pcepTunnelData pcep tunnel data
     */
    public void handleCreateTunnelRequestQueue(int srpId, PcepTunnelData pcepTunnelData) {

        int value = tunnelIdMap.get(pcepTunnelData.tunnel().tunnelId());
        tunnelDB.put(new Integer(value), pcepTunnelData);
        tunnelRequestQueue.remove(new Integer(srpId), pcepTunnelData);
        log.debug("Tunnel Added to TunnelDBQueue and removed from TunnelRequestQueue. tunnel id {}"
                + (new Integer(value)).toString());
    }

    /**
     * Handle report which comes for update message.
     *
     * @param srpId srp id
     * @param pcepTunnelData pcep tunnel data
     */
    public void handleUpdateTunnelRequestQueue(int srpId, PcepTunnelData pcepTunnelData) {
        if (pcepTunnelData.rptFlag()) {
            pcepTunnelData.setRptFlag(false);
            int value = tunnelIdMap.get(pcepTunnelData.tunnel().tunnelId());
            tunnelDB.put(new Integer(value), pcepTunnelData);
            tunnelRequestQueue.remove(new Integer(srpId), pcepTunnelData);
            log.debug("Tunnel Added to TunnelDBQueue and removed from TunnelRequestQueue. tunnel id {}",
                      (new Integer(value)).toString());
        } else {
            pcepTunnelData.setRptFlag(true);
            tunnelRequestQueue.put(new Integer(srpId), pcepTunnelData);
            log.debug("Tunnel updated in TunnelRequestQueue");
        }
    }

    /**
     * Handle report for tunnel Release request.
     *
     * @param srpId srp id
     * @param pcepTunnelData pcep tunnel data
     */
    public void handleRemoveFromTunnelRequestQueue(int srpId, PcepTunnelData pcepTunnelData) {

        int value = tunnelIdMap.get(pcepTunnelData.tunnel().tunnelId());
        tunnelIdMap.remove(pcepTunnelData.tunnel().tunnelId());
        tunnelDB.remove(new Integer(value));
        tunnelRequestQueue.remove(srpId);
        log.debug("Tunnel removed from  TunnelDBQueue and TunnelRequestQueue");
    }

    /**
     * Returns PcepTunnelData from the tunnel request queue.
     *
     * @param srpId srp id
     * @return PcepTunnelData pcep tunnel data
     */
    public PcepTunnelData getDataFromTunnelRequestQueue(int srpId) {
        return tunnelRequestQueue.get(new Integer(srpId));

    }

    /**
     * Returns PcepTunnelData from the tunnel DB.
     *
     * @param tunnelId tunnel id
     * @return PcepTunnelData pcep tunnel data
     */
    public PcepTunnelData getDataFromTunnelDBQueue(TunnelId tunnelId) {
        int value = tunnelIdMap.get(tunnelId);
        return tunnelDB.get((new Integer(value)));
    }

    /**
     * Checks whether the tunnel exist in tunnel request queue.
     *
     * @param srpId srp id
     * @return true if tunnel exist in reuest queue, false otherwise
     */
    public boolean checkFromTunnelRequestQueue(int srpId) {
        boolean retValue = tunnelRequestQueue.containsKey(srpId);
        return retValue;
    }

    /**
     * Returns whether tunnel exist in tunnel db.
     *
     * @param tunnelId tunnel id
     * @return true/false if the tunnel exists in the tunnel db
     */
    public boolean checkFromTunnelDBQueue(TunnelId tunnelId) {
        int value = tunnelIdMap.get(tunnelId);
        boolean retValue = tunnelDB.containsKey((new Integer(value)));
        return retValue;
    }
}

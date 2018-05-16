/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.utils.tapi;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.ElementId;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * TAPI instance resolver.
 * FIXME: This resolver should provide DCS read cache
 */
public class TapiResolver {

    private final Logger log = getLogger(getClass());

    private List<TapiNodeRef> tapiNodeRefList = new ArrayList<>();
    private List<TapiNepRef> tapiNepRefList = new ArrayList<>();


    /**
     * Check existence of TAPI node associated with deviceId.
     * @param deviceId search key
     * @return boolean
     */
    public boolean hasNodeRef(ElementId deviceId) {
        return tapiNodeRefList.stream()
                .anyMatch(node -> node.getDeviceId().equals(deviceId));
    }

    /**
     * Check existence of TAPI nep associated with ConnectPoint.
     * @param cp search key
     * @return TapiNepRef
     */
    public boolean hasNepRef(ConnectPoint cp) {
        return tapiNepRefList.stream()
                .anyMatch(nep -> nep.getConnectPoint().equals(cp));
    }

    /**
     * Check existence of TAPI nep associated with TAPI sipId.
     * @param sipId search key
     * @return TapiNepRef
     */
    public boolean hasNepRef(String sipId) {
        return tapiNepRefList.stream()
                .anyMatch(nep -> nep.getSipId().equals(sipId));
    }

    /**
     * Resolve TAPI node associated with deviceId.
     * @param deviceId search key
     * @return TapiNodeRef
     */
    public TapiNodeRef getNodeRef(ElementId deviceId) {
        TapiNodeRef ret = null;
        try {
            ret = tapiNodeRefList.stream()
                    .filter(node -> node.getDeviceId().equals(deviceId))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.error("Node not found associated with {}", deviceId);
            throw e;
        }
        return ret;
    }

    /**
     * Resolve TAPI nep associated with ConnectPoint.
     * @param cp search key
     * @return TapiNepRef
     */
    public TapiNepRef getNepRef(ConnectPoint cp) {
        TapiNepRef ret = null;
        try {
            ret = tapiNepRefList.stream()
                    .filter(nep -> nep.getConnectPoint().equals(cp))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.error("Nep not found associated with {}", cp);
            throw e;
        }
        return ret;
    }

    /**
     * Resolve TAPI nep associated with TAPI sipId.
     * @param sipId search key
     * @return TapiNepRef
     */
    public TapiNepRef getNepRef(String sipId) {
        TapiNepRef ret = null;
        try {
            ret = tapiNepRefList.stream()
                    .filter(nep -> nep.getSipId().equals(sipId))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.error("Nep not found associated with {}", sipId);
            throw e;
        }
        return ret;
    }

    public TapiResolver addNodeRef(TapiNodeRef nodeRef) {
        tapiNodeRefList.add(nodeRef);
        log.info("Nodes: {}", tapiNodeRefList);
        return this;
    }

    public TapiResolver addNepRef(TapiNepRef nepRef) {
        tapiNepRefList.add(nepRef);
        log.info("Neps: {}", tapiNepRefList);
        return this;
    }

}

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

package org.onosproject.odtn;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.ElementId;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiNodeRef;

/**
 * TAPI Yang object resolver.
 * <p>
 * This service works as TAPI Yang object cache, and provide
 * TAPI object resolve service, e.g. TAPI Node/NodeEdgePoint/Link.
 * This is independent with DCS and uses not DCS ModelObject directly
 * but DCS-independent object like TapiNodeRef/TapiNepRef.
 * This should work together with TapiDataProducer, which acts as
 * data producer by reading TAPI modelObjects from DCS and convert them
 * to DCS-independent objects.
 */
public interface TapiResolver {

    /**
     * Check existence of TAPI node associated with deviceId.
     *
     * @param deviceId search key
     * @return boolean
     */
    boolean hasNodeRef(ElementId deviceId);

    /**
     * Check existence of TAPI nep associated with ConnectPoint.
     *
     * @param cp search key
     * @return TapiNepRef
     */
    boolean hasNepRef(ConnectPoint cp);

    /**
     * Check existence of TAPI nep associated with TAPI sipId.
     *
     * @param sipId search key
     * @return TapiNepRef
     */
    boolean hasNepRef(String sipId);

    /**
     * Resolve TAPI node associated with deviceId.
     *
     * @param deviceId search key
     * @return TapiNodeRef
     * @throws NoSuchElementException if target not found
     */
    TapiNodeRef getNodeRef(ElementId deviceId);

    /**
     * Resolve TAPI node with Node unique keys.
     *
     * @param nodeRef Node reference object which has node unique keys.
     * @return TapiNodeRef
     * @throws NoSuchElementException if target not found
     */
    TapiNodeRef getNodeRef(TapiNodeRef nodeRef);

    /**
     * Get all NodeRefs.
     *
     * @return List of all NodeRefs
     */
    List<TapiNodeRef> getNodeRefs();

    /**
     * Apply filter and get filtered NodeRefs.
     *
     * @param filter key value map for filter
     * @return List of filtered NodeRefs, which matches all of {@code filter}
     */
    List<TapiNodeRef> getNodeRefs(Map<String, String> filter);

    /**
     * Resolve TAPI nep with Nep unique keys.
     *
     * @param nepRef Nep reference object which has Nep unique keys.
     * @return TapiNepRef
     * @throws NoSuchElementException if target not found
     */
    TapiNepRef getNepRef(TapiNepRef nepRef);

    /**
     * Resolve TAPI nep associated with ConnectPoint.
     *
     * @param cp search key
     * @return TapiNepRef
     * @throws NoSuchElementException if target not found
     */
    TapiNepRef getNepRef(ConnectPoint cp);

    /**
     * Resolve TAPI nep associated with TAPI sipId.
     *
     * @param sipId search key
     * @return TapiNepRef
     * @throws NoSuchElementException if target not found
     */
    TapiNepRef getNepRef(String sipId);

    /**
     * Get all NepRefs.
     *
     * @return List of all NepRefs
     */
    List<TapiNepRef> getNepRefs();

    /**
     * Apply filter and get filtered NepRefs.
     *
     * @param filter key value map for filter
     * @return List of filtered NepRefs, which matches all of {@code filter}
     */
    List<TapiNepRef> getNepRefs(Map<String, String> filter);

    /**
     * Inform the cache is already got dirty and let it update cache.
     * The cache update process is conducted when next resolve request
     * (hasXXX or getXXX) comes.
     */
    void makeDirty();

}

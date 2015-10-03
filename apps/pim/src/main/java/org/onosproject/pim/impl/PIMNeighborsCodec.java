/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.pim.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;

import java.util.HashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * PIM neighbors Codec.
 */
public class PIMNeighborsCodec extends JsonCodec<HashMap<ConnectPoint, PIMNeighbors>> {
    // JSON field names
    //Return Name
    private static final String CPNBRLIST = "connect_point_list";

    // PIM Neightbors Fields
    private static final String IP = "ip";
    private static final String PRIORITY = "priority";
    private static final String NBRLIST = "neighbor_list";

    // PIM neighbor Files
    private static final String DR = "designated";
    private static final String NBR_IP = "ip";
    private static final String PR = "priority";
    private static final String HOLDTIME = "hold_time";

    /**
     * Encode the PIM Neighbors.
     *
     * @param cpn ConnectPoint neighbors
     * @param context encoding context
     *
     * @return Encoded neighbors used by CLI and REST
     */
    @Override
    public ObjectNode encode(HashMap<ConnectPoint, PIMNeighbors> cpn, CodecContext context) {
        checkNotNull(cpn, "Pim Neighbors cannot be null");

        ObjectNode pimNbrJsonCodec = context.mapper().createObjectNode();
        ArrayNode cpnList = context.mapper().createArrayNode();

        for (PIMNeighbors pn: cpn.values()) {
            // get the PimNeighbors Obj, contains Neighbors list
            // create the json object for a single Entry in the Neighbors list
            ObjectNode cp = context.mapper().createObjectNode();
            cp.put(IP, pn.getOurIpAddress().toString());
            cp.put(PRIORITY, String.valueOf(pn.getOurPriority()));

            // create the array for the neighbors list
            ArrayNode nbrsList = context.mapper().createArrayNode();
            for (PIMNeighbor nbr : pn.getOurNeighborsList().values()) {
                nbrsList.add(neighbor(nbr, context));
            }
            // adds pim neighbor to list
            cp.set(NBRLIST, nbrsList);
            // adds to arraynode which will represent the connect point neighbors hash map.
            cpnList.add(cp);
        }
        pimNbrJsonCodec.set(CPNBRLIST, cpnList);
        return pimNbrJsonCodec;
    }

    /**
     * Encode a single PIM Neighbor.
     *
     * @param nbr the neighbor to be encoded
     * @param context encoding context
     * @return the encoded neighbor
     */
    private ObjectNode neighbor(PIMNeighbor nbr, CodecContext context) {
        return context.mapper().createObjectNode()
                .put(DR, Boolean.toString(nbr.isDr()))
                .put(NBR_IP, nbr.getPrimaryAddr().toString())
                .put(PR, String.valueOf(nbr.getPriority()))
                .put(HOLDTIME, String.valueOf(nbr.getHoldtime()));
    }
}

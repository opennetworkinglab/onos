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
package org.onosproject.mfwd.impl;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

import org.onlab.packet.IpPrefix;

import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Encode and Decode the Multicast Route Table in JSON for CLI and REST commands.
 */
public class MRibCodec extends JsonCodec<McastRouteTable> {

    private final Logger log = getLogger(getClass());
    private static final String SOURCE_ADDRESS = "sourceAddress";
    private static final String GROUP_ADDRESS = "groupAddress";
    private static final String INGRESS_POINT = "ingressPoint";
    private static final String EGRESS_POINT = "egressPoint";
    private static final String MCASTCONNECTPOINT = "McastConnectPoint";
    private static final String ELEMENTID = "elementId";
    private static final String PORTNUMBER = "portNumber";
    private static final String MCAST_GROUP = "mcastGroup";

    /**
     * Encode the MRIB into json format.
     *
     * @param mcastRouteTable McastRouteTable
     * @param context CodecContext
     * @return result ObjectNode
     */
    @Override
    public ObjectNode encode(McastRouteTable mcastRouteTable, CodecContext context) {

        final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        final ObjectNode macastRouteTabNode = nodeFactory.objectNode();
        ArrayNode mcastGroupNode = context.mapper().createArrayNode();
        Optional<McastRouteTable> mcastRouteTabOpt = Optional.ofNullable(mcastRouteTable);

        //checking whether the McastRouteTable is present.
        if (mcastRouteTabOpt.isPresent()) {
            Map<IpPrefix, McastRouteGroup> mrib4 = mcastRouteTabOpt.get().getMrib4();
            Optional<Map<IpPrefix, McastRouteGroup>> mrib4Opt = Optional.ofNullable(mrib4);

            //checking whether the mrib4 is present.
            if (mrib4Opt.isPresent()) {

                for (McastRouteGroup mg : mrib4Opt.get().values()) {
                    Collection<McastRouteSource> mcastRoute = mg.getSources().values();
                    Optional<Collection<McastRouteSource>> mcastRouteOpt = Optional.ofNullable(mcastRoute);

                    //checking whether the McastRouteSource List is present.
                    if (mcastRouteOpt.isPresent()) {
                        for (McastRouteSource mcastRouteSource : mcastRouteOpt.get()) {
                            mcastGroupNode.add(createMcastGroupNode(mcastRouteSource, context));
                        }
                        macastRouteTabNode.put(MCAST_GROUP, mcastGroupNode);
                    }
                }
            }
        }
        return macastRouteTabNode;
    }
    /**
     * Method for creating the McastGroup object node.
     *
     * @param mcastRouteSource McastRouteSource
     */
    private ObjectNode createMcastGroupNode(McastRouteSource mcastRouteSource, CodecContext context) {

        final ObjectNode mcastGroupNode = context.mapper().createObjectNode();
        final ObjectNode ingressNode = context.mapper().createObjectNode();
        final ObjectNode egressNode = context.mapper().createObjectNode();
        final ArrayNode jsonLabelIds = context.mapper().createArrayNode();
        final String sAddr = mcastRouteSource.getSaddr().toString();
        final String gAddr = mcastRouteSource.getGaddr().toString();

        Optional<String> saddrOpt = Optional.ofNullable(sAddr);
        Optional<String> gaddrOpt = Optional.ofNullable(gAddr);

        //checking source address and group address are present.
        if (saddrOpt.isPresent() && gaddrOpt.isPresent()) {
            mcastGroupNode.put(SOURCE_ADDRESS, saddrOpt.get().toString());
            mcastGroupNode.put(GROUP_ADDRESS, gaddrOpt.get().toString());
            McastConnectPoint mcastIngCP = mcastRouteSource.getIngressPoint();
            Optional<McastConnectPoint> mcastIngCPOpt = Optional.ofNullable(mcastIngCP);

            //checking whether the ingress connection point is present.
            if (mcastIngCPOpt.isPresent()) {
                ingressNode.put(MCASTCONNECTPOINT, mcastConnectPoint(mcastIngCPOpt.get(), context));
            }

            mcastGroupNode.put(INGRESS_POINT , ingressNode);
            Set<McastConnectPoint> mcastEgCPSet = mcastRouteSource.getEgressPoints();
            Optional<Set<McastConnectPoint>> mcastEgCPOpt = Optional.ofNullable(mcastEgCPSet);

            //checking whether the egress connection points are present.
            if (mcastEgCPOpt.isPresent()) {
                for (final McastConnectPoint mcastConnectPoint : mcastEgCPOpt.get()) {
                    jsonLabelIds.add(mcastConnectPoint(mcastConnectPoint, context));
                }
            }

            egressNode.put(MCASTCONNECTPOINT , jsonLabelIds);
            mcastGroupNode.put(EGRESS_POINT , egressNode);
        }
        return mcastGroupNode;
    }

    /**
     * Method for creating the McastConnectPoint object node.
     *
     * @param mcastConnectPoint McastConnectPoint
     * @param context CodecContext
     * @return mcastCpNode ObjectNode
     */
    private ObjectNode mcastConnectPoint(McastConnectPoint mcastConnectPoint, CodecContext context) {
        final ObjectNode mcastCpNode = context.mapper().createObjectNode();
        mcastCpNode.put(ELEMENTID , mcastConnectPoint.getConnectPoint().elementId().toString());
        mcastCpNode.put(PORTNUMBER , mcastConnectPoint.getConnectPoint().port().toLong());
        return mcastCpNode;
    }

    /**
     * Decode json format and insert into the flow table.
     *
     * @param json ObjectNode
     * @param context CodecContext
     * @return mr McastRouteBase
     */
    @Override
    public McastRouteTable decode(ObjectNode json, CodecContext context) {

        String macAddr = null;
        String portNo = null;
        String sAddr = json.path(SOURCE_ADDRESS).asText();
        String gAddr = json.path(GROUP_ADDRESS).asText();
        JsonNode inPntObjNode = (JsonNode) json.path(INGRESS_POINT);
        JsonNode egPntArrNode = (JsonNode) json.path(EGRESS_POINT);

        log.debug("sAddr :" + sAddr + " gAddr :" + gAddr + " inPntObjNode :" + inPntObjNode);
        log.debug("egPntArrNode :" + egPntArrNode.toString());

        McastRouteTable mrib = McastRouteTable.getInstance();
        McastRouteBase mr = mrib.addRoute(sAddr, gAddr);
        Optional<JsonNode> inPntOpt = Optional.ofNullable(inPntObjNode);

        if (inPntOpt.isPresent()) {

            JsonNode inMcastCP = inPntOpt.get().path(MCASTCONNECTPOINT);
            Optional<JsonNode> inCpOpt = Optional.ofNullable(inMcastCP);

            if (inCpOpt.isPresent()) {
                macAddr = inCpOpt.get().path(ELEMENTID).asText();
                portNo = inCpOpt.get().path(PORTNUMBER).asText();
                mr.addIngressPoint(macAddr + "/" + Long.parseLong(portNo));
            }
        }

        Optional<JsonNode> egPntOpt = Optional.ofNullable(egPntArrNode);

        if (egPntOpt.isPresent()) {
            JsonNode egMcastCP = egPntOpt.get().path(MCASTCONNECTPOINT);
            Optional<JsonNode> egMcCpOpt = Optional.ofNullable(egMcastCP);

            if (egMcCpOpt.isPresent()) {
                Iterator<JsonNode> egCpIt = egMcCpOpt.get().elements();

                while (egCpIt.hasNext()) {

                    JsonNode egMcastCPObj = egCpIt.next();
                    Optional<JsonNode> egMcCpObOpt = Optional.ofNullable(egMcastCPObj);
                    if (egMcCpObOpt.isPresent()) {
                        macAddr = egMcCpObOpt.get().path(ELEMENTID).asText();
                        portNo = egMcCpObOpt.get().path(PORTNUMBER).asText();
                        log.debug("macAddr egPort : " + macAddr + " portNo egPort :" + portNo);
                        mr.addEgressPoint(macAddr + "/" + Long.parseLong(portNo), McastConnectPoint.JoinSource.STATIC);
                    }
                }
            }
        }
       return mrib;
    }
}

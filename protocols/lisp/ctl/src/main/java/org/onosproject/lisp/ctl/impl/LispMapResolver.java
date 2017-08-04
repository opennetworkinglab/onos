/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.lisp.ctl.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.lisp.msg.protocols.DefaultLispEncapsulatedControl.DefaultEcmBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapReply.DefaultReplyBuilder;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispEncapsulatedControl;
import org.onosproject.lisp.msg.protocols.LispEncapsulatedControl.EcmBuilder;
import org.onosproject.lisp.msg.protocols.LispLocator;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord.MapRecordBuilder;
import org.onosproject.lisp.msg.protocols.LispMapReply.ReplyBuilder;
import org.onosproject.lisp.msg.protocols.LispMapReplyAction;
import org.onosproject.lisp.msg.protocols.LispMapRequest;
import org.onosproject.lisp.msg.protocols.LispMapReply;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispIpAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * LISP map resolver class.
 * Handles map-request message and acknowledges with map-reply message.
 */
public final class LispMapResolver {

    private static final Logger log = LoggerFactory.getLogger(LispMapResolver.class);

    private static final int ECM_DST_PORT = 4342;
    private static final int NEGATIVE_REPLY_DST_PORT = 4342;
    private static final int MAP_REPLY_RECORD_TTL = 15;
    private static final short MAP_VERSION_NUMBER = 0;
    private static final String NO_ITR_RLOCS_MSG =
                                "No ITR RLOC is found, cannot respond to ITR.";
    private static final String NO_ETR_RLOCS_MSG =
                                "No ETR RLOC is found, cannot relay to ETR.";
    private static final String NO_MAP_INFO_MSG  = "Map information is not found.";

    private LispMappingDatabase mapDb = LispExpireMapDatabase.getInstance();

    // non-instantiable (except for our Singleton)
    private LispMapResolver() {
    }

    static LispMapResolver getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Handles encapsulated control message and replies with map-reply message.
     *
     * @param message encapsulated control message
     * @return map-reply message
     */
    List<LispMessage> processMapRequest(LispMessage message) {

        LispEncapsulatedControl ecm = (LispEncapsulatedControl) message;
        LispMapRequest request = (LispMapRequest) ecm.getControlMessage();

        List<LispMapRecord> mapReplyRecords =
                mapDb.getMapRecordByEidRecords(request.getEids(), true);

        List<LispMapRecord> mapRequestRecords =
                mapDb.getMapRecordByEidRecords(request.getEids(), false);

        if (mapReplyRecords.size() + mapRequestRecords.size() == 0) {

            List<LispMessage> mapReplies = Lists.newArrayList();

            // build natively-forward map reply messages based on map-request from ITR
            ReplyBuilder replyBuilder = initMapReplyBuilder(request);
            replyBuilder.withMapRecords(getNegativeMapRecords(request.getEids()));
            LispMessage mapReply = replyBuilder.build();
            mapReply.configSender(new InetSocketAddress(ecm.getSender().getAddress(),
                                                        NEGATIVE_REPLY_DST_PORT));
            mapReplies.add(mapReply);

            log.warn(NO_MAP_INFO_MSG);

        } else {

            if (!mapReplyRecords.isEmpty()) {

                List<LispMessage> mapReplies = Lists.newArrayList();

                // build map-reply message based on map-request from ITR
                ReplyBuilder replyBuilder = initMapReplyBuilder(request);
                replyBuilder.withMapRecords(mapReplyRecords);

                List<InetSocketAddress> addresses =
                                        getItrAddresses(request.getItrRlocs(),
                                                ecm.innerUdp().getSourcePort());

                addresses.forEach(address -> {
                    if (address != null) {
                        LispMapReply reply = replyBuilder.build();
                        reply.configSender(address);
                        mapReplies.add(reply);
                    } else {
                        log.warn(NO_ITR_RLOCS_MSG);
                    }
                });

                return mapReplies;
            }

            if (!mapRequestRecords.isEmpty()) {

                List<LispMessage> ecms = Lists.newArrayList();

                // re-encapsulate encapsulated control message from ITR
                List<InetSocketAddress> addresses =
                                getEtrAddresses(mapRequestRecords, ECM_DST_PORT);

                addresses.forEach(address -> {
                    if (address != null) {
                        LispEncapsulatedControl reencapEcm = cloneEcm(ecm);
                        reencapEcm.configSender(address);
                        ecms.add(reencapEcm);
                    } else {
                        log.warn(NO_ETR_RLOCS_MSG);
                    }
                });

                return ecms;
            }
        }
        return ImmutableList.of();
    }

    /**
     * Initializes MapReply builder without specifying map records.
     *
     * @param request received map request from ITR
     * @return initialized MapReply builder
     */
    private ReplyBuilder initMapReplyBuilder(LispMapRequest request) {
        ReplyBuilder replyBuilder = new DefaultReplyBuilder();
        replyBuilder.withNonce(request.getNonce());
        replyBuilder.withIsEtr(false);
        replyBuilder.withIsSecurity(false);
        replyBuilder.withIsProbe(request.isProbe());

        return replyBuilder;
    }

    /**
     * Clones ECM from original ECM.
     *
     * @param ecm original ECM
     * @return cloned ECM
     */
    private LispEncapsulatedControl cloneEcm(LispEncapsulatedControl ecm) {
        EcmBuilder ecmBuilder = new DefaultEcmBuilder();
        ecmBuilder.innerLispMessage(ecm.getControlMessage());
        ecmBuilder.isSecurity(ecm.isSecurity());
        ecmBuilder.innerIpHeader(ecm.innerIpHeader());
        ecmBuilder.innerUdpHeader(ecm.innerUdp());

        return ecmBuilder.build();
    }

    /**
     * Obtains a collection of map records with natively-forward action.
     *
     * @param eids endpoint identifier records
     * @return a collection of map records with natively-forward action
     */
    private List<LispMapRecord> getNegativeMapRecords(List<LispEidRecord> eids) {
        List<LispMapRecord> mapRecords = Lists.newArrayList();

        MapRecordBuilder recordBuilder = new DefaultMapRecordBuilder();
        recordBuilder.withRecordTtl(MAP_REPLY_RECORD_TTL);
        recordBuilder.withLocators(Lists.newArrayList());
        recordBuilder.withIsAuthoritative(false);
        recordBuilder.withMapVersionNumber(MAP_VERSION_NUMBER);
        recordBuilder.withAction(LispMapReplyAction.NativelyForward);

        eids.forEach(eid -> {
            recordBuilder.withEidPrefixAfi(eid.getPrefix());
            recordBuilder.withMaskLength(eid.getMaskLength());
            mapRecords.add(recordBuilder.build());
        });

        return mapRecords;
    }

    /**
     * Obtains a collection of valid ITR addresses with a port number specified.
     * These addresses will be used to acknowledge map-reply to ITR.
     *
     * @param itrRlocs a collection of ITR RLOCs
     * @param port     port number
     * @return a collection of valid ITR addresses with a port number specified
     */
    private List<InetSocketAddress> getItrAddresses(List<LispAfiAddress> itrRlocs,
                                                    int port) {
        List<InetSocketAddress> addresses = Lists.newArrayList();
        for (LispAfiAddress itrRloc : itrRlocs) {
            addresses.add(new InetSocketAddress(((LispIpAddress)
                    itrRloc).getAddress().toInetAddress(), port));
        }
        return addresses;
    }

    /**
     * Obtains a collection of valid ETR addresses with a port number specified.
     * These addresses will be used to relay map-request to ETR.
     *
     * @param mapRecords a collection of map records
     * @param port       port number
     * @return a collection of valid ETR addresses with a port number specified
     */
    private List<InetSocketAddress> getEtrAddresses(List<LispMapRecord> mapRecords,
                                                    int port) {
        List<InetSocketAddress> addresses = Lists.newArrayList();
        for (LispMapRecord mapRecord : mapRecords) {

            // we only select the first locator record in all cases...
            LispLocator locatorRecord = mapRecord.getLocators().get(0);
            if (locatorRecord != null) {
                addresses.add(new InetSocketAddress(((LispIpAddress)
                                locatorRecord.getLocatorAfi()).getAddress()
                                                    .toInetAddress(), port));
            }
        }
        return addresses;
    }

    /**
     * Prevents object instantiation from external.
     */
    private static final class SingletonHelper {
        private static final String ILLEGAL_ACCESS_MSG = "Should not instantiate this class.";
        private static final LispMapResolver INSTANCE = new LispMapResolver();

        private SingletonHelper() {
            throw new IllegalAccessError(ILLEGAL_ACCESS_MSG);
        }
    }
}

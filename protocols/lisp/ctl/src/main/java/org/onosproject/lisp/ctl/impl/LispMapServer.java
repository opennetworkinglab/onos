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
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.ctl.LispRouter;
import org.onosproject.lisp.ctl.LispRouterFactory;
import org.onosproject.lisp.ctl.impl.util.LispMapUtil;
import org.onosproject.lisp.msg.authentication.LispAuthenticationConfig;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoReply.DefaultInfoReplyBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoRequest.DefaultInfoRequestBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.DefaultNotifyBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRegister.DefaultRegisterBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRequest.DefaultRequestBuilder;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispInfoReply;
import org.onosproject.lisp.msg.protocols.LispInfoReply.InfoReplyBuilder;
import org.onosproject.lisp.msg.protocols.LispInfoRequest;
import org.onosproject.lisp.msg.protocols.LispInfoRequest.InfoRequestBuilder;
import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapNotify.NotifyBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispMapRegister;
import org.onosproject.lisp.msg.protocols.LispMapRegister.RegisterBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRequest;
import org.onosproject.lisp.msg.protocols.LispMapRequest.RequestBuilder;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.LispIpv6Address;
import org.onosproject.lisp.msg.types.lcaf.LispNatLcafAddress.NatAddressBuilder;
import org.onosproject.lisp.msg.types.LispNoAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.onlab.packet.IpAddress.valueOf;
import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.valueOf;

/**
 * LISP map server class.
 * Handles map-register message and acknowledges with map-notify message.
 */
public final class LispMapServer {

    private static final Logger log = LoggerFactory.getLogger(LispMapServer.class);

    private static final int MAP_NOTIFY_PORT = 4342;
    private static final int INFO_REPLY_PORT = 4342;

    private static final String INVALID_AUTHENTICATION_DATA_MSG =
                                "Unmatched authentication data of {}.";
    private static final String FAILED_TO_FORMULATE_NAT_MSG =
                                "Fails during formulate NAT address.";

    private boolean enableSmr = false;

    private LispMappingDatabase mapDb = LispExpireMapDatabase.getInstance();
    private LispAuthenticationConfig authConfig = LispAuthenticationConfig.getInstance();

    // non-instantiable (except for our Singleton)
    private LispMapServer() {
    }

    static LispMapServer getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Enable LISP Map server sends SMR(Solicit Map Request) message.
     *
     * @param enable whether enable or disable sending SMR
     */
    public void enableSmr(boolean enable) {
        this.enableSmr = enable;
    }

    /**
     * Handles map-register message and replies with map-notify message.
     *
     * @param message map-register message
     * @return map-notify message
     */
    LispMapNotify processMapRegister(LispMessage message) {

        LispMapRegister register = (LispMapRegister) message;

        if (!checkMapRegisterAuthData(register)) {
            log.warn(INVALID_AUTHENTICATION_DATA_MSG, "Map-Register");
            return null;
        }

        register.getMapRecords().forEach(mapRecord -> {
            LispEidRecord eidRecord =
                                new LispEidRecord(mapRecord.getMaskLength(),
                                                  mapRecord.getEidPrefixAfi());

            LispMapRecord oldMapRecord = mapDb.getMapRecordByEidRecord(eidRecord,
                    register.isProxyMapReply());
            if (oldMapRecord == null) {
                mapDb.putMapRecord(eidRecord, mapRecord, register.isProxyMapReply());
            } else {
                if (oldMapRecord.getMapVersionNumber() <= mapRecord.getMapVersionNumber()) {
                    mapDb.putMapRecord(eidRecord, mapRecord, register.isProxyMapReply());

                    if (enableSmr) {
                        sendSmrMessage(eidRecord);
                    }
                }
            }
        });

        // we only acknowledge back to ETR when want-map-notify bit is set to true
        // otherwise, we do not acknowledge back to ETR
        if (register.isWantMapNotify()) {
            NotifyBuilder notifyBuilder = new DefaultNotifyBuilder();
            notifyBuilder.withKeyId(authConfig.lispAuthKeyId());
            notifyBuilder.withAuthDataLength(valueOf(authConfig.lispAuthKeyId()).getHashLength());
            notifyBuilder.withAuthKey(authConfig.lispAuthKey());
            notifyBuilder.withNonce(register.getNonce());
            notifyBuilder.withMapRecords(register.getMapRecords());

            LispMapNotify notify = notifyBuilder.build();

            InetSocketAddress address =
                    new InetSocketAddress(register.getSender().getAddress(), MAP_NOTIFY_PORT);
            notify.configSender(address);

            return notify;
        }

        return null;
    }

    /**
     * Handles info-request message and replies with info-reply message.
     *
     * @param message info-request message
     * @return info-reply message
     */
    LispInfoReply processInfoRequest(LispMessage message) {
        LispInfoRequest request = (LispInfoRequest) message;

        if (!checkInfoRequestAuthData(request)) {
            log.warn(INVALID_AUTHENTICATION_DATA_MSG, "Info-Request");
            return null;
        }

        NatAddressBuilder natBuilder = new NatAddressBuilder();
        try {
            LispAfiAddress msAddress =
                        new LispIpv4Address(valueOf(InetAddress.getLocalHost()));
            natBuilder.withMsRlocAddress(msAddress);
            natBuilder.withMsUdpPortNumber((short) INFO_REPLY_PORT);

            // try to extract global ETR RLOC address from info-request
            IpAddress globalRlocIp = valueOf(request.getSender().getAddress());
            LispAfiAddress globalRlocAddress;
            if (globalRlocIp.isIp4()) {
                globalRlocAddress = new LispIpv4Address(globalRlocIp);
            } else {
                globalRlocAddress = new LispIpv6Address(globalRlocIp);
            }
            natBuilder.withGlobalEtrRlocAddress(globalRlocAddress);
            natBuilder.withEtrUdpPortNumber((short) request.getSender().getPort());
            natBuilder.withPrivateEtrRlocAddress(new LispNoAddress());

            // TODO: need to specify RTR addresses

        } catch (UnknownHostException e) {
            log.warn(FAILED_TO_FORMULATE_NAT_MSG, e);
        }

        InfoReplyBuilder replyBuilder = new DefaultInfoReplyBuilder();
        replyBuilder.withKeyId(request.getKeyId());
        replyBuilder.withAuthDataLength(valueOf(authConfig.lispAuthKeyId()).getHashLength());
        replyBuilder.withAuthKey(authConfig.lispAuthKey());
        replyBuilder.withNonce(request.getNonce());
        replyBuilder.withEidPrefix(request.getPrefix());
        replyBuilder.withMaskLength(request.getMaskLength());
        replyBuilder.withTtl(request.getTtl());
        replyBuilder.withNatLcafAddress(natBuilder.build());
        replyBuilder.withIsInfoReply(true);

        LispInfoReply reply = replyBuilder.build();
        reply.configSender(request.getSender());

        return reply;
    }

    /**
     * Checks the integrity of the received map-register message by calculating
     * authentication data from received map-register message.
     *
     * @param register map-register message
     * @return evaluation result
     */
    private boolean checkMapRegisterAuthData(LispMapRegister register) {
        RegisterBuilder registerBuilder = new DefaultRegisterBuilder();
        registerBuilder.withKeyId(register.getKeyId());
        registerBuilder.withAuthKey(authConfig.lispAuthKey());
        registerBuilder.withNonce(register.getNonce());
        registerBuilder.withIsProxyMapReply(register.isProxyMapReply());
        registerBuilder.withIsWantMapNotify(register.isWantMapNotify());
        registerBuilder.withMapRecords(register.getMapRecords());

        LispMapRegister authRegister = registerBuilder.build();

        return Arrays.equals(authRegister.getAuthData(), register.getAuthData());
    }

    /**
     * Checks the integrity of the received info-request message by calculating
     * authentication data from received info-request message.
     *
     * @param request info-request message
     * @return evaluation result
     */
    private boolean checkInfoRequestAuthData(LispInfoRequest request) {
        InfoRequestBuilder requestBuilder = new DefaultInfoRequestBuilder();
        requestBuilder.withKeyId(request.getKeyId());
        requestBuilder.withAuthKey(authConfig.lispAuthKey());
        requestBuilder.withNonce(request.getNonce());
        requestBuilder.withTtl(request.getTtl());
        requestBuilder.withEidPrefix(request.getPrefix());
        requestBuilder.withIsInfoReply(request.isInfoReply());
        requestBuilder.withMaskLength(request.getMaskLength());

        LispInfoRequest authRequest = requestBuilder.build();

        return Arrays.equals(authRequest.getAuthData(), request.getAuthData());
    }

    /**
     * Sends SMR (Solicit Map Request) to their subscribers.
     *
     * @param eidRecord the updated EID
     */
    private void sendSmrMessage(LispEidRecord eidRecord) {

        RequestBuilder builder = new DefaultRequestBuilder();

        LispAfiAddress msAddress = null;
        try {
            msAddress = new LispIpv4Address(IpAddress.valueOf(InetAddress.getLocalHost()));
        } catch (UnknownHostException e) {
            log.warn("Source EID is not found, {}", e.getMessage());
        }

        LispMapRequest msg = builder.withIsSmr(true)
                .withIsSmrInvoked(true)
                .withIsProbe(false)
                .withIsPitr(false)
                .withIsAuthoritative(false)
                .withIsMapDataPresent(false)
                .withSourceEid(msAddress)
                .withEidRecords(ImmutableList.of(eidRecord))
                .build();

        LispRouterFactory routerFactory = LispRouterFactory.getInstance();
        Collection<LispRouter> routers = routerFactory.getRouters();
        routers.forEach(router -> {
            if (isInEidRecordRange(eidRecord, router.getEidRecords())) {
                router.sendMessage(msg);
            }
        });
    }

    private boolean isInEidRecordRange(LispEidRecord originalRecord, List<LispEidRecord> records) {

        for (LispEidRecord record : records) {
            return LispMapUtil.isInRange(record, originalRecord) ||
                    LispMapUtil.isInRange(originalRecord, record);
        }
        return false;
    }

    /**
     * Prevents object instantiation from external.
     */
    private static final class SingletonHelper {
        private static final String ILLEGAL_ACCESS_MSG = "Should not instantiate this class.";
        private static final LispMapServer INSTANCE = new LispMapServer();

        private SingletonHelper() {
            throw new IllegalAccessError(ILLEGAL_ACCESS_MSG);
        }
    }
}

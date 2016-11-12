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
package org.onosproject.lisp.ctl;

import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoReply;
import org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.DefaultNotifyBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRegister.DefaultRegisterBuilder;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispInfoReply;
import org.onosproject.lisp.msg.protocols.LispInfoReply.InfoReplyBuilder;
import org.onosproject.lisp.msg.protocols.LispInfoRequest;
import org.onosproject.lisp.msg.protocols.LispInfoRequest.InfoRequestBuilder;
import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapNotify.NotifyBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRegister;
import org.onosproject.lisp.msg.protocols.LispMapRegister.RegisterBuilder;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.LispIpv6Address;
import org.onosproject.lisp.msg.types.LispNoAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.valueOf;
import static org.onosproject.lisp.msg.protocols.DefaultLispInfoRequest.DefaultInfoRequestBuilder;
import static org.onosproject.lisp.msg.types.LispNatLcafAddress.NatAddressBuilder;

/**
 * LISP map server class.
 * Handles map-register message and acknowledges with map-notify message.
 */
public class LispMapServer {

    private static final int MAP_NOTIFY_PORT = 4342;
    private static final int INFO_REPLY_PORT = 4342;

    // TODO: need to be configurable
    private static final String AUTH_KEY = "onos";

    // TODO: need to be configurable
    private static final short AUTH_METHOD = 1;

    private static final Logger log = LoggerFactory.getLogger(LispMapServer.class);

    private LispEidRlocMap mapInfo;

    public LispMapServer() {
        mapInfo = LispEidRlocMap.getInstance();
    }

    /**
     * Handles map-register message and replies with map-notify message.
     *
     * @param message map-register message
     * @return map-notify message
     */
    public LispMapNotify processMapRegister(LispMessage message) {

        LispMapRegister register = (LispMapRegister) message;

        if (!checkMapRegisterAuthData(register)) {
            log.warn("Unmatched authentication data of Map-Register");
            return null;
        }

        NotifyBuilder notifyBuilder = new DefaultNotifyBuilder();
        notifyBuilder.withKeyId(AUTH_METHOD);
        notifyBuilder.withAuthDataLength(valueOf(AUTH_METHOD).getHashLength());
        notifyBuilder.withAuthKey(AUTH_KEY);
        notifyBuilder.withNonce(register.getNonce());
        notifyBuilder.withMapRecords(register.getMapRecords());

        LispMapNotify notify = notifyBuilder.build();

        InetSocketAddress address =
                new InetSocketAddress(register.getSender().getAddress(), MAP_NOTIFY_PORT);
        notify.configSender(address);

        register.getMapRecords().forEach(record -> {
            LispEidRecord eidRecord =
                    new LispEidRecord(record.getMaskLength(), record.getEidPrefixAfi());
            mapInfo.insertMapRecord(eidRecord, record);
        });

        return notify;
    }

    /**
     * Handles info-request message and replies with info-reply message.
     *
     * @param message info-request message
     * @return info-reply message
     */
    public LispInfoReply processInfoRequest(LispMessage message) {
        LispInfoRequest request = (LispInfoRequest) message;

        if (!checkInfoRequestAuthData(request)) {
            log.warn("Unmatched authentication data of Info-Request");
            return null;
        }

        NatAddressBuilder natBuilder = new NatAddressBuilder();
        try {
            LispAfiAddress msAddress =
                    new LispIpv4Address(IpAddress.valueOf(InetAddress.getLocalHost()));
            natBuilder.withMsRlocAddress(msAddress);
            natBuilder.withMsUdpPortNumber((short) INFO_REPLY_PORT);

            // try to extract global ETR RLOC address from info-request
            IpAddress globalRlocIp = IpAddress.valueOf(request.getSender().getAddress());
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
            log.warn("Fails during formulate NAT address", e);
        }

        InfoReplyBuilder replyBuilder = new DefaultLispInfoReply.DefaultInfoReplyBuilder();
        replyBuilder.withKeyId(request.getKeyId());
        replyBuilder.withAuthDataLength(valueOf(AUTH_METHOD).getHashLength());
        replyBuilder.withAuthKey(AUTH_KEY);
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
        registerBuilder.withAuthKey(AUTH_KEY);
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
        requestBuilder.withAuthKey(AUTH_KEY);
        requestBuilder.withNonce(request.getNonce());
        requestBuilder.withTtl(request.getTtl());
        requestBuilder.withEidPrefix(request.getPrefix());
        requestBuilder.withIsInfoReply(request.isInfoReply());
        requestBuilder.withMaskLength(request.getMaskLength());

        LispInfoRequest authRequest =  requestBuilder.build();

        return Arrays.equals(authRequest.getAuthData(), request.getAuthData());
    }
}

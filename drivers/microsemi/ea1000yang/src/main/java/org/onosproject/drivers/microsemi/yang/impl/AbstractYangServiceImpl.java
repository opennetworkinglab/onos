/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.microsemi.yang.impl;

import static org.onosproject.yms.ych.YangProtocolEncodingFormat.XML;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REPLY;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REQUEST;

import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yms.ych.YangCodecHandler;
import org.onosproject.yms.ych.YangCompositeEncoding;
import org.onosproject.yms.ymsm.YmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that implements some of the core functions of a YANG model service.
 *
 */
@Component(immediate = true)
@Service
public abstract class AbstractYangServiceImpl {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected boolean alreadyLoaded = false;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YmsService ymsService;

    protected ApplicationId appId;

    // YCH is not a service and is a class variable. Can be lost on deactivate.
    // Must be recreated on activate
    protected YangCodecHandler ych;

    @Activate
    public abstract void activate();

    @Deactivate
    public void deactivate() {
        alreadyLoaded = false;
    }

    /**
     * Internal method to generically make a NETCONF get query from YANG objects.
     * @param yangObjectOpParamFilter A YANG object model
     * @param session A NETCONF session
     * @return YangObjectModel
     * @throws NetconfException if the session has any error
     */
    protected final Object getNetconfObject(
            Object yangObjectOpParamFilter, NetconfSession session)
                throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getNetconfObject()");
        }
        if (yangObjectOpParamFilter == null) {
            throw new NetconfException("Query object cannot be null");
        }
        //Convert the param to XML to use as a filter
        YangCompositeEncoding xmlQuery =
                ych.encodeCompositeOperation(null, null,
                        yangObjectOpParamFilter, XML, QUERY_REQUEST);

        String xmlQueryStr = xmlQuery.getResourceInformation().replace("<>", "").replace("</>", "").trim();
        log.debug("Sending <get> query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.get(xmlQueryStr, null);

        List<Object> objectList = ych.decode(xmlResult, XML, QUERY_REPLY);
        if (objectList != null && objectList.size() > 0) {
            Object systemObject = objectList.get(0);
            return systemObject;
        } else {
            return null;
        }
    }

    /**
     * Internal method to generically make a NETCONF get-config query from YANG objects.
     *
     * @param yangObjectOpParamFilter A YANG object model
     * @param session A NETCONF session
     * @param targetDs - running,candidate or startup
     * @return YangObjectModel
     * @throws NetconfException if the session has any error
     */
    protected final Object getConfigNetconfObject(
            Object yangObjectOpParamFilter, NetconfSession session, TargetConfig targetDs)
                throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getConfigNetconfObject()");
        }

        if (yangObjectOpParamFilter == null) {
            throw new NetconfException("Query object cannot be null");
        }
        //Convert the param to XML to use as a filter
        YangCompositeEncoding xmlQuery =
                ych.encodeCompositeOperation(null, null,
                        yangObjectOpParamFilter, XML, QUERY_REQUEST);

        String xmlQueryStr = xmlQuery.getResourceInformation().replace("<>", "").replace("</>", "").trim();
        log.debug("Sending <get-config> for " + targetDs +
                " query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.getConfig(targetDs, xmlQueryStr);

        List<Object> objectList = ych.decode(xmlResult, XML, QUERY_REPLY);
        if (objectList != null && objectList.size() > 0) {
            Object systemObject = objectList.get(0);
            return systemObject;
        } else {
            return null;
        }
    }

    /**
     * Internal method to generically make a NETCONF edit-config call from a set of YANG objects.
     *
     * @param yangObjectOpParamFilter A YANG object model
     * @param session A NETCONF session
     * @param targetDs - running,candidate or startup
     * @throws NetconfException if the session has any error
     */
    protected final void setNetconfObject(
            Object yangObjectOpParamFilter, NetconfSession session, TargetConfig targetDs)
                throws NetconfException {
        if (yangObjectOpParamFilter == null) {
            throw new NetconfException("Query object cannot be null");
        } else if (session == null) {
            throw new NetconfException("Session is null when calling getMseaSaFiltering()");
        }
        //Convert the param to XML to use as a filter
        YangCompositeEncoding xmlContent =
                ych.encodeCompositeOperation(null, null,
                        yangObjectOpParamFilter, XML, EDIT_CONFIG_REQUEST);

        String xmlContentStr = xmlContent.getResourceInformation()
                .replace("<>", "").replace("</>", "")
                //FIXME: Necessary for MEP ccmInterval
                .replaceAll("yangAutoPrefix", "")
                .trim();

        log.debug("Sending XML <edit-config> on NETCONF session " + session.getSessionId() +
                ":\n" + xmlContentStr);

        boolean succeeded = session.editConfig(targetDs, null, xmlContentStr);
        if (succeeded) {
            log.debug("<edit-config> succeeded through NETCONF");
        } else {
            throw new NetconfException("Failed to run edit-config through NETCONF");
        }
    }
}

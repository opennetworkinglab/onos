/*
 * Copyright 2017-present Open Networking Foundation
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.CcmIntervalEnum;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.lossmeasurements.lossmeasurement.MessagePeriodEnum;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.SchemaContext;
import org.onosproject.yang.model.SchemaContextProvider;
import org.onosproject.yang.runtime.AnnotatedNodeInfo;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultCompositeData;
import org.onosproject.yang.runtime.DefaultCompositeStream;
import org.onosproject.yang.runtime.DefaultYangSerializerContext;
import org.onosproject.yang.runtime.YangModelRegistry;
import org.onosproject.yang.runtime.YangSerializer;
import org.onosproject.yang.runtime.YangSerializerContext;
import org.onosproject.yang.runtime.YangSerializerRegistry;
import org.onosproject.yang.serializers.xml.XmlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that implements some of the core functions of a YANG model service.
 *
 */
@Component(immediate = true)
@Service
public abstract class AbstractYangServiceImpl {
    public static final String NC_OPERATION = "nc:operation";
    public static final String OP_DELETE = "delete";

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected boolean alreadyLoaded = false;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YangModelRegistry yangModelRegistry;

    protected ApplicationId appId;

    // xSer is not a service and is a class variable. Can be lost on deactivate.
    // Must be recreated on activate
    protected XmlSerializer xSer = null;
    protected YangSerializerContext yCtx = null;

    protected static final Pattern REGEX_XML_HEADER =
            Pattern.compile("(<\\?xml).*(\\?>)", Pattern.DOTALL);
    protected static final Pattern REGEX_RPC_REPLY =
            Pattern.compile("(<rpc-reply)[ ]*" +
                    "(xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\")[ ]*" +
                    "(message-id=\")[0-9]*(\">)", Pattern.DOTALL);
    protected static final Pattern REGEX_RPC_REPLY_DATA_NS =
            Pattern.compile("(<data)[ ]*(xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)");
    protected static final Pattern REGEX_RPC_REPLY_DATA =
            Pattern.compile("(<data>)");
    protected static final Pattern REGEX_RPC_REPLY_DATA_CLOSE =
            Pattern.compile("(</data>)");
    protected static final Pattern REGEX_RPC_REPLY_DATA_EMPTY =
            Pattern.compile("(<data/>)");
    protected static final Pattern REGEX_RPC_REPLY_CLOSE =
            Pattern.compile("(</rpc-reply>)");
    protected static final Pattern REGEX_RPC_OK =
            Pattern.compile("\\R?\\s*(<ok/>)\\R?");
    @Activate
    public void activate() {
        Set<YangSerializer> yangSer = ((YangSerializerRegistry) yangModelRegistry).getSerializers();
        xSer = (XmlSerializer) yangSer.stream()
                .filter(ser -> (ser instanceof XmlSerializer)).findFirst().get();
        SchemaContext context = ((SchemaContextProvider) yangModelRegistry)
                .getSchemaContext(ResourceId.builder().addBranchPointSchema("/", null).build());

        yCtx = new DefaultYangSerializerContext(context, null);
    };

    @Deactivate
    public void deactivate() {
        alreadyLoaded = false;
    }

    /**
     * Internal method to generically make a NETCONF get query from YANG objects.
     * @param moFilter A YANG object model
     * @param session A NETCONF session
     * @return YangObjectModel
     * @throws NetconfException if the session has any error
     */
    protected final ModelObjectData getNetconfObject(
            ModelObjectData moFilter, NetconfSession session)
                throws NetconfException {

        return getConfigNetconfObject(moFilter, session, null);
    }

    /**
     * Internal method to generically make a NETCONF get-config query from YANG objects.
     *
     * @param moFilter A YANG object model
     * @param session A NETCONF session
     * @param targetDs - running,candidate or startup
     * @return YangObjectModel
     * @throws NetconfException if the session has any error
     */
    protected final ModelObjectData getConfigNetconfObject(
            ModelObjectData moFilter, NetconfSession session, DatastoreId targetDs)
                throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getConfigNetconfObject()");
        }

        if (moFilter == null) {
            throw new NetconfException("Query object cannot be null");
        }

        String xmlQueryStr = encodeMoToXmlStr(moFilter, null);

        log.debug("Sending <get-(config)> query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);
        String xmlResult;
        if (targetDs == null) {
            xmlResult = session.get(xmlQueryStr, null);
        } else {
            xmlResult = session.getConfig(targetDs, xmlQueryStr);
        }
        xmlResult = removeRpcReplyData(xmlResult);

        DefaultCompositeStream resultDcs = new DefaultCompositeStream(
                null, new ByteArrayInputStream(xmlResult.getBytes()));
        CompositeData compositeData = xSer.decode(resultDcs, yCtx);

        return ((ModelConverter) yangModelRegistry).createModel(compositeData.resourceData());
    }

    /**
     * Internal method to generically make a NETCONF edit-config call from a set of YANG objects.
     *
     * @param moConfig A YANG object model
     * @param session A NETCONF session
     * @param targetDs - running,candidate or startup
     * @param annotations A list of AnnotatedNodeInfos to be added to the DataNodes
     * @return Boolean value indicating success or failure of command
     * @throws NetconfException if the session has any error
     */
    protected final boolean setNetconfObject(
            ModelObjectData moConfig, NetconfSession session, DatastoreId targetDs,
            List<AnnotatedNodeInfo> annotations) throws NetconfException {
        if (moConfig == null) {
            throw new NetconfException("Query object cannot be null");
        } else if (session == null) {
            throw new NetconfException("Session is null when calling setNetconfObject()");
        } else if (targetDs == null) {
            throw new NetconfException("TargetDs is null when calling setNetconfObject()");
        }

        String xmlQueryStr = encodeMoToXmlStr(moConfig, annotations);
        log.debug("Sending <edit-config> query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        //Some encoded values just have to be replaced
        xmlQueryStr = xmlQueryStr.replace(MessagePeriodEnum.YANGAUTOPREFIX3MS.toString(), "3ms");
        xmlQueryStr = xmlQueryStr.replace(MessagePeriodEnum.YANGAUTOPREFIX10MS.toString(), "10ms");
        xmlQueryStr = xmlQueryStr.replace(MessagePeriodEnum.YANGAUTOPREFIX100MS.toString(), "100ms");
        xmlQueryStr = xmlQueryStr.replace(MessagePeriodEnum.YANGAUTOPREFIX1000MS.toString(), "1000ms");

        xmlQueryStr = xmlQueryStr.replace(CcmIntervalEnum.YANGAUTOPREFIX3_3MS.toString(), "3.3ms");
        xmlQueryStr = xmlQueryStr.replace(CcmIntervalEnum.YANGAUTOPREFIX10MS.toString(), "10ms");
        xmlQueryStr = xmlQueryStr.replace(CcmIntervalEnum.YANGAUTOPREFIX100MS.toString(), "100ms");
        xmlQueryStr = xmlQueryStr.replace(CcmIntervalEnum.YANGAUTOPREFIX1S.toString(), "1s");

        return session.editConfig(targetDs, null, xmlQueryStr);
    }

    /**
     * Internal method to generically call a NETCONF custom RPC from a set of YANG objects.
     *
     * @param customRpcInput A YANG object model
     * @param rpcName The name of the RPC - replaces 'input' in the XML payload
     * @param session A NETCONF session
     * @return ModelObjectData value indicating success or failure of command
     * @throws NetconfException if the session has any error
     */
    protected final ModelObjectData customRpcNetconf(
            ModelObjectData customRpcInput, String rpcName, NetconfSession session)
            throws NetconfException {
        if (customRpcInput == null) {
            throw new NetconfException("Input object cannot be null");
        } else if (session == null) {
            throw new NetconfException("Session is null when calling customRpcNetconf()");
        }

        String xmlQueryStr = encodeMoToXmlStr(customRpcInput, null);
        xmlQueryStr = xmlQueryStr.replace("input", rpcName);
        log.debug("Sending <edit-config> query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.doWrappedRpc(xmlQueryStr);
        xmlResult = removeRpcReplyData(xmlResult);
        if (REGEX_RPC_OK.matcher(xmlResult).matches()) {
            return null;
        }

        DefaultCompositeStream resultDcs = new DefaultCompositeStream(
                null, new ByteArrayInputStream(xmlResult.getBytes()));
        CompositeData compositeData = xSer.decode(resultDcs, yCtx);

        return ((ModelConverter) yangModelRegistry).createModel(compositeData.resourceData());
    }

    protected final String encodeMoToXmlStr(ModelObjectData yangObjectOpParamFilter,
                                            List<AnnotatedNodeInfo> annotations)
            throws NetconfException {
        //Convert the param to XML to use as a filter
        ResourceData rd = ((ModelConverter) yangModelRegistry).createDataNode(yangObjectOpParamFilter);

        DefaultCompositeData.Builder cdBuilder =
                        DefaultCompositeData.builder().resourceData(rd);
        if (annotations != null) {
            for (AnnotatedNodeInfo ani : annotations) {
                cdBuilder.addAnnotatedNodeInfo(ani);
            }
        }
        CompositeStream cs = xSer.encode(cdBuilder.build(), yCtx);
        //Convert the param to XML to use as a filter

        try {
            ByteSource byteSource = new ByteSource() {
                @Override
                public InputStream openStream() throws IOException {
                    return cs.resourceData();
                }
            };

            return byteSource.asCharSource(Charsets.UTF_8).read();
        } catch (IOException e) {
            throw new NetconfException("Error decoding CompositeStream to String", e);
        }
    }

    protected static final String removeRpcReplyData(String rpcReplyXml) throws NetconfException {
        rpcReplyXml = REGEX_XML_HEADER.matcher(rpcReplyXml).replaceFirst("");
        if (rpcReplyXml.contains("<rpc-error")) {
            throw new NetconfException("NETCONF rpc-error: " + rpcReplyXml);
        }

        rpcReplyXml = REGEX_RPC_REPLY.matcher(rpcReplyXml).replaceFirst("");
        rpcReplyXml = REGEX_RPC_REPLY_DATA_NS.matcher(rpcReplyXml).replaceFirst("");
        rpcReplyXml = REGEX_RPC_REPLY_DATA.matcher(rpcReplyXml).replaceFirst("");
        rpcReplyXml = REGEX_RPC_REPLY_DATA_CLOSE.matcher(rpcReplyXml).replaceFirst("");
        rpcReplyXml = REGEX_RPC_REPLY_DATA_EMPTY.matcher(rpcReplyXml).replaceFirst("");
        rpcReplyXml = REGEX_RPC_REPLY_CLOSE.matcher(rpcReplyXml).replaceFirst("");
        rpcReplyXml = rpcReplyXml.replace("\t", "");
        return rpcReplyXml;
    }
}

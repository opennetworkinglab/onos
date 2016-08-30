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

package org.onosproject.yms.ych;

import java.util.List;
import java.util.Map;

import org.onosproject.yms.ydt.YmsOperationType;

/**
 * Abstraction of an entity which provides interfaces to YANG codec handler.
 *
 * In SBI, the provider or driver uses YANG management system as a CODEC
 * utility. Providers/drivers register the device schema with YANG management
 * system. YANG utils is used to generate the java files corresponding to the
 * device schema. Provider or driver use these classes to seamlessly manage
 * the device as java objects. While sending the request to device, drivers
 * use the utility to translate the objects to protocol specific data
 * representation and then send to the device. Protocol or driver use the
 * same instance of the codec utility across multiple translation request.
 * Protocol or driver should not use the same instance of utility concurrently.
 */
public interface YangCodecHandler {
    /**
     * Provider / driver needs to register the device schema with code handler.
     * Then the provider / driver can use the codec handler to perform the
     * codec operation. When the codec operation is  being performed, the
     * codec utility finds the mapping registered device model and perform the
     * translation against the device schema.
     *
     * @param yangModule YANG utils generated class corresponding to SBI
     *                   device schema module
     */
    void addDeviceSchema(Class yangModule);


    /**
     * When the drivers / providers need to encode a protocol operation
     * requests, which is in a single block, this encode API is used.
     * A single protocol operation can span across multiple application, then
     * the driver / provider need to provide the list of application(s) module
     * object. Each module object contains the request information
     * corresponding to that application.
     *
     * The protocols can have a logical root node which acts as a container
     * of applications module node. For example in NETCONF, it could be
     * data/filter/config, etc. Protocols needs to pass this parameter in the
     * encode request, so that it is part of the encoded protocol packet.
     * There is no validation done on the value of this parameter. It is up to
     * the protocol to use it. It is a mandatory parameter and protocols must
     * pass this parameter. In protocols like NETCONF, these logical root
     * node may be in a specific name space, in such cases, it needs to be
     * passed to encode it as part of the request. There could be additional
     * tags that can be attached to the root node, for example in NETCONF,
     * the tag type="subtree" can be specified. In such scenarios the
     * required tags should be sent as a parameter.
     *
     * The provider / driver would require to operate on multiple schema
     * nodes in a single request, for example it may be require to configure
     * a tunnel and associate a QOS to this tunnel, in this scenario, it
     * needs to have the tunnel related information in the tunnel module's
     * Java object and and QOS related  information in QOS modules Java
     * object. So in a single request, it can send the list of Java objects
     * corresponding to the modules which need to participate in the operation.
     * If the request to be generated needs to be a wild card for no
     * application(s), then this parameter needs to be null. For example a
     * "empty filter" request in NETCONF get request.
     * If the request to be generated needs to be a wild card for  all
     * application(s), then the driver / provider should not invoke this API,
     * as there is no encoding of application related information for the
     * operation, it is only limited to the protocol operation. For example a
     * "no filter" request in NETCONF get request.
     *
     * @param rootName              name of logical root node as required by
     *                              the protocol
     * @param rootNamespace         namespace of logical root node as
     *                              required by the protocol encoding. It is
     *                              an optional parameter.
     * @param tagAttributeLinkedMap Specifies the list of attributes that
     *                              needs to be tagged with the logical root
     *                              node. It is an optional parameter
     *                              if not required for the protocol.
     * @param yangModuleList        list of YANG module's object(s)
     *                              participating in the operation.
     * @param dataFormat            data format to which encoding to be done.
     * @param protocolOperation     protocol operation being performed
     * @return string containing the requested applications object
     * information encoded in protocol format.
     */
    String encodeOperation(String rootName, String rootNamespace,
                           Map<String, String> tagAttributeLinkedMap,
                           List<Object> yangModuleList,
                           YangProtocolEncodingFormat dataFormat,
                           YmsOperationType protocolOperation);

    /**
     * When the drivers / providers need to encode protocol composite
     * operation requests, which is split in a composite blocks, this encode
     * composite operation API is used. The application module object
     * containing the request information has both the resource identifier
     * part and the resource information part.
     *
     * The protocols can have a logical root node which acts as a container
     * of applications module node.  For example in RESTCONF, it could be
     * RootResource/data, etc. There is no validation done on the value
     * of this parameter. It is upto the protocol to use it. It is a
     * mandatory parameter and protocols must pass this parameter.
     *
     * The resource to be operated upon in the device is identified in a
     * module's schema object. This modules object should contain the
     * information about the resource on which the operation needs to be
     * performed. The resource is identified by initial chain of objects for
     * which operation type is none. Once the resource is reached using none
     * operations, the actual information about the operation on the device
     * is encoded.
     *
     * @param rootName          name of logical root node as required by the
     *                          protocol
     * @param rootNamespace     namespace of logical root node as required by
     *                          the
     *                          protocol encoding. It is optional, and there
     *                          is no
     *                          namespace set to the logical root node
     * @param appModuleObject   object containing the information about the
     *                          resource on which the operation is being
     *                          performed
     * @param dataFormat        data format to which request needs to
     *                          be encoded
     * @param protocolOperation protocol operation being performed
     * @return the composite protocol operation request.
     */
    YangCompositeEncoding encodeCompositeOperation(String rootName,
                                                   String rootNamespace,
                                                   Object appModuleObject,
                                                   YangProtocolEncodingFormat
                                                           dataFormat,
                                                   YmsOperationType
                                                           protocolOperation);

    /**
     * When the driver or provider receive the data from the SBI protocol, It
     * will be in the protocol specific data representation. Drivers /
     * provider need to interact with the device using native JAVA objects.
     * Drivers use this decode method to translate the received
     * protocol specific simple data to YANG modeled Java objects.
     * If the response received is not in line with the schema, for example,
     * there is some error info, etc, then the decode operation will throw an
     * exception and decode operation will fail.
     *
     * @param protocolData      input string containing the resource information
     *                          in protocol format
     * @param dataFormat        data format from which decoding has to be done
     * @param protocolOperation protocol operation being performed
     * @return list of applications module/notification objects corresponding
     * to the protocol data input
     */
    List<Object> decode(String protocolData,
                        YangProtocolEncodingFormat dataFormat,
                        YmsOperationType protocolOperation);

    /**
     * When the driver or provider receive the composite data from the SBI
     * protocol, It will be in the protocol specific data representation.
     * Drivers / provider need to interact with the device
     * using native JAVA objects. Drivers use this Decode method to translate
     * the received protocol specific composite data to YANG modeled Java
     * objects.
     * If the response received is not in line with the schema, for example,
     * there is some error info, etc, then the decode operation will throw an
     * exception and decode operation will fail.
     *
     * @param protocolData      composite protocol data containing the resource
     *                          information
     * @param dataFormat        data format from which decoding has to be done
     * @param protocolOperation protocol operation being performed
     * @return application module/notification object corresponding to the
     * protocol data infput
     */
    Object decode(YangCompositeEncoding protocolData,
                  YangProtocolEncodingFormat dataFormat,
                  YmsOperationType protocolOperation);

    /**
     * Register the provider / driver specific overridden codec. This is can
     * be used by provider to support  any protocol specific extension or
     * vendor specific implementation. This framework can also be used
     * by providers / drivers to support any new protocol data format which
     * is not supported by default in YANG codec utility.
     *
     * @param overriddenCodec provider / driver specific overridden instance
     *                        of the codec
     * @param dataFormat      data format to which encoding to be done.
     */
    void registerOverriddenCodec(YangDataTreeCodec overriddenCodec,
                                 YangProtocolEncodingFormat dataFormat);
}

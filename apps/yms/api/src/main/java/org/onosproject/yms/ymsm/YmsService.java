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

package org.onosproject.yms.ymsm;

import java.util.List;

import org.onosproject.yms.ych.YangCodecHandler;
import org.onosproject.yms.ych.YangDataTreeCodec;
import org.onosproject.yms.ych.YangProtocolEncodingFormat;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtResponse;
import org.onosproject.yms.ydt.YdtWalker;
import org.onosproject.yms.ydt.YmsOperationType;
import org.onosproject.yms.ynh.YangNotificationService;
import org.onosproject.yms.ysr.YangModuleLibrary;

/**
 * Abstraction of an entity which provides interfaces to YANG management
 * system. YMS is a core of YANG in ONOS.
 *
 * In NBI, it acts as a broker in between the protocol and application,
 * here there is a separate protocol implementation, which does the conversion
 * of protocol representation to abstract data tree. The protocol
 * implementation takes care of the protocol specific actions for
 * e.g. RESTCONF handling the entity-tag / timestamp related operations.
 *
 * In SBI, driver or provider uses YANG codec handler as a utility to translate
 * the request information in java(YANG utils generated) to protocol specific
 * format and vice versa.
 */
public interface YmsService {

    /**
     * Returns YANG data tree builder.
     *
     * NBI protocol implementation takes care of the protocol specific
     * operations. They are abstracted from the intricacies of understanding
     * the application identification or handling the interaction with
     * applications.
     *
     * NBI protocols need to handle the encoding and decoding of data to the
     * protocol specific format. They are unaware of the YANG of applications,
     * i.e. protocols are unaware of the data structure / organization in
     * applications.
     *
     * They need to translate the protocol operation request, into a protocol
     * independent abstract tree called the YANG data tree (YDT). In order to
     * enable the protocol in building these abstract data tree, YANG
     * management system provides a utility called the YANG data tree builder.
     *
     * Using the YANG data tree utility API's protocols are expected to walk
     * the data received in request and pass the information during the walk.
     * YANG data tree builder, identifies the application which supports the
     * request and validates it against the schema defined in YANG, and
     * constructs the abstract YANG data tree.
     *
     * Interaction type is a MANDATORY parameter which is used by YANG
     * management system to perform the required operation in ONOS.
     *
     * NOTE: Same YDT builder instance cannot be reused across different
     * operation request. A new instance needs to be used for every operation.
     *
     * Returns YANG data tree builder logical root container node.
     * Protocol use this to logical root container to hold schema specific data
     * that spans across different modules schema.
     *
     * @param logicalRootName name of a protocol specific logical container
     *                        node to group data across multiple applications.
     *                        This is only a logical container to group more
     *                        than one application's root node. It is not
     *                        validated against any YANG definition
     * @param rootNamespace   namespace of logical root container, if any,
     *                        otherwise it can be sent as null
     * @param operationType   maps the request type to a corresponding
     *                        operation request type to YANG management system
     * @return YANG data tree builder, using which the abstract tree can be
     * built corresponding to the data exchanged between protocol and YANG
     * management system.
     */
    YdtBuilder getYdtBuilder(String logicalRootName, String rootNamespace,
                             YmsOperationType operationType);

    /**
     * Returns YANG data tree builder attached with a given schema registry.
     *
     * YMS provides a framework where-in protocols can register their protocol
     * data format specific CODECS with YMS. These registered CODEC will be
     * used by YMS to perform translations from data format to YDT and YDT to
     * data format. YMS may intend to use these CODECS both for NBI and SBI.
     *
     * To perform decode i.e. generate YDT for given data format string, these
     * CODECS implementation needs to call the API's of YDT. YDT referred the
     * registered schema information while building tree. In case of NBI their
     * is a single schema registry, but for SBI schema registry is per
     * driver/provider.
     *
     * Now this schema registry information is provided to protocols
     * CODECS while invoking "decodeProtocolDataToYdt" and
     * "decodeCompositeProtocolDataToYdt", protocol CODECS  needs to provide
     * the schema registry while getting instance of YDT builder.
     * The schemaRegistry may be null when the YMS is performing decode for NBI
     * protocols.
     *
     * Validations for NBI and SBI will vary, schemaRegistry value will also
     * indicate the usage scenario and will be used by YdtBuilder to carry out
     * necessary validations.
     *
     * This is an overloaded method to YdtBuilder which MUST be used by the
     * overridden protocols CODECS.
     *
     * @param logicalRootName      name of a protocol specific logical container
     *                             node to group data across multiple
     *                             applications.
     *                             This is only a logical container to group
     *                             more
     *                             than one application's root node. It is not
     *                             validated against any YANG definition
     * @param rootNamespace        namespace of logical root container, if any,
     *                             otherwise it can be sent as null
     * @param operationType        maps the request type to a corresponding
     *                             operation request type to YANG management
     *                             system
     * @param schemaRegistryForYdt schema registry for Ydt, protocol CODECS get
     *                             this value from YMS in
     *                             "decodeProtocolDataToYdt" and
     *                             "decodeCompositeProtocolDataToYdt" and
     *                             provide it while obtaining YdtBuilder
     * @return YANG data tree builder, using which the abstract tree can be
     * built corresponding to the data exchanged between protocol and YANG
     * management system.
     */
    YdtBuilder getYdtBuilder(String logicalRootName, String rootNamespace,
                             YmsOperationType operationType,
                             Object schemaRegistryForYdt);

    /**
     * Returns YANG data tree walker.
     *
     * YANG management system gets data from application to be returned
     * in protocol operation or to notify to protocol(s) clients, YANG
     * management system encodes the data in a YANG data tree and informs the
     * protocol.
     * Protocols can use the YANG data tree walker utility to have their
     * callbacks to be invoked as per the YANG data tree walking.
     * By this way protocols can encode the data from abstract YANG data tree
     * into a protocol specific representation.
     *
     * @return YANG data tree walker utility
     */
    YdtWalker getYdtWalker();

    /**
     * Once the NBI protocol translates the request information into an abstract
     * YANG data tree, it uses YANG management system as a broker to get the
     * operation executed in ONOS.
     *
     * YANG management system is responsible to split the protocol operation
     * across application(s) which needs to participate, and collate the
     * response(s) from application(s) and return an effective result of the
     * operation request.
     *
     * YMS identifies the type of operation to be performed using the
     * operation type in YANG builder data tree and process the corresponding
     * operation request on the applicable application(s).
     * The response information maintained in response YANG data tree and
     * given to NBI protocol's to encode it using a YANG data tree walker.
     *
     * Depending on the operation type set in the YANG builder tree, the
     * application(s) get / set / operation interface is invoked.
     * These interface are part to the YANG modelled service interface.
     * Depending on the operation type, the YANG response data tree can have
     * the following information.
     *
     * In case of EDIT_CONFIG operation, it will have the status of the
     * operation execution. If there is any applications schema specific
     * error, then the schema error information will be encoded in the
     * corresponding context node. In case the edit operation is successful
     * there is no YANG response data tree created, hence getRootNode will
     * return null.
     *
     * In case of query operation, it will have the status of the operation
     * execution. If there is any application schema specific error, then
     * schema error information will be encoded in the corresponding YANG
     * context. In case the query operation is successful, YANG data tree
     * contains the application data that matched the filter in the
     * operation request. NBI protocol to use a Yang data tree walker to
     * construct the protocol specific reply.
     *
     * In case of RPC operation, it will have the status of the operation
     * execution. If there is any application schema specific error, then
     * schema error information will be encoded in the corresponding YANG
     * context. In case the RPC operation is successful, and the RPC
     * does not have any RPC reply in YANG, then the YANG data tree will
     * be null.
     * In case the RPC has a RPC reply in YANG, then the YANG data tree
     * will contain the application's RPC reply schema specific .
     * NBI protocol to use a Yang data tree walker to construct the
     * protocol specific reply.
     *
     * @param operationRequest operation request that was constructed
     *                         by NBI protocol using YANG data tree
     *                         builder. This operation request contains
     *                         operation request that needs to be
     *                         executed on the applicable application(s)
     * @return returns the result of the operation execution.
     */
    YdtResponse executeOperation(YdtBuilder operationRequest);

    /* TODO add execute operation which directly take data format string as
     input.*/

    /**
     * Returns YANG notification service.
     *
     * NBI Protocols which can support notification delivery for application(s)
     * needs to add themselves as a listeners with YANG notification service.
     * Also protocols can use YANG notification service to check if a received
     * notification should be filtered against any of their protocol specific
     * filtering mechanism.
     *
     * @return YANG notification service instance
     */
    YangNotificationService getYangNotificationService();

    /**
     * Registers service with YANG management system.
     *
     * Applications model their exposed interface in YANG, and register with
     * YANG management system, so that it can be configured / managed by the
     * set of protocols supported in ONOS.
     *
     * ONOS YANG tools generate the applications service interface
     * corresponding to the application's interface designed in YANG.
     *
     * The Application which implements this service registers the generated
     * service with YANG management system. The generated service interfaces
     * have all the information modeled by applications in YANG.
     *
     * Registers application's YANG model with YANG management system. This
     * is used by applications/core to register their service with YMS.
     *
     * @param appManager           application manager instance which is
     *                             implementing the service defined in YANG.
     * @param yangService          service interface generated by ONOS YANG
     *                             tools corresponding to the interface modeled
     *                             in YANG.
     * @param supportedFeatureList mentions the list of YANG features supported
     *                             by the application implementation.
     *                             If it is null, then the application
     *                             implementation supports all the features
     *                             defined in the registered YANG module.
     */
    void registerService(Object appManager, Class<?> yangService,
                         List<String> supportedFeatureList);

    /**
     * Unregisters service which is registered in YANG management system.
     *
     * Applications model their exposed interface in YANG, and register with
     * YANG management system, so that it can be configured / managed by the
     * set of protocols supported in ONOS.
     *
     * ONOS YANG tools generate the applications service interface
     * corresponding to the application's interface designed in YANG.
     *
     * The Application which implements this service registers the generated
     * service with YANG management system. The generated service interfaces
     * have all the information modeled by applications in YANG.
     *
     * Registers application's YANG model with YANG management system. This
     * is used by applications/core to register their service with YMS.
     *
     * @param appManager  application manager instance which is implementing
     *                    the service defined in YANG.
     * @param yangService service interface generated by ONOS YANG tools
     *                    corresponding to the interface modeled in YANG.
     */
    void unRegisterService(Object appManager, Class<?> yangService);

    /**
     * Protocols like RESTCONF, share the list of YANG modules it support.
     * using ietf-yang-library
     *
     * Retrieves the YANG module library supported by the server.
     *
     * @return YANG module library supported by the server
     */
    YangModuleLibrary getYangModuleLibrary();

    /**
     * Protocols like RESTCONF, use the definitions within the YANG modules
     * advertised by the server are used to construct an RPC operation or
     * data resource identifier.
     *
     * Schema Resource:
     * The server can optionally support retrieval of the YANG modules it
     * supports.
     *
     * @param moduleName      YANG module name.
     * @param moduleNamespace namespace in which the module is defined.
     * @return YANG file contents of the requested YANG module.
     */
    String getYangFile(String moduleName, String moduleNamespace);

    /**
     * Register protocol specific default CODEC. This is can be used by 1st
     * protocol
     * to support a protocol format CODEC. This CODEC will be used in both
     * NBI and SBI.
     *
     * @param defaultCodec default codec to be used for a particular protocol
     *                     data format
     * @param dataFormat   data format to which encoding to be done.
     *                     Currently XML and
     *                     JSON formats are supported.
     */
    void registerDefaultCodec(YangDataTreeCodec defaultCodec,
                              YangProtocolEncodingFormat dataFormat);

    /**
     * Returns YANG codec handler utility.
     *
     * In SBI, the provider or driver uses YANG management system as a CODEC
     * utility. These providers/drivers use the YANG codec utility to register
     * the device schema. YANG utils is used to generate the java files
     * corresponding to the device schema. Provider or driver use these classes
     * to seamlessly manage the device as java objects. While sending the
     * request
     * to device, drivers use the utility to translate the objects to protocol
     * specific data representation and then send to the device.
     * Protocol or driver use the same instance of the codec utility across
     * multiple
     * translation request.
     * Protocol or driver should not use the same instance of utility
     * concurrently.
     *
     * @return YANG codec utility
     */
    YangCodecHandler getYangCodecHandler();

    // TODO exceptions handling and sending.
}

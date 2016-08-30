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

import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YmsOperationType;

/**
 * Abstraction of an entity which overrides the default codec.
 *
 * YANG has it extension framework which allows vendor / implementation
 * specific operation or extensions. The default CODECs will fail to handle
 * such protocol requests. In such scenarios, the providers can register
 * their specific CODEC's with the YANG codec utility to translate the protocol
 * specific data to abstract YANG data tree, from which it can be translate into
 * the YANG modelled java objects for the provider / driver to operate on the
 * device.
 */

public interface YangDataTreeCodec {
    /**
     * When the YMS need to encode simple protocol operation request,
     * it will translate the YANG objects into an abstract YANG data tree and
     * invoke  this registered encode method. Protocol CODEC implementation
     * needs to ensure the overridden method can handle any specific
     * extension or representation of protocol data.
     * The operation type will be set in YANG data tree builder.
     *
     * @param ydtBuilder        Abstract YANG data tree contains the  operation
     *                          request
     * @param protocolOperation protocol operation being performed
     * @return protocol specific string representation.
     */
    String encodeYdtToProtocolFormat(YdtBuilder ydtBuilder,
                                     YmsOperationType protocolOperation);

    /**
     * When the YMS need to encode composite protocol operation request, it
     * will translate the YANG objects into an abstract YANG data
     * tree and invoke this registered encode method. Protocol CODEC
     * implementation needs to ensure the overridden method can  handle any
     * specific extension or representation of protocol data.
     * The Initial chain of node in the YDT will have the operation type set
     * to NONE to specify it is a resource identifier. The Resource
     * information is maintained as a subtree to the resource identifier node.
     *
     * @param ydtBuilder        Abstract YANG data tree contains the  operation
     *                          request
     * @param protocolOperation protocol operation being performed
     * @return composite response containing the requested operation
     * information
     */
    YangCompositeEncoding encodeYdtToCompositeProtocolFormat(
            YdtBuilder ydtBuilder, YmsOperationType protocolOperation);

    /**
     * When YMS decode simple protocol operation request it uses the
     * registered decode method to translate the protocol data into a
     * abstract YANG data tree. Then translate the abstract YANG data
     * tree into the YANG modeled Java objects.
     * The CODEC implementation are unaware of the schema against which they
     * are performing the codec operation, so YMS will send the schema
     * registry on which the YANG data tree needs to operate, so the code
     * implementation needs to pass this schema registry to the get a YDT
     * builder.
     *
     * @param protocolData         input string containing the simple
     *                             protocol data which needs to be decoded
     * @param schemaRegistryForYdt Schema registry based on which the YANG
     *                             data tree will be built
     * @param protocolOperation    protocol operation being performed
     * @return decoded operation request in YANG data tree
     */
    YdtBuilder decodeProtocolDataToYdt(String protocolData,
                                       Object schemaRegistryForYdt,
                                       YmsOperationType protocolOperation);

    /**
     * When YMS decode composite protocol operation request it uses the
     * registered decode method to translate the protocol data into a
     * abstract YANG data tree. Then translate the abstract YANG data
     * tree into the YANG modeled Java objects.
     * The CODEC implementation are unaware of the schema against which they
     * are performing the codec operation, so YMS will send the schema
     * registry on which the YANG data tree needs to operate, so the code
     * implementation needs to pass this schema registry to the get a YDT
     * builder.
     *
     * @param protocolData         composite input string containing the
     *                             protocol data which needs to be decoded
     * @param schemaRegistryForYdt Schema registry based on which the YANG
     *                             data tree will be built
     * @param protocolOperation    protocol operation being performed
     * @return decoded operation request in YANG data tree
     */
    YdtBuilder decodeCompositeProtocolDataToYdt(
            YangCompositeEncoding protocolData, Object schemaRegistryForYdt,
            YmsOperationType protocolOperation);
}

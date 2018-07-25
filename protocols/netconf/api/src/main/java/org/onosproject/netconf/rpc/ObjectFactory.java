/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
// CHECKSTYLE:OFF

package org.onosproject.netconf.rpc;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.onosproject.netconf.rpc package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Rpc_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "rpc");
    private final static QName _RpcReply_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "rpc-reply");
    private final static QName _RpcError_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "rpc-error");
    private final static QName _RpcOperation_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "rpcOperation");
    private final static QName _RpcResponse_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "rpcResponse");
    private final static QName _ErrorInfoTypeBadAttribute_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "bad-attribute");
    private final static QName _ErrorInfoTypeBadElement_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "bad-element");
    private final static QName _ErrorInfoTypeOkElement_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "ok-element");
    private final static QName _ErrorInfoTypeErrElement_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "err-element");
    private final static QName _ErrorInfoTypeNoopElement_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "noop-element");
    private final static QName _ErrorInfoTypeBadNamespace_QNAME = new QName("urn:ietf:params:xml:ns:netconf:base:1.0", "bad-namespace");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.onosproject.netconf.rpc
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Hello }
     * @return hello message
     */
    public Hello createHello() {
        return new Hello();
    }

    /**
     * Create an instance of {@link RpcErrorType }
     * @return error type
     */
    public RpcErrorType createRpcErrorType() {
        return new RpcErrorType();
    }

    /**
     * Create an instance of {@link RpcType }
     * @return rpc type
     */
    public RpcType createRpcType() {
        return new RpcType();
    }

    /**
     * Create an instance of {@link RpcReplyType }
     * @return rpc reply type
     */
    public RpcReplyType createRpcReplyType() {
        return new RpcReplyType();
    }

    /**
     * Create an instance of {@link RpcOperationType }
     * @return rpc operation type
     */
    public RpcOperationType createRpcOperationType() {
        return new RpcOperationType();
    }

    /**
     * Create an instance of {@link RpcResponseType }
     * @return rpc response type
     */
    public RpcResponseType createRpcResponseType() {
        return new RpcResponseType();
    }

    /**
     * Create an instance of {@link Hello.Capabilities }
     * @return hello capabilities
     */
    public Hello.Capabilities createHelloCapabilities() {
        return new Hello.Capabilities();
    }

    /**
     * Create an instance of {@link ErrorInfoType }
     * @return error info type
     */
    public ErrorInfoType createErrorInfoType() {
        return new ErrorInfoType();
    }

    /**
     * Create an instance of {@link RpcErrorType.ErrorMessage }
     * @return error message
     */
    public RpcErrorType.ErrorMessage createRpcErrorTypeErrorMessage() {
        return new RpcErrorType.ErrorMessage();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RpcType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RpcType }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "rpc")
    public JAXBElement<RpcType> createRpc(RpcType value) {
        return new JAXBElement<RpcType>(_Rpc_QNAME, RpcType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RpcReplyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RpcReplyType }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "rpc-reply")
    public JAXBElement<RpcReplyType> createRpcReply(RpcReplyType value) {
        return new JAXBElement<RpcReplyType>(_RpcReply_QNAME, RpcReplyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RpcErrorType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RpcErrorType }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "rpc-error")
    public JAXBElement<RpcErrorType> createRpcError(RpcErrorType value) {
        return new JAXBElement<RpcErrorType>(_RpcError_QNAME, RpcErrorType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RpcOperationType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RpcOperationType }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "rpcOperation")
    public JAXBElement<RpcOperationType> createRpcOperation(RpcOperationType value) {
        return new JAXBElement<RpcOperationType>(_RpcOperation_QNAME, RpcOperationType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RpcResponseType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RpcResponseType }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "rpcResponse")
    public JAXBElement<RpcResponseType> createRpcResponse(RpcResponseType value) {
        return new JAXBElement<RpcResponseType>(_RpcResponse_QNAME, RpcResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "bad-attribute", scope = ErrorInfoType.class)
    public JAXBElement<QName> createErrorInfoTypeBadAttribute(QName value) {
        return new JAXBElement<QName>(_ErrorInfoTypeBadAttribute_QNAME, QName.class, ErrorInfoType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "bad-element", scope = ErrorInfoType.class)
    public JAXBElement<QName> createErrorInfoTypeBadElement(QName value) {
        return new JAXBElement<QName>(_ErrorInfoTypeBadElement_QNAME, QName.class, ErrorInfoType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "ok-element", scope = ErrorInfoType.class)
    public JAXBElement<QName> createErrorInfoTypeOkElement(QName value) {
        return new JAXBElement<QName>(_ErrorInfoTypeOkElement_QNAME, QName.class, ErrorInfoType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "err-element", scope = ErrorInfoType.class)
    public JAXBElement<QName> createErrorInfoTypeErrElement(QName value) {
        return new JAXBElement<QName>(_ErrorInfoTypeErrElement_QNAME, QName.class, ErrorInfoType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QName }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "noop-element", scope = ErrorInfoType.class)
    public JAXBElement<QName> createErrorInfoTypeNoopElement(QName value) {
        return new JAXBElement<QName>(_ErrorInfoTypeNoopElement_QNAME, QName.class, ErrorInfoType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", name = "bad-namespace", scope = ErrorInfoType.class)
    public JAXBElement<String> createErrorInfoTypeBadNamespace(String value) {
        return new JAXBElement<String>(_ErrorInfoTypeBadNamespace_QNAME, String.class, ErrorInfoType.class, value);
    }

}

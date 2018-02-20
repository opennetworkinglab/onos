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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for rpcErrorType complex type.
 * 
 * <p>The following schema fragment specifies the expected         content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="rpcErrorType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="error-type" type="{urn:ietf:params:xml:ns:netconf:base:1.0}ErrorType"/&gt;
 *         &lt;element name="error-tag" type="{urn:ietf:params:xml:ns:netconf:base:1.0}ErrorTag"/&gt;
 *         &lt;element name="error-severity" type="{urn:ietf:params:xml:ns:netconf:base:1.0}ErrorSeverity"/&gt;
 *         &lt;element name="error-app-tag" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="error-path" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="error-message" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;simpleContent&gt;
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *               &lt;/extension&gt;
 *             &lt;/simpleContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="error-info" type="{urn:ietf:params:xml:ns:netconf:base:1.0}errorInfoType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rpcErrorType", propOrder = {
    "errorType",
    "errorTag",
    "errorSeverity",
    "errorAppTag",
    "errorPath",
    "errorMessage",
    "errorInfo"
})
public class RpcErrorType {

    @XmlElement(name = "error-type", required = true)
    @XmlSchemaType(name = "string")
    protected ErrorType errorType;
    @XmlElement(name = "error-tag", required = true)
    @XmlSchemaType(name = "string")
    protected ErrorTag errorTag;
    @XmlElement(name = "error-severity", required = true)
    @XmlSchemaType(name = "string")
    protected ErrorSeverity errorSeverity;
    @XmlElement(name = "error-app-tag")
    protected String errorAppTag;
    @XmlElement(name = "error-path")
    protected String errorPath;
    @XmlElement(name = "error-message")
    protected RpcErrorType.ErrorMessage errorMessage;
    @XmlElement(name = "error-info")
    protected ErrorInfoType errorInfo;

    /**
     * Gets the value of the errorType property.
     * 
     * @return
     *     possible object is
     *     {@link ErrorType }
     *     
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Sets the value of the errorType property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorType }
     *     
     */
    public void setErrorType(ErrorType value) {
        this.errorType = value;
    }

    /**
     * Gets the value of the errorTag property.
     * 
     * @return
     *     possible object is
     *     {@link ErrorTag }
     *     
     */
    public ErrorTag getErrorTag() {
        return errorTag;
    }

    /**
     * Sets the value of the errorTag property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorTag }
     *     
     */
    public void setErrorTag(ErrorTag value) {
        this.errorTag = value;
    }

    /**
     * Gets the value of the errorSeverity property.
     * 
     * @return
     *     possible object is
     *     {@link ErrorSeverity }
     *     
     */
    public ErrorSeverity getErrorSeverity() {
        return errorSeverity;
    }

    /**
     * Sets the value of the errorSeverity property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorSeverity }
     *     
     */
    public void setErrorSeverity(ErrorSeverity value) {
        this.errorSeverity = value;
    }

    /**
     * Gets the value of the errorAppTag property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorAppTag() {
        return errorAppTag;
    }

    /**
     * Sets the value of the errorAppTag property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorAppTag(String value) {
        this.errorAppTag = value;
    }

    /**
     * Gets the value of the errorPath property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorPath() {
        return errorPath;
    }

    /**
     * Sets the value of the errorPath property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorPath(String value) {
        this.errorPath = value;
    }

    /**
     * Gets the value of the errorMessage property.
     * 
     * @return
     *     possible object is
     *     {@link RpcErrorType.ErrorMessage }
     *     
     */
    public RpcErrorType.ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the value of the errorMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link RpcErrorType.ErrorMessage }
     *     
     */
    public void setErrorMessage(RpcErrorType.ErrorMessage value) {
        this.errorMessage = value;
    }

    /**
     * Gets the value of the errorInfo property.
     * 
     * @return
     *     possible object is
     *     {@link ErrorInfoType }
     *     
     */
    public ErrorInfoType getErrorInfo() {
        return errorInfo;
    }

    /**
     * Sets the value of the errorInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorInfoType }
     *     
     */
    public void setErrorInfo(ErrorInfoType value) {
        this.errorInfo = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected         content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;simpleContent&gt;
     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *     &lt;/extension&gt;
     *   &lt;/simpleContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class ErrorMessage {

        @XmlValue
        protected String value;
        @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
        protected String lang;

        /**
         * Gets the value of the value property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the value of the value property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * Gets the value of the lang property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getLang() {
            return lang;
        }

        /**
         * Sets the value of the lang property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setLang(String value) {
            this.lang = value;
        }

    }

}

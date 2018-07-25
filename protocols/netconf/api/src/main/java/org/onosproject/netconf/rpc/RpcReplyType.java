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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for rpcReplyType complex type.
 * 
 * <p>The following schema fragment specifies the expected         content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="rpcReplyType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice&gt;
 *         &lt;element name="ok" type="{http://www.w3.org/2001/XMLSchema}anyType"/&gt;
 *         &lt;sequence&gt;
 *           &lt;element ref="{urn:ietf:params:xml:ns:netconf:base:1.0}rpc-error" maxOccurs="unbounded" minOccurs="0"/&gt;
 *           &lt;element ref="{urn:ietf:params:xml:ns:netconf:base:1.0}rpcResponse" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;/sequence&gt;
 *       &lt;/choice&gt;
 *       &lt;attribute name="message-id" type="{urn:ietf:params:xml:ns:netconf:base:1.0}messageIdType" /&gt;
 *       &lt;anyAttribute processContents='lax'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rpcReplyType", propOrder = {
    "ok",
    "rpcError",
    "rpcResponse"
})
public class RpcReplyType {

    protected Object ok;
    @XmlElement(name = "rpc-error")
    protected List<RpcErrorType> rpcError;
    protected List<RpcResponseType> rpcResponse;
    @XmlAttribute(name = "message-id")
    protected String messageId;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the ok property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getOk() {
        return ok;
    }

    /**
     * Sets the value of the ok property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setOk(Object value) {
        this.ok = value;
    }

    /**
     * Gets the value of the rpcError property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rpcError property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRpcError().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RpcErrorType }
     * 
     * @return list of rpc error types
     */
    public List<RpcErrorType> getRpcError() {
        if (rpcError == null) {
            rpcError = new ArrayList<RpcErrorType>();
        }
        return this.rpcError;
    }

    /**
     * Gets the value of the rpcResponse property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rpcResponse property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRpcResponse().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RpcResponseType }
     * 
     * @return list of rpc response types
     */
    public List<RpcResponseType> getRpcResponse() {
        if (rpcResponse == null) {
            rpcResponse = new ArrayList<RpcResponseType>();
        }
        return this.rpcResponse;
    }

    /**
     * Gets the value of the messageId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Sets the value of the messageId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessageId(String value) {
        this.messageId = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * 
     * <p>
     * the map is keyed by the name of the attribute and 
     * the value is the string value of the attribute.
     * 
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     * 
     * 
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}

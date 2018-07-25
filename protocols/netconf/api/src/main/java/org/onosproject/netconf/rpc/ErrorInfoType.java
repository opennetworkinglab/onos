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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;


/**
 * <p>Java class for errorInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected         content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="errorInfoType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;choice&gt;
 *           &lt;element name="session-id" type="{urn:ietf:params:xml:ns:netconf:base:1.0}SessionIdOrZero"/&gt;
 *           &lt;sequence maxOccurs="unbounded" minOccurs="0"&gt;
 *             &lt;sequence&gt;
 *               &lt;element name="bad-attribute" type="{http://www.w3.org/2001/XMLSchema}QName" minOccurs="0"/&gt;
 *               &lt;element name="bad-element" type="{http://www.w3.org/2001/XMLSchema}QName" minOccurs="0"/&gt;
 *               &lt;element name="ok-element" type="{http://www.w3.org/2001/XMLSchema}QName" minOccurs="0"/&gt;
 *               &lt;element name="err-element" type="{http://www.w3.org/2001/XMLSchema}QName" minOccurs="0"/&gt;
 *               &lt;element name="noop-element" type="{http://www.w3.org/2001/XMLSchema}QName" minOccurs="0"/&gt;
 *               &lt;element name="bad-namespace" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *             &lt;/sequence&gt;
 *           &lt;/sequence&gt;
 *         &lt;/choice&gt;
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "errorInfoType", propOrder = {
    "sessionId",
    "badAttributeAndBadElementAndOkElement",
    "any"
})
public class ErrorInfoType {

    @XmlElement(name = "session-id")
    @XmlSchemaType(name = "unsignedInt")
    protected Long sessionId;
    @XmlElementRefs({
        @XmlElementRef(name = "bad-attribute", namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "bad-element", namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "ok-element", namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "err-element", namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "noop-element", namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "bad-namespace", namespace = "urn:ietf:params:xml:ns:netconf:base:1.0", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<? extends Serializable>> badAttributeAndBadElementAndOkElement;
    @XmlAnyElement(lax = true)
    protected List<Object> any;

    /**
     * Gets the value of the sessionId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getSessionId() {
        return sessionId;
    }

    /**
     * Sets the value of the sessionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSessionId(Long value) {
        this.sessionId = value;
    }

    /**
     * Gets the value of the badAttributeAndBadElementAndOkElement property.
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the
     * badAttributeAndBadElementAndOkElement property.
     * </p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBadAttributeAndBadElementAndOkElement().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link QName }{@code >}
     * {@link JAXBElement }{@code <}{@link QName }{@code >}
     * {@link JAXBElement }{@code <}{@link QName }{@code >}
     * {@link JAXBElement }{@code <}{@link QName }{@code >}
     * {@link JAXBElement }{@code <}{@link QName }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * </p>
     * @return list of properties
     */
    public List<JAXBElement<? extends Serializable>> getBadAttributeAndBadElementAndOkElement() {
        if (badAttributeAndBadElementAndOkElement == null) {
            badAttributeAndBadElementAndOkElement = new ArrayList<JAXBElement<? extends Serializable>>();
        }
        return this.badAttributeAndBadElementAndOkElement;
    }

    /**
     * Gets the value of the any property.
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the any property.
     * </p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAny().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Element }
     * {@link Object }
     * </p>
     * @return list of properties
     */
    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<Object>();
        }
        return this.any;
    }

}

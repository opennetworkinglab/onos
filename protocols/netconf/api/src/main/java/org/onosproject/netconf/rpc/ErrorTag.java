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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * Java class for ErrorTag.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * </p>
 * <pre>
 * &lt;simpleType name="ErrorTag"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="in-use"/&gt;
 *     &lt;enumeration value="invalid-value"/&gt;
 *     &lt;enumeration value="too-big"/&gt;
 *     &lt;enumeration value="missing-attribute"/&gt;
 *     &lt;enumeration value="bad-attribute"/&gt;
 *     &lt;enumeration value="unknown-attribute"/&gt;
 *     &lt;enumeration value="missing-element"/&gt;
 *     &lt;enumeration value="bad-element"/&gt;
 *     &lt;enumeration value="unknown-element"/&gt;
 *     &lt;enumeration value="unknown-namespace"/&gt;
 *     &lt;enumeration value="access-denied"/&gt;
 *     &lt;enumeration value="lock-denied"/&gt;
 *     &lt;enumeration value="resource-denied"/&gt;
 *     &lt;enumeration value="rollback-failed"/&gt;
 *     &lt;enumeration value="data-exists"/&gt;
 *     &lt;enumeration value="data-missing"/&gt;
 *     &lt;enumeration value="operation-not-supported"/&gt;
 *     &lt;enumeration value="operation-failed"/&gt;
 *     &lt;enumeration value="partial-operation"/&gt;
 *     &lt;enumeration value="malformed-message"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ErrorTag")
@XmlEnum
public enum ErrorTag {

    @XmlEnumValue("in-use")
    IN_USE("in-use"),
    @XmlEnumValue("invalid-value")
    INVALID_VALUE("invalid-value"),
    @XmlEnumValue("too-big")
    TOO_BIG("too-big"),
    @XmlEnumValue("missing-attribute")
    MISSING_ATTRIBUTE("missing-attribute"),
    @XmlEnumValue("bad-attribute")
    BAD_ATTRIBUTE("bad-attribute"),
    @XmlEnumValue("unknown-attribute")
    UNKNOWN_ATTRIBUTE("unknown-attribute"),
    @XmlEnumValue("missing-element")
    MISSING_ELEMENT("missing-element"),
    @XmlEnumValue("bad-element")
    BAD_ELEMENT("bad-element"),
    @XmlEnumValue("unknown-element")
    UNKNOWN_ELEMENT("unknown-element"),
    @XmlEnumValue("unknown-namespace")
    UNKNOWN_NAMESPACE("unknown-namespace"),
    @XmlEnumValue("access-denied")
    ACCESS_DENIED("access-denied"),
    @XmlEnumValue("lock-denied")
    LOCK_DENIED("lock-denied"),
    @XmlEnumValue("resource-denied")
    RESOURCE_DENIED("resource-denied"),
    @XmlEnumValue("rollback-failed")
    ROLLBACK_FAILED("rollback-failed"),
    @XmlEnumValue("data-exists")
    DATA_EXISTS("data-exists"),
    @XmlEnumValue("data-missing")
    DATA_MISSING("data-missing"),
    @XmlEnumValue("operation-not-supported")
    OPERATION_NOT_SUPPORTED("operation-not-supported"),
    @XmlEnumValue("operation-failed")
    OPERATION_FAILED("operation-failed"),
    @XmlEnumValue("partial-operation")
    PARTIAL_OPERATION("partial-operation"),
    @XmlEnumValue("malformed-message")
    MALFORMED_MESSAGE("malformed-message");
    private final String value;

    ErrorTag(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ErrorTag fromValue(String v) {
        for (ErrorTag c: ErrorTag.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}

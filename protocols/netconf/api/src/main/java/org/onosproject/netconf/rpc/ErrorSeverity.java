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
 * Java class for ErrorSeverity.
 * 
 * <p>The following schema fragment specifies the expected         content contained within this class.
 * </p>
 * <pre>
 * &lt;simpleType name="ErrorSeverity"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="error"/&gt;
 *     &lt;enumeration value="warning"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ErrorSeverity")
@XmlEnum
public enum ErrorSeverity {

    @XmlEnumValue("error")
    ERROR("error"),
    @XmlEnumValue("warning")
    WARNING("warning");
    private final String value;

    ErrorSeverity(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ErrorSeverity fromValue(String v) {
        for (ErrorSeverity c: ErrorSeverity.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}

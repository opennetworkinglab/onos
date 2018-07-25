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
 * Java class for editOperationType.
 * <p>
 * The following schema fragment specifies the expected content contained
 * within this class.
 * </p>
 * <pre>
 * &lt;simpleType name="editOperationType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="merge"/&gt;
 *     &lt;enumeration value="replace"/&gt;
 *     &lt;enumeration value="create"/&gt;
 *     &lt;enumeration value="delete"/&gt;
 *     &lt;enumeration value="remove"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "editOperationType")
@XmlEnum
public enum EditOperationType {

    @XmlEnumValue("merge")
    MERGE("merge"),
    @XmlEnumValue("replace")
    REPLACE("replace"),
    @XmlEnumValue("create")
    CREATE("create"),
    @XmlEnumValue("delete")
    DELETE("delete"),
    @XmlEnumValue("remove")
    REMOVE("remove");
    private final String value;

    EditOperationType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EditOperationType fromValue(String v) {
        for (EditOperationType c: EditOperationType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}

/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.yangutils.translator.tojava;

import java.io.Serializable;

import org.onosproject.yangutils.datamodel.YangType;

/**
 * Maintains the attribute info corresponding to class/interface generated.
 */
public class AttributeInfo implements Serializable {

    /**
     * version of serialized info.
     */
    private static final long serialVersionUID = 201602151004L;

    /**
     * The data type info of attribute.
     */
    private YangType<?> attrType;

    /**
     * Name of the attribute.
     */
    private String name;

    /**
     * If the added attribute is a list of info.
     */
    private boolean isListAttr;

    /**
     * If the added attribute has to be accessed in a fully qualified manner.
     */
    private boolean isQualifiedName;

    /**
     * Default constructor.
     */
    public AttributeInfo() {
    }

    /**
     * Get the data type info of attribute.
     *
     * @return the data type info of attribute.
     */
    public YangType<?> getAttributeType() {
        return attrType;
    }

    /**
     * Set the data type info of attribute.
     *
     * @param type the data type info of attribute.
     */
    public void setAttributeType(YangType<?> type) {
        attrType = type;
    }

    /**
     * Get name of the attribute.
     *
     * @return name of the attribute.
     */
    public String getAttributeName() {
        return name;
    }

    /**
     * Set name of the attribute.
     *
     * @param attrName name of the attribute.
     */
    public void setAttributeName(String attrName) {
        name = attrName;
    }

    /**
     * Get if the added attribute is a list of info.
     *
     * @return the if the added attribute is a list of info.
     */
    public boolean isListAttr() {
        return isListAttr;
    }

    /**
     * Set if the added attribute is a list of info.
     *
     * @param isList if the added attribute is a list of info.
     */
    public void setListAttr(boolean isList) {
        isListAttr = isList;
    }

    /**
     * Get if the added attribute has to be accessed in a fully qualified
     * manner.
     *
     * @return the if the added attribute has to be accessed in a fully
     *         qualified manner.
     */
    public boolean isQualifiedName() {
        return isQualifiedName;
    }

    /**
     * Set if the added attribute has to be accessed in a fully qualified
     * manner.
     *
     * @param isQualified if the added attribute has to be accessed in a fully
     *            qualified manner.
     */
    public void setQualifiedName(boolean isQualified) {
        isQualifiedName = isQualified;
    }

}

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

package org.onosproject.yangutils.datamodel;

/**
 * Represents the derived information.
 *
 * @param <T> extended information.
 */
public class YangDerivedInfo<T> {

    /**
     * YANG typedef reference.
     */
    private YangTypeDef referredTypeDef;

    /**
     * Resolved additional information about data type after linking, example
     * restriction info, named values, etc. The extra information is based
     * on the data type. Based on the data type, the extended info can vary.
     */
    private T resolvedExtendedInfo;

    /**
     * Additional information about data type, example restriction info, named
     * values, etc. The extra information is based on the data type. Based on
     * the data type, the extended info can vary.
     */
    private T extendedInfo;

    /**
     * Returns the referred typedef reference.
     *
     * @return referred typedef reference
     */
    public YangTypeDef getReferredTypeDef() {
        return referredTypeDef;
    }

    /**
     * Sets the referred typedef reference.
     *
     * @param referredTypeDef referred typedef reference
     */
    public void setReferredTypeDef(YangTypeDef referredTypeDef) {
        this.referredTypeDef = referredTypeDef;
    }

    /**
     * Returns resolved extended information after successful linking.
     *
     * @return resolved extended information
     */
    public T getResolvedExtendedInfo() {
        return resolvedExtendedInfo;
    }

    /**
     * Sets resolved extended information after successful linking.
     *
     * @param resolvedExtendedInfo resolved extended information
     */
    public void setResolvedExtendedInfo(T resolvedExtendedInfo) {
        this.resolvedExtendedInfo = resolvedExtendedInfo;
    }

    /**
     * Returns extended information.
     *
     * @return extended information
     */
    public T getExtendedInfo() {
        return extendedInfo;
    }

    /**
     * Sets extended information.
     *
     * @param extendedInfo extended information
     */
    public void setExtendedInfo(T extendedInfo) {
        this.extendedInfo = extendedInfo;
    }
}

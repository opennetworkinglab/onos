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

import java.io.Serializable;

/*-
 * Reference RFC 6020.
 *
 * Binary can be restricted with "length" statements alone.
 *
 */

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangUint64;

/**
 * Represents the restriction for length data type.
 */
public class YangLengthRestriction implements YangDesc, YangReference, YangAppErrorInfo, Parsable, Serializable {

    /*-
     * Reference RFC 6020.
     * The length Statement
     *
     * The "length" statement, which is an optional sub-statement to the
     * "type" statement, takes as an argument a length expression string.
     * It is used to restrict the built-in type "string", or types derived
     * from "string".
     * A "length" statement restricts the number of unicode characters in
     * the string.
     * A length range consists of an explicit value, or a lower bound, two
     * consecutive dots "..", and an upper bound.  Multiple values or ranges
     * can be given, separated by "|".  Length-restricting values MUST NOT
     * be negative.  If multiple values or ranges are given, they all MUST
     * be disjoint and MUST be in ascending order.  If a length restriction
     * is applied to an already length-restricted type, the new restriction
     * MUST be equal or more limiting, that is, raising the lower bounds,
     * reducing the upper bounds, removing explicit length values or ranges,
     * or splitting ranges into multiple ranges with intermediate gaps.  A
     * length value is a non-negative integer, or one of the special values
     * "min" or "max". "min" and "max" mean the minimum and maximum length
     * accepted for the type being restricted, respectively.  An
     * implementation is not required to support a length value larger than
     * 18446744073709551615.
     * The length's sub-statements
     *
     *  +---------------+---------+-------------+-----------------+
     *  | substatement  | section | cardinality | mapped data type|
     *  +---------------+---------+-------------+-----------------+
     *  | description   | 7.19.3  | 0..1        | string          |
     *  | error-app-tag | 7.5.4.2 | 0..1        | string          |
     *  | error-message | 7.5.4.1 | 0..1        | string          |
     *  | reference     | 7.19.4  | 0..1        | string          |
     *  +---------------+---------+-------------+-----------------+
     */

    private static final long serialVersionUID = 806201645L;

    /**
     * Length restriction information.
     */
    private YangRangeRestriction<YangUint64> lengthRestriction;

    /**
     * Textual reference.
     */
    private String reference;

    /**
     * Application's error message, to be used for data error.
     */
    private String errorMessage;

    /**
     * Application's error tag, to be filled in data validation error response.
     */
    private String errorAppTag;

    /**
     * Textual description.
     */
    private String description;

    /**
     * Creates a YANG length restriction object.
     */
    public YangLengthRestriction() {
    }

    /**
     * Returns the length restriction on the string data.
     *
     * @return length restriction on the string data
     */
    public YangRangeRestriction<YangUint64> getLengthRestriction() {
        return lengthRestriction;
    }

    /**
     * Sets the length restriction on the string data.
     *
     * @param lengthRestriction length restriction on the string data
     */
    public void setLengthRestriction(YangRangeRestriction<YangUint64> lengthRestriction) {
        this.lengthRestriction = lengthRestriction;
    }

    /**
     * Returns the textual reference of the length restriction.
     *
     * @return textual reference of the length restriction
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Sets the textual reference of the length restriction.
     *
     * @param ref textual reference of the length restriction
     */
    @Override
    public void setReference(String ref) {
        reference = ref;
    }

    /**
     * Returns the description of the length restriction.
     *
     * @return description of the length restriction
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the length restriction.
     *
     * @param desc description of the length restriction
     */
    @Override
    public void setDescription(String desc) {
        description = desc;

    }

    /**
     * Returns application's error message, to be used for data error.
     *
     * @return Application's error message, to be used for data error
     */
    @Override
    public String getGetErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets Application's error message, to be used for data error.
     *
     * @param errMsg Application's error message, to be used for data error
     */
    @Override
    public void setErrorMessage(String errMsg) {
        errorMessage = errMsg;

    }

    /**
     * Returns application's error tag, to be used for data error.
     *
     * @return application's error tag, to be used for data error
     */
    @Override
    public String getGetErrorAppTag() {
        return errorAppTag;
    }

    /**
     * Sets application's error tag, to be used for data error.
     *
     * @param errTag application's error tag, to be used for data error.
     */
    @Override
    public void setErrorAppTag(String errTag) {
        errorAppTag = errTag;
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.PATTERN_DATA;
    }

    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO: implement the method.
    }

    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO: implement the method.
    }
}

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

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangUint64;

/*-
 * Reference RFC 6020.
 *
 * A string can be restricted with the "length" and "pattern" statements.
 *
 */

/**
 * Represents the restriction for string data type.
 */
public class YangStringRestriction implements YangDesc, YangReference, YangAppErrorInfo, Parsable, Serializable {

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

    private static final long serialVersionUID = 8062016053L;

    /**
     * Length restriction information.
     */
    private YangRangeRestriction<YangUint64> lengthRestriction;

    /**
     * Effective pattern restriction for the type.
     */
    private YangPatternRestriction patternRestriction;

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
     * Creates a YANG string restriction object.
     */
    public YangStringRestriction() {
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
     * Returns the pattern restriction for the type.
     *
     * @return pattern restriction for the type
     */
    public YangPatternRestriction getPatternRestriction() {
        return patternRestriction;
    }

    /**
     * Sets the pattern restriction for the type.
     *
     * @param patternRestriction pattern restriction for the type
     */
    public void setPatternRestriction(YangPatternRestriction patternRestriction) {
        this.patternRestriction = patternRestriction;
    }

    /**
     * Adds a new pattern restriction for the type.
     *
     * @param newPattern new pattern restriction for the type
     */
    public void addPattern(String newPattern) {
        if (getPatternRestriction() == null) {
            setPatternRestriction(new YangPatternRestriction());
        }
        getPatternRestriction().addPattern(newPattern);
    }

    /**
     * Returns the textual reference of the string restriction.
     *
     * @return textual reference of the string restriction
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Sets the textual reference of the string restriction.
     *
     * @param ref textual reference of the string restriction
     */
    @Override
    public void setReference(String ref) {
        reference = ref;
    }

    /**
     * Returns the description of the string restriction.
     *
     * @return description of the string restriction
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the string restriction.
     *
     * @param desc description of the string restriction
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

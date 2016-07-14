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
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;

import com.google.common.base.Strings;

import static org.onosproject.yangutils.datamodel.YangDataTypes.BINARY;
import static org.onosproject.yangutils.datamodel.YangDataTypes.BITS;
import static org.onosproject.yangutils.datamodel.YangDataTypes.BOOLEAN;
import static org.onosproject.yangutils.datamodel.YangDataTypes.DERIVED;
import static org.onosproject.yangutils.datamodel.YangDataTypes.EMPTY;
import static org.onosproject.yangutils.datamodel.YangDataTypes.ENUMERATION;
import static org.onosproject.yangutils.datamodel.YangDataTypes.IDENTITYREF;
import static org.onosproject.yangutils.datamodel.YangDataTypes.LEAFREF;
import static org.onosproject.yangutils.datamodel.YangDataTypes.STRING;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UNION;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.INTRA_FILE_RESOLVED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.RESOLVED;
import static org.onosproject.yangutils.datamodel.utils.RestrictionResolver.isOfRangeRestrictedType;
import static org.onosproject.yangutils.datamodel.utils.RestrictionResolver.processLengthRestriction;
import static org.onosproject.yangutils.datamodel.utils.RestrictionResolver.processRangeRestriction;

/**
 * Represents the derived information.
 *
 * @param <T> extended information.
 */
public class YangDerivedInfo<T>
        implements LocationInfo, Cloneable, Serializable {

    private static final long serialVersionUID = 806201641L;

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
     * Line number of pattern restriction in YANG file.
     */
    private int lineNumber;

    /**
     * Position of pattern restriction in line.
     */
    private int charPositionInLine;

    /**
     * Effective built-in type, requried in case type of typedef is again a
     * derived type. This information is to be added during linking.
     */
    private YangDataTypes effectiveBuiltInType;

    /**
     * Length restriction string to temporary store the length restriction when the type
     * is derived.
     */
    private String lengthRestrictionString;

    /**
     * Range restriction string to temporary store the range restriction when the type
     * is derived.
     */
    private String rangeRestrictionString;

    /**
     * Pattern restriction string to  temporary store the pattern restriction when the type
     * is derived.
     */
    private YangPatternRestriction patternRestriction;

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

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int getCharPosition() {
        return charPositionInLine;
    }

    @Override
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public void setCharPosition(int charPositionInLine) {
        this.charPositionInLine = charPositionInLine;
    }

    /**
     * Returns the length restriction string.
     *
     * @return the length restriction string
     */
    public String getLengthRestrictionString() {
        return lengthRestrictionString;
    }

    /**
     * Sets the length restriction string.
     *
     * @param lengthRestrictionString the length restriction string
     */
    public void setLengthRestrictionString(String lengthRestrictionString) {
        this.lengthRestrictionString = lengthRestrictionString;
    }

    /**
     * Returns the range restriction string.
     *
     * @return the range restriction string
     */
    public String getRangeRestrictionString() {
        return rangeRestrictionString;
    }

    /**
     * Sets the range restriction string.
     *
     * @param rangeRestrictionString the range restriction string
     */
    public void setRangeRestrictionString(String rangeRestrictionString) {
        this.rangeRestrictionString = rangeRestrictionString;
    }

    /**
     * Returns the pattern restriction.
     *
     * @return the pattern restriction
     */
    public YangPatternRestriction getPatternRestriction() {
        return patternRestriction;
    }

    /**
     * Sets the pattern restriction.
     *
     * @param patternRestriction the pattern restriction
     */
    public void setPatternRestriction(YangPatternRestriction patternRestriction) {
        this.patternRestriction = patternRestriction;
    }

    /**
     * Returns effective built-in type.
     *
     * @return effective built-in type
     */
    public YangDataTypes getEffectiveBuiltInType() {
        return effectiveBuiltInType;
    }

    /**
     * Sets effective built-in type.
     *
     * @param effectiveBuiltInType effective built-in type
     */
    public void setEffectiveBuiltInType(YangDataTypes effectiveBuiltInType) {
        this.effectiveBuiltInType = effectiveBuiltInType;
    }

    /**
     * Resolves the type derived info, by obtaining the effective built-in type
     * and resolving the restrictions.
     *
     * @return resolution status
     * @throws DataModelException a violation in data mode rule
     */
    public ResolvableStatus resolve()
            throws DataModelException {

        YangType<?> baseType = getReferredTypeDef().getTypeDefBaseType();

        /*
         * Checks the data type of the referred typedef, if it's derived, obtain
         * effective built-in type and restrictions from it's derived info,
         * otherwise take from the base type of type itself.
         */
        if (baseType.getDataType() == DERIVED) {
            /*
             * Check whether the referred typedef is resolved.
             */
            if (baseType.getResolvableStatus() != INTRA_FILE_RESOLVED && baseType.getResolvableStatus() != RESOLVED) {
                throw new DataModelException("Linker Error: Referred typedef is not resolved for type.");
            }

            /*
             * Check if the referred typedef is intra file resolved, if yes sets
             * current status also to intra file resolved .
             */
            if (getReferredTypeDef().getTypeDefBaseType().getResolvableStatus() == INTRA_FILE_RESOLVED) {
                return INTRA_FILE_RESOLVED;
            }
            setEffectiveBuiltInType(((YangDerivedInfo<?>) baseType.getDataTypeExtendedInfo())
                    .getEffectiveBuiltInType());
            YangDerivedInfo refDerivedInfo = (YangDerivedInfo<?>) baseType.getDataTypeExtendedInfo();
            /*
             * Check whether the effective built-in type can have range
             * restrictions, if yes call resolution of range.
             */
            if (isOfRangeRestrictedType(getEffectiveBuiltInType())) {
                if (refDerivedInfo.getResolvedExtendedInfo() == null) {
                    resolveRangeRestriction(null);
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve range/string restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                } else {
                    if (!(refDerivedInfo.getResolvedExtendedInfo() instanceof YangRangeRestriction)) {
                        throw new DataModelException("Linker error: Referred typedef restriction info is of invalid " +
                                "type.");
                    }
                    resolveRangeRestriction((YangRangeRestriction) refDerivedInfo.getResolvedExtendedInfo());
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve range/string restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                }
                /*
                 * If the effective built-in type is of type string calls for
                 * string resolution.
                 */
            } else if (getEffectiveBuiltInType() == STRING) {
                if (refDerivedInfo.getResolvedExtendedInfo() == null) {
                    resolveStringRestriction(null);
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve range/string restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                } else {
                    if (!(refDerivedInfo.getResolvedExtendedInfo() instanceof YangStringRestriction)) {
                        throw new DataModelException("Linker error: Referred typedef restriction info is of invalid " +
                                "type.");
                    }
                    resolveStringRestriction((YangStringRestriction) refDerivedInfo.getResolvedExtendedInfo());
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve range/string restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                }
            } else if (getEffectiveBuiltInType() == BINARY) {
                if (refDerivedInfo.getResolvedExtendedInfo() == null) {
                    resolveLengthRestriction(null);
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve length restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                } else {
                    if (!(refDerivedInfo.getResolvedExtendedInfo() instanceof YangRangeRestriction)) {
                        throw new DataModelException("Linker error: Referred typedef restriction info is of invalid " +
                                "type.");
                    }
                    resolveLengthRestriction((YangRangeRestriction) refDerivedInfo.getResolvedExtendedInfo());
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve length restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                }
            }
        } else {
            setEffectiveBuiltInType(baseType.getDataType());
            /*
             * Check whether the effective built-in type can have range
             * restrictions, if yes call resolution of range.
             */
            if (isOfRangeRestrictedType(getEffectiveBuiltInType())) {
                if (baseType.getDataTypeExtendedInfo() == null) {
                    resolveRangeRestriction(null);
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve range/string restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                } else {
                    if (!(baseType.getDataTypeExtendedInfo() instanceof YangRangeRestriction)) {
                        throw new DataModelException("Linker error: Referred typedef restriction info is of invalid " +
                                "type.");
                    }
                    resolveRangeRestriction((YangRangeRestriction) baseType.getDataTypeExtendedInfo());
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve range/string restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                }
                /*
                 * If the effective built-in type is of type string calls for
                 * string resolution.
                 */
            } else if (getEffectiveBuiltInType() == STRING) {
                if (baseType.getDataTypeExtendedInfo() == null) {
                    resolveStringRestriction(null);
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve range/string restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                } else {
                    if (!(baseType.getDataTypeExtendedInfo() instanceof YangStringRestriction)) {
                        throw new DataModelException("Linker error: Referred typedef restriction info is of invalid " +
                                "type.");
                    }
                    resolveStringRestriction((YangStringRestriction) baseType.getDataTypeExtendedInfo());
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve range/string restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                }
            } else if (getEffectiveBuiltInType() == BINARY) {
                if (baseType.getDataTypeExtendedInfo() == null) {
                    resolveLengthRestriction(null);
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve length restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                } else {
                    if (!(baseType.getDataTypeExtendedInfo() instanceof YangRangeRestriction)) {
                        throw new DataModelException("Linker error: Referred typedef restriction info is of invalid " +
                                "type.");
                    }
                    resolveLengthRestriction((YangRangeRestriction) baseType.getDataTypeExtendedInfo());
                    /*
                     * Return the resolution status as resolved, if it's not
                     * resolve length restriction will throw exception in
                     * previous function.
                     */
                    return RESOLVED;
                }
            }
        }

        /*
         * Check if the data type is the one which can't be restricted, in this
         * case check whether no self restrictions should be present.
         */
        if (isOfValidNonRestrictedType(getEffectiveBuiltInType())) {
            if (Strings.isNullOrEmpty(getLengthRestrictionString())
                    && Strings.isNullOrEmpty(getRangeRestrictionString())
                    && getPatternRestriction() == null) {
                return RESOLVED;
            } else {
                throw new DataModelException("YANG file error: Restrictions can't be applied to a given type");
            }
        }

        // Throw exception for unsupported types
        throw new DataModelException("Linker error: Unable to process the derived type.");
    }

    /**
     * Resolves the string restrictions.
     *
     * @param refStringRestriction referred string restriction of typedef
     * @throws DataModelException a violation in data model rule
     */
    private void resolveStringRestriction(YangStringRestriction refStringRestriction)
            throws DataModelException {
        YangStringRestriction curStringRestriction = null;
        YangRangeRestriction refRangeRestriction = null;
        YangPatternRestriction refPatternRestriction = null;

        /*
         * Check that range restriction should be null when built-in type is
         * string.
         */
        if (!Strings.isNullOrEmpty(getRangeRestrictionString())) {
            DataModelException dataModelException = new DataModelException("YANG file error: Range restriction " +
                    "should't be present for string data type.");
            dataModelException.setLine(lineNumber);
            dataModelException.setCharPosition(charPositionInLine);
            throw dataModelException;
        }

        /*
         * If referred restriction and self restriction both are null, no
         * resolution is required.
         */
        if (refStringRestriction == null && Strings.isNullOrEmpty(getLengthRestrictionString())
                && getPatternRestriction() == null) {
            return;
        }

        /*
         * If referred string restriction is not null, take value of length and
         * pattern restriction and assign.
         */
        if (refStringRestriction != null) {
            refRangeRestriction = refStringRestriction.getLengthRestriction();
            refPatternRestriction = refStringRestriction.getPatternRestriction();
        }

        YangRangeRestriction lengthRestriction = resolveLengthRestriction(refRangeRestriction);
        YangPatternRestriction patternRestriction = resolvePatternRestriction(refPatternRestriction);

        /*
         * Check if either of length or pattern restriction is present, if yes
         * create string restriction and assign value.
         */
        if (lengthRestriction != null || patternRestriction != null) {
            curStringRestriction = new YangStringRestriction();
            curStringRestriction.setLengthRestriction(lengthRestriction);
            curStringRestriction.setPatternRestriction(patternRestriction);
        }
        setResolvedExtendedInfo((T) curStringRestriction);
    }

    /**
     * Resolves pattern restriction.
     *
     * @param refPatternRestriction referred pattern restriction of typedef
     * @return resolved pattern restriction
     */
    private YangPatternRestriction resolvePatternRestriction(YangPatternRestriction refPatternRestriction) {
        /*
         * If referred restriction and self restriction both are null, no
         * resolution is required.
         */
        if (refPatternRestriction == null && getPatternRestriction() == null) {
            return null;
        }

        /*
         * If self restriction is null, and referred restriction is present
         * shallow copy the referred to self.
         */
        if (getPatternRestriction() == null) {
            return refPatternRestriction;
        }

        /*
         * If referred restriction is null, and self restriction is present
         * carry out self resolution.
         */
        if (refPatternRestriction == null) {
            return getPatternRestriction();
        }

        /*
         * Get patterns of referred type and add it to current pattern
         * restrictions.
         */
        for (String pattern : refPatternRestriction.getPatternList()) {
            getPatternRestriction().addPattern(pattern);
        }
        return getPatternRestriction();
    }

    /**
     * Resolves the length restrictions.
     *
     * @param refLengthRestriction referred length restriction of typedef
     * @return resolved length restriction
     * @throws DataModelException a violation in data model rule
     */
    private YangRangeRestriction resolveLengthRestriction(YangRangeRestriction refLengthRestriction)
            throws DataModelException {

        /*
         * If referred restriction and self restriction both are null, no
         * resolution is required.
         */
        if (refLengthRestriction == null && Strings.isNullOrEmpty(getLengthRestrictionString())) {
            return null;
        }

        /*
         * If self restriction is null, and referred restriction is present
         * shallow copy the referred to self.
         */
        if (Strings.isNullOrEmpty(getLengthRestrictionString())) {
            return refLengthRestriction;
        }

        /*
         * If referred restriction is null, and self restriction is present
         * carry out self resolution.
         */
        if (refLengthRestriction == null) {
            YangRangeRestriction curLengthRestriction = processLengthRestriction(null, lineNumber,
                    charPositionInLine, false, getLengthRestrictionString());
            return curLengthRestriction;
        }

        /*
         * Carry out self resolution based with obtained effective built-in type
         * and MIN/MAX values as per the referred typedef's values.
         */
        YangRangeRestriction curLengthRestriction = processLengthRestriction(refLengthRestriction, lineNumber,
                charPositionInLine, true, getLengthRestrictionString());

        // Resolve the range with referred typedef's restriction.
        resolveLengthAndRangeRestriction(refLengthRestriction, curLengthRestriction);
        return curLengthRestriction;
    }

    /**
     * Resolves the length/range self and referred restriction, to check whether
     * the all the range interval in self restriction is stricter than the
     * referred typedef's restriction.
     *
     * @param refRestriction referred restriction
     * @param curRestriction self restriction
     */
    private void resolveLengthAndRangeRestriction(YangRangeRestriction refRestriction,
            YangRangeRestriction curRestriction)
            throws DataModelException {
        for (Object curInterval : curRestriction.getAscendingRangeIntervals()) {
            if (!(curInterval instanceof YangRangeInterval)) {
                throw new DataModelException("Linker error: Current range intervals not processed correctly.");
            }
            try {
                refRestriction.isValidInterval((YangRangeInterval) curInterval);
            } catch (DataModelException e) {
                DataModelException dataModelException = new DataModelException(e);
                dataModelException.setLine(lineNumber);
                dataModelException.setCharPosition(charPositionInLine);
                throw dataModelException;
            }
        }
    }

    /**
     * Resolves the range restrictions.
     *
     * @param refRangeRestriction referred range restriction of typedef
     * @throws DataModelException a violation in data model rule
     */
    private void resolveRangeRestriction(YangRangeRestriction refRangeRestriction)
            throws DataModelException {

        /*
         * Check that string restriction should be null when built-in type is of
         * range type.
         */
        if (!Strings.isNullOrEmpty(getLengthRestrictionString()) || getPatternRestriction() != null) {
            DataModelException dataModelException = new DataModelException("YANG file error: Length/Pattern " +
                    "restriction should't be present for int/uint/decimal data type.");
            dataModelException.setLine(lineNumber);
            dataModelException.setCharPosition(charPositionInLine);
            throw dataModelException;
        }

        /*
         * If referred restriction and self restriction both are null, no
         * resolution is required.
         */
        if (refRangeRestriction == null && Strings.isNullOrEmpty(getRangeRestrictionString())) {
            return;
        }

        /*
         * If self restriction is null, and referred restriction is present
         * shallow copy the referred to self.
         */
        if (Strings.isNullOrEmpty(getRangeRestrictionString())) {
            setResolvedExtendedInfo((T) refRangeRestriction);
            return;
        }

        /*
         * If referred restriction is null, and self restriction is present
         * carry out self resolution.
         */
        if (refRangeRestriction == null) {
            YangRangeRestriction curRangeRestriction = processRangeRestriction(null, lineNumber,
                    charPositionInLine, false, getRangeRestrictionString(), getEffectiveBuiltInType());
            setResolvedExtendedInfo((T) curRangeRestriction);
            return;
        }

        /*
         * Carry out self resolution based with obtained effective built-in type
         * and MIN/MAX values as per the referred typedef's values.
         */
        YangRangeRestriction curRangeRestriction = processRangeRestriction(refRangeRestriction, lineNumber,
                charPositionInLine, true, getRangeRestrictionString(), getEffectiveBuiltInType());

        // Resolve the range with referred typedef's restriction.
        resolveLengthAndRangeRestriction(refRangeRestriction, curRangeRestriction);
        setResolvedExtendedInfo((T) curRangeRestriction);
    }

    /**
     * Returns whether the data type is of non restricted type.
     *
     * @param dataType data type to be checked
     * @return true, if data type can't be restricted, false otherwise
     */
    private boolean isOfValidNonRestrictedType(YangDataTypes dataType) {
        return dataType == BOOLEAN
                || dataType == ENUMERATION
                || dataType == BITS
                || dataType == EMPTY
                || dataType == UNION
                || dataType == IDENTITYREF
                || dataType == LEAFREF;
    }
}

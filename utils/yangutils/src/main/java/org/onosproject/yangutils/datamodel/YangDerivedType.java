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

package org.onosproject.yangutils.datamodel;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.utils.YangConstructType;

/*-
 * Reference RFC 6020.
 *
 * The typedef Statement
 *
 * The "typedef" statement defines a new type that may be used locally
 * in the module, in modules or submodules which include it, and by
 * other modules that import from it. The new type is called the
 * "derived type", and the type from which it was derived is called
 * the "base type".  All derived types can be traced back to a YANG
 * built-in type.
 *
 * The "typedef" statement's argument is an identifier that is the name
 * of the type to be defined, and MUST be followed by a block of
 * sub-statements that holds detailed typedef information.
 *
 * The name of the type MUST NOT be one of the YANG built-in types.  If
 * the typedef is defined at the top level of a YANG module or
 * submodule, the name of the type to be defined MUST be unique within
 * the module.
 */
/**
 * Derived type information.
 */
public class YangDerivedType implements Parsable {

    /**
     * All derived types can be traced back to a YANG built-in type.
     */
    private YangDataTypes effectiveYangBuiltInType;

    /**
     * Base type from which the current type is derived.
     */
    private YangType<?> baseType;

    /**
     * Default constructor.
     */
    public YangDerivedType() {
    }

    /**
     * Get the effective YANG built-in type of the derived data type.
     *
     * @return effective YANG built-in type of the derived data type
     */
    public YangDataTypes getEffectiveYangBuiltInType() {
        return effectiveYangBuiltInType;
    }

    /**
     * Set the effective YANG built-in type of the derived data type.
     *
     * @param builtInType effective YANG built-in type of the derived data type
     */
    public void setEffectiveYangBuiltInType(YangDataTypes builtInType) {
        effectiveYangBuiltInType = builtInType;
    }

    /**
     * Get the base type information.
     *
     * @return base type information
     */
    public YangType<?> getBaseType() {
        return baseType;
    }

    /**
     * Get the base type information.
     *
     * @param baseType base type information
     */
    public void setBaseType(YangType<?> baseType) {
        this.baseType = baseType;
    }

    /**
     * Get the parsable type.
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.DERIVED;
    }

    /**
     * TODO.
     */
    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO Auto-generated method stub

    }

    /**
     * TODO.
     */
    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO Auto-generated method stub

    }

}

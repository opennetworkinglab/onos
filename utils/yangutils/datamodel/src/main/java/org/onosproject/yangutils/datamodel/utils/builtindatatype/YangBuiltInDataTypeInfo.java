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
package org.onosproject.yangutils.datamodel.utils.builtindatatype;

import org.onosproject.yangutils.datamodel.YangDataTypes;

/**
 * Represents the list of utility functions to be supported by YANG built in
 * data type implementations.
 *
 * @param <T> The target data type
 */
public interface YangBuiltInDataTypeInfo<T> extends Comparable<T> {

    /**
     * Returns the YANG built in type.
     *
     * @return the YANG built in type
     */
    YangDataTypes getYangType();
}

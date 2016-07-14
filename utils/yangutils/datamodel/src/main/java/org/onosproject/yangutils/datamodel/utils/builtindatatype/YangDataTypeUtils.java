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

package org.onosproject.yangutils.datamodel.utils.builtindatatype;

import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT16;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT32;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT64;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT8;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT16;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT32;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT64;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT8;

/**
 * Represents YANG data type utilities.
 */
public final class YangDataTypeUtils {

    /**
     * Restricts creation of YANG data type utils instance.
     */
    private YangDataTypeUtils() {
    }

    /**
     * Returns whether the data type is of range restricted type.
     *
     * @param dataType data type to be checked
     * @return true, if data type can have range restrictions, false otherwise
     */
    public static boolean isOfRangeRestrictedType(YangDataTypes dataType) {
        return dataType == INT8
                || dataType == INT16
                || dataType == INT32
                || dataType == INT64
                || dataType == UINT8
                || dataType == UINT16
                || dataType == UINT32
                || dataType == UINT64;
    }
}

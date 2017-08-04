/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.rfc.utils;

import org.onosproject.ovsdb.rfc.notation.Condition;
import org.onosproject.ovsdb.rfc.notation.Condition.Function;

/**
 * Condition utility class.
 */
public final class ConditionUtil {

    /**
     * Constructs a ConditionUtil object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully. This
     * class should not be instantiated.
     */
    private ConditionUtil() {
    }

    /**
     * Returns a Condition that means Function.EQUALS .
     * @param columnName column name
     * @param data column value
     * @return Condition
     */
    public static Condition isEqual(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Condition(columnName, Function.EQUALS, value);
    }

    /**
     * Returns a Condition that means Function.NOT_EQUALS .
     * @param columnName column name
     * @param data column value
     * @return Condition
     */
    public static Condition unEquals(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Condition(columnName, Function.NOT_EQUALS, value);
    }

    /**
     * Returns a Condition that means Function.GREATER_THAN .
     * @param columnName column name
     * @param data column value
     * @return Condition
     */
    public static Condition greaterThan(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Condition(columnName, Function.GREATER_THAN, value);
    }

    /**
     * Returns a Condition that means Function.GREATER_THAN_OR_EQUALS .
     * @param columnName column name
     * @param data column value
     * @return Condition
     */
    public static Condition greaterThanOrEquals(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Condition(columnName, Function.GREATER_THAN_OR_EQUALS, value);
    }

    /**
     * Returns a Condition that means Function.LESS_THAN .
     * @param columnName column name
     * @param data column value
     * @return Condition
     */
    public static Condition lesserThan(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Condition(columnName, Function.LESS_THAN, value);
    }

    /**
     * Returns a Condition that means Function.LESS_THAN_OR_EQUALS .
     * @param columnName column name
     * @param data column value
     * @return Condition
     */
    public static Condition lesserThanOrEquals(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Condition(columnName, Function.LESS_THAN_OR_EQUALS, value);
    }

    /**
     * Returns a Condition that means Function.INCLUDES .
     * @param columnName column name
     * @param data column value
     * @return Condition
     */
    public static Condition includes(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Condition(columnName, Function.INCLUDES, value);
    }

    /**
     * Returns a Condition that means Function.EXCLUDES .
     * @param columnName column name
     * @param data column value
     * @return Condition
     */
    public static Condition excludes(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Condition(columnName, Function.EXCLUDES, value);
    }

}

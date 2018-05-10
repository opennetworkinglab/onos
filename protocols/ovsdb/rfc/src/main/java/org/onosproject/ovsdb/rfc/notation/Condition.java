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
package org.onosproject.ovsdb.rfc.notation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.ovsdb.rfc.notation.json.ConditionSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Condition is a 3-element JSON array of the form [column, function, value]
 * that represents a test on a column value.
 */
@JsonSerialize(using = ConditionSerializer.class)
public final class Condition {
    /**
     * Function of Notation. Refer to RFC 7047 Section 5.1.
     */
    public enum Function {
        LESS_THAN("<"), LESS_THAN_OR_EQUALS("<="), EQUALS("=="),
        NOT_EQUALS("!="), GREATER_THAN(">"), GREATER_THAN_OR_EQUALS(">="),
        INCLUDES("includes"), EXCLUDES("excludes");

        private final String function;

        Function(String function) {
            this.function = function;
        }

        /**
         * Returns the function for Function.
         * @return the function
         */
        public String function() {
            return function;
        }
    }

    private final String column;
    private final Function function;
    private final Object value;

    /**
     * Constructs a Condition object.
     * @param column the column name
     * @param function Function
     * @param value column data
     */
    public Condition(String column, Function function, Object value) {
        checkNotNull(column, "column cannot be null");
        checkNotNull(function, "function cannot be null");
        checkNotNull(value, "value cannot be null");
        this.column = column;
        this.function = function;
        this.value = value;
    }

    /**
     * Returns column name.
     * @return column name
     */
    public String getColumn() {
        return column;
    }

    /**
     * Returns Function.
     * @return Function
     */
    public Function getFunction() {
        return function;
    }

    /**
     * Returns column data.
     * @return column data
     */
    public Object getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, function, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Condition) {
            final Condition other = (Condition) obj;
            return Objects.equals(this.column, other.column)
                    && Objects.equals(this.function, other.function)
                    && Objects.equals(this.value, other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("column", column)
                .add("function", function).add("value", value).toString();
    }
}

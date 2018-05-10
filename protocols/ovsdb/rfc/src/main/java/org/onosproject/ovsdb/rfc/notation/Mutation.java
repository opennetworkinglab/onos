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

import org.onosproject.ovsdb.rfc.notation.json.MutationSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Mutation is s 3-element JSON array of the form [column, mutator, value] that
 * represents a change to a column value.
 */
@JsonSerialize(using = MutationSerializer.class)
public final class Mutation {
    /**
     * Mutator must be "+=", "-=", "*=", "/=", or (integer only) "%=". The value
     * of column is changed to the sum, difference, product, quotient, or
     * remainder, respectively, of column and value.
     */
    public enum Mutator {
        SUM("+="), DIFFERENCE("-="), PRODUCT("*="), QUOTIENT("/="),
        REMAINDER("%="), INSERT("insert"), DELETE("delete");

        private final String mutator;

        Mutator(String mutator) {
            this.mutator = mutator;
        }

        /**
         * Returns the mutator for Mutator.
         * @return the mutator
         */
        public String mutator() {
            return mutator;
        }
    }

    private final String column;
    private final Mutator mutator;
    private final Object value;

    /**
     * Mutation constructor.
     * @param column the column name
     * @param mutator Mutator
     * @param value column data
     */
    public Mutation(String column, Mutator mutator, Object value) {
        checkNotNull(column, "column cannot be null");
        checkNotNull(mutator, "mutator cannot be null");
        checkNotNull(value, "value cannot be null");
        this.column = column;
        this.mutator = mutator;
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
     * Returns Mutator.
     * @return Mutator
     */
    public Mutator getMutator() {
        return mutator;
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
        return Objects.hash(column, mutator, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Mutation) {
            final Mutation other = (Mutation) obj;
            return Objects.equals(this.column, other.column)
                    && Objects.equals(this.mutator, other.mutator)
                    && Objects.equals(this.value, other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("column", column)
                .add("mutator", mutator).add("value", value).toString();
    }
}

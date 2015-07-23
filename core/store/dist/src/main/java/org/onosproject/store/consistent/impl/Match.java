/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Utility class for checking matching values.
 *
 * @param <T> type of value
 */
public final class Match<T> {

    private final boolean matchAny;
    private final T value;

    /**
     * Returns a Match that matches any value.
     * @param <T> match type
     * @return new instance
     */
    public static <T> Match<T> any() {
        return new Match<>();
    }

    /**
     * Returns a Match that matches null values.
     * @param <T> match type
     * @return new instance
     */
    public static <T> Match<T> ifNull() {
        return ifValue(null);
    }

    /**
     * Returns a Match that matches only specified value.
     * @param value value to match
     * @param <T> match type
     * @return new instance
     */
    public static <T> Match<T> ifValue(T value) {
        return new Match<>(value);
    }

    private Match() {
        matchAny = true;
        value = null;
    }

    private Match(T value) {
        matchAny = false;
        this.value = value;
    }

    /**
     * Maps this instance to a Match of another type.
     * @param mapper transformation function
     * @param <V> new match type
     * @return new instance
     */
    public <V> Match<V> map(Function<T, V> mapper) {
        if (matchAny) {
            return any();
        } else if (value == null) {
            return ifNull();
        } else {
            return ifValue(mapper.apply(value));
        }
    }

    /**
     * Checks if this instance matches specified value.
     * @param other other value
     * @return true if matches; false otherwise
     */
    public boolean matches(T other) {
        if (matchAny) {
            return true;
        } else if (other == null) {
            return value == null;
        } else {
            if (value instanceof byte[]) {
                return Arrays.equals((byte[]) value, (byte[]) other);
            }
            return Objects.equals(value, other);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchAny, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Match)) {
            return false;
        }
        Match<T> that = (Match<T>) other;
        return Objects.equals(this.matchAny, that.matchAny) &&
               Objects.equals(this.value, that.value);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("matchAny", matchAny)
                .add("value", value)
                .toString();
    }
}

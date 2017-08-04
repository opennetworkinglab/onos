/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onlab.util;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Filters content on a given object with String representation.
 * This is carried out through restrictive (AND) or loose (OR) searches.
 * If not provided, strategy defaults to ADD.
 */
public class StringFilter {

    /**
     * Defines the filtering strategy.
     */
    public enum Strategy {
        AND,
        OR
    }

    private Strategy strategy = Strategy.AND;
    private List<String> filter = new ArrayList<>();

    /**
     * Creates a new filter to apply on some data.
     *
     * @param filter list with filters to apply
     */
    public StringFilter(List<String> filter) {
        this.filter = filter;
    }

    /**
     * Creates a new filter to apply on some data,
     * given a specific strategy (AND, OR).
     *
     * @param filter   list with filters to apply
     * @param strategy type of strategy (AND, OR)
     */
    public StringFilter(List<String> filter, Strategy strategy) {
        this(filter);
        checkArgument(strategy == Strategy.AND || strategy == Strategy.OR,
                      "Chosen strategy is not allowed (should be one of {AND, OR})");
        this.strategy = strategy;
    }

    /**
     * Filters data according to a set of restrictions and the AND strategy.
     *
     * @param data Object with data to filter
     * @return true if data honours the filter, false otherwise
     */
    private boolean filterAnd(Object data) {
        return filter.isEmpty() ||
                filter.stream().filter(data.toString()::contains)
                        .count() == filter.size();
    }

    /**
     * Filters data according to a set of restrictions and the OR strategy.
     *
     * @param data Object with data to filter
     * @return true if data honours the filter, false otherwise
     */
    private boolean filterOr(Object data) {
        return filter.isEmpty() ||
                filter.stream().filter(data.toString()::contains)
                        .count() > 0;
    }

    /**
     * Filters data according to a set of restrictions and a specific strategy.
     *
     * @param data Object with data to filter
     * @return true if data honours the filters, false otherwise
     */
    public boolean filter(Object data) {
        if (strategy == Strategy.OR) {
            return filterOr(data);
        } else {
            return filterAnd(data);
        }
    }
}

/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.ui.table;

/**
 * Defines a comparator for cell values.
 */
public interface CellComparator {

    /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.
     * <p>
     * Note that nulls are permitted, and should be sorted to the beginning
     * of an ascending sort; i.e. null is considered to be "smaller" than
     * non-null values.
     *
     * @see java.util.Comparator#compare(Object, Object)
     *
     * @param o1 the first object to be compared
     * @param o2 the second object to be compared
     * @return an integer representing relative ordering
     * @throws ClassCastException if the arguments' types prevent them from
     *         being compared by this comparator
     */
    int compare(Object o1, Object o2);

}

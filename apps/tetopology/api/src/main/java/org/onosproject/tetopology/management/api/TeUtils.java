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
package org.onosproject.tetopology.management.api;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * TE utility functions.
 */
public final class TeUtils {

    // no instantiation
    private TeUtils() {
    }

    /**
     * Returns true if the given collection is empty; false otherwise.
     *
     * @param c the given collection
     * @return true or false
     */
    public static boolean nonEmpty(Collection<?> c) {
        return c != null && !c.isEmpty();
    }

    /**
     * Adds a given element to a given list. If element is null, the
     * given list is returned without modification. If the list is null,
     * the function will instantiate and return a new list.
     *
     * @param list    the given list
     * @param element the given list element
     * @param <T>     the element type
     * @return the resulting list
     */
    public static <T> List<T> addListElement(List<T> list, T element) {
        if (element == null) {
            return list;
        }

        List<T> result = (list == null) ? Lists.newArrayList() : list;

        result.add(element);

        return result;
    }
}

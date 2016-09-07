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
package org.onosproject.net.optical.device;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onosproject.net.Annotations;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Filtered {@link Annotations} view.
 */
@Beta
public class FilteredAnnotation implements Annotations {

    private final Annotations delegate;
    private final Set<String> filtered;

    /**
     * Creates filtered {@link Annotations} view based on {@code delegate}.
     *
     * @param delegate input {@link Annotations}
     * @param keys to filter-out
     */
    public FilteredAnnotation(Annotations delegate, Set<String> keys) {
        this.delegate = checkNotNull(delegate);
        this.filtered = ImmutableSet.copyOf(keys);
    }

    @Override
    public String value(String key) {
        if (filtered.contains(key)) {
            return null;
        }
        return delegate.value(key);
    }

    @Override
    public Set<String> keys() {
        return Sets.difference(delegate.keys(), filtered);
    }

    @Override
    public String toString() {
        Map<String, String> mapView = new HashMap<>();
        keys().forEach(key -> mapView.put(key, delegate.value(key)));
        return mapView.toString();
    }
}

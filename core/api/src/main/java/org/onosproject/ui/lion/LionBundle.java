/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

package org.onosproject.ui.lion;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encapsulates a bundle of localization strings.
 */
public final class LionBundle {

    private final String id;
    private final Set<LionItem> items;
    private final Map<String, String> mapped;

    private LionBundle(String id, Set<LionItem> items) {
        this.id = id;
        this.items = ImmutableSortedSet.copyOf(items);
        mapped = createLookup();
    }

    private Map<String, String> createLookup() {
        Map<String, String> lookup = new HashMap<>(items.size());
        for (LionItem item : items) {
            lookup.put(item.key(), item.value());
        }
        return ImmutableSortedMap.copyOf(lookup);
    }

    /**
     * Returns the bundle's identifier.
     *
     * @return the bundle's ID
     */
    public String id() {
        return id;
    }

    /**
     * Returns the number of entries in this bundle.
     *
     * @return number of entries
     */
    public int size() {
        return items.size();
    }

    @Override
    public String toString() {
        return "LionBundle{id=" + id + ", #items=" + size() + "}";
    }

    /**
     * Returns the localized value for the given key, or null if no such
     * mapping exists.
     *
     * @param key the key
     * @return the localized value
     */
    public String getValue(String key) {
        return mapped.get(key);
    }

    /**
     * Returns the localized value for the given key, or, if no such mapping
     * exists, returns the key wrapped in '%' characters.
     *
     * @param key the key
     * @return the localized value (or a wrapped key placeholder)
     */
    public String getSafe(String key) {
        String value = mapped.get(key);
        return value == null ? "%" + key + "%" : value;
    }

    /**
     * Converts the given enum constant to lowercase and then uses that as the
     * key to invoke {@link #getSafe(String)}.
     *
     * @param enumConst the constant to use as the key
     * @return the localized value (or a wrapped key placeholder)
     */
    public String getSafe(Enum<?> enumConst) {
        return getSafe(enumConst.name().toLowerCase());
    }

    /**
     * Returns an immutable set of the items in this bundle.
     *
     * @return the items in this bundle
     */
    public Set<LionItem> getItems() {
        return items;
    }

    /**
     * Dump the contents of the bundle.
     *
     * @return dumped contents
     */
    public String dump() {
        return mapped.toString();
    }

    // === --------------------------------------------------------------------

    /**
     * Builder of Lion Bundles.
     */
    public static final class Builder {
        private final String id;
        private final Set<LionItem> items = new HashSet<>();

        /**
         * Creates a builder of Lion Bundles.
         *
         * @param id the bundle's identifier
         */
        public Builder(String id) {
            this.id = id;
        }

        /**
         * Returns the bundle ID.
         *
         * @return the bundle ID
         */
        public String id() {
            return id;
        }

        /**
         * Adds an item to the bundle.
         *
         * @param key   the item key
         * @param value the item value
         * @return self, for chaining
         */
        public Builder addItem(String key, String value) {
            items.add(new LionItem(key, value));
            return this;
        }

        /**
         * Builds the lion bundle from this builder instance.
         *
         * @return the lion bundle
         */
        public LionBundle build() {
            return new LionBundle(id, items);
        }
    }

    // === --------------------------------------------------------------------

    /**
     * Represents a single localization item.
     */
    public static final class LionItem implements Comparable<LionItem> {
        private final String key;
        private final String value;

        /**
         * Creates a lion item with the given key and value.
         *
         * @param key   the key
         * @param value the value
         */
        private LionItem(String key, String value) {
            checkNotNull(key);
            checkNotNull(value);
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "LionItem{key=" + key + ", value=\"" + value + "\"}";
        }

        @Override
        public int compareTo(LionItem o) {
            return key.compareTo(o.key);
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LionBundle.LionItem that = (LionBundle.LionItem) obj;
            return Objects.equal(this.key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.key);
        }

        /**
         * Returns the key.
         *
         * @return the key
         */
        public String key() {
            return key;
        }

        /**
         * Returns the value.
         *
         * @return the value
         */
        public String value() {
            return value;
        }
    }
}

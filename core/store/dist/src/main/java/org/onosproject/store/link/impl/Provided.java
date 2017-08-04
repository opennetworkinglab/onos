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
package org.onosproject.store.link.impl;

import java.util.Objects;

import org.onosproject.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Encapsulation of a provider supplied key.
 *
 * @param <K> key
 */
public class Provided<K> {
    private final K key;
    private final ProviderId providerId;

    public Provided(K key, ProviderId providerId) {
        this.key = key;
        this.providerId = providerId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public K key() {
        return key;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, providerId);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Provided) {
            Provided<K> that = (Provided) other;
            return Objects.equals(key, that.key) &&
                    Objects.equals(providerId, that.providerId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("key", key)
                .add("providerId", providerId)
                .toString();
    }
}

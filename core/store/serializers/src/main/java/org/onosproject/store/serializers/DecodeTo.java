/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.serializers;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;

/**
 * Function to convert byte[] into {@code T}.
 *
 * @param <T> Type after decoding
 */
public final class DecodeTo<T> implements Function<byte[], T> {

    private StoreSerializer serializer;

    public DecodeTo(StoreSerializer serializer) {
        this.serializer = checkNotNull(serializer);
    }

    @Override
    public T apply(byte[] input) {
        return serializer.decode(input);
    }
}

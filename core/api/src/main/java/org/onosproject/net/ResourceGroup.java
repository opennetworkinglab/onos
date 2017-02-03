/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net;

import com.google.common.annotations.Beta;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.onlab.util.Identifier;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceConsumerId;

import java.nio.charset.StandardCharsets;

/**
 * Intent identifier suitable as an external key.
 * <p>This class is immutable.</p>
 */
@Beta
public final class ResourceGroup extends Identifier<Long> implements ResourceConsumer {

    private static final String HEX_PREFIX = "0x";
    private static final HashFunction HASH_FN = Hashing.md5();

    /**
     * Creates a resource group identifier from the specified long
     * representation.
     *
     * @param value long value
     * @return resource group identifier
     */
    public static ResourceGroup of(long value) {
        return new ResourceGroup(value);
    }

    /**
     * Creates a resource group identifier from the specified string
     * representation.
     * Warning: it is caller responsibility to make sure the hashed value of
     * {@code value} is unique.
     *
     * @param value string value
     * @return resource group identifier
     */
    public static ResourceGroup of(String value) {
        return new ResourceGroup(HASH_FN.newHasher()
                                        .putString(value, StandardCharsets.UTF_8)
                                        .hash()
                                        .asLong());
    }

    /**
     * Constructor for serializer.
     */
    protected ResourceGroup() {
        super(0L);
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param value the underlying value of this ID
     */
    protected ResourceGroup(long value) {
        super(value);
    }

    /**
     * Returns the backing value.
     *
     * @return the value
     */
    public long fingerprint() {
        return identifier;
    }

    @Override
    public String toString() {
        return HEX_PREFIX + Long.toHexString(identifier);
    }

    @Override
    public ResourceConsumerId consumerId() {
        return ResourceConsumerId.of(this);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ResourceGroup other = (ResourceGroup) obj;
        if (this.fingerprint() != other.fingerprint()) {
            return false;
        }
        return true;
    }
}

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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceConsumerId;

/**
 * Intent identifier suitable as an external key.
 * <p>This class is immutable.</p>
 */
@Beta
public final class IntentId extends Identifier<Long> implements ResourceConsumer {

    /**
     * Creates an intent identifier from the specified long representation.
     *
     * @param value long value
     * @return intent identifier
     */
    public static IntentId valueOf(long value) {
        return new IntentId(value);
    }

    /**
     * Constructor for serializer.
     */
    IntentId() {
        super(0L);
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param value the underlying value of this ID
     */
    IntentId(long value) {
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
        return "0x" + Long.toHexString(identifier);
    }

    @Override
    public ResourceConsumerId consumerId() {
        return ResourceConsumerId.of(this);
    }
}

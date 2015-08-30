/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import org.onosproject.net.newresource.ResourceConsumer;

/**
 * Intent identifier suitable as an external key.
 * <p>This class is immutable.</p>
 */
@Beta
public final class IntentId implements ResourceConsumer {

    private final long value;

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
        this.value = 0;
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param value the underlying value of this ID
     */
    IntentId(long value) {
        this.value = value;
    }

    /**
     * Returns the backing value.
     *
     * @return the value
     */
    public long fingerprint() {
        return value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof IntentId)) {
            return false;
        }
        IntentId that = (IntentId) obj;
        return this.value == that.value;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(value);
    }

}

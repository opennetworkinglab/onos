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
 */
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a Queue identity.
 */
@Beta
public final class QueueId {
    private final String name;

    private QueueId(String name) {
        this.name = checkNotNull(name);
    }

    public static QueueId queueId(String name) {
        return new QueueId(name);
    }

    public String name() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QueueId) {
            final QueueId that = (QueueId) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.name, that.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}

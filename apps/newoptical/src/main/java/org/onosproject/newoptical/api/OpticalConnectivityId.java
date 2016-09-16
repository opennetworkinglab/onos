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
package org.onosproject.newoptical.api;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onlab.util.Identifier;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceConsumerId;

// TODO: After ResourceManager is made to accept app-defined ResourceConsumer,
//       this class should be implemented as ResourceConsumer.
/**
 * ID for optical connectivity.
 */
@Beta
public final class OpticalConnectivityId extends Identifier<Long> implements ResourceConsumer {

    public static OpticalConnectivityId of(long value) {
        return new OpticalConnectivityId(value);
    }

    OpticalConnectivityId(long value) {
        super(value);
    }

    @Override
    public ResourceConsumerId consumerId() {
        return ResourceConsumerId.of(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", id())
                .toString();
    }
}

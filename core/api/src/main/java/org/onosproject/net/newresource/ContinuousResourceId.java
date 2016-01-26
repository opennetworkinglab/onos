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
package org.onosproject.net.newresource;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ResourceId for {@link ContinuousResource}
 *
 * Note: This class is exposed to the public, but intended to be used in the resource API
 * implementation only. It is not for resource API user.
 */
@Beta
// TODO: consider how to restrict the visibility
public final class ContinuousResourceId extends ResourceId {
    // for printing purpose only (used in toString() implementation)
    private final String name;

    ContinuousResourceId(ImmutableList<Object> components, String name) {
        super(components);
        this.name = checkNotNull(name);
    }

    @Override
    public String toString() {
        // due to performance consideration, the value might need to be stored in a field
        return ImmutableList.builder()
                .addAll(components.subList(0, components.size() - 1))
                .add(name)
                .build().toString();
    }
}

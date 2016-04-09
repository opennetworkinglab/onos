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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import org.onosproject.net.resource.Resource;

/**
 * Class providing resource information to constraints.
 * This class is subject to be removed during refactorings on Constraint.
 */
@Beta
public interface ResourceContext {
    /**
     * Returns the availability of the specified resource.
     *
     * @param resource resource to check the availability
     * @return true if available, otherwise false
     */
    boolean isAvailable(Resource resource);
}

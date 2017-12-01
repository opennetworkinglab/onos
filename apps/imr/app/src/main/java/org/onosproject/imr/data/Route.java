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

package org.onosproject.imr.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.intent.Key;

import java.util.List;
import java.util.Map;

/**
 * Representation of a route submitted by the off-platform application
 * to be applied to an existing intent.
 * It is composed by the key and the application id of the intent to modify
 * and a list of possible {@link Path}.
 */
public class Route {
    private Key key;
    private ApplicationId appId;
    private List<Path> paths;

    /**
     * Returns the intent key the route refers to.
     * @return the intent key
     */
    public Key key() {
        return key;
    }

    /**
     * Returns the Application ID of the intent that has to be modified.
     * @return the Application ID
     */
    public ApplicationId appId() {
        return appId;
    }

    /**
     * Returns the list of the {@link Path} on which the intent has to be routed.
     * @return the list of path
     */
    public List<Path> paths() {
        return paths;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("IntentKey", this.key)
                .add("ApplicationId", this.appId)
                .add("Paths", this.paths)
                .toString();
    }

    /**
     * Creates the route using Jackson from a JSON Object.
     * @param iKey the intent key
     * @param appId application id
     * @param paths list of paths
     */
    @JsonCreator
    public Route(@JsonProperty("key") String iKey,
                @JsonProperty("appId") Map<String, String> appId,
                @JsonProperty("paths") List<Path> paths) {
        this.paths = paths;
        this.appId = new DefaultApplicationId(Integer.valueOf(appId.get("id")), appId.get("name"));
        this.key = Key.of(iKey, this.appId);
    }
}

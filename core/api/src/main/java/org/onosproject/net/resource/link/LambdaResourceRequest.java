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
package org.onosproject.net.resource.link;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onosproject.net.resource.ResourceRequest;
import org.onosproject.net.resource.ResourceType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a request for lambda resource.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public class LambdaResourceRequest implements ResourceRequest {

    private final LambdaResource lambda;

    /**
     * Constructs a request specifying the given lambda.
     *
     * @param lambda lambda to be requested
     */
    @Beta
    public LambdaResourceRequest(LambdaResource lambda) {
        this.lambda = checkNotNull(lambda);
    }

    /**
     * Constructs a request asking an arbitrary available lambda.
     *
     * @deprecated in Emu Release
     */
    @Deprecated
    public LambdaResourceRequest() {
        this.lambda = null;
    }

    /**
     * Returns the lambda this request expects.
     *
     * @return the lambda this request expects
     */
    @Beta
    public LambdaResource lambda() {
        return lambda;
    }

    @Override
    public ResourceType type() {
        return ResourceType.LAMBDA;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("lambda", lambda)
                .toString();
    }
}

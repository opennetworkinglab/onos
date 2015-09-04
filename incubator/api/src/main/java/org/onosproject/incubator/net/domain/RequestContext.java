/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.domain;

import com.google.common.annotations.Beta;
import org.onosproject.net.Path;

/**
 * Context for intent primitive requests to an intent domain provider. A context
 * must be explicitly applied before it can be used. The purpose of the request
 * context is so that an application can coordinate multiple requests across multiple
 * domains before committing. Contexts can be explicitly cancelled if they are not
 * needed (due to a better context or incomplete path across domains); they can
 * also be automatically cancelled by a provider after a short timeout.
 */
@Beta
public class RequestContext {
    private final IntentDomain domain;
    private final IntentResource resource;
    private final Path path;
    //TODO other common parameters:
    //String cost;

    public RequestContext(IntentDomain domain, IntentResource resource, Path path) {
        this.domain = domain;
        this.resource = resource;
        this.path = path;
    }

    public IntentDomain domain() {
        return domain;
    }

    public IntentResource resource() {
        return resource;
    }

    public Path path() {
        return path;
    }
}

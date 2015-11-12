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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.app.ApplicationService;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.Application;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Application JSON codec.
 */
public final class ApplicationCodec extends JsonCodec<Application> {

    @Override
    public ObjectNode encode(Application app, CodecContext context) {
        checkNotNull(app, "Application cannot be null");
        ApplicationService service = context.getService(ApplicationService.class);
        return context.mapper().createObjectNode()
                .put("name", app.id().name())
                .put("id", app.id().id())
                .put("version", app.version().toString())
                .put("description", app.description())
                .put("origin", app.origin())
                .put("permissions", app.permissions().toString()) // FIXME: change to an array
                .put("featuresRepo", app.featuresRepo().isPresent() ?
                        app.featuresRepo().get().toString() : "")
                .put("features", app.features().toString()) // FIXME: change to an array
                .put("requiredApps", app.requiredApps().toString()) // FIXME: change to an array
                .put("state", service.getState(app.id()).toString());
    }

}

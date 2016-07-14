/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringEscapeUtils;
import org.onosproject.app.ApplicationService;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.Application;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Application JSON codec.
 */
public final class ApplicationCodec extends JsonCodec<Application> {

    @Override
    public ObjectNode encode(Application app, CodecContext context) {
        checkNotNull(app, "Application cannot be null");
        ApplicationService service = context.getService(ApplicationService.class);

        ArrayNode permissions = context.mapper().createArrayNode();
        ArrayNode features = context.mapper().createArrayNode();
        ArrayNode requiredApps = context.mapper().createArrayNode();

        app.permissions().forEach(p -> permissions.add(p.toString()));
        app.features().forEach(f -> features.add(f));
        app.requiredApps().forEach(a -> requiredApps.add(a));

        ObjectNode result = context.mapper().createObjectNode()
                .put("name", app.id().name())
                .put("id", app.id().id())
                .put("version", app.version().toString())
                .put("category", app.category())
                .put("description", StringEscapeUtils.escapeJson(app.description()))
                .put("readme", StringEscapeUtils.escapeJson(app.readme()))
                .put("origin", app.origin())
                .put("url", app.url())
                .put("featuresRepo", app.featuresRepo().map(URI::toString).orElse(""))
                .put("state", service.getState(app.id()).toString());

        result.set("features", features);
        result.set("permissions", permissions);
        result.set("requiredApps", requiredApps);

        return result;
    }
}

/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.ui.impl.gui2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.onlab.rest.BaseResource;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiView;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for serving the list of UIExtensions.
 */
@Path("nav")
public class NavResource extends BaseResource {

    @GET
    @Path("uiextensions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNavigation() throws JsonProcessingException {
        UiExtensionService service = get(UiExtensionService.class);
        UiViewSerializer serializer = new UiViewSerializer(UiView.class);
        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module =
                new SimpleModule("UiViewSerializer");
        module.addSerializer(serializer);
        mapper.registerModule(module);
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (UiExtension uiExt : service.getExtensions()) {
            for (UiView view : uiExt.views()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(mapper.writeValueAsString(view));
            }
        }
        sb.append("]");

        return ok(sb.toString()).build();
    }

}

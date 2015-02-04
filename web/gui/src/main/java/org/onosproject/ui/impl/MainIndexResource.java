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
package org.onosproject.ui.impl;

import com.google.common.collect.ImmutableList;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.io.ByteStreams.toByteArray;

/**
 * Resource for serving the dynamically composed index.html.
 */
@Path("/")
public class MainIndexResource extends AbstractInjectionResource {

    private static final String INDEX = "/index-template.html";

    private static final String INJECT_CSS = "<!-- {INJECTED-STYLESHEETS} -->";
    private static final String INJECT_JS = "<!-- {INJECTED-JAVASCRIPT} -->";

    @Path("/")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getMainIndex() throws IOException {
        UiExtensionService service = get(UiExtensionService.class);
        InputStream indexTemplate = getClass().getClassLoader().getResourceAsStream(INDEX);
        String index = new String(toByteArray(indexTemplate));

        int p1 = split(index, 0, INJECT_JS);
        int p2 = split(index, p1, INJECT_CSS);
        int p3 = split(index, p2, null);

        StreamEnumeration streams =
                new StreamEnumeration(of(stream(index, 0, p1),
                                         includeJs(service),
                                         stream(index, p1, p2),
                                         includeCss(service),
                                         stream(index, p2, p3)));

        return Response.ok(new SequenceInputStream(streams)).build();
    }

    // Produces an input stream including CSS injections from all extensions.
    private InputStream includeCss(UiExtensionService service) {
        ImmutableList.Builder<InputStream> builder = ImmutableList.builder();
        for (UiExtension extension : service.getExtensions()) {
            builder.add(extension.css());
        }
        return new SequenceInputStream(new StreamEnumeration(builder.build()));
    }

    // Produces an input stream including JS injections from all extensions.
    private InputStream includeJs(UiExtensionService service) {
        ImmutableList.Builder<InputStream> builder = ImmutableList.builder();
        for (UiExtension extension : service.getExtensions()) {
            builder.add(extension.js());
        }
        return new SequenceInputStream(new StreamEnumeration(builder.build()));
    }

}

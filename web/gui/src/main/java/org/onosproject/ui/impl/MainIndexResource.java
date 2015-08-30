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
import com.google.common.collect.ImmutableList.Builder;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.rest.AbstractInjectionResource;
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

    private static final String INDEX = "index.html";
    private static final String NOT_READY = "not-ready.html";

    private static final String INJECT_CSS_START = "<!-- {INJECTED-STYLESHEETS-START} -->";
    private static final String INJECT_CSS_END = "<!-- {INJECTED-STYLESHEETS-END} -->";

    private static final String INJECT_JS_START = "<!-- {INJECTED-JAVASCRIPT-START} -->";
    private static final String INJECT_JS_END = "<!-- {INJECTED-JAVASCRIPT-END} -->";

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getMainIndex() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        UiExtensionService service;
        try {
            service = get(UiExtensionService.class);
        } catch (ServiceNotFoundException e) {
            return Response.ok(classLoader.getResourceAsStream(NOT_READY)).build();
        }

        InputStream indexTemplate = classLoader.getResourceAsStream(INDEX);
        String index = new String(toByteArray(indexTemplate));

        int p1s = split(index, 0, INJECT_JS_START);
        int p1e = split(index, p1s, INJECT_JS_END);
        int p2s = split(index, p1e, INJECT_CSS_START);
        int p2e = split(index, p2s, INJECT_CSS_END);
        int p3s = split(index, p2e, null);

        StreamEnumeration streams =
                new StreamEnumeration(of(stream(index, 0, p1s),
                                         includeJs(service),
                                         stream(index, p1e, p2s),
                                         includeCss(service),
                                         stream(index, p2e, p3s)));

        return Response.ok(new SequenceInputStream(streams)).build();
    }

    // Produces an input stream including CSS injections from all extensions.
    private InputStream includeCss(UiExtensionService service) {
        Builder<InputStream> builder = ImmutableList.builder();
        service.getExtensions().forEach(ext -> add(builder, ext.css()));
        return new SequenceInputStream(new StreamEnumeration(builder.build()));
    }

    // Produces an input stream including JS injections from all extensions.
    private InputStream includeJs(UiExtensionService service) {
        Builder<InputStream> builder = ImmutableList.builder();
        service.getExtensions().forEach(ext -> add(builder, ext.js()));
        return new SequenceInputStream(new StreamEnumeration(builder.build()));
    }

    // Safely adds the stream to the list builder only if stream is not null.
    private void add(Builder<InputStream> builder, InputStream inputStream) {
        if (inputStream != null) {
            builder.add(inputStream);
        }
    }

}

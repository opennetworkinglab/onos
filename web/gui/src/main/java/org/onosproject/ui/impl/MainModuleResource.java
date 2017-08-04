/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.rest.AbstractInjectionResource;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiView;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.io.ByteStreams.toByteArray;
import static org.onosproject.ui.impl.MainViewResource.SCRIPT;

/**
 * Resource for serving the dynamically composed onos.js.
 */
@Path("/")
public class MainModuleResource extends AbstractInjectionResource {

    private static final String MAIN_JS = "onos.js";

    private static final String INJECT_VIEW_IDS_START = "// {INJECTED-VIEW-DATA-START}";
    private static final String INJECT_VIEW_IDS_END = "// {INJECTED-VIEW-DATA-END}";
    private static final String PREFIX = "        '";
    private static final String MIDFIX = "' : '";
    private static final String SUFFIX = String.format("',%n");

    @GET
    @Produces(SCRIPT)
    public Response getMainModule() throws IOException {
        UiExtensionService service = get(UiExtensionService.class);
        InputStream jsTemplate = getClass().getClassLoader().getResourceAsStream(MAIN_JS);
        String js = new String(toByteArray(jsTemplate));

        int p1s = split(js, 0, INJECT_VIEW_IDS_START) - INJECT_VIEW_IDS_START.length();
        int p1e = split(js, 0, INJECT_VIEW_IDS_END);
        int p2s = split(js, p1e, null);

        StreamEnumeration streams =
                new StreamEnumeration(of(stream(js, 0, p1s),
                                         includeViewIds(service),
                                         stream(js, p1e, p2s)));

        return Response.ok(new SequenceInputStream(streams)).build();
    }

    // Produces an input stream including view id injections from all extensions.
    private InputStream includeViewIds(UiExtensionService service) {
        StringBuilder sb = new StringBuilder("\n");
        for (UiExtension extension : service.getExtensions()) {
            for (UiView view : extension.views()) {
                sb.append(PREFIX)
                        .append(view.id())
                        .append(MIDFIX)
                        .append(sanitizeUrl(view.helpPageUrl()))
                        .append(SUFFIX);
            }
        }
        return new ByteArrayInputStream(sb.toString().getBytes());
    }

    private String sanitizeUrl(String url) {
        // TODO: add logic for validating URL
        return url;
    }

}

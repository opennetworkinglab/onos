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

import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.io.ByteStreams.toByteArray;

/**
 * Resource for serving the dynamically composed nav.html.
 */
@Path("/")
public class MainNavResource extends AbstractInjectionResource {

    private static final String NAV_HTML = "nav.html";

    private static final String INJECT_VIEW_ITEMS_START = "<!-- {INJECTED-VIEW-NAV-START} -->";
    private static final String INJECT_VIEW_ITEMS_END = "<!-- {INJECTED-VIEW-NAV-END} -->";

    private static final String NAV_FORMAT =
            "<a ng-click=\"navCtrl.hideNav()\" href=\"#/%s\">%s</a>\n";

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getNavigation() throws IOException {
        UiExtensionService service = get(UiExtensionService.class);
        InputStream navTemplate = getClass().getClassLoader().getResourceAsStream(NAV_HTML);
        String js = new String(toByteArray(navTemplate));

        int p1s = split(js, 0, INJECT_VIEW_ITEMS_START);
        int p1e = split(js, 0, INJECT_VIEW_ITEMS_END);
        int p2s = split(js, p1e, null);

        StreamEnumeration streams =
                new StreamEnumeration(of(stream(js, 0, p1s),
                                         includeNavItems(service),
                                         stream(js, p1e, p2s)));

        return Response.ok(new SequenceInputStream(streams)).build();
    }

    // Produces an input stream including nav item injections from all extensions.
    private InputStream includeNavItems(UiExtensionService service) {
        StringBuilder sb = new StringBuilder("\n");
        for (UiExtension extension : service.getExtensions()) {
            for (UiView view : extension.views()) {
                if (!(view instanceof UiViewHidden)) {
                    sb.append(String.format(NAV_FORMAT, view.id(), view.label()));
                }
            }
        }
        return new ByteArrayInputStream(sb.toString().getBytes());
    }
}

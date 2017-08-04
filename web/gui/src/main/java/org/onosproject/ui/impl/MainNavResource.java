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
import org.onosproject.ui.lion.LionBundle;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.io.ByteStreams.toByteArray;

/**
 * Resource for serving the dynamically composed nav.html.
 */
@Path("/")
public class MainNavResource extends AbstractInjectionResource {

    private static final String NAV_HTML = "nav.html";

    private static final String INJECT_VIEW_ITEMS_START =
            "<!-- {INJECTED-VIEW-NAV-START} -->";
    private static final String INJECT_VIEW_ITEMS_END =
            "<!-- {INJECTED-VIEW-NAV-END} -->";

    private static final String HDR_FORMAT =
            "<div class=\"nav-hdr\">%s</div>%n";
    private static final String NAV_FORMAT =
            "<a ng-click=\"navCtrl.hideNav()\" href=\"#/%s\">%s %s</a>%n";
    private static final String ICON_FORMAT =
            "<div icon icon-id=\"%s\"></div>";

    private static final String BLANK_GLYPH = "unknown";

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getNavigation() throws IOException {
        UiExtensionService service = get(UiExtensionService.class);
        InputStream navTemplate =
                getClass().getClassLoader().getResourceAsStream(NAV_HTML);
        String html = new String(toByteArray(navTemplate));

        int p1s = split(html, 0, INJECT_VIEW_ITEMS_START);
        int p1e = split(html, 0, INJECT_VIEW_ITEMS_END);
        int p2s = split(html, p1e, null);

        StreamEnumeration streams =
                new StreamEnumeration(of(stream(html, 0, p1s),
                                         includeNavItems(service),
                                         stream(html, p1e, p2s)));

        return Response.ok(new SequenceInputStream(streams)).build();
    }

    // Produces an input stream of nav item injections from all extensions.
    private InputStream includeNavItems(UiExtensionService service) {
        List<UiExtension> extensions = service.getExtensions();
        LionBundle navLion = service.getNavLionBundle();
        StringBuilder sb = new StringBuilder("\n");

        for (UiView.Category cat : UiView.Category.values()) {
            if (cat == UiView.Category.HIDDEN) {
                continue;
            }

            List<UiView> catViews = getViewsForCat(extensions, cat);
            if (!catViews.isEmpty()) {
                addCatHeader(sb, cat, navLion);
                addCatItems(sb, catViews);
            }
        }

        return new ByteArrayInputStream(sb.toString().getBytes());
    }

    private List<UiView> getViewsForCat(List<UiExtension> extensions,
                                        UiView.Category cat) {
        List<UiView> views = new ArrayList<>();
        for (UiExtension extension : extensions) {
            views.addAll(extension.views().stream().filter(
                    view -> cat.equals(view.category())
            ).collect(Collectors.toList()));
        }
        return views;
    }

    private void addCatHeader(StringBuilder sb, UiView.Category cat,
                              LionBundle navLion) {
        String key = "cat_" + cat.name().toLowerCase();
        sb.append(String.format(HDR_FORMAT, navLion.getValue(key)));
    }

    private void addCatItems(StringBuilder sb, List<UiView> catViews) {
        for (UiView view : catViews) {
            sb.append(String.format(NAV_FORMAT, view.id(), icon(view), view.label()));
        }
    }

    private String icon(UiView view) {
        String gid = view.iconId() == null ? BLANK_GLYPH : view.iconId();
        return String.format(ICON_FORMAT, gid);
    }
}

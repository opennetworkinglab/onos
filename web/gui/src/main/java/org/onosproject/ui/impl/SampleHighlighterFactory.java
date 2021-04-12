/*
 * Copyright 2021-present Open Networking Foundation
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

import org.onosproject.net.EdgeLink;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiTopoHighlighter;
import org.onosproject.ui.UiTopoHighlighterFactory;
import org.onosproject.ui.topo.BaseLink;
import org.onosproject.ui.topo.BaseLinkMap;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.LinkHighlight;
import org.onosproject.ui.topo.Mod;
import org.onosproject.ui.topo.NodeBadge;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import static org.onosproject.net.DefaultEdgeLink.createEdgeLinks;
import static org.onosproject.ui.topo.NodeBadge.text;

@Component(enabled = false)
public class SampleHighlighterFactory {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected UiExtensionService uiExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    private UiTopoHighlighterFactory foo = () ->
            new TestHighlighter("foo", new Mod("style=\"stroke: #0ff; stroke-width: 10px;\""));
    private UiTopoHighlighterFactory bar = () ->
            new TestHighlighter("bar", new Mod("style=\"stroke: #f0f; stroke-width: 4px; stroke-dasharray: 5 2;\""));

    @Activate
    protected void activate() {
        uiExtensionService.register(foo);
        uiExtensionService.register(bar);
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(foo);
        uiExtensionService.unregister(bar);
    }

    private final class TestHighlighter implements UiTopoHighlighter {

        private final String name;
        private final Mod mod;

        private TestHighlighter(String name, Mod mod) {
            this.name = name;
            this.mod = mod;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Highlights createHighlights() {
            Highlights highlights = new Highlights();
            BaseLinkMap linkMap = new BaseLinkMap();

            // Create a map of base bi-links from the set of active links first.
            for (Link link : linkService.getActiveLinks()) {
                linkMap.add(link);
            }

            for (Host host : hostService.getHosts()) {
                for (EdgeLink link : createEdgeLinks(host, false)) {
                    linkMap.add(link);
                }

                // Also add a host badge for kicks.
                HostHighlight hostHighlight = new HostHighlight(host.id().toString());
                hostHighlight.setBadge(text(NodeBadge.Status.WARN, name));
                highlights.add(hostHighlight);
            }

            // Now scan through the links and annotate them with desired highlights
            for (BaseLink link : linkMap.biLinks()) {
                highlights.add(new LinkHighlight(link.linkId(), LinkHighlight.Flavor.PRIMARY_HIGHLIGHT)
                                       .addMod(mod).setLabel(name + "-" + link.one().src().port()));
            }

            return highlights;
        }
    }

}

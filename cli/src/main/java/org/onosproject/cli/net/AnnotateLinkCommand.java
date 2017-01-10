/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.cli.net;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.ConnectPoint.deviceConnectPoint;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Link;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.ProviderId;

/**
 * Annotates network link model.
 */
@Command(scope = "onos", name = "annotate-link",
         description = "Annotates network model entities")
public class AnnotateLinkCommand extends AbstractShellCommand {

    static final ProviderId PID = new ProviderId("cli", "org.onosproject.cli", true);

    @Option(name = "--both",
            description = "Add to both direction")
    private boolean both = false;

    @Argument(index = 0, name = "srcConnectPoint", description = "source Connect Point",
            required = true, multiValued = false)
    private String srcCp = null;

    @Argument(index = 1, name = "dstConnectPoint", description = "destination Connect Point",
            required = true, multiValued = false)
    private String dstCp = null;



    @Argument(index = 2, name = "key", description = "Annotation key",
            required = true, multiValued = false)
    private String key = null;

    @Argument(index = 3, name = "value",
            description = "Annotation value (null to remove)",
            required = false, multiValued = false)
    private String value = null;


    @Override
    protected void execute() {
        LinkService service = get(LinkService.class);
        ConnectPoint src = deviceConnectPoint(srcCp);
        ConnectPoint dst = deviceConnectPoint(dstCp);

        LinkProviderRegistry registry = get(LinkProviderRegistry.class);
        CliLinkProvider provider = new CliLinkProvider();
        LinkProviderService providerService = registry.register(provider);
        try {
            providerService.linkDetected(description(service.getLink(src, dst),
                                                     key, value));
            if (both) {
                providerService.linkDetected(description(service.getLink(dst, src),
                                                         key, value));
            }
        } finally {
            registry.unregister(provider);
        }
    }


    private LinkDescription description(Link link, String key, String value) {
        checkNotNull(key, "Key cannot be null");
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        if (value != null) {
            builder.set(key, value);
        } else {
            builder.remove(key);
        }
        return new DefaultLinkDescription(link.src(),
                                          link.dst(),
                                          link.type(),
                                          link.isExpected(),
                                          builder.build());
    }

    private static final class CliLinkProvider implements LinkProvider {
        @Override
        public ProviderId id() {
            return PID;
        }
    }

}

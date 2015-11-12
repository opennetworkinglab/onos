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
package org.onosproject.cli.app;

import java.util.Collections;
import java.util.List;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.app.ApplicationService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.Comparators;
import org.onosproject.core.Application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static com.google.common.collect.Lists.newArrayList;
import static org.onosproject.app.ApplicationState.ACTIVE;

/**
 * Lists application information.
 */
@Command(scope = "onos", name = "apps",
        description = "Lists application information")
public class ApplicationsListCommand extends AbstractShellCommand {

    private static final String FMT =
            "%s id=%d, name=%s, version=%s, origin=%s, description=%s, " +
                    "features=%s, featuresRepo=%s, apps=%s, permissions=%s";

    private static final String SHORT_FMT =
            "%s %3d %-32s %-8s %s";

    @Option(name = "-s", aliases = "--short", description = "Show short output only",
            required = false, multiValued = false)
    private boolean shortOnly = false;

    @Option(name = "-a", aliases = "--active", description = "Show active only",
            required = false, multiValued = false)
    private boolean activeOnly = false;


    @Override
    protected void execute() {
        ApplicationService service = get(ApplicationService.class);
        List<Application> apps = newArrayList(service.getApplications());
        Collections.sort(apps, Comparators.APP_COMPARATOR);

        if (outputJson()) {
            print("%s", json(service, apps));
        } else {
            for (Application app : apps) {
                boolean isActive = service.getState(app.id()) == ACTIVE;
                if (activeOnly && isActive || !activeOnly) {
                    if (shortOnly) {
                        print(SHORT_FMT, isActive ? "*" : " ",
                              app.id().id(), app.id().name(), app.version(),
                              app.description());
                    } else {
                        print(FMT, isActive ? "*" : " ",
                              app.id().id(), app.id().name(), app.version(), app.origin(),
                              app.description(), app.features(),
                              app.featuresRepo().isPresent() ? app.featuresRepo().get().toString() : "",
                              app.requiredApps(), app.permissions());
                    }
                }
            }
        }
    }

    private JsonNode json(ApplicationService service, List<Application> apps) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Application app : apps) {
            boolean isActive = service.getState(app.id()) == ACTIVE;
            if (activeOnly && isActive || !activeOnly) {
                result.add(jsonForEntity(app, Application.class));
            }
        }
        return result;
    }



}

/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.NetworkResource;
import org.onlab.onos.net.intent.ConnectivityIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.LinkCollectionIntent;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.net.intent.SinglePointToMultiPointIntent;

import java.util.List;
import java.util.Set;

/**
 * Lists the inventory of intents and their states.
 */
@Command(scope = "onos", name = "intents",
         description = "Lists the inventory of intents and their states")
public class IntentsListCommand extends AbstractShellCommand {

    @Option(name = "-i", aliases = "--installable", description = "Output Installable Intents",
            required = false, multiValued = false)
    private boolean showInstallable = false;


    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);
        if (outputJson()) {
            print("%s", json(service, service.getIntents()));
        } else {
            for (Intent intent : service.getIntents()) {
                IntentState state = service.getIntentState(intent.id());
                print("id=%s, state=%s, type=%s, appId=%s",
                      intent.id(), state, intent.getClass().getSimpleName(),
                      intent.appId().name());
                printDetails(service, intent);
            }
        }
    }

    private void printDetails(IntentService service, Intent intent) {
        if (intent.resources() != null && !intent.resources().isEmpty()) {
            print("    resources=%s", intent.resources());
        }
        if (intent instanceof ConnectivityIntent) {
            ConnectivityIntent ci = (ConnectivityIntent) intent;
            if (!ci.selector().criteria().isEmpty()) {
                print("    selector=%s", ci.selector().criteria());
            }
            if (!ci.treatment().instructions().isEmpty()) {
                print("    treatment=%s", ci.treatment().instructions());
            }
            if (ci.constraints() != null && !ci.constraints().isEmpty()) {
                print("    constraints=%s", ci.constraints());
            }
        }

        if (intent instanceof PointToPointIntent) {
            PointToPointIntent pi = (PointToPointIntent) intent;
            print("    ingress=%s, egress=%s", pi.ingressPoint(), pi.egressPoint());
        } else if (intent instanceof MultiPointToSinglePointIntent) {
            MultiPointToSinglePointIntent pi = (MultiPointToSinglePointIntent) intent;
            print("    ingress=%s, egress=%s", pi.ingressPoints(), pi.egressPoint());
        } else if (intent instanceof SinglePointToMultiPointIntent) {
            SinglePointToMultiPointIntent pi = (SinglePointToMultiPointIntent) intent;
            print("    ingress=%s, egress=%s", pi.ingressPoint(), pi.egressPoints());
        } else if (intent instanceof PathIntent) {
            PathIntent pi = (PathIntent) intent;
            print("    path=%s, cost=%d", pi.path().links(), pi.path().cost());
        } else if (intent instanceof LinkCollectionIntent) {
            LinkCollectionIntent li = (LinkCollectionIntent) intent;
            print("    links=%s", li.links());
            print("    egress=%s", li.egressPoint());
        }

        List<Intent> installable = service.getInstallableIntents(intent.id());
        if (showInstallable && installable != null && !installable.isEmpty()) {
            print("    installable=%s", installable);
        }
    }

    // Produces JSON array of the specified intents.
    private JsonNode json(IntentService service, Iterable<Intent> intents) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Intent intent : intents) {
            result.add(json(service, mapper, intent));
        }
        return result;
    }

    private JsonNode json(IntentService service, ObjectMapper mapper, Intent intent) {
        ObjectNode result = mapper.createObjectNode()
                .put("id", intent.id().toString())
                .put("type", intent.getClass().getSimpleName())
                .put("appId", intent.appId().name());

        IntentState state = service.getIntentState(intent.id());
        if (state != null) {
            result.put("state", state.toString());
        }

        if (intent.resources() != null && !intent.resources().isEmpty()) {
            ArrayNode rnode = mapper.createArrayNode();
            for (NetworkResource resource : intent.resources()) {
                rnode.add(resource.toString());
            }
            result.set("resources", rnode);
        }

        if (intent instanceof ConnectivityIntent) {
            ConnectivityIntent ci = (ConnectivityIntent) intent;
            if (!ci.selector().criteria().isEmpty()) {
                result.put("selector", ci.selector().criteria().toString());
            }
            if (!ci.treatment().instructions().isEmpty()) {
                result.put("treatment", ci.treatment().instructions().toString());
            }
        }

        if (intent instanceof PathIntent) {
            PathIntent pi = (PathIntent) intent;
            ArrayNode pnode = mapper.createArrayNode();
            for (Link link : pi.path().links()) {
                pnode.add(link.toString());
            }
            result.set("path", pnode);

        } else if (intent instanceof PointToPointIntent) {
            PointToPointIntent pi = (PointToPointIntent) intent;
            result.set("ingress", LinksListCommand.json(mapper, pi.ingressPoint()));
            result.set("egress", LinksListCommand.json(mapper, pi.egressPoint()));

        } else if (intent instanceof MultiPointToSinglePointIntent) {
            MultiPointToSinglePointIntent pi = (MultiPointToSinglePointIntent) intent;
            result.set("ingress", json(mapper, pi.ingressPoints()));
            result.set("egress", LinksListCommand.json(mapper, pi.egressPoint()));

        } else if (intent instanceof SinglePointToMultiPointIntent) {
            SinglePointToMultiPointIntent pi = (SinglePointToMultiPointIntent) intent;
            result.set("ingress", LinksListCommand.json(mapper, pi.ingressPoint()));
            result.set("egress", json(mapper, pi.egressPoints()));

        } else if (intent instanceof LinkCollectionIntent) {
            LinkCollectionIntent li = (LinkCollectionIntent) intent;
            result.set("links", LinksListCommand.json(li.links()));
        }

        List<Intent> installable = service.getInstallableIntents(intent.id());
        if (installable != null && !installable.isEmpty()) {
            result.set("installable", json(service, installable));
        }
        return result;
    }

    private JsonNode json(ObjectMapper mapper, Set<ConnectPoint> connectPoints) {
        ArrayNode result = mapper.createArrayNode();
        for (ConnectPoint cp : connectPoints) {
            result.add(LinksListCommand.json(mapper, cp));
        }
        return result;
    }

}

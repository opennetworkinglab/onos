package org.onlab.onos.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
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

import java.util.Set;

/**
 * Lists the inventory of intents and their states.
 */
@Command(scope = "onos", name = "intents",
         description = "Lists the inventory of intents and their states")
public class IntentsListCommand extends AbstractShellCommand {

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
                printDetails(intent);
            }
        }
    }

    private void printDetails(Intent intent) {
        if (intent.resources() != null && !intent.resources().isEmpty()) {
            print("    resources=%s", intent.resources());
        }
        if (intent instanceof ConnectivityIntent) {
            ConnectivityIntent ci = (ConnectivityIntent) intent;
            print("    selector=%s", ci.selector().criteria());
            print("    treatment=%s", ci.treatment().instructions());
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
                .put("state", service.getIntentState(intent.id()).toString())
                .put("type", intent.getClass().getSimpleName())
                .put("appId", intent.appId().name());

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

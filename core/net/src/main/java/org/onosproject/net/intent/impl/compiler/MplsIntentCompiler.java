package org.onosproject.net.intent.impl.compiler;

import static java.util.Arrays.asList;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.MplsIntent;
import org.onosproject.net.intent.MplsPathIntent;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.link.LinkResourceAllocations;


@Component(immediate = true)
public class MplsIntentCompiler  extends ConnectivityIntentCompiler<MplsIntent> {

    // TODO: use off-the-shell core provider ID
    private static final ProviderId PID =
            new ProviderId("core", "org.onosproject.core", true);
    // TODO: consider whether the default cost is appropriate or not
    public static final int DEFAULT_COST = 1;


    @Activate
    public void activate() {
        intentManager.registerCompiler(MplsIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(MplsIntent.class);
    }

    @Override
    public List<Intent> compile(MplsIntent intent, List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        ConnectPoint ingressPoint = intent.ingressPoint();
        ConnectPoint egressPoint = intent.egressPoint();

        if (ingressPoint.deviceId().equals(egressPoint.deviceId())) {
            List<Link> links = asList(createEdgeLink(ingressPoint, true), createEdgeLink(egressPoint, false));
            return asList(createPathIntent(new DefaultPath(PID, links, DEFAULT_COST), intent));
        }

        List<Link> links = new ArrayList<>();
        Path path = getPath(intent, ingressPoint.deviceId(),
                egressPoint.deviceId());

        links.add(createEdgeLink(ingressPoint, true));
        links.addAll(path.links());

        links.add(createEdgeLink(egressPoint, false));

        return asList(createPathIntent(new DefaultPath(PID, links, path.cost(),
                                                       path.annotations()), intent));
    }

    /**
     * Creates a path intent from the specified path and original
     * connectivity intent.
     *
     * @param path   path to create an intent for
     * @param intent original intent
     */
    private Intent createPathIntent(Path path,
                                    MplsIntent intent) {
        return MplsPathIntent.builder()
                .appId(intent.appId())
                .selector(intent.selector())
                .treatment(intent.treatment())
                .path(path)
                .ingressLabel(intent.ingressLabel())
                .egressLabel(intent.egressLabel())
                .constraints(intent.constraints())
                .priority(intent.priority())
                .build();
    }


}

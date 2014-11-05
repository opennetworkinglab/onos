package org.onlab.onos.net.intent.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.LinkCollectionIntent;
import org.onlab.onos.net.intent.SinglePointToMultiPointIntent;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.resource.LinkResourceAllocations;

@Component(immediate = true)
public class SinglePointToMultiPointIntentCompiler
        extends ConnectivityIntentCompiler<SinglePointToMultiPointIntent> {

    // TODO: use off-the-shell core provider ID
    private static final ProviderId PID =
            new ProviderId("core", "org.onlab.onos.core", true);

    @Activate
    public void activate() {
        intentManager.registerCompiler(SinglePointToMultiPointIntent.class,
                                       this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(SinglePointToMultiPointIntent.class);
    }


    @Override
    public List<Intent> compile(SinglePointToMultiPointIntent intent,
                                List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        Set<Link> links = new HashSet<>();
        //FIXME: need to handle the case where ingress/egress points are on same switch
        for (ConnectPoint egressPoint : intent.egressPoints()) {
            Path path = getPath(intent, intent.ingressPoint().deviceId(), egressPoint.deviceId());
            links.addAll(path.links());
        }

        Intent result = new LinkCollectionIntent(intent.appId(),
                                                 intent.selector(),
                                                 intent.treatment(), links,
                                                 intent.egressPoints(), null);

        return Arrays.asList(result);
    }
}

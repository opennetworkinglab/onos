package org.onlab.onos.net.resource;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;
import org.slf4j.Logger;

/**
 * Provides basic implementation of link resources allocation.
 */
@Component(immediate = true)
@Service
public class LinkResourceManager implements LinkResourceService {

    private final Logger log = getLogger(getClass());

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public LinkResourceAllocations requestResources(LinkResourceRequest req) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void releaseResources(LinkResourceAllocations allocations) {
        // TODO Auto-generated method stub

    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations(Link link) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<IntentId> getIntents(Link link) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceRequest getAvailableResources(Link link) {
        // TODO Auto-generated method stub
        return null;
    }

}

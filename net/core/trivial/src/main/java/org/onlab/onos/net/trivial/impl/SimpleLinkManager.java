package org.onlab.onos.net.trivial.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.AbstractListenerManager;
import org.onlab.onos.event.EventDispatchService;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderBroker;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.provider.AbstractProviderBroker;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides basic implementation of the link SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class SimpleLinkManager
        extends AbstractProviderBroker<LinkProvider, LinkProviderService>
        implements LinkProviderBroker {

    private final Logger log = getLogger(getClass());

    private final AbstractListenerManager<LinkEvent, LinkListener>
            listenerManager = new AbstractListenerManager<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private EventDispatchService eventDispatcher;

    @Activate
    public void activate() {
        eventDispatcher.addSink(LinkEvent.class, listenerManager);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(LinkEvent.class);
        log.info("Stopped");
    }

    @Override
    protected LinkProviderService createProviderService(LinkProvider provider) {
        return new InternalLinkProviderService(provider);
    }

    // Personalized link provider service issued to the supplied provider.
    private class InternalLinkProviderService extends AbstractProviderService<LinkProvider>
            implements LinkProviderService {

        public InternalLinkProviderService(LinkProvider provider) {
            super(provider);
        }

        @Override
        public void linkDetected(LinkDescription linkDescription) {
            log.info("Link {} detected", linkDescription);
        }

        @Override
        public void linkVanished(LinkDescription linkDescription) {
            log.info("Link {} vanished", linkDescription);
        }
    }
}

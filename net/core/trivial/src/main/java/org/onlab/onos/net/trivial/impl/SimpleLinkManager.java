package org.onlab.onos.net.trivial.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderBroker;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.provider.AbstractProviderBroker;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides basic implementation of the link SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class SimpleLinkManager implements LinkProviderBroker {

    private Logger log = LoggerFactory.getLogger(SimpleLinkManager.class);

    private final LinkProviderBroker broker = new InternalBroker();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public LinkProviderService register(LinkProvider provider) {
        return broker.register(provider);
    }

    @Override
    public void unregister(LinkProvider provider) {
        broker.unregister(provider);
    }

    // Internal delegate for tracking various providers and issuing them a
    // personalized provider service.
    private class InternalBroker extends AbstractProviderBroker<LinkProvider, LinkProviderService>
            implements LinkProviderBroker {
        @Override
        protected LinkProviderService createProviderService(LinkProvider provider) {
            return new InternalLinkProviderService(provider);
        }
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

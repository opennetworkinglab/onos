package org.onlab.onos.net.trivial.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.HostProviderBroker;
import org.onlab.onos.net.host.HostProviderService;
import org.onlab.onos.net.provider.AbstractProviderBroker;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides basic implementation of the host SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class SimpleHostManager implements HostProviderBroker {

    private Logger log = LoggerFactory.getLogger(SimpleHostManager.class);

    private final HostProviderBroker broker = new InternalBroker();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public HostProviderService register(HostProvider provider) {
        log.info("Registering provider {}", provider.id());
        return broker.register(provider);
    }

    @Override
    public void unregister(HostProvider provider) {
        log.info("Unregistering provider {}", provider.id());
        broker.unregister(provider);
    }

    // Internal delegate for tracking various providers and issuing them a
    // personalized provider service.
    private class InternalBroker extends AbstractProviderBroker<HostProvider, HostProviderService>
            implements HostProviderBroker {
        @Override
        protected HostProviderService createProviderService(HostProvider provider) {
            return new InternalHostProviderService(provider);
        }
    }

    // Personalized host provider service issued to the supplied provider.
    private class InternalHostProviderService extends AbstractProviderService<HostProvider>
            implements HostProviderService {

        public InternalHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostDescription hostDescription) {
            log.info("Host {} detected", hostDescription);
        }

        @Override
        public void hostVanished(HostDescription hostDescription) {
            log.info("Host {} vanished", hostDescription);
        }
    }
}

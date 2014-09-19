package org.onlab.onos.store.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component responsible for initializing a shared Hazelcast instance.
 */
@Component(immediate = true)
public class HazelcastBootstrap {

    private final Logger log = getLogger(getClass());

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

}

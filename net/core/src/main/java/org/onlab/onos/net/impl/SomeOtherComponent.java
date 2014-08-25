package org.onlab.onos.net.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.GreetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example of a component that does not provide any service, but consumes one.
 */
@Component(immediate = true)
public class SomeOtherComponent {

    private final Logger log = LoggerFactory.getLogger(SomeOtherComponent.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GreetService service;
    // protected to allow injection for testing;
    // alternative is to write bindSeedService and unbindSeedService, which is more code

    @Activate
    public void activate() {
        log.info("SomeOtherComponent started");
        service.yo("neighbour");
    }

    @Deactivate
    public void deactivate() {
        log.info("SomeOtherComponent stopped");
    }

}

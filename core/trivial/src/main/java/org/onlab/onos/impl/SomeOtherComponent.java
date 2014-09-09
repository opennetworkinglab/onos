package org.onlab.onos.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.GreetService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Example of a component that does not provide any service, but consumes one.
 */
@Component(immediate = true)
public class SomeOtherComponent {

    private final Logger log = getLogger(getClass());

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

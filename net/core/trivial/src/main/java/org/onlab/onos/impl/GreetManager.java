package org.onlab.onos.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.GreetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Trivial implementation of the seed service to demonstrate component and
 * service annotations.
 */
@Component(immediate = true)
@Service
public class GreetManager implements GreetService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Set<String> names = new HashSet<>();

    @Override
    public synchronized String yo(String name) {
        checkNotNull(name, "Name cannot be null");
        names.add(name);
        log.info("Greeted '{}'", name);
        return "Whazup " + name + "?";
    }

    @Override
    public synchronized Iterable<String> names() {
        return ImmutableSet.copyOf(names);
    }

    @Activate
    public void activate() {
        log.info("SeedManager started");
    }

    @Deactivate
    public void deactivate() {
        log.info("SeedManager stopped");
    }

}

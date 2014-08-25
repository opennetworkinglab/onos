package org.projectfloodlight.protocol.match;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFVersion;

public class MatchFieldIteration13Test extends MatchFieldIterationBase {
    public MatchFieldIteration13Test() {
        super(OFFactories.getFactory(OFVersion.OF_13));
    }
}

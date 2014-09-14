package org.onlab.onos.net.trivial.packet.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.Instructions;
import org.onlab.onos.net.packet.PacketContext;
import org.onlab.onos.net.packet.PacketProcessor;
import org.slf4j.Logger;

public class ReactivePacketProcessor implements PacketProcessor {

    private final Logger log = getLogger(getClass());

    @Override
    public void process(PacketContext context) {
        context.treatmentBuilder().add(Instructions.createOutput(PortNumber.FLOOD));
        context.send();
    }

}

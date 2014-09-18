package org.onlab.onos.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.TimeUnit;

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.onos.openflow.controller.OpenFlowSwitch;
import org.onlab.util.Timer;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;

public class FlowStatsCollector implements TimerTask {

    private final Logger log = getLogger(getClass());

    private final HashedWheelTimer timer = Timer.getTimer();
    private final OpenFlowSwitch sw;
    private final int refreshInterval;

    private Timeout timeout;

    private boolean stopTimer = false;;

    public FlowStatsCollector(OpenFlowSwitch sw, int refreshInterval) {
        this.sw = sw;
        this.refreshInterval = refreshInterval;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        log.debug("Collecting stats for {}", this.sw.getStringId());

        sendFlowStatistics();

        if (!this.stopTimer) {
            log.debug("Scheduling stats collection in {} seconds for {}",
                    this.refreshInterval, this.sw.getStringId());
            timeout.getTimer().newTimeout(this, refreshInterval,
                    TimeUnit.SECONDS);
        }


    }

    private void sendFlowStatistics() {
        OFFlowStatsRequest request = sw.factory().buildFlowStatsRequest()
                .setMatch(sw.factory().matchWildcardAll())
                .setTableId(TableId.ALL)
                .setOutPort(OFPort.NO_MASK)
                .build();

        this.sw.sendMsg(request);

    }

    public void start() {

        /*
         * Initially start polling quickly. Then drop down to configured value
         */
        log.info("Starting Stats collection thread for {}",
                this.sw.getStringId());
        timeout = timer.newTimeout(this, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        log.info("Stopping Stats collection thread for {}",
                this.sw.getStringId());
        this.stopTimer = true;
        timeout.cancel();
    }

}

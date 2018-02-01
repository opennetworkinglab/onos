/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.bgp.controller.impl;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement sending keepalive message to connected peer periodically based on negotiated holdtime.
 */
public class BgpKeepAliveTimer {

    private Timer keepAliveTimer;
    private BgpChannelHandler handler;
    private static final Logger log = LoggerFactory.getLogger(BgpKeepAliveTimer.class);

    /**
     * Gets keepalive timer object.
     *
     * @return keepAliveTimer keepalive timer.
     */
    public Timer getKeepAliveTimer() {
        return keepAliveTimer;
    }

    /**
     * Initialize timer to send keepalive message periodically.
     *
     * @param h channel handler
     * @param seconds time interval.
     */
    public BgpKeepAliveTimer(BgpChannelHandler h, int seconds) {
        this.handler = h;
        this.keepAliveTimer = new Timer();
        this.keepAliveTimer.schedule(new SendKeepAlive(), 0, seconds * 1000L);
    }

    /**
     * Send keepalive message to connected peer on schedule.
     */
    class SendKeepAlive extends TimerTask {
        @Override
        public void run() {
            log.debug("Sending periodic KeepAlive");

            try {
                // Send keep alive message
                handler.sendKeepAliveMessage();
                handler.getBgpPacketStats().addOutPacket();
            } catch (Exception e) {
                log.info("Exception occurred while sending keepAlive message" + e.toString());
            }
        }
    }
}

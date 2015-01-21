/*
 * Copyright 2014 Open Networking Laboratory
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

package org.onosproject.igp.controller.driver;

import java.util.List;

import org.jboss.netty.channel.Channel;
import org.onosproject.igp.controller.IgpDpid;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract representation of an OpenFlow switch. Can be extended by others
 * to serve as a base for their vendor specific representation of a switch.
 */
public abstract class AbstractIgpSwitch implements IgpSwitchDriver {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected Channel channel;

    private boolean connected;
    private final IgpDpid dpid;
    private IgpAgent agent;


    /**
     * Given a dpid build this switch.
     * @param dp the dpid
     */
    protected AbstractIgpSwitch(IgpDpid dp) {
        this.dpid = dp;
    }

    //************************
    // Channel related
    //************************

    @Override
    public final void disconnectSwitch() {
        this.channel.close();
    }

    @Override
    public final void sendMsg(FlowRuleBatchExtRequest m) {
        this.write(m);
    }

    @Override
    public final void sendMsg(List<FlowRuleBatchExtRequest> msgs) {
        this.write(msgs);
    }

    @Override
    public abstract void write(FlowRuleBatchExtRequest msg);

    @Override
    public abstract void write(List<FlowRuleBatchExtRequest> msgs);

    @Override
    public final boolean isConnected() {
        return this.connected;
    }

    @Override
    public final void setConnected(boolean connected) {
        this.connected = connected;
    };

    @Override
    public final void setChannel(Channel channel) {
        this.channel = channel;
    };

    @Override
    public final boolean connectSwitch() {
        return this.agent.addConnectedSwitch(dpid, this);
    }

    @Override
    public final void removeConnectedSwitch() {
        this.agent.removeConnectedSwitch(dpid);
    }

    @Override
    public final void setAgent(IgpAgent ag) {
        if (this.agent == null) {
            this.agent = ag;
        }
    }

}

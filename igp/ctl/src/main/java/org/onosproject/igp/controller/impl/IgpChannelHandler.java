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

//CHECKSTYLE:OFF
package org.onosproject.igp.controller.impl;

import java.io.IOException;
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.onosproject.igp.controller.IgpDpid;
import org.onosproject.igp.controller.driver.IgpSwitchDriver;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel handler deals with the switch connection and dispatches switch
 * messages to the appropriate locations.
 */
class IgpChannelHandler extends IdleStateAwareChannelHandler {
	private static final Logger log = LoggerFactory
			.getLogger(IgpChannelHandler.class);
	private final Controller controller;
	private IgpSwitchDriver sw;
	private long thisdpid; // channelHandler cached value of connected switch id
	private Channel channel;

	/**
	 * Create a new unconnected OFChannelHandler.
	 * 
	 * @param controller
	 *            parent controller
	 */
	IgpChannelHandler(Controller controller) {
		this.controller = controller;
	}

	// XXX S consider if necessary
	public void disconnectSwitch() {
		sw.disconnectSwitch();
	}

	// *************************
	// Channel handler methods
	// *************************

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		channel = e.getChannel();
		log.info("New switch connection from {}", channel.getRemoteAddress());
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		log.info("Switch disconnected callback for sw:{}. Cleaning up ...");
		if (thisdpid != 0) {
			// if the disconnected switch (on this ChannelHandler)
			// was not one with a duplicate-dpid, it is safe to remove all
			// state for it at the controller. Notice that if the disconnected
			// switch was a duplicate-dpid, calling the method below would clear
			// all state for the original switch (with the same dpid),
			// which we obviously don't want.
			log.info("{}:removal called");
			if (sw != null) {
				sw.removeConnectedSwitch();
			}
		} else {
			log.warn("no dpid in channelHandler registered for "
					+ "disconnected switch {}");
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if (e.getMessage() instanceof List) {
			@SuppressWarnings("unchecked")
			List<FlowRuleBatchExtRequest> msglist = (List<FlowRuleBatchExtRequest>) e
					.getMessage();

			for (FlowRuleBatchExtRequest ofm : msglist) {
				// Do the actual packet processing
				processIgpMessage(this, ofm);
			}
		} else {
			processIgpMessage(this, (FlowRuleBatchExtRequest) e.getMessage());
		}
	}

	void processIgpMessage(IgpChannelHandler h, FlowRuleBatchExtRequest m)
			throws IOException {
		IgpDpid dpid = new IgpDpid(h.thisdpid);
		h.sw = h.controller.getOFSwitchInstance(dpid);
		h.sw.setConnected(true);
		h.sw.setChannel(h.channel);
		h.sw.connectSwitch();
		log.info("process message");
	}
}

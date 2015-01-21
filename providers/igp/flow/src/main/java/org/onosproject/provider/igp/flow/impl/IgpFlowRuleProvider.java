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
package org.onosproject.provider.igp.flow.impl;

import java.util.Collection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.igp.controller.IGPController;
import org.onosproject.igp.controller.IgpDpid;
import org.onosproject.igp.controller.IgpSwitch;
import org.onosproject.igp.controller.IgpSwitchListener;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.onosproject.net.flowext.FlowRuleExtEntry;
import org.onosproject.net.flowext.FlowRuleExtProvider;
import org.onosproject.net.flowext.FlowRuleExtProviderRegistry;
import org.onosproject.net.flowext.FlowRuleExtProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network end-station
 * hosts.
 */
@Component(immediate = true)
public class IgpFlowRuleProvider extends AbstractProvider implements
		FlowRuleExtProvider {

	enum BatchState {
		STARTED, FINISHED, CANCELLED
	};

	private final Logger log = getLogger(getClass());

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected FlowRuleExtProviderRegistry providerRegistry;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected IGPController controller;

	//private FlowRuleExtendProviderService providerService;

	private final InternalFlowProvider listener = new InternalFlowProvider();

	/**
	 * Creates an sdn provider.
	 */
	public IgpFlowRuleProvider() {
		super(new ProviderId("igp", "org.onosproject.provider.igp"));
	}

	@Activate
	public void activate() {
		//providerService = providerRegistry.register(this);
		controller.addListener(listener);
		log.info("Started");
	}

	@Deactivate
	public void deactivate() {
		providerRegistry.unregister(this);
		//providerService = null;
		log.info("Stopped");
	}

	private class InternalFlowProvider implements IgpSwitchListener {

		@Override
		public void switchAdded(IgpDpid dpid) {
			log.info("switch added");
		}

		@Override
		public void switchRemoved(IgpDpid dpid) {
			// TODO Auto-generated method stub
			log.info("switch deleted");
		}
	}

	@Override
	public void applyFlowRule(FlowRuleBatchExtRequest flowRules) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		IgpDpid dpid = null;
		Collection<FlowRuleExtEntry> toAdd = flowRules.getBatch();
		for (FlowRuleExtEntry flowRule : toAdd) {
			dpid =  IgpDpid.dpid((flowRule.getDeviceId().uri()));
		}
		if (dpid == null) {
			return;
		}
		IgpSwitch sw = controller
				.getSwitch(dpid);
		sw.sendMsg(flowRules);
	}

}

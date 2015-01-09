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
package org.onosproject.cli;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.match.MatchField;

import com.google.common.collect.Maps;
/**
 * Lists all flowrules.
 */
@Command(scope = "onos", name = "ofmessages",
         description = "Lists all FlowRules")
public class FlowRuleListCommand extends AbstractShellCommand {

    private static final String FMT_OMSG =
            "   deviceid:%s,  version:%s,  type:%s,  match:%s,  instruction:%s";
    private static final String FMT_GMSG =
            "   deviceid:%s,  version:%s,  type:%s,  command:%s";

    @Override
    protected void execute() {
        FlowRuleService  flowRuleService = get(FlowRuleService.class);
        DeviceService deviceService = get(DeviceService.class); 
        Iterator<Device> devices = deviceService.getDevices();
        for (Device d: devices) {
        	Iterator<OFMessage> ofs = flowRuleService.getOFMessages(d.id);
        	for (OFMessage msg : ofs) {
        		if (msg instanceof OFFlowMod) {
	        		OFFlowMod ofMod = (OFFlowMod) msg;
	        		String matches = "";
	        		for (MatchField<?> field : ofMod.getMatch().getMatchFields()) {
	        			matches = matches + field.getName();
	        		}
	        		print(FMT_OMSG , d.id().toString(), ofMod.getVersion(), ofMod.getType() , matches, ofMod.getInstructions().toString());
        		} else if (msg instanceof OFGroupMod) {
        			OFGroupMod groupMod = (OFGroupMod)msg;
        			print(FMT_GMSG , d.id().toString(), groupMod.getVersion(), groupMod.getType() , groupMod.getCommand());
        		}
        		
        	}
        }
    }
    
    protected Map<Device,List<FlowEntry>> getSortedFlows(DeviceService deviceService, FlowRuleService service){
    	Map<Device,List<FlowEntry>> flows = Maps.newHashMap();
    	List<FlowEntry> rules;
    	FlowEntryState s = null;
    	Iterator<Device> devices = deviceService.getDevices();
    	for (Device d : devices) {
    		if (s == null) {
    			rules = newArrayList(service.getFlowEntries(d.id()));
    		}else{
    			rules = newArrayList();
    			for (FlowEntry f : service.getFlowEntries(d.id())){
    				if (f.state().equals(s)) {
    					rules.add(f);
    				}
    			}
    		}
    		Collections.sort(rules, Comparators.FLOW_RULE_COMPARATOR);
    		flows.put(d, rules);
    	}
    	return flows;
    	
    }
}

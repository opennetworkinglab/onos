package org.projectfloodlight.openflow.util;

import java.util.List;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFInstructionType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;

import com.google.common.collect.ImmutableList;

public class ActionUtils {
    private ActionUtils() {}

    public static List<OFAction> getActions(OFFlowStatsEntry e) {
        if(e.getVersion() == OFVersion.OF_10) {
            return e.getActions();
        } else {
            for(OFInstruction i: e.getInstructions()) {
                if(i.getType() == OFInstructionType.APPLY_ACTIONS) {
                    return ((OFInstructionApplyActions) i).getActions();
                }
            }
            return ImmutableList.of();
        }
    }

    public static List<OFAction> getActions(OFFlowMod e) {
        if(e.getVersion() == OFVersion.OF_10) {
            return e.getActions();
        } else {
            for(OFInstruction i: e.getInstructions()) {
                if(i.getType() == OFInstructionType.APPLY_ACTIONS) {
                    return ((OFInstructionApplyActions) i).getActions();
                }
            }
            return ImmutableList.of();
        }
    }
}

package org.onosproject.pcelabelstore.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.onosproject.incubator.net.tunnel.DefaultLabelStack;
import org.onosproject.incubator.net.tunnel.LabelStack;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.net.Path;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepClientListener;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcep.controller.PcepNodeListener;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.types.PcepValueType;

public class MockPcepClientController implements PcepClientController {

    Map<PccId, PcepClient> clientMap = new HashMap<>();

    @Override
    public Collection<PcepClient> getClients() {
        // TODO Auto-generated method stub
        return null;
    }

    public void addClient(PccId pccId, PcepClient pc) {
        clientMap.put(pccId, pc);
        return;
    }

    @Override
    public PcepClient getClient(PccId pccId) {
        return clientMap.get(pccId);
    }

    @Override
    public void addListener(PcepClientListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(PcepClientListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addEventListener(PcepEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeEventListener(PcepEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addNodeListener(PcepNodeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeNodeListener(PcepNodeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeMessage(PccId pccId, PcepMessage msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processClientMessage(PccId pccId, PcepMessage msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void closeConnectedClients() {
        // TODO Auto-generated method stub

    }

    @Override
    public LabelStack computeLabelStack(Path path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LinkedList<PcepValueType> createPcepLabelStack(DefaultLabelStack labelStack, Path path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean allocateLocalLabel(Tunnel tunnel) {
        // TODO Auto-generated method stub
        return false;
    }

}

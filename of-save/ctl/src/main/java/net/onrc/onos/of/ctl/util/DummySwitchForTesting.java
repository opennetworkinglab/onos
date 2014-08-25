package net.onrc.onos.of.ctl.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.netty.channel.Channel;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.onrc.onos.of.ctl.IOFSwitch;
import net.onrc.onos.of.ctl.Role;
import net.onrc.onos.of.ctl.debugcounter.IDebugCounterService;
import net.onrc.onos.of.ctl.debugcounter.IDebugCounterService.CounterException;

public class DummySwitchForTesting implements IOFSwitch {

    protected static final Logger log = LoggerFactory.getLogger(DummySwitchForTesting.class);

    private Channel channel;
    private boolean connected = false;
    private OFVersion ofv = OFVersion.OF_10;

    private Collection<OFPortDesc> ports;

    private DatapathId datapathId;

    private Set<OFCapabilities> capabilities;

    private int buffers;

    private byte tables;

    private String stringId;

    private Role role;

    @Override
    public void disconnectSwitch() {
        this.channel.close();
    }

    @Override
    public void write(OFMessage m) throws IOException {
        this.channel.write(m);

    }

    @Override
    public void write(List<OFMessage> msglist) throws IOException {
        for (OFMessage m : msglist) {
            this.channel.write(m);
        }

    }

    @Override
    public Date getConnectedSince() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNextTransactionId() {
        return 0;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public void setConnected(boolean connected) {
        this.connected  = connected;

    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;

    }

    @Override
    public long getId() {
        if (this.stringId == null) {
            throw new RuntimeException("Features reply has not yet been set");
        }
        return this.datapathId.getLong();
    }

    @Override
    public String getStringId() {
        // TODO Auto-generated method stub
        return "DummySwitch";
    }

    @Override
    public int getNumBuffers() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Set<OFCapabilities> getCapabilities() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte getNumTables() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public OFDescStatsReply getSwitchDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void cancelFeaturesReply(int transactionId) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<OFActionType> getActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setOFVersion(OFVersion version) {
        // TODO Auto-generated method stub

    }

    @Override
    public OFVersion getOFVersion() {
        return this.ofv;
    }

    @Override
    public Collection<OFPortDesc> getEnabledPorts() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Integer> getEnabledPortNumbers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OFPortDesc getPort(int portNumber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OFPortDesc getPort(String portName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OrderedCollection<PortChangeEvent> processOFPortStatus(
            OFPortStatus ps) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<OFPortDesc> getPorts() {
        return ports;
    }

    @Override
    public boolean portEnabled(int portName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public OrderedCollection<PortChangeEvent> setPorts(
            Collection<OFPortDesc> p) {
        this.ports = p;
        return null;
    }

    @Override
    public Map<Object, Object> getAttributes() {
        return null;
    }

    @Override
    public boolean hasAttribute(String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Object getAttribute(String name) {
        return Boolean.FALSE;
    }

    @Override
    public void setAttribute(String name, Object value) {
        // TODO Auto-generated method stub

    }

    @Override
    public Object removeAttribute(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deliverStatisticsReply(OFMessage reply) {
        // TODO Auto-generated method stub

    }

    @Override
    public void cancelStatisticsReply(int transactionId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void cancelAllStatisticsReplies() {
        // TODO Auto-generated method stub

    }

    @Override
    public Future<List<OFStatsReply>> getStatistics(OFStatsRequest<?> request)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void clearAllFlowMods() {
        // TODO Auto-generated method stub

    }

    @Override
    public Role getRole() {
        return this.role;
    }

    @Override
    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public U64 getNextGenerationId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDebugCounterService(IDebugCounterService debugCounter)
            throws CounterException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startDriverHandshake() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDriverHandshakeComplete() {
        return true;
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {

    }

    @Override
    public void setTableFull(boolean isFull) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFeaturesReply(OFFeaturesReply featuresReply) {
        if (featuresReply == null) {
            log.error("Error setting featuresReply for switch: {}", getStringId());
            return;
        }
        this.datapathId = featuresReply.getDatapathId();
        this.capabilities = featuresReply.getCapabilities();
        this.buffers = (int) featuresReply.getNBuffers();
        this.tables = (byte) featuresReply.getNTables();
        this.stringId = this.datapathId.toString();

    }

    @Override
    public void setPortDescReply(OFPortDescStatsReply portDescReply) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleMessage(OFMessage m) {
        log.info("Got packet {} but I am dumb so I don't know what to do.", m);
    }

    @Override
    public boolean portEnabled(String portName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public OrderedCollection<PortChangeEvent> comparePorts(
            Collection<OFPortDesc> p) {
        // TODO Auto-generated method stub
        return null;
    }

}

package org.onlab.onos.sdnip;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultHost;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.sdnip.Router.InternalHostListener;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.Sets;

/**
 * Test version of the HostService which is used to simulate delays in
 * receiving ARP replies, as you would see in a real system due to the time
 * it takes to proxy ARP packets to/from the host. Requests are asynchronous,
 * and replies may come back to the requestor in a different order than the
 * requests were sent, which again you would expect to see in a real system.
 */
public class TestHostService implements HostService {

    /**
     * The maximum possible delay before an ARP reply is received.
     */
    private static final int MAX_ARP_REPLY_DELAY = 30; // milliseconds

    /**
     * The probability that we already have the MAC address cached when the
     * caller calls {@link #getHostsByIp(IpAddress ipAddress)}.
     */
    private static final float MAC_ALREADY_KNOWN_PROBABILITY = 0.3f;

    private final ScheduledExecutorService replyTaskExecutor;
    private final Random random;

    /**
     * Class constructor.
     */
    public TestHostService() {
        replyTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        random = new Random();
    }

    /**
     * Task used to reply to ARP requests from a different thread. Replies
     * usually come on a different thread in the real system, so we need to
     * ensure we test this behavior.
     */
    private class ReplyTask implements Runnable {
        private HostListener listener;
        private IpAddress ipAddress;

        /**
         * Class constructor.
         *
         * @param listener the client who requests and waits the MAC address
         * @param ipAddress the target IP address of the request
         */
        public ReplyTask(InternalHostListener listener,
                IpAddress ipAddress) {
            this.listener = listener;
            this.ipAddress = ipAddress;
        }

        @Override
        public void run() {
            Host host = getHostsByIp(ipAddress).iterator().next();
            HostEvent hostevent =
                    new HostEvent(HostEvent.Type.HOST_ADDED, host);
            listener.event(hostevent);
        }
    }

    @Override
    public Set<Host> getHostsByIp(IpAddress ipAddress) {
        float replyChance = random.nextFloat();

        // We don't care what the attachment point is in the test,
        // so for all the hosts, we use a same ConnectPoint.
        Host host = new DefaultHost(ProviderId.NONE, HostId.NONE,
                SdnIpTest.generateMacAddress(ipAddress), VlanId.NONE,
                new HostLocation(SdnIpTest.SW1_ETH1, 1),
                Sets.newHashSet(ipAddress));

        if (replyChance < MAC_ALREADY_KNOWN_PROBABILITY) {
            // Some percentage of the time we already know the MAC address, so
            // we reply directly when the requestor asks for the MAC address
            return Sets.newHashSet(host);
        }
        return new HashSet<Host>();
    }

    @Override
    public void startMonitoringIp(IpAddress ipAddress) {

        // Randomly select an amount of time to delay the reply coming back to
        int delay = random.nextInt(MAX_ARP_REPLY_DELAY);
        ReplyTask replyTask = new ReplyTask(
                (SdnIpTest.router.new InternalHostListener()), ipAddress);
        replyTaskExecutor.schedule(replyTask, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public int getHostCount() {
        return 0;
    }

    @Override
    public Iterable<Host> getHosts() {
        return null;
    }

    @Override
    public Host getHost(HostId hostId) {
        return null;
    }

    @Override
    public Set<Host> getHostsByVlan(VlanId vlanId) {
        return null;
    }

    @Override
    public Set<Host> getHostsByMac(MacAddress mac) {
        return null;
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        return null;
    }

    @Override
    public void stopMonitoringIp(IpAddress ip) {

    }

    @Override
    public void requestMac(IpAddress ip) {

    }

    @Override
    public Set<PortAddresses> getAddressBindings() {
        return null;
    }

    @Override
    public Set<PortAddresses> getAddressBindingsForPort(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public void addListener(HostListener listener) {

    }

    @Override
    public void removeListener(HostListener listener) {

    }

}

package org.onlab.onos.net.flow.criteria;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.criteria.Criterion.Type;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

/**
 * Factory class to create various traffic selection criteria.
 */
public final class Criteria {

    //TODO: incomplete type implementation. Need to implement complete list from Criterion

    // Ban construction
    private Criteria() {
    }

    /**
     * Creates a match on IN_PORT field using the specified value.
     *
     * @param port inport value
     * @return match criterion
     */
    public static Criterion matchInPort(PortNumber port) {
        return new PortCriterion(port);
    }

    /**
     * Creates a match on ETH_SRC field using the specified value. This value
     * may be a wildcard mask.
     *
     * @param macValue MAC address value or wildcard mask
     * @return match criterion
     */
    public static Criterion matchEthSrc(MacAddress mac) {
        return new EthCriterion(mac, Type.ETH_SRC);
    }

    /**
     * Creates a match on ETH_DST field using the specified value. This value
     * may be a wildcard mask.
     *
     * @param macValue MAC address value or wildcard mask
     * @return match criterion
     */
    public static Criterion matchEthDst(MacAddress mac) {
        return new EthCriterion(mac, Type.ETH_DST);
    }

    /**
     * Creates a match on ETH_TYPE field using the specified value.
     *
     * @param ethType eth type value
     * @return match criterion
     */
    public static Criterion matchEthType(Short ethType) {
        return new EthTypeCriterion(ethType);
    }

    /**
     * Creates a match on VLAN ID field using the specified value.
     *
     * @param vlanId vlan id value
     * @return match criterion
     */
    public static Criterion matchVlanId(VlanId vlanId) {
        return new VlanIdCriterion(vlanId);
    }

    /**
     * Creates a match on VLAN PCP field using the specified value.
     *
     * @param vlanPcp vlan pcp value
     * @return match criterion
     */
    public static Criterion matchVlanId(Byte vlanPcp) {
        return new VlanPcpCriterion(vlanPcp);
    }

    /**
     * Creates a match on IP proto field using the specified value.
     *
     * @param proto ip protocol value
     * @return match criterion
     */
    public static Criterion matchIPProtocol(Byte proto) {
        return new IPProtocolCriterion(proto);
    }

    /**
     * Creates a match on IP src field using the specified value.
     *
     * @param ip ip src value
     * @return match criterion
     */
    public static Criterion matchIPSrc(IpAddress ip) {
        return new IPCriterion(ip, Type.IPV4_SRC);
    }

    /**
     * Creates a match on IP dst field using the specified value.
     *
     * @param ip ip src value
     * @return match criterion
     */
    public static Criterion matchIPDst(IpAddress ip) {
        return new IPCriterion(ip, Type.IPV4_DST);
    }


    /*
     * Implementations of criteria.
     */

    public static final class PortCriterion implements Criterion {
        private final PortNumber port;

        public PortCriterion(PortNumber port) {
            this.port = port;
        }

        @Override
        public Type type() {
            return Type.IN_PORT;
        }

        public PortNumber port() {
            return this.port;
        }
    }


    public static final class EthCriterion implements Criterion {
        private final MacAddress mac;
        private final Type type;

        public EthCriterion(MacAddress mac, Type type) {
            this.mac = mac;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        public MacAddress mac() {
            return this.mac;
        }
    }

    public static final class EthTypeCriterion implements Criterion {

        private final Short ethType;

        public EthTypeCriterion(Short ethType) {
            this.ethType = ethType;
        }

        @Override
        public Type type() {
            return Type.ETH_TYPE;
        }

        public Short ethType() {
            return ethType;
        }

    }


    public static final class IPCriterion implements Criterion {

        private final IpAddress ip;
        private final Type type;

        public IPCriterion(IpAddress ip, Type type) {
            this.ip = ip;
            this.type = type;
        }

        @Override
        public Type type() {
            return this.type;
        }

        public IpAddress ip() {
            return this.ip;
        }


    }


    public static final class IPProtocolCriterion implements Criterion {

        private final Byte proto;

        public IPProtocolCriterion(Byte protocol) {
            this.proto = protocol;
        }

        @Override
        public Type type() {
            return Type.IP_PROTO;
        }

        public Byte protocol() {
            return proto;
        }

    }


    public static final class VlanPcpCriterion implements Criterion {

        private final Byte vlanPcp;

        public VlanPcpCriterion(Byte vlanPcp) {
            this.vlanPcp = vlanPcp;
        }

        @Override
        public Type type() {
            return Type.VLAN_PCP;
        }

        public Byte protocol() {
            return vlanPcp;
        }

    }


    public static final class VlanIdCriterion implements Criterion {


        private final VlanId vlanId;

        public VlanIdCriterion(VlanId vlanId) {
            this.vlanId = vlanId;
        }

        @Override
        public Type type() {
            return Type.VLAN_VID;
        }

        public VlanId vlanId() {
            return vlanId;
        }

    }


}

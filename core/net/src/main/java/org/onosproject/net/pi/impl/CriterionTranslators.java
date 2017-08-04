/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.pi.impl;

import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.flow.criteria.ArpHaCriterion;
import org.onosproject.net.flow.criteria.ArpOpCriterion;
import org.onosproject.net.flow.criteria.ArpPaCriterion;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.IPv6ExthdrFlagsCriterion;
import org.onosproject.net.flow.criteria.IPv6FlowLabelCriterion;
import org.onosproject.net.flow.criteria.IPv6NDLinkLayerAddressCriterion;
import org.onosproject.net.flow.criteria.IPv6NDTargetAddressCriterion;
import org.onosproject.net.flow.criteria.IcmpCodeCriterion;
import org.onosproject.net.flow.criteria.IcmpTypeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.MetadataCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.MplsTcCriterion;
import org.onosproject.net.flow.criteria.PbbIsidCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.SctpPortCriterion;
import org.onosproject.net.flow.criteria.TcpFlagsCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;

import static org.onlab.util.ImmutableByteSequence.ByteSequenceTrimException;
import static org.onlab.util.ImmutableByteSequence.copyFrom;

/**
 * Factory class of criterion translator implementations.
 */
final class CriterionTranslators {

    /**
     * Translator of PortCriterion.
     */
    static final class PortCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            PortCriterion c = (PortCriterion) criterion;
            initAsExactMatch(copyFrom(c.port().toLong()), bitWidth);
        }
    }

    /**
     * Translator of EthTypeCriterion.
     */
    static final class EthTypeCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            EthTypeCriterion c = (EthTypeCriterion) criterion;
            initAsExactMatch(copyFrom(c.ethType().toShort()), bitWidth);
        }
    }

    /**
     * Translator of EthCriterion.
     */
    static final class EthCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            EthCriterion c = (EthCriterion) criterion;
            ImmutableByteSequence value = copyFrom(c.mac().toBytes());
            if (c.mask() == null) {
                initAsExactMatch(value, bitWidth);
            } else {
                ImmutableByteSequence mask = copyFrom(c.mask().toBytes());
                initAsTernaryMatch(value, mask, bitWidth);
            }
        }
    }

    /**
     * Translator of IpCriterion.
     */
    static final class IpCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IPCriterion c = (IPCriterion) criterion;
            initAsLpm(copyFrom(c.ip().address().toOctets()), c.ip().prefixLength(), bitWidth);
        }
    }

    /**
     * Translator of VlanIdCriterion.
     */
    static final class VlanIdCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            VlanIdCriterion c = (VlanIdCriterion) criterion;
            initAsExactMatch(copyFrom(c.vlanId().toShort()), bitWidth);
        }
    }

    /**
     * Translator of UdpPortCriterion.
     */
    static final class UdpPortCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            UdpPortCriterion c = (UdpPortCriterion) criterion;
            ImmutableByteSequence value = copyFrom(c.udpPort().toInt());
            if (c.mask() == null) {
                initAsExactMatch(value, bitWidth);
            } else {
                ImmutableByteSequence mask = copyFrom(c.mask().toInt());
                initAsTernaryMatch(value, mask, bitWidth);
            }
        }
    }

    /**
     * Translator of IPDscpCriterion.
     */
    static final class IPDscpCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IPDscpCriterion c = (IPDscpCriterion) criterion;
            ImmutableByteSequence value = copyFrom(c.ipDscp());
            initAsExactMatch(value, bitWidth);
        }
    }

    /**
     * Translator of IPProtocolCriterion.
     */
    static final class IPProtocolCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IPProtocolCriterion c = (IPProtocolCriterion) criterion;
            initAsExactMatch(copyFrom(c.protocol()), bitWidth);
        }
    }

    /**
     * Translator of IPv6ExthdrFlagsCriterion.
     */
    static final class IPv6ExthdrFlagsCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IPv6ExthdrFlagsCriterion c = (IPv6ExthdrFlagsCriterion) criterion;
            initAsExactMatch(copyFrom(c.exthdrFlags()), bitWidth);
        }
    }

    /**
     * Translator of IPv6FlowLabelCriterion.
     */
    static final class IPv6FlowLabelCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IPv6FlowLabelCriterion c = (IPv6FlowLabelCriterion) criterion;
            initAsExactMatch(copyFrom(c.flowLabel()), bitWidth);
        }
    }

    /**
     * Translator of IPv6NDLinkLayerAddressCriterion.
     */
    static final class IPv6NDLinkLayerAddressCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IPv6NDLinkLayerAddressCriterion c = (IPv6NDLinkLayerAddressCriterion) criterion;
            initAsExactMatch(copyFrom(c.mac().toLong()), bitWidth);
        }
    }

    /**
     * Translator of IPv6NDTargetAddressCriterion.
     */
    static final class IPv6NDTargetAddressCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IPv6NDTargetAddressCriterion c = (IPv6NDTargetAddressCriterion) criterion;
            initAsExactMatch(copyFrom(c.targetAddress().getIp6Address().toOctets()), bitWidth);
        }
    }

    /**
     * Translator of IcmpCodeCriterion.
     */
    static final class IcmpCodeCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IcmpCodeCriterion c = (IcmpCodeCriterion) criterion;
            initAsExactMatch(copyFrom(c.icmpCode()), bitWidth);
        }
    }

    /**
     * Translator of IcmpTypeCriterion.
     */
    static final class IcmpTypeCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IcmpTypeCriterion c = (IcmpTypeCriterion) criterion;
            initAsExactMatch(copyFrom(c.icmpType()), bitWidth);
        }
    }

    /**
     * Translator of Icmpv6CodeCriterion.
     */
    static final class Icmpv6CodeCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            Icmpv6CodeCriterion c = (Icmpv6CodeCriterion) criterion;
            initAsExactMatch(copyFrom(c.icmpv6Code()), bitWidth);
        }
    }

    /**
     * Translator of Icmpv6TypeCriterion.
     */
    static final class Icmpv6TypeCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            Icmpv6TypeCriterion c = (Icmpv6TypeCriterion) criterion;
            initAsExactMatch(copyFrom(c.icmpv6Type()), bitWidth);
        }
    }

    /**
     * Translator of MetadataCriterion.
     */
    static final class MetadataCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            MetadataCriterion c = (MetadataCriterion) criterion;
            initAsExactMatch(copyFrom(c.metadata()), bitWidth);
        }
    }

    /**
     * Translator of MplsBosCriterion.
     */
    static final class MplsBosCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            MplsBosCriterion c = (MplsBosCriterion) criterion;
            initAsExactMatch(copyFrom(c.mplsBos() ? 1 : 0), bitWidth);
        }
    }

    /**
     * Translator of MplsCriterion.
     */
    static final class MplsCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            MplsCriterion c = (MplsCriterion) criterion;
            initAsExactMatch(copyFrom(c.label().toInt()), bitWidth);
        }
    }

    /**
     * Translator of MplsTcCriterion.
     */
    static final class MplsTcCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            MplsTcCriterion c = (MplsTcCriterion) criterion;
            initAsExactMatch(copyFrom(c.tc()), bitWidth);
        }
    }

    /**
     * Translator of PbbIsidCriterion.
     */
    static final class PbbIsidCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            PbbIsidCriterion c = (PbbIsidCriterion) criterion;
            initAsExactMatch(copyFrom(c.pbbIsid()), bitWidth);
        }
    }

    /**
     * Translator of SctpPortCriterion.
     */
    static final class SctpPortCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            SctpPortCriterion c = (SctpPortCriterion) criterion;
            ImmutableByteSequence value = copyFrom(c.sctpPort().toInt());
            if (c.mask() == null) {
                initAsExactMatch(value, bitWidth);
            } else {
                ImmutableByteSequence mask = copyFrom(c.mask().toInt());
                initAsTernaryMatch(value, mask, bitWidth);
            }
        }
    }

    /**
     * Translator of TcpFlagsCriterion.
     */
    static final class TcpFlagsCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            TcpFlagsCriterion c = (TcpFlagsCriterion) criterion;
            initAsExactMatch(copyFrom(c.flags()), bitWidth);
        }
    }

    /**
     * Translator of TcpPortCriterion.
     */
    static final class TcpPortCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            TcpPortCriterion c = (TcpPortCriterion) criterion;
            ImmutableByteSequence value = copyFrom(c.tcpPort().toInt());
            if (c.mask() == null) {
                initAsExactMatch(value, bitWidth);
            } else {
                ImmutableByteSequence mask = copyFrom(c.mask().toInt());
                initAsTernaryMatch(value, mask, bitWidth);
            }
        }
    }

    /**
     * Translator of TunnelIdCriterion.
     */
    static final class TunnelIdCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            TunnelIdCriterion c = (TunnelIdCriterion) criterion;
            initAsExactMatch(copyFrom(c.tunnelId()), bitWidth);
        }
    }

    /**
     * Translator of  VlanPcpCriterion.
     */
    static final class VlanPcpCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            VlanPcpCriterion c = (VlanPcpCriterion) criterion;
            initAsExactMatch(copyFrom(c.priority()), bitWidth);
        }
    }

    /**
     * Translator of ArpHaCriterion.
     */
    static final class ArpHaCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            ArpHaCriterion c = (ArpHaCriterion) criterion;
            initAsExactMatch(copyFrom(c.mac().toLong()), bitWidth);
        }
    }

    /**
     * Translator of  ArpOpCriterion.
     */
    static final class ArpOpCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            ArpOpCriterion c = (ArpOpCriterion) criterion;
            initAsExactMatch(copyFrom(c.arpOp()), bitWidth);
        }
    }

    /**
     * Translator of  ArpPaCriterion.
     */
    static final class ArpPaCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            ArpPaCriterion c = (ArpPaCriterion) criterion;
            initAsExactMatch(copyFrom(c.ip().toInt()), bitWidth);
        }
    }

    /**
     * Translator of IPEcnCriterion.
     */
    static final class IPEcnCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IPEcnCriterion c = (IPEcnCriterion) criterion;
            initAsExactMatch(copyFrom(c.ipEcn()), bitWidth);
        }
    }

}

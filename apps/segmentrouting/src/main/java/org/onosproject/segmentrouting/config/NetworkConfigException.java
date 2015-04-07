package org.onosproject.segmentrouting.config;

import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NetworkConfigExceptions specifies a set of unchecked runtime exceptions that
 * can be thrown by the {@link NetworkConfigManager}. It indicates errors that
 * must be fixed in the config file before controller execution can proceed.
 */
public class NetworkConfigException extends RuntimeException {

    private static final long serialVersionUID = 4959684709803000652L;
    protected static final Logger log = LoggerFactory
            .getLogger(NetworkConfigException.class);

    /**
     * Exception for duplicate device identifier configuration.
     */
    public static class DuplicateDpid extends RuntimeException {
        private static final long serialVersionUID = 5491113234592145335L;

        public DuplicateDpid(DeviceId dpid) {
            super();
            log.error("Duplicate dpid found in switch-config Dpid:{}",
                    dpid);
        }
    }

    /**
     * Exception for duplicate device name configuration.
     */
    public static class DuplicateName extends RuntimeException {
        private static final long serialVersionUID = -4090171438031376129L;

        public DuplicateName(String name) {
            super();
            log.error("Duplicate name found in switch-config name:{}", name);
        }
    }

    /**
     * Exception for unspecified device identifier for a switch.
     */
    public static class DpidNotSpecified extends RuntimeException {
        private static final long serialVersionUID = -8494418855597117254L;

        public DpidNotSpecified(String name) {
            super();
            log.error("Dpid not specified for switch-config name:{}", name);
        }
    }

    /**
     * Exception for unspecified device name for a switch.
     */
    public static class NameNotSpecified extends RuntimeException {
        private static final long serialVersionUID = -3518881744110422891L;

        public NameNotSpecified(DeviceId dpid) {
            super();
            log.error("Name not specified for switch-config dpid:{}",
                    dpid);
        }
    }

    /**
     * Exception for unspecified device type for a switch.
     */
    public static class SwitchTypeNotSpecified extends RuntimeException {
        private static final long serialVersionUID = 2527453336226053753L;

        public SwitchTypeNotSpecified(DeviceId dpid) {
            super();
            log.error("Switch type not specified for switch-config dpid:{}",
                    dpid);
        }
    }

    /**
     * Exception for unknown device type configured for a switch.
     */
    public static class UnknownSwitchType extends RuntimeException {
        private static final long serialVersionUID = 7758418165512249170L;

        public UnknownSwitchType(String type, String name) {
            super();
            log.error("Unknown switch type {} for switch name:{}", type, name);
        }
    }

    /**
     * Exception for missing required parameter configuration for a switch.
     */
    public static class ParamsNotSpecified extends RuntimeException {
        private static final long serialVersionUID = 6247582323691265513L;

        public ParamsNotSpecified(String name) {
            super();
            log.error("Params required - not specified for switch:{}", name);
        }
    }

    /**
     * Reserved for future use.
     */
    public static class LinkTypeNotSpecified extends RuntimeException {
        private static final long serialVersionUID = -2089470389588542215L;

        public LinkTypeNotSpecified(String dpid1, String dpid2) {
            super();
            log.error("Link type not specified for link-config between "
                    + "dpid1:{} and dpid2:{}", dpid1, dpid2);
        }
    }

    /**
     * Reserved for future use.
     */
    public static class LinkDpidNotSpecified extends RuntimeException {
        private static final long serialVersionUID = -5701825916378616004L;

        public LinkDpidNotSpecified(String dpid1, String dpid2) {
            super();
            if (dpid1 == null) {
                log.error("nodeDpid1 not specified for link-config ");
            }
            if (dpid2 == null) {
                log.error("nodeDpid2 not specified for link-config ");
            }
        }
    }

    /**
     * Reserved for future use.
     */
    public static class LinkForUnknownSwitchConfig extends RuntimeException {
        private static final long serialVersionUID = -2910458439881964094L;

        public LinkForUnknownSwitchConfig(String dpid) {
            super();
            log.error("Link configuration was specified for a switch-dpid {} "
                    + "that has not been configured", dpid);
        }
    }

    /**
     * Reserved for future use.
     */
    public static class UnknownLinkType extends RuntimeException {
        private static final long serialVersionUID = -5505376193106542305L;

        public UnknownLinkType(String linktype, String dpid1, String dpid2) {
            super();
            log.error("unknown link type {} for links between dpid1:{} "
                    + "and dpid2:{}", linktype, dpid1, dpid2);
        }
    }

    /**
     * Exception for generic configuration errors.
     */
    public static class ErrorConfig extends RuntimeException {
        private static final long serialVersionUID = -2827406314700193147L;

        public ErrorConfig(String errorMsg) {
            super();
            log.error(errorMsg);
        }

    }

    /**
     * Reserved for future use.
     */
    public static class SwitchDpidNotConverted extends RuntimeException {
        private static final long serialVersionUID = 5640347104590170426L;

        public SwitchDpidNotConverted(String name) {
            super();
            log.error("Switch dpid specified as a HexString {} does not match "
                    + "with long value", name);
        }
    }

    /**
     * Reserved for future use.
     */
    public static class LinkDpidNotConverted extends RuntimeException {
        private static final long serialVersionUID = 2397245646094080774L;

        public LinkDpidNotConverted(String dpid1, String dpid2) {
            log.error("Dpids expressed as HexStrings for links between dpid1:{} "
                    + "and dpid2:{} do not match with long values", dpid1, dpid2);
        }
    }

}


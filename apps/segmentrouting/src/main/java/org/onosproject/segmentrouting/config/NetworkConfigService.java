package org.onosproject.segmentrouting.config;

import java.util.List;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.segmentrouting.config.NetworkConfig.LinkConfig;
import org.onosproject.segmentrouting.config.NetworkConfig.SwitchConfig;

/**
 * Exposes methods to retrieve network configuration.
 *
 * TODO: currently only startup-configuration is exposed and such configuration
 * cannot be changed at runtime. Need to add runtime support for changes to
 * configuration (via REST/CLI) in future releases.
 *
 * TODO: return immutable objects or defensive copies of network config so that
 * users of this API do not inadvertently or maliciously change network config.
 */
public interface NetworkConfigService {

    /**
     * Suggests the action to be taken by the caller given the configuration
     * associated with the queried network-object (eg. switch, link etc.).
     */
    enum NetworkConfigState {
        /**
         * Associated network object has been configured to not be allowed in
         * the network.
         */
        DENY,

        /**
         * Associated network object has been configured to be allowed in the
         * network.
         */
        ACCEPT,

        /**
         * Associated network object has been configured to be allowed in the
         * network. In addition, there are configured parameters that should be
         * added to the object.
         */
        ACCEPT_ADD,
    }

    /**
     * Returns the configuration outcome (accept, deny etc.), and any configured
     * parameters to the caller, in response to a query for the configuration
     * associated with a switch.
     */
    class SwitchConfigStatus {
        private NetworkConfigState configState;
        private SwitchConfig switchConfig;
        private String msg;

        SwitchConfigStatus(NetworkConfigState configState,
                SwitchConfig switchConfig, String msg) {
            this.configState = configState;
            this.switchConfig = switchConfig;
            this.msg = msg;
        }

        SwitchConfigStatus(NetworkConfigState configState,
                SwitchConfig switchConfig) {
            this.configState = configState;
            this.switchConfig = switchConfig;
            this.msg = "";
        }

        /**
         * Returns the configuration state for the switch.
         *
         * @return non-null NetworkConfigState
         */
        public NetworkConfigState getConfigState() {
            return configState;
        }

        /**
         * Returns the switch configuration, which may be null if no
         * configuration exists, or if the configuration state disallows the
         * switch.
         *
         * @return SwitchConfig, the switch configuration, or null
         */
        public SwitchConfig getSwitchConfig() {
            return switchConfig;
        }

        /**
         * User readable string typically used to specify the reason why a
         * switch is being disallowed.
         *
         * @return A non-null but possibly empty String
         */
        public String getMsg() {
            return msg;
        }

    }

    /**
     * Reserved for future use.
     *
     * Returns the configuration outcome (accept, deny etc.), and any configured
     * parameters to the caller, in response to a query for the configuration
     * associated with a link.
     */
    class LinkConfigStatus {
        private NetworkConfigState configState;
        private LinkConfig linkConfig;
        private String msg;

        LinkConfigStatus(NetworkConfigState configState,
                LinkConfig linkConfig, String msg) {
            this.configState = configState;
            this.linkConfig = linkConfig;
            this.msg = msg;
        }

        LinkConfigStatus(NetworkConfigState configState,
                LinkConfig linkConfig) {
            this.configState = configState;
            this.linkConfig = linkConfig;
            this.msg = "";
        }

        /**
         * Returns the configuration state for the link.
         *
         * @return non-null NetworkConfigState
         */
        public NetworkConfigState getConfigState() {
            return configState;
        }

        /**
         * Returns the link configuration, which may be null if no configuration
         * exists, or if the configuration state disallows the link.
         *
         * @return SwitchConfig, the switch configuration, or null
         */
        public LinkConfig getLinkConfig() {
            return linkConfig;
        }

        /**
         * User readable string typically used to specify the reason why a link
         * is being disallowed.
         *
         * @return msg A non-null but possibly empty String
         */
        public String getMsg() {
            return msg;
        }

    }

    /**
     * Checks the switch configuration (if any) associated with the 'dpid'.
     * Determines if the switch should be allowed or denied according to
     * configuration rules.
     *
     * The method always returns a non-null SwitchConfigStatus. The enclosed
     * ConfigState contains the result of the check. The enclosed SwitchConfig
     * may or may not be null, depending on the outcome of the check.
     *
     * @param dpid device id of the switch to be queried
     * @return SwitchConfigStatus with outcome of check and associated config.
     */
    SwitchConfigStatus checkSwitchConfig(DeviceId dpid);

    /**
     * Reserved for future use.
     *
     * Checks the link configuration (if any) associated with the 'link'.
     * Determines if the link should be allowed or denied according to
     * configuration rules. Note that the 'link' is a unidirectional link which
     * checked against configuration that is typically defined for a
     * bidirectional link. The caller may make a second call if it wishes to
     * check the 'reverse' direction.
     *
     * Also note that the configuration may not specify ports for a given
     * bidirectional link. In such cases, the configuration applies to all links
     * between the two switches. This method will check the given 'link' against
     * such configuration.

     * The method always returns a non-null LinkConfigStatus. The enclosed
     * ConfigState contains the result of the check. The enclosed LinkConfig may
     * or may not be null, depending on the outcome of the check.
     *
     * @param linkTuple unidirectional link to be queried
     * @return LinkConfigStatus with outcome of check and associated config.
     */
    LinkConfigStatus checkLinkConfig(Link linkTuple);

    /**
     * Retrieves a list of switches that have been configured, and have been
     * determined to be 'allowed' in the network, according to configuration
     * rules.
     *
     * Note that it is possible that there are other switches that are allowed
     * in the network that have NOT been configured. Such switches will not be a
     * part of the returned list.
     *
     * Also note that it is possible that some switches will not be discovered
     * and the only way the controller can know about these switches is via
     * configuration. Such switches will be included in this list. It is up to
     * the caller to determine which SwitchConfig applies to non-discovered
     * switches.
     *
     * @return a non-null List of SwitchConfig which may be empty
     */
    List<SwitchConfig> getConfiguredAllowedSwitches();

    /**
     * Reserved for future use.
     *
     * Retrieves a list of links that have been configured, and have been
     * determined to be 'allowed' in the network, according to configuration
     * rules.
     *
     * Note that it is possible that there are other links that are allowed in
     * the network that have NOT been configured. Such links will not be a part
     * of the returned list.
     *
     * Also note that it is possible that some links will not be discovered and
     * the only way the controller can know about these links is via
     * configuration. Such links will be included in this list. It is up to the
     * caller to determine which LinkConfig applies to non-discovered links.
     *
     * In addition, note that the LinkConfig applies to the configured
     * bi-directional link, which may or may not have declared ports. The
     * associated unidirectional LinkTuple can be retrieved from the
     * getLinkTupleList() method in the LinkConfig object.
     *
     * @return a non-null List of LinkConfig which may be empty
     */
    List<LinkConfig> getConfiguredAllowedLinks();

    /**
     * Retrieves the Dpid associated with a 'name' for a configured switch
     * object. This method does not check of the switches are 'allowed' by
     * config.
     *
     * @param name device name
     * @return the Dpid corresponding to a given 'name', or null if no
     *         configured switch was found for the given 'name'.
     */
    DeviceId getDpidForName(String name);

}

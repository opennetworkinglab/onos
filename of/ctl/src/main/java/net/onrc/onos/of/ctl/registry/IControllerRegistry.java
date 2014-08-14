package net.onrc.onos.of.ctl.registry;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.onrc.onos.of.ctl.util.InstanceId;

/**
 * A registry service that allows ONOS to register controllers and switches in a
 * way that is global to the entire ONOS cluster. The registry is the arbiter
 * for allowing controllers to control switches.
 * <p/>
 * The OVS/OF1.{2,3} fault tolerance model is a switch connects to multiple
 * controllers, and the controllers send role requests to tell the switch their
 * role in controlling the switch.
 * <p/>
 * The ONOS fault tolerance model allows only a single controller to have
 * control of a switch (MASTER role) at once. Controllers therefore need a
 * mechanism that enables them to decide who should control a each switch. The
 * registry service provides this mechanism.
 */
public interface IControllerRegistry {

    /**
     * Callback interface for control change events.
     */
    public interface ControlChangeCallback {
        /**
         * Called whenever the control changes from the point of view of the
         * registry. The callee can check whether they have control or not using
         * the hasControl parameter.
         *
         * @param dpid The switch that control has changed for
         * @param hasControl Whether the listener now has control or not
         */
        void controlChanged(long dpid, boolean hasControl);
    }

    /**
     * Request for control of a switch. This method does not block. When control
     * for a switch changes, the controlChanged method on the callback object
     * will be called. This happens any time the control changes while the
     * request is still active (until releaseControl is called)
     *
     * @param dpid Switch to request control for
     * @param cb Callback that will be used to notify caller of control changes
     * @throws RegistryException Errors contacting the registry service
     */
    public void requestControl(long dpid, ControlChangeCallback cb)
            throws RegistryException;

    /**
     * Stop trying to take control of a switch. This removes the entry for this
     * controller requesting this switch in the registry. If the controller had
     * control when this is called, another controller will now gain control of
     * the switch. This call doesn't block.
     *
     * @param dpid Switch to release control of
     */
    public void releaseControl(long dpid);

    /**
     * Check whether the controller has control of the switch This call doesn't
     * block.
     *
     * @param dpid Switch to check control of
     * @return true if controller has control of the switch.
     */
    public boolean hasControl(long dpid);

    /**
     * Check whether this instance is the leader for the cluster. This call
     * doesn't block.
     *
     * @return true if the instance is the leader for the cluster, otherwise
     *         false.
     */
    public boolean isClusterLeader();

    /**
     * Gets the unique ID used to identify this ONOS instance in the cluster.
     *
     * @return Instance ID.
     */
    public InstanceId getOnosInstanceId();

    /**
     * Register a controller to the ONOS cluster. Must be called before the
     * registry can be used to take control of any switches.
     *
     * @param controllerId A unique string ID identifying this controller in the
     *        cluster
     * @throws RegistryException for errors connecting to registry service,
     *         controllerId already registered
     */
    public void registerController(String controllerId)
            throws RegistryException;

    /**
     * Get all controllers in the cluster.
     *
     * @return Collection of controller IDs
     * @throws RegistryException on error
     */
    public Collection<String> getAllControllers() throws RegistryException;

    /**
     * Get all switches in the cluster, along with which controller is in
     * control of them (if any) and any other controllers that have requested
     * control.
     *
     * @return Map of all switches.
     */
    public Map<String, List<ControllerRegistryEntry>> getAllSwitches();

    /**
     * Get the controller that has control of a given switch.
     *
     * @param dpid Switch to find controller for
     * @return controller ID
     * @throws RegistryException Errors contacting registry service
     */
    public String getControllerForSwitch(long dpid) throws RegistryException;

    /**
     * Get all switches controlled by a given controller.
     *
     * @param controllerId ID of the controller
     * @return Collection of dpids
     */
    public Collection<Long> getSwitchesControlledByController(String controllerId);

    /**
     * Get a unique Id Block.
     *
     * @return Id Block.
     */
    public IdBlock allocateUniqueIdBlock();

    /**
     * Get next unique id and retrieve a new range of ids if needed.
     *
     * @param range range to use for the identifier
     * @return Id Block.
     */
    public IdBlock allocateUniqueIdBlock(long range);

    /**
     * Get a globally unique ID.
     *
     * @return a globally unique ID.
     */
    public long getNextUniqueId();
}

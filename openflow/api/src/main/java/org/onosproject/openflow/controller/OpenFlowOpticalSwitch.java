package org.onosproject.openflow.controller;

/**
 * A marker interface for optical switches, which require the ability to pass
 * port information to a Device provider.
 */
public interface OpenFlowOpticalSwitch extends OpenFlowSwitch, WithTypedPorts {
}

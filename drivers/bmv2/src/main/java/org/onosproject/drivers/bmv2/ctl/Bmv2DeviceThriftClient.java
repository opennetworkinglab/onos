/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 */

package org.onosproject.drivers.bmv2.ctl;

import org.apache.thrift.TException;
import org.onosproject.bmv2.thriftapi.SimplePreLAG;
import org.onosproject.drivers.bmv2.api.Bmv2DeviceAgent;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreGroup;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreNode;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.drivers.bmv2.impl.Bmv2PreGroupTranslatorImpl;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.bmv2.ctl.Bmv2TExceptionParser.parseTException;

/**
 * Implementation of a Thrift client to control a BMv2 device.
 */
final class Bmv2DeviceThriftClient implements Bmv2DeviceAgent {

    // FIXME: make context_id arbitrary for each call
    // See: https://github.com/p4lang/behavioral-model/blob/master/modules/bm_sim/include/bm_sim/context.h
    private static final int CONTEXT_ID = 0;
    private static final String DEFAULT_LAG_MAP = "";
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SimplePreLAG.Iface simplePreLagClient;
    private final DeviceId deviceId;

    // ban constructor
    protected Bmv2DeviceThriftClient(DeviceId deviceId, SimplePreLAG.Iface simplePreLagClient) {
        this.deviceId = deviceId;
        this.simplePreLagClient = simplePreLagClient;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public Bmv2PreGroup writePreGroup(Bmv2PreGroup preGroup) throws Bmv2RuntimeException {
        log.debug("Creating a multicast group... > deviceId={}, {}", deviceId, preGroup);

        GroupRollbackMachine groupRollbackMachine = new GroupRollbackMachine(preGroup);
        try {
            //first create mc group
            preGroup.setNativeGroupHandle(createMcGroup(preGroup.groupId()));
            groupRollbackMachine.setState(GroupOperationState.GROUP_CREATED);
            //create mc nodes
            createMcNodesOfGroup(preGroup);
            groupRollbackMachine.setState(GroupOperationState.NODES_CREATED);
            //associate nodes with group
            associateMcNodesOfGroup(preGroup);
            groupRollbackMachine.setState(GroupOperationState.NODES_ASSOCIATED);

            log.debug("Multicast group created successfully. deviceId={}, {}", deviceId, preGroup);

            return preGroup;
        } finally {
            groupRollbackMachine.rollbackIfNecessary();
        }
    }

    @Override
    public void deletePreGroup(Bmv2PreGroup preGroup) throws Bmv2RuntimeException {
        log.debug("Deleting a multicast group... > deviceId={}, {}", deviceId, preGroup);
        //disassociate mc nodes from group
        disassociateMcNodesOfGroup(preGroup);
        //delete mc nodes
        deleteMcNodesOfGroup(preGroup);
        //delete group
        deleteMcGroup(preGroup);

        log.debug("Multicast group deleted. deviceId={}, {}", deviceId, preGroup);
    }

    @Override
    public List<Bmv2PreGroup> getPreGroups() throws Bmv2RuntimeException {
        try {
            String entries = simplePreLagClient.bm_mc_get_entries(CONTEXT_ID);
            return Bmv2PreGroupTranslatorImpl.translate(entries);

        } catch (TException | IOException e) {
            log.debug("Exception while getting multicast groups. deviceId={}", deviceId, e);

            if (e instanceof TException) {
                throw parseTException((TException) e);
            } else {
                throw new Bmv2RuntimeException(e);
            }
        }
    }

    /**
     * Creates multicast nodes one by one.
     * Node handles obtained as the results of node creation operations are stored
     * in given Bmv2PreGroup object.
     *
     * @param preGroup Bmv2PreGroup object
     * @throws Bmv2RuntimeException
     */
    private void createMcNodesOfGroup(Bmv2PreGroup preGroup) throws Bmv2RuntimeException {
        for (Bmv2PreNode node : preGroup.nodes().nodes()) {
            node.setL1Handle(createMcNode(node));
        }
    }

    /**
     * Associates multicast nodes with a group one by one.
     *
     * @param preGroup Bmv2PreGroup object
     * @throws Bmv2RuntimeException
     */
    private void associateMcNodesOfGroup(Bmv2PreGroup preGroup) throws Bmv2RuntimeException {
        int nativeGroupHandle = preGroup.nativeGroupHandle();
        for (Bmv2PreNode node : preGroup.nodes().nodes()) {
            associateMcNode(nativeGroupHandle, node);
        }
    }

    /**
     * Deletes multicast nodes one by one.
     *
     * @param preGroup Bmv2PreGroup object
     * @throws Bmv2RuntimeException
     */
    private void deleteMcNodesOfGroup(Bmv2PreGroup preGroup) throws Bmv2RuntimeException {
        for (Bmv2PreNode node : preGroup.nodes().nodes()) {
            destroyMcNode(node);
        }
    }

    /**
     * Disassociates multicast nodes from a group one by one.
     *
     * @param preGroup Bmv2PreGroup object
     * @throws Bmv2RuntimeException
     */
    private void disassociateMcNodesOfGroup(Bmv2PreGroup preGroup) throws Bmv2RuntimeException {
        int nativeGroupHandle = preGroup.nativeGroupHandle();
        for (Bmv2PreNode node : preGroup.nodes().nodes()) {
            disassociateMcNode(nativeGroupHandle, node);
        }
    }


    /**
     * Creates a multicast group with specified group Id.
     *
     * @param groupId identifier of a group
     * @return group handle (BMv2 specific identifier associated with the group)
     * @throws Bmv2RuntimeException
     */
    private int createMcGroup(int groupId) throws Bmv2RuntimeException {
        log.debug("Creating the multicast group... > deviceId={}, groupId={}", deviceId, groupId);
        try {
            return simplePreLagClient.bm_mc_mgrp_create(CONTEXT_ID, groupId);
        } catch (TException e) {
            log.debug("Exception during creating multicast group. deviceId={}, groupId={}", deviceId, groupId);
            throw parseTException(e);
        }
    }

    /**
     * Deletes a multicast group from a BMv2 device.
     *
     * @param preGroup
     * @throws Bmv2RuntimeException
     */
    private void deleteMcGroup(Bmv2PreGroup preGroup) throws Bmv2RuntimeException {
        log.debug("Destroying the multicast group... > deviceId={}, groupId={}, groupHandle={}",
                  deviceId, preGroup.groupId(), preGroup.nativeGroupHandle());
        try {
            simplePreLagClient.bm_mc_mgrp_destroy(CONTEXT_ID, preGroup.nativeGroupHandle());
        } catch (TException e) {
            log.debug("Exception during destroying multicast group. deviceId={}, groupId={}, groupHandle={}",
                      deviceId, preGroup.groupId(), preGroup.nativeGroupHandle());
            throw parseTException(e);
        }
    }

    /**
     * Creates a multicast node on the BMv2 device.
     *
     * @param node Bmv2PreNode
     * @return L1 handle
     * @throws Bmv2RuntimeException
     */
    private int createMcNode(Bmv2PreNode node) throws Bmv2RuntimeException {
        log.debug("Creating the multicast node... > deviceId={}, {}", deviceId, node);
        try {
            return simplePreLagClient.bm_mc_node_create(CONTEXT_ID, node.rid(), node.portMap(), DEFAULT_LAG_MAP);
        } catch (TException e) {
            log.debug("Exception during creating multicast node: {}", node);
            throw parseTException(e);
        }
    }

    /**
     * Associates a multicast node with a group.
     *
     * @param groupHandle handle of the group that the node will be associated with
     * @param node        Bmv2PreNode
     * @throws Bmv2RuntimeException
     */
    private void associateMcNode(int groupHandle, Bmv2PreNode node) throws Bmv2RuntimeException {
        log.debug("Associating the multicast node with the group... > deviceId={}, groupHandle:{}, node:{}",
                  deviceId, groupHandle, node);
        try {
            simplePreLagClient.bm_mc_node_associate(CONTEXT_ID, groupHandle, node.l1Handle());
        } catch (TException e) {
            log.debug("Exception during associating multicast node with group. deviceId={} groupHandle:{}, node:{}",
                      deviceId, groupHandle, node);
            throw parseTException(e);
        }
    }

    /**
     * Disassociates a multicast node from a group.
     *
     * @param groupHandle handle of the group that the node will be disassociated from
     * @param node        Bmv2PreNode
     * @throws Bmv2RuntimeException
     */
    private void disassociateMcNode(int groupHandle, Bmv2PreNode node) throws Bmv2RuntimeException {
        log.debug("Disassociating the multicast node from the group... > deviceId={}, groupHandle:{}, node:{}",
                  deviceId, groupHandle, node);
        try {
            simplePreLagClient.bm_mc_node_dissociate(CONTEXT_ID, groupHandle, node.l1Handle());
        } catch (TException e) {
            log.debug("Failed to disassociate multicast node from group. deviceId={} groupHandle:{}, node:{}",
                      deviceId, groupHandle, node);
            throw parseTException(e);
        }
    }

    /**
     * Destroys the multicast node in a BMv2 device.
     *
     * @param node PRE node which is about to be destroyed
     * @throws Bmv2RuntimeException
     */
    private void destroyMcNode(Bmv2PreNode node) throws Bmv2RuntimeException {
        log.debug("Destroying the multicast node... > deviceId={}, node:{}", deviceId, node);
        try {
            simplePreLagClient.bm_mc_node_destroy(CONTEXT_ID, node.l1Handle());
        } catch (TException e) {
            log.debug("Exception during destroying multicast node. deviceId={}, node:{}", deviceId, node);
            throw parseTException(e);
        }
    }


    /**
     * Defines identifiers of main group operation steps.
     */
    private enum GroupOperationState {
        IDLE, // nothing has been done
        GROUP_CREATED, //the last successful step is group creation
        NODES_CREATED, //the last successful step is node creation
        NODES_ASSOCIATED //the last successful step is node association.
    }

    /**
     * Implementation of a simple state machine to keep track of complex (non-atomic) operations on groups and
     * to execute essential rollback steps accordingly.
     * For example, creating a multicast group is composed of multiple steps:
     * 1- Group creation
     * 2- Node creation
     * 3- Node association
     * Each step associates with a GroupOperationState to keep track of group creation operation.
     * A rollback flow is executed with respect to the current state.
     */
    private class GroupRollbackMachine {
        Bmv2PreGroup preGroup;
        //indicates the last successful step
        GroupOperationState state = GroupOperationState.IDLE;

        private GroupRollbackMachine() {
            //hidden constructor
        }

        public GroupRollbackMachine(Bmv2PreGroup preGroup) {
            this.preGroup = preGroup;
        }

        GroupOperationState state() {
            return state;
        }

        void setState(GroupOperationState state) {
            this.state = checkNotNull(state);
        }

        /**
         * Checks the state and executes necessary rollback flow if necessary.
         */
        void rollbackIfNecessary() {
            switch (state) {
                case GROUP_CREATED:
                    //means node creation failed. Delete already created nodes and the group
                    onGroupCreated();
                    break;
                case NODES_CREATED:
                    //means node association failed.
                    //Disassociate already associated nodes then delete nodes and the group.
                    onNodesCreated();
                    break;
                default:
                    //do nothing in IDLE and NODES_ASSOCIATED states. They do not signify a failure.
                    break;
            }
        }

        /**
         * Executes necessary steps in case the last successful step is group creation.
         * This means one of the node creation operations has been failed and all previous steps should rollback.
         */
        private void onGroupCreated() {
            log.warn("One of the steps of mc group creation operation has been failed." +
                             "Rolling back in state {}...> deviceId={}, groupId={}",
                     state, deviceId, preGroup.groupId());
            deleteNodes(preGroup);
            deleteGroup(preGroup);
        }

        /**
         * Executes necessary steps in case the last successful step is node creation.
         * This means one of the node association operations has been failed and all previous steps should rollback.
         */
        private void onNodesCreated() {
            log.warn("One of the steps of mc group creation operation has been failed." +
                             "Rolling back in state {}...> deviceId={}, groupId={}",
                     state, deviceId, preGroup.groupId());
            disassociateNodes(preGroup);
            deleteNodes(preGroup);
            deleteGroup(preGroup);
        }

        /**
         * Deletes a group in the scope of rollback operation.
         */
        private void deleteGroup(Bmv2PreGroup preGroup) {
            try {
                deleteMcGroup(preGroup);
            } catch (Bmv2RuntimeException e) {
                log.error("Unable to destroy multicast group in the scope of rollback operation." +
                                  "deviceId={}, groupId={}", deviceId, preGroup.groupId());
            }
        }


        /**
         * Disassociates all nodes from their group in the scope of rollback operation.
         */
        private void disassociateNodes(Bmv2PreGroup preGroup) {
            preGroup.nodes().nodes().forEach(node -> {
                try {
                    disassociateMcNode(preGroup.nativeGroupHandle(), node);
                } catch (Bmv2RuntimeException e) {
                    log.error("Unable to disassociate the node in the scope of rollback operation." +
                                      "deviceId={}, groupHandle={}, l1Handle={}",
                              deviceId, preGroup.nativeGroupHandle(), node.l1Handle(), e);
                }
            });
        }

        /**
         * Deletes all nodes of a group in the scope of rollback operation.
         */
        private void deleteNodes(Bmv2PreGroup preGroup) {
            //filter created nodes and destroy them
            preGroup.nodes().nodes().stream()
                    .filter(node -> node.l1Handle() != null)
                    .forEach(node -> {
                        try {
                            destroyMcNode(node);
                        } catch (Bmv2RuntimeException e) {
                            log.error("Unable to destroy the node in the scope of rollback operation." +
                                              "deviceId={}, l1Handle={}", deviceId, node.l1Handle(), e);
                        }
                    });
        }
    }
}

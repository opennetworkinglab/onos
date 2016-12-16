/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.yms.app.ysr;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network1.rev20151208.IetfNetwork1Service;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network2.rev20151208.IetfNetwork2Service;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.TestService;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangRevision;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yms.app.yab.TestManager;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit test case for default schema registry.
 */
public class DefaultYangSchemaRegistryTest {

    private static final String SERVICE_NAME_1 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network1.rev20151208.IetfNetwork1Service";
    private static final String SCHEMA_NAME_1 = "ietf-network1";
    private static final String EVENT_NAME_1 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network1.rev20151208.ietfnetwork1.IetfNetwork1Event";
    private static final String INTERFACE_NAME_1 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network1.rev20151208.IetfNetwork1";
    private static final String OP_PARAM_NAME_1 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network1.rev20151208.IetfNetwork1OpParam";

    private static final String SERVICE_NAME_2 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network2.rev20151208.IetfNetwork2Service";
    private static final String SCHEMA_NAME_2 = "ietf-network2";
    private static final String EVENT_NAME_2 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network2.rev20151208.ietfnetwork2.IetfNetwork2Event";
    private static final String INTERFACE_NAME_2 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network2.rev20151208.IetfNetwork2";
    private static final String OP_PARAM_NAME_2 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network2.rev20151208.IetfNetwork2OpParam";

    private static final String SERVICE_NAME_3 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network3.rev20151208.IetfNetwork3Service";
    private static final String SCHEMA_NAME_3 = "ietf-network3";
    private static final String EVENT_NAME_3 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network3.rev20151208.ietfnetwork3.IetfNetwork3Event";
    private static final String INTERFACE_NAME_3 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network3.rev20151208.IetfNetwork3";
    private static final String OP_PARAM_NAME_3 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network3.rev20151208.IetfNetwork3OpParam";

    private static final String SCHEMA_NAME_4_14 = "ietf-network4@2014-00-08";
    private static final String SCHEMA_NAME_4_15 = "ietf-network4@2015-00-08";
    private static final String SCHEMA_NAME_4_16 = "ietf-network4@2016-00-08";
    private static final String SCHEMA_NAME_4_17 = "ietf-network4@2017-00-08";
    private static final String SCHEMA_NAME_4 = "ietf-network4";
    private static final String SERVICE_NAME_REV_14 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20141208.IetfNetwork4Service";
    private static final String EVENT_NAME_REV_14 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20141208.ietfnetwork4.IetfNetwork4Event";
    private static final String INTERFACE_NAME_REV_14 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20141208.IetfNetwork4";
    private static final String OP_PARAM_NAME_REV_14 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20141208.IetfNetwork4OpParam";

    private static final String SERVICE_NAME_REV_15 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20151208.IetfNetwork4Service";
    private static final String EVENT_NAME_REV_15 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20151208.ietfnetwork4.IetfNetwork4Event";
    private static final String INTERFACE_NAME_REV_15 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20151208.IetfNetwork4";
    private static final String OP_PARAM_NAME_REV_15 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20151208.IetfNetwork4OpParam";

    private static final String SERVICE_NAME_REV_16 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20161208.IetfNetwork4Service";

    private static final String EVENT_NAME_REV_16 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20161208.ietfnetwork4.IetfNetwork4Event";
    private static final String INTERFACE_NAME_REV_16 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20161208.IetfNetwork4";
    private static final String OP_PARAM_NAME_REV_16 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20161208.IetfNetwork4OpParam";

    private static final String SERVICE_NAME_REV_17 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20171208.IetfNetwork4Service";
    private static final String EVENT_NAME_REV_17 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20171208.ietfnetwork4.IetfNetwork4Event";
    private static final String INTERFACE_NAME_REV_17 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20171208.IetfNetwork4";
    private static final String OP_PARAM_NAME_REV_17 =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.rev20171208.IetfNetwork4OpParam";

    private static final String SERVICE_NAME_NO_REV =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.IetfNetwork4Service";
    private static final String EVENT_NAME_NO_REV =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.ietfnetwork4.IetfNetwork4Event";
    private static final String INTERFACE_NAME_NO_REV =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.IetfNetwork4";
    private static final String OP_PARAM_NAME_NO_REV =
            "org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf" +
                    ".network4.IetfNetwork4OpParam";

    private static final String DATE = "2014-00-08";
    private static final String SCHEMA_NAME_WITH_DATE =
            "ietf-network4@2014-00-08";

    private static final String UN_REG_SCHEMA_NAME = "ietf-routing";
    private static final String UN_REG_INTERFACE_NAME = "IetfRouting";
    private static final String UN_REG_OP_PARAM_NAME = "IetfRoutingOpParam";
    private static final String UN_REG_SERVICE_NAME = "IetfRoutingService";
    private static final String UN_REG_EVENT_NAME = "IetfRoutingEvent";
    private static final String CHECK = "check";
    private static final String DATE_NAMESPACE = "2015-00-08";
    private static final String NAMESPACE =
            "urn:ietf:params:xml:ns:yang:ietf-network4:check:namespace";

    private final TestYangSchemaNodeProvider testYangSchemaNodeProvider =
            new TestYangSchemaNodeProvider();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Unit test case in which schema node should be present.
     *
     * @throws IOException when fails to do IO operation
     */
    @Test
    public void testForGetSchemaNode()
            throws IOException {

        testYangSchemaNodeProvider.processSchemaRegistry(null);

        DefaultYangSchemaRegistry registry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangSchemaNode yangNode =
                registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_1);
        assertThat(true, is(SCHEMA_NAME_1.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_1);
        assertThat(true, is(SCHEMA_NAME_1.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_1);
        assertThat(true, is(SCHEMA_NAME_1.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_1);
        assertThat(true, is(SCHEMA_NAME_1.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(EVENT_NAME_1);
        assertThat(true, is(SCHEMA_NAME_1.equals(yangNode.getName())));

        //As we have not registered an  application this object should be null.
        Object object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_1);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_1);
        assertThat(true, is(yangNode == null));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_1);
        assertThat(true, is(yangNode == null));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_1);
        assertThat(true, is(yangNode == null));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_1);
        assertThat(true, is(yangNode == null));

        yangNode = registry.getRootYangSchemaNodeForNotification(EVENT_NAME_1);
        assertThat(true, is(yangNode == null));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));

        //With second service.
        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_2);
        assertThat(true, is(SCHEMA_NAME_2.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_2);
        assertThat(true, is(SCHEMA_NAME_2.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_2);
        assertThat(true, is(SCHEMA_NAME_2.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_2);
        assertThat(true, is(SCHEMA_NAME_2.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(EVENT_NAME_2);
        assertThat(true, is(SCHEMA_NAME_2.equals(yangNode.getName())));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));

        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_2);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_2);
        assertThat(true, is(yangNode == null));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_2);
        assertThat(true, is(yangNode == null));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_2);
        assertThat(true, is(yangNode == null));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_2);
        assertThat(true, is(yangNode == null));

        yangNode = registry.getRootYangSchemaNodeForNotification(EVENT_NAME_2);
        assertThat(true, is(yangNode == null));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));

        //With third service.
        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_3);
        assertThat(true, is(SCHEMA_NAME_3.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_3);
        assertThat(true, is(SCHEMA_NAME_3.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_3);
        assertThat(true, is(SCHEMA_NAME_3.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(EVENT_NAME_3);
        assertThat(true, is(yangNode == null));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_3);
        assertThat(true, is(SCHEMA_NAME_3.equals(yangNode.getName())));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));

        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_3);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_3);
        assertThat(true, is(yangNode == null));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_3);
        assertThat(true, is(yangNode == null));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_3);
        assertThat(true, is(yangNode == null));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_3);
        assertThat(true, is(yangNode == null));

        yangNode = registry.getRootYangSchemaNodeForNotification(EVENT_NAME_3);
        assertThat(true, is(yangNode == null));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
    }

    /**
     * Unit test case in which schema node should not be present.
     *
     * @throws IOException when fails to do IO operation
     */
    @Test
    public void testForDoNotGetSchemaNode()
            throws IOException {

        DefaultYangSchemaRegistry registry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        // here all nodes should be null as we have not done any registration
        // for this application.
        YangSchemaNode yangNode =
                registry.getYangSchemaNodeUsingAppName(UN_REG_SERVICE_NAME);
        assertThat(true, is(yangNode == null));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(UN_REG_SCHEMA_NAME);
        assertThat(true, is(yangNode == null));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        UN_REG_INTERFACE_NAME);
        assertThat(true, is(yangNode == null));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        UN_REG_OP_PARAM_NAME);
        assertThat(true, is(yangNode == null));

        yangNode = registry.getRootYangSchemaNodeForNotification(
                UN_REG_EVENT_NAME);
        assertThat(true, is(yangNode == null));

        //As we have not registered an  application this object should be null.
        Object object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
    }

    /**
     * Unit test case in which schema node should be present with multi
     * revisions.
     *
     * @throws IOException when fails to do IO operation
     */
    @Test
    public void testForGetSchemaNodeWhenNoRevision()
            throws IOException {

        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        //Service with rev.
        YangSchemaNode yangNode =
                registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_REV_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_REV_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(
                EVENT_NAME_REV_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        //As we have not registered an  application this object should be null.
        Object object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_REV_15);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_15);
        assertThat(true, is(yangNode == null));

        //Here the yangNode should be the node which does not have revision.
        // asset should pass with false.
        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4);
        assertThat(true, is(((YangNode) yangNode).getRevision() == null));

        //Service no revision.
        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_NO_REV);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_NO_REV);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_NO_REV);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(EVENT_NAME_NO_REV);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_NO_REV);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_NO_REV);
        assertThat(true, is(yangNode == null));

        //Here the yangNode should be the node which have different revision.
        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4);
        assertThat(true, is(yangNode != null));
        assertThat(true, is(((YangNode) yangNode).getRevision() != null));
    }

    /**
     * Unit test case in which schema node should be present with multi
     * revisions.
     *
     * @throws IOException when fails to do IO operation
     */
    @Test
    public void testForGetSchemaNodeWhenMultiRevision()
            throws IOException {

        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        //Service with rev.
        YangSchemaNode yangNode =
                registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_REV_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_REV_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(
                EVENT_NAME_REV_15);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        //As we have not registered an  application this object should be null.
        Object object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_REV_15);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_15);
        assertThat(true, is(yangNode == null));

        //Here the yangNode should be the node which does not have revision.
        // asset should pass with false.
        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4);
        assertThat(true, is(((YangNode) yangNode).getRevision() == null));

        //Service with different revision.
        yangNode = registry
                .getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_16);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4_16);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_REV_16);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_REV_16);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(EVENT_NAME_REV_16);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_REV_16);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_16);
        assertThat(true, is(yangNode == null));

        //Here the yangNode should be the node which have different revision.
        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4);
        assertThat(true, is(((YangNode) yangNode).getRevision() == null));

        //Service with different revision.
        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_17);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4_17);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_REV_17);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_REV_17);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(
                EVENT_NAME_REV_17);
        assertThat(true, is(yangNode == null));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_REV_17);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_17);
        assertThat(true, is(yangNode == null));

        //Here the yangNode should be the node which have different revision.
        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4);
        assertThat(true, is(((YangNode) yangNode).getRevision() == null));

        //Service no revision.
        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_NO_REV);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_NO_REV);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_NO_REV);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(EVENT_NAME_NO_REV);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_NO_REV);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_NO_REV);
        assertThat(true, is(yangNode == null));

        //Here the yangNode should be the node which have different revision.
        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4);
        assertThat(true, is(yangNode != null));
        assertThat(true, is(((YangNode) yangNode).getRevision() != null));

        //Service with different revision.
        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_14);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_4_14);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        INTERFACE_NAME_REV_14);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode =
                registry.getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                        OP_PARAM_NAME_REV_14);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        yangNode = registry.getRootYangSchemaNodeForNotification(
                EVENT_NAME_REV_14);
        assertThat(true, is(SCHEMA_NAME_4.equals(yangNode.getName())));

        //As we have not registered an  application this object should be null.
        object = registry.getRegisteredApplication(yangNode);
        assertThat(true, is(object == null));
        testYangSchemaNodeProvider.unregisterService(SERVICE_NAME_REV_14);

        yangNode = registry.getYangSchemaNodeUsingAppName(SERVICE_NAME_REV_14);
        assertThat(true, is(yangNode == null));
    }

    /**
     * Unit test case should generate exceptions.
     */
    @Test
    public void testRegistration() {
        thrown.expect(RuntimeException.class);
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        testYangSchemaNodeProvider.processRegistrationOfApp();
    }

    /**
     * Unit test case should not generate any exceptions and should
     * return specific revision node.
     */
    @Test
    public void testGetWithSpecificRevision() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        YangSchemaNode schemaNode = testYangSchemaNodeProvider
                .getDefaultYangSchemaRegistry()
                .getYangSchemaNodeUsingSchemaName(SCHEMA_NAME_WITH_DATE);

        assertThat(true, is(schemaNode.getName().equals(SCHEMA_NAME_4)));
        String date = testYangSchemaNodeProvider
                .getDefaultYangSchemaRegistry()
                .getDateInStringFormat(schemaNode);
        assertThat(true, is(date.equals(DATE)));
    }

    /**
     * Unit test case should not generate any exceptions
     * verify notification should be checked for registration.
     */
    @Test
    public void testNotification() {
        MockIetfManager manager = new MockIetfManager();
        testYangSchemaNodeProvider.processSchemaRegistry(manager);
        boolean isRegWithNotification =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry()
                        .verifyNotificationObject(manager, IetfNetwork1Service.class);
        assertThat(true, is(isRegWithNotification));
        isRegWithNotification = testYangSchemaNodeProvider
                .getDefaultYangSchemaRegistry()
                .verifyNotificationObject(manager, IetfNetwork2Service.class);
        assertThat(false, is(isRegWithNotification));
        isRegWithNotification = testYangSchemaNodeProvider
                .getDefaultYangSchemaRegistry()
                .verifyNotificationObject(new TestManager(), TestService.class);
        assertThat(false, is(isRegWithNotification));
    }

    /**
     * get schema for namespace in decode test.
     */
    @Test
    public void testGetNodeWrtNamespace() {
        MockIetfManager manager = new MockIetfManager();
        testYangSchemaNodeProvider.processSchemaRegistry(manager);
        DefaultYangSchemaRegistry registry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangSchemaNode yangNode =
                registry.getSchemaWrtNameSpace(NAMESPACE);
        assertThat(true, is(CHECK.equals(yangNode.getName())));

        YangRevision rev = ((YangNode) yangNode).getRevision();
        assertThat(true, is(rev != null));

        String date = registry.getDateInStringFormat(yangNode);
        assertThat(true, is(DATE_NAMESPACE.equals(date)));
    }
}
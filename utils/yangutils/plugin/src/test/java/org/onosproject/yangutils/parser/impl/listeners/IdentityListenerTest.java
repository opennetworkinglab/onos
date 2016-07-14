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
package org.onosproject.yangutils.parser.impl.listeners;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangIdentity;
import org.onosproject.yangutils.datamodel.YangIdentityRef;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;
import java.util.ListIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test case for identity listener.
 */
public class IdentityListenerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks for updating datamodel for identity/identityref.
     */
    @Test
    public void processIdentityrefType() throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/IdentityListener.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("IdentityListener"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("tunnel"));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling();
        assertThat(yangIdentity.getName(), is("tunnel-type"));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling().getNextSibling();
        assertThat(yangIdentity.getName(), is("ref-address-family"));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling().getNextSibling()
                .getNextSibling();
        assertThat(yangIdentity.getName(), is("ipv4-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling().getNextSibling()
                .getNextSibling().getNextSibling();
        assertThat(yangIdentity.getName(), is("ipv6-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("tunnel"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        YangIdentityRef yangIdentityRef = (YangIdentityRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangIdentityRef.getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("network-ref"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        yangIdentityRef = (YangIdentityRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether identityref type got resolved.
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
    }

    /**
     * Checks for updating datamodel for intrafile resolution identity/identityref.
     */
    @Test
    public void processIntraIdentityrefType() throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/IdentityIntraFile.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("IdentityIntraFile"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("ipv4-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.INTRA_FILE_RESOLVED));
    }

    /**
     * Checks for updating datamodel for identityref used in tydedef.
     */
    @Test
    public void processIdentityTypedefStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IdentityTypedef.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("tunnel"));

        YangTypeDef typedef = (YangTypeDef) yangNode.getChild().getNextSibling();
        assertThat(typedef.getName(), is("type15"));

        YangType type = typedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.IDENTITYREF));
        assertThat(type.getDataTypeName(), is("identityref"));

        YangIdentityRef identityRef = (YangIdentityRef) type.getDataTypeExtendedInfo();
        assertThat(identityRef.getName(), is("tunnel"));
        assertThat(identityRef.getBaseIdentity().getName(), is("tunnel"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("tunnel-value"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        YangIdentityRef yangIdentityRef = (YangIdentityRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangIdentityRef.getName(), is("tunnel"));
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("tunnel"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("tunnel"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
   }

    /**
     * Checks for updating datamodel for unresolved status of identityref used in tydedef.
     */
    @Test
    public void processIdentityUnresolvedTypedefStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IdentityTypedefUnresolved.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("tunnel"));

        YangTypeDef typedef = (YangTypeDef) yangNode.getChild().getNextSibling();
        assertThat(typedef.getName(), is("type15"));

        YangType type = typedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.IDENTITYREF));
        assertThat(type.getDataTypeName(), is("identityref"));

        YangIdentityRef identityRef = (YangIdentityRef) type.getDataTypeExtendedInfo();
        assertThat(identityRef.getName(), is("tunnel"));
        assertThat(identityRef.getBaseIdentity().getName(), is("tunnel"));
        assertThat(identityRef.getResolvableStatus(), is(ResolvableStatus.UNRESOLVED));

    }
}

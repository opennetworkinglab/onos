package org.onosproject.yangutils.parser.impl.listeners;

import java.io.IOException;
import java.util.ListIterator;

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test case for type listener.
 */
public class TypeListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks derived statement without contraints.
     */
    @Test
    public void processDerivedTypeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/DerivedTypeStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("\"hello\""));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DERIVED));
    }

    /**
     * Checks valid yang data type.
     */
    @Test
    public void processIntegerTypeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IntegerTypeStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("\"uint16\""));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
    }

    /**
     * Checks type for leaf-list.
     */
    @Test
    public void processLeafListSubStatementType() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LeafListSubStatementType.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        assertThat(leafListInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("\"uint16\""));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
    }
}
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

import java.io.IOException;
import java.util.ListIterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.DataTypeException;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangInt64;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing default listener functionality.
 */
public class DefaultListenerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if default value is set correctly.
     */
    @Test
    public void processDefaultValueInLeafSubStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueInLeafSubStatement.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDefaultValueInString(), is("1"));
    }

    /**
     * Validates default invalid value in leaf.
     */
    @Test
    public void processDefaultInalueInLeafSubStatement() throws IOException, ParserException {

        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"x\" is not a valid uint16.");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueInLeafSubStatement.yang");
    }

    /**
     * Validates default case value in choice statement.
     */
    @Test
    public void processDefaultCaseInChoiceSubStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultCaseInChoiceSubStatement.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("food"));

        YangChoice yangChoice = (YangChoice) yangContainer.getChild();
        assertThat(yangChoice.getName(), is("snack"));
        assertThat(yangChoice.getDefaultValueInString(), is("sports-arena"));
    }

    /**
     * Validates default invalide case in choice statement.
     */
    @Test
    public void processDefaultInvalidCaseInChoiceSubStatement() throws IOException, ParserException {

        thrown.expect(ParserException.class);
        thrown.expectMessage("Internal parser error detected: Invalid content in choice \"snack\" after processing.");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueInChoiceSubStmt.yang");
    }

    /**
     * Validates default value in typedef.
     */
    @Test
    public void processDefaultInTypedef() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueInTypeDef.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // check typedef
        YangTypeDef typedef = (YangTypeDef) yangNode.getChild();
        assertThat(typedef.getName(), is("topInt"));
        assertThat(typedef.getDefaultValueInString(), is("10"));

        YangType type = typedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.INT64));
        assertThat(type.getDataTypeName(), is("int64"));

        // check leaf
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();
        assertThat(leafInfo.getName(), is("myValue"));

        // Check leaf reffered typedef
        assertThat(leafInfo.getDataType().getDataTypeName(), is("topInt"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DERIVED));
        YangType<YangDerivedInfo> typeDerived = (YangType<YangDerivedInfo>) leafInfo.getDataType();
        YangDerivedInfo derivedInfo = (YangDerivedInfo) typeDerived.getDataTypeExtendedInfo();
        YangTypeDef prevTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(prevTypedef.getName(), is("topInt"));
        assertThat(prevTypedef.getDefaultValueInString(), is("10"));
        YangType topType = prevTypedef.getTypeList().iterator().next();
        assertThat(topType.getDataType(), is(YangDataTypes.INT64));
        assertThat(topType.getDataTypeName(), is("int64"));
        YangType<YangInt64> typeInt64 = (YangType<YangInt64>) topType;
        YangInt64 int64Obj = typeInt64.getDataTypeExtendedInfo();
    }

    /**
     * Validates invalid default value in typedef.
     */
    @Test
    public void processInvalidDefaultValueInTypdeDef() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"x\" is not a valid int64.");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueInTypeDef.yang");
    }

    /**
     * Validates default invalid value in typedef.
     */
    @Test
    public void processDefaultInvalidValueInTypedef() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"0\" is not a valid INT32");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueWithRangeInTypedef.yang");
    }

    /**
     * Validates default value decimal64 in leaf.
     */
    @Test
    public void processDefaultValueDecimal64InLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueDecimal64InLeaf.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // check leaf
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check the default value
        // This default value is verified by YangType.isValidValue() called from LeafListener.processLeafExit()
        assertThat(leafInfo.getName(), is("mydecimal"));
        assertThat(leafInfo.getDefaultValueInString(), is("5"));
    }

    /**
     * Validates default invalid value decimal64 in leaf.
     */
    @Test
    public void processDefaultInvalidValueDecimal64InLeaf() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"x\" is not a valid decimal64.");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueDecimal64InLeaf.yang");
    }

    /**
     * Validates default value string in leaf.
     */
    @Test
    public void processDefaultValueStringInLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueStringInLeaf.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // check leaf
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check the default value
        // This default value is verified by YangType.isValidValue() called from LeafListener.processLeafExit()
        assertThat(leafInfo.getName(), is("MyString"));
        assertThat(leafInfo.getDefaultValueInString(), is("2bB"));
    }

    /**
     * Validates default invalid value string in leaf.
     */
    @Test
    public void processDefaultInvalidValueStringInLeaf() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"2bB2bB\" is not a valid STRING");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueStringInLeaf.yang");
    }

    /**
     * Validates default value boolean in leaf.
     */
    @Test
    public void processDefaultValueBooleanInLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueBooleanInLeaf.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // check leaf
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check the default value
        // This default value is verified by YangType.isValidValue() called from LeafListener.processLeafExit()
        assertThat(leafInfo.getName(), is("myboolean"));
        assertThat(leafInfo.getDefaultValueInString(), is("true"));
    }

    /**
     * Validates default invalid value boolean in leaf.
     */
    @Test
    public void processDefaultInvalidValueBooleanInLeaf() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"yes\" is not a valid BOOLEAN");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueBooleanInLeaf.yang");
    }

    /**
     * Validates default value enumeration in leaf.
     */
    @Test
    public void processDefaultValueEnumberationInLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueEnumerationInLeaf.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // check leaf
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check the default value
        // This default value is verified by YangType.isValidValue() called from LeafListener.processLeafExit()
        assertThat(leafInfo.getName(), is("myenum"));
        assertThat(leafInfo.getDefaultValueInString(), is("one"));
    }

    /**
     * Validates default invalid value enumeration in leaf.
     */
    @Test
    public void processDefaultInvalidValueEnumberationInLeaf() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"xyz\" is not a valid ENUMERATION");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueEnumerationInLeaf.yang");
    }

    /**
     * Validates default value bits in leaf.
     */
    @Test
    public void processDefaultValueBitsInLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueBitsInLeaf.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // check leaf
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check the default value
        // This default value is verified by YangType.isValidValue() called from LeafListener.processLeafExit()
        assertThat(leafInfo.getName(), is("mybits"));
        assertThat(leafInfo.getDefaultValueInString(), is("auto-sense-speed"));
    }

    /**
     * Validates default invalid value bits in leaf.
     */
    @Test
    public void processDefaultInvalidValueBitsInLeaf() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"xyz\" is not a valid BITS");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueBitsInLeaf.yang");
    }

    /**
     * Validates default value binary in leaf.
     */
    @Test
    public void processDefaultValueBinaryInLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueBinaryInLeaf.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // check leaf
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check the default value
        // This default value is verified by YangType.isValidValue() called from LeafListener.processLeafExit()
        assertThat(leafInfo.getName(), is("message"));
        assertThat(leafInfo.getDefaultValueInString(), is("10010010"));
    }

    /**
     * Validates default invalid value binary in leaf.
     */
    @Test
    public void processDefaultInvlaidValueBinaryInLeaf() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"000\" is not a valid BINARY");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueBinaryInLeaf.yang");
    }

    /**
     * Validates default value empty in leaf.
     */
    @Test
    public void processDefaultValueEmptyInLeaf() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"something\" is not allowed for a data type EMPTY");

        manager.getDataModel("src/test/resources/default/DefaultValueEmptyInLeaf.yang");
    }

    /**
     * Validates default value union in leaf.
     */
    @Test
    public void processDefaultValueUnionInLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueUnionInLeaf.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // check leaf
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check the default value
        // This default value is verified by YangType.isValidValue() called from LeafListener.processLeafExit()
        assertThat(leafInfo.getName(), is("message"));
        assertThat(leafInfo.getDefaultValueInString(), is("unbounded"));
    }

    /**
     * Validates default invalid value union in leaf.
     */
    @Test
    public void processDefaultInvalidValueUnionInLeaf() throws IOException, ParserException {
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("YANG file error : Input value \"xyz\" is not a valid UNION");

        manager.getDataModel("src/test/resources/default/DefaultInvalidValueUnionInLeaf.yang");
    }

    /**
     * Validates default value in multiple typedef.
     */
    @Test
    public void processDefaultInMultiTypedef() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/default/DefaultValueInMultiTypeDef.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // check typedef
        YangTypeDef typedef = (YangTypeDef) yangNode.getChild();
        assertThat(typedef.getName(), is("topInt"));
        assertThat(typedef.getDefaultValueInString(), is("10"));

        YangType type = typedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.INT64));
        assertThat(type.getDataTypeName(), is("int64"));

        // check leaf
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();
        assertThat(leafInfo.getName(), is("lowInt"));

        // check leaf type
        assertThat(leafInfo.getName(), is("lowInt"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("midInt"));
        YangType<YangDerivedInfo> derivedInfoType = (YangType<YangDerivedInfo>) leafInfo.getDataType();
        assertThat(derivedInfoType.getDataType(), is(YangDataTypes.DERIVED));
        YangDerivedInfo derivedInfo = (YangDerivedInfo) derivedInfoType.getDataTypeExtendedInfo();

        // check previous typedef
        YangTypeDef prevTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(prevTypedef.getName(), is("midInt"));
        type = prevTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DERIVED));
        derivedInfo = (YangDerivedInfo) type.getDataTypeExtendedInfo();

        // check top typedef
        YangTypeDef topTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(topTypedef.getName(), is("topInt"));
        assertThat(topTypedef.getDefaultValueInString(), is("10"));
        YangType topType = topTypedef.getTypeList().iterator().next();
        assertThat(topType.getDataType(), is(YangDataTypes.INT64));
        assertThat(topType.getDataTypeName(), is("int64"));
        YangType<YangInt64> typeInt64 = (YangType<YangInt64>) topType;
        YangInt64 int64Obj = typeInt64.getDataTypeExtendedInfo();
    }
}

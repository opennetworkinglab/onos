/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.yangutils.translator.tojava.utils;

import org.junit.Test;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.tojava.AttributeInfo;
import org.onosproject.yangutils.translator.tojava.ImportInfo;
import org.onosproject.yangutils.utils.UtilConstants;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for generated methods from the file type.
 */
public final class MethodsGeneratorTest {

    public static AttributeInfo testAttr = new AttributeInfo();
    public static YangType<?> attrType = new YangType<>();

    /**
     * Unit test for private constructor.
     *
     * @throws SecurityException if any security violation is observed
     * @throws NoSuchMethodException if when the method is not found
     * @throws IllegalArgumentException if there is illegal argument found
     * @throws InstantiationException if instantiation is provoked for the private constructor
     * @throws IllegalAccessException if instance is provoked or a method is provoked
     * @throws InvocationTargetException when an exception occurs by the method or constructor
     */
    @Test
    public void callPrivateConstructors() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
    InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?>[] classesToConstruct = {MethodsGenerator.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * Unit test case for checking the parse builder and typedef constructor.
     */
    @Test
    public void getParseBuilderInterfaceMethodConstructorTest() {
        ImportInfo forSetter = new ImportInfo();
        attrType.setDataTypeName("binary");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.BINARY);
        attrType.getDataType();
        testAttr.setAttributeName("attributeTest");
        testAttr.setAttributeType(attrType);
        forSetter.setPkgInfo("test1/test3");
        forSetter.setClassInfo("This class contains");
        testAttr.setImportInfo(forSetter);
        String parseBuilderInterface = MethodsGenerator.parseBuilderInterfaceMethodString(testAttr, "newTestName");
        assertThat(parseBuilderInterface.contains("attributeTest") && parseBuilderInterface.contains("newTestName"),
                is(true));
        String parseBuilderInterfaceBuild = MethodsGenerator.parseBuilderInterfaceBuildMethodString("testname7");
        assertThat(parseBuilderInterfaceBuild.contains("Builds object of")
                && parseBuilderInterfaceBuild.contains("testname7"), is(true));
        String stringTypeDef = MethodsGenerator.getTypeDefConstructor(testAttr, "Testname");
    }

    /**
     * Unit test case for checking the values received from constructor, default constructor and build string formation.
     */
    @Test
    public void getValuesTest() {
        String stringConstructor = MethodsGenerator.getConstructorString("testname");
        assertThat(stringConstructor.contains(UtilConstants.JAVA_DOC_CONSTRUCTOR)
                && stringConstructor.contains(UtilConstants.JAVA_DOC_PARAM)
                && stringConstructor.contains(UtilConstants.BUILDER_OBJECT), is(true));
        String stringDefaultConstructor = MethodsGenerator.getDefaultConstructorString("testnameBuilder", "public");
        assertThat(stringDefaultConstructor.contains(UtilConstants.JAVA_DOC_DEFAULT_CONSTRUCTOR)
                && stringDefaultConstructor.contains(UtilConstants.BUILDER)
                && stringDefaultConstructor.contains("testname"), is(true));
        String stringBuild = MethodsGenerator.getBuildString("testname");
        assertThat(stringBuild.contains(UtilConstants.OVERRIDE) && stringBuild.contains(UtilConstants.BUILD)
                && stringBuild.contains(UtilConstants.RETURN), is(true));

    }

    /**
     * Unit test for checking the values received for class getter, class and typedef setters with list data type.
     */
    @Test
    public void getGetterSetterTest() {

        ImportInfo forGetterSetter = new ImportInfo();
        attrType.setDataTypeName("int");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.UINT8);
        attrType.getDataType();
        testAttr.setAttributeName("AttributeTest1");
        testAttr.setAttributeType(attrType);
        forGetterSetter.setPkgInfo(null);
        forGetterSetter.setClassInfo("This class contains");
        testAttr.setImportInfo(forGetterSetter);
        testAttr.setListAttr(true);
        String getterForClass = MethodsGenerator.getGetterForClass(testAttr);
        assertThat(getterForClass.contains(UtilConstants.GET_METHOD_PREFIX) && getterForClass.contains("List<")
                && getterForClass.contains("attributeTest1"), is(true));
        String setterForClass = MethodsGenerator.getSetterForClass(testAttr, "TestThis");
        assertThat(setterForClass.contains(UtilConstants.SET_METHOD_PREFIX) && setterForClass.contains("List<")
                && setterForClass.contains("attributeTest1"), is(true));
        String typeDefSetter = MethodsGenerator.getSetterForTypeDefClass(testAttr);
        assertThat(typeDefSetter.contains(UtilConstants.SET_METHOD_PREFIX) && typeDefSetter.contains("List<")
                && typeDefSetter.contains("attributeTest1") && typeDefSetter.contains("this."), is(true));
    }

    /**
     * Unit test case for checking the parse builder and typedef constructor with list data type.
     */
    @Test
    public void getConstructorWithListTypeTest() {
        ImportInfo forSetter = new ImportInfo();
        attrType.setDataTypeName("binary");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.BINARY);
        attrType.getDataType();
        testAttr.setAttributeName("attributeTest");
        testAttr.setAttributeType(attrType);
        forSetter.setPkgInfo(null);
        forSetter.setClassInfo("This class contains");
        testAttr.setImportInfo(forSetter);
        testAttr.setListAttr(true);
        String parseBuilderInterface = MethodsGenerator.parseBuilderInterfaceMethodString(testAttr, "newTestName");
        assertThat(parseBuilderInterface.contains("attributeTest") && parseBuilderInterface.contains("List<"),
                is(true));
        String parseBuilderInterfaceBuild = MethodsGenerator.parseBuilderInterfaceBuildMethodString("testname7");
        assertThat(parseBuilderInterfaceBuild.contains("Builds object of")
                && parseBuilderInterfaceBuild.contains("testname7"), is(true));
        String stringTypeDef = MethodsGenerator.getTypeDefConstructor(testAttr, "Testname");
        assertThat(stringTypeDef.contains("(List<") && stringTypeDef.contains("Testname")
                && stringTypeDef.contains(UtilConstants.THIS), is(true));
    }
}

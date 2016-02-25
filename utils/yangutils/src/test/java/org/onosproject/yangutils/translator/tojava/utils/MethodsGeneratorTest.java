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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.AttributeInfo;
import org.onosproject.yangutils.translator.tojava.GeneratedMethodTypes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for generated methods from the file type.
 */
public final class MethodsGeneratorTest {

    public static AttributeInfo testAttr = new AttributeInfo();
    public static YangType<?> attrType = new YangType<>();

    /**
     * Unit test for private constructor.
     *
     * @throws SecurityException if any security violation is observed.
     * @throws NoSuchMethodException if when the method is not found.
     * @throws IllegalArgumentException if there is illegal argument found.
     * @throws InstantiationException if instantiation is provoked for the private constructor.
     * @throws IllegalAccessException if instance is provoked or a method is provoked.
     * @throws InvocationTargetException when an exception occurs by the method or constructor.
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
     * Unit test for checking the generated builder class method.
     */
    @Test
    public void getMethodBuilderClassTest() {

        attrType.setDataTypeName("integer");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.INT8);
        attrType.getDataType();
        testAttr.setAttributeName("attributeBuilderClassTest");
        testAttr.setAttributeType(attrType);
        String builderClassMethod = MethodsGenerator.getMethodString(testAttr, GeneratedFileType.BUILDER_CLASS);
        assertThat(builderClassMethod.contains("public Byte getAttributeBuilderClassTest() {"), is(true));
        assertThat(builderClassMethod.contains(
                "public testnameof setAttributeBuilderClassTest(Byte attributeBuilderClassTest) {"), is(true));
    }

    /**
     * Unit test for checking the generated builder interface method.
     */
    @Test
    public void getMethodBuilderInterfaceTest() {

        attrType.setDataTypeName("integer16");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.INT16);
        attrType.getDataType();
        testAttr.setAttributeName("attributeBuilderInterfaceTest");
        testAttr.setAttributeType(attrType);
        String builderInterfaceMethod = MethodsGenerator.getMethodString(testAttr, GeneratedFileType.BUILDER_INTERFACE);
        assertThat(builderInterfaceMethod.contains("Returns the attribute attributeBuilderInterfaceTest.")
                && builderInterfaceMethod.contains("Short getAttributeBuilderInterfaceTest();")
                && builderInterfaceMethod.contains("Returns the builder object of attributeBuilderInterfaceTest.")
                && builderInterfaceMethod
                .contains("Builder setAttributeBuilderInterfaceTest(Short attributeBuilderInterfaceTest);"),
                is(true));
    }

    /**
     * Unit test for checking the generated impl method.
     */
    @Test
    public void getMethodImplTest() {

        attrType.setDataTypeName("integer16");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.INT16);
        attrType.getDataType();
        testAttr.setAttributeName("attributeImplTest");
        testAttr.setAttributeType(attrType);
        String implMethod = MethodsGenerator.getMethodString(testAttr, GeneratedFileType.IMPL);
        assertThat(implMethod.contains("public Short getAttributeImplTest() {")
                && implMethod.contains("return attributeImplTest;"), is(true));
    }

    /**
     * Unit test for checking the generated interface method.
     */
    @Test
    public void getMethodInterfaceTest() {

        attrType.setDataTypeName("binary");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.INT32);
        attrType.getDataType();
        testAttr.setAttributeName("attributeInterfaceTest");
        testAttr.setAttributeType(attrType);
        String interfaceMethod = MethodsGenerator.getMethodString(testAttr, GeneratedFileType.INTERFACE);
        assertThat(interfaceMethod.contains("Returns the attribute attributeInterfaceTest.")
                && interfaceMethod.contains("@return attributeInterfaceTest")
                && interfaceMethod.contains("Int getAttributeInterfaceTest();"), is(true));
    }

    /**
     * Unit test for checking the response for an invalid input.
     */
    @Test
    public void getMethodInvalidTest() {

        attrType.setDataTypeName("decimal64");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.DECIMAL64);
        attrType.getDataType();
        testAttr.setAttributeName("attributeInvalidTest");
        testAttr.setAttributeType(attrType);
        String invalidMethod = MethodsGenerator.getMethodString(testAttr, GeneratedFileType.ALL);
        assertThat(invalidMethod, is(nullValue()));
    }

    /**
     * Unit test for checking the generated construct method info.
     */
    @Test
    public void constructMethodInfoTest() {

        attrType.setDataTypeName("decimal64");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.DECIMAL64);
        attrType.getDataType();
        MethodsGenerator.setBuilderClassName("testnameof");
        String builderClassName = MethodsGenerator.getBuilderClassName();
        assertThat(builderClassName.equals("testnameof"), is(true));
        String implTypenullMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.IMPL, "testname",
                GeneratedMethodTypes.GETTER, null);
        assertThat(implTypenullMethod.contains("public Testname getTestname() {")
                && implTypenullMethod.contains("return testname;"), is(true));
        String implTypeGetterMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.IMPL, "testname",
                GeneratedMethodTypes.GETTER, attrType);
        assertThat(implTypeGetterMethod.contains("public Decimal64 getTestname()")
                && implTypeGetterMethod.contains("return testname;"), is(true));
        String implTypeConstructorMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.IMPL, "testname",
                GeneratedMethodTypes.CONSTRUCTOR, attrType);
        assertThat(implTypeConstructorMethod.contains("public testnameImpl(testnameBuilder testnameObject) {")
                && implTypeConstructorMethod.contains("}"), is(true));
        String implTypeDefaultConstructorMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.IMPL,
                "testname", GeneratedMethodTypes.DEFAULT_CONSTRUCTOR, attrType);
        assertThat(implTypeDefaultConstructorMethod.contains("public testnameImpl() {")
                && implTypeDefaultConstructorMethod.contains("}"), is(true));
        String implTypeSetterMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.IMPL, "testname",
                GeneratedMethodTypes.SETTER, attrType);
        assertThat(implTypeSetterMethod, is(nullValue()));
        String builderInterfaceTypeSetterMethod = MethodsGenerator.constructMethodInfo(
                GeneratedFileType.BUILDER_INTERFACE, "testname2", GeneratedMethodTypes.SETTER, attrType);
        assertThat(builderInterfaceTypeSetterMethod.contains("Builder setTestname2(Decimal64 testname2);"), is(true));
        String builderInterfaceTypeGetterMethod = MethodsGenerator.constructMethodInfo(
                GeneratedFileType.BUILDER_INTERFACE, "testname2", GeneratedMethodTypes.GETTER, attrType);
        assertThat(builderInterfaceTypeGetterMethod.contains("Decimal64 getTestname2();"), is(true));
        String builderInterfaceTypeBuildMethod = MethodsGenerator.constructMethodInfo(
                GeneratedFileType.BUILDER_INTERFACE, "testname2", GeneratedMethodTypes.BUILD, attrType);
        assertThat(builderInterfaceTypeBuildMethod.contains("testname2 build();"), is(true));
        String builderInterfaceTypeConstructorMethod = MethodsGenerator.constructMethodInfo(
                GeneratedFileType.BUILDER_INTERFACE, "testname2", GeneratedMethodTypes.CONSTRUCTOR, attrType);
        assertThat(builderInterfaceTypeConstructorMethod, is(nullValue()));
        String builderClassTypeBuildMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.BUILDER_CLASS,
                "testname2", GeneratedMethodTypes.BUILD, attrType);
        assertThat(builderClassTypeBuildMethod.contains("public testname2 build() {")
                && builderClassTypeBuildMethod.contains("return new testname2Impl(this);"), is(true));
        String builderClassTypeGetterMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.BUILDER_CLASS,
                "testname2", GeneratedMethodTypes.GETTER, attrType);
        assertThat(builderClassTypeGetterMethod.contains("public Decimal64 getTestname2() {")
                && builderClassTypeGetterMethod.contains("return testname2;"), is(true));
        String builderClassTypeSetterMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.BUILDER_CLASS,
                "testname2", GeneratedMethodTypes.SETTER, attrType);
        assertThat(builderClassTypeSetterMethod.contains("public testnameof setTestname2(Decimal64 testname2) {")
                && builderClassTypeSetterMethod.contains("this.testname2 = testname2;"), is(true));
        String builderClassTypeDefaultConstructorMethod = MethodsGenerator.constructMethodInfo(
                GeneratedFileType.BUILDER_CLASS, "testname2", GeneratedMethodTypes.DEFAULT_CONSTRUCTOR, attrType);
        assertThat(builderClassTypeDefaultConstructorMethod.contains("public testname2Builder() {"), is(true));
        String builderClassTypeConstructorMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.BUILDER_CLASS,
                "testname2", GeneratedMethodTypes.CONSTRUCTOR, attrType);
        assertThat(builderClassTypeConstructorMethod, is(nullValue()));
        String invalidMethod = MethodsGenerator.constructMethodInfo(GeneratedFileType.ALL, "testname2",
                GeneratedMethodTypes.CONSTRUCTOR, attrType);
        assertThat(invalidMethod, is(nullValue()));
    }

    /**
     * Unit test for checking the method constructor.
     */
    @Test
    public void getMethodConstructorTest() {

        MethodsGenerator.parseBuilderInterfaceBuildMethodString("testname7");
        attrType.setDataTypeName("binary");
        attrType.getDataTypeName();
        attrType.setDataType(YangDataTypes.BINARY);
        attrType.getDataType();
        testAttr.setAttributeName("attributeTest");
        testAttr.setAttributeType(attrType);
        List<AttributeInfo> settingAttributes = new ArrayList<AttributeInfo>();
        settingAttributes.add(testAttr);
        MethodsGenerator.setAttrInfo(settingAttributes);
        String methodConstructor = MethodsGenerator.constructMethodInfo(GeneratedFileType.IMPL, "testname",
                GeneratedMethodTypes.CONSTRUCTOR, attrType);
        assertThat(
                methodConstructor.contains("public testnameImpl(testnameBuilder testnameObject) {")
                && methodConstructor.contains("this.attributeTest = testnameObject.getAttributeTest();"),
                is(true));
    }

    /**
     * Unit test for checking the values received from constructor, default constructor and build string formation.
     */
    @Test
    public void getValuesTest() {
        String stringConstructor = MethodsGenerator.getConstructorString("testname");
        assertThat(
                stringConstructor.contains("Construct the object of testnameImpl.")
                && stringConstructor.contains("@param testnameObject builder object of  testname")
                && stringConstructor.contains("public testnameImpl(testnameBuilder testnameObject) {"),
                is(true));
        String stringDefaultConstructor = MethodsGenerator.getDefaultConstructorString(GeneratedFileType.BUILDER_CLASS,
                "testname");
        assertThat(stringDefaultConstructor.contains("Default Constructor.")
                && stringDefaultConstructor.contains("public testnameBuilder() {")
                && stringDefaultConstructor.contains("}"), is(true));
        String stringBuild = MethodsGenerator.getBuildString("testname");
        assertThat(
                stringBuild.contains("public testname build() {")
                && stringBuild.contains("return new testnameImpl(this);") && stringBuild.contains("}"),
                is(true));
    }
}
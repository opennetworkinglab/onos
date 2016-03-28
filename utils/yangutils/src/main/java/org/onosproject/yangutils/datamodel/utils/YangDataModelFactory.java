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
package org.onosproject.yangutils.datamodel.utils;

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.datamodel.YangNotification;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaAugment;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaCase;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaChoice;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaContainer;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaGrouping;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaList;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModule;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaSubModule;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaTypeDef;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaUses;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaNotification;

/**
 * Factory to create data model objects based on the target file type.
 */
public final class YangDataModelFactory {

    /**
     * Utility class, hence private to prevent creating objects.
     */
    private YangDataModelFactory() {
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangModule getYangModuleNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaModule();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangAugment getYangAugmentNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaAugment();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangCase getYangCaseNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaCase();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangChoice getYangChoiceNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaChoice();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangContainer getYangContainerNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaContainer();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangGrouping getYangGroupingNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaGrouping();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangList getYangListNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaList();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangSubModule getYangSubModuleNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaSubModule();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangTypeDef getYangTypeDefNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaTypeDef();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangUses getYangUsesNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaUses();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }

    /**
     * Based on the target language generate the inherited data model node.
     *
     * @param targetLanguage target language in which YANG mapping needs to be
     *            generated
     * @return the corresponding inherited node based on the target language
     */
    public static YangNotification getYangNotificationNode(GeneratedLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA_GENERATION: {
                return new YangJavaNotification();
            }
            default: {
                throw new RuntimeException("Only YANG to Java is supported.");
            }
        }
    }
}

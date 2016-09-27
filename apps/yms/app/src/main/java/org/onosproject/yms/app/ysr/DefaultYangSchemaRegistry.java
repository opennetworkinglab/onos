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

import org.onosproject.event.ListenerService;
import org.onosproject.yangutils.datamodel.RpcNotificationContainer;
import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yms.app.ynh.YangNotificationExtendedService;
import org.onosproject.yms.ysr.YangModuleIdentifier;
import org.onosproject.yms.ysr.YangModuleInformation;
import org.onosproject.yms.ysr.YangModuleLibrary;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.parseJarFile;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.OP_PARAM;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.osgi.framework.FrameworkUtil.getBundle;


/**
 * Representation of default YANG schema registry. Yang schema registry
 * provides interface to an application to register its YANG schema
 * with YMS. It provides YANG schema nodes to YDT, YNB and YSB.
 */
public class DefaultYangSchemaRegistry
        implements YangSchemaRegistry {

    private static final String SYSTEM = File.separator + "system" +
            File.separator;
    private static final String MAVEN = "mvn:";
    private static final String HYPHEN = "-";
    private static final String DELIMITER = ".";
    private static final String SERVICE = "Service";
    private static final String JAR = ".jar";
    private static final String USER_DIRECTORY = "user.dir";
    private static final String SLASH = File.separator;
    private static final String AT = "@";
    private static final String DATE_FORMAT = "yyyy-mm-dd";
    private static final Logger log =
            LoggerFactory.getLogger(DefaultYangSchemaRegistry.class);

    /*
     * Map for storing app objects.
     */
    private final ConcurrentMap<String, YsrAppContext> appObjectStore;

    /*
     * Map for storing YANG schema nodes.
     */
    private final ConcurrentMap<String, YsrAppContext> yangSchemaStore;

    /*
     * Map for storing YANG schema nodes with respect to root's generated
     * interface file name.
     */
    private final ConcurrentMap<String, YsrAppContext>
            yangSchemaStoreForRootInterface;

    /*
     * Map for storing YANG schema nodes root's generated op param file name.
     */
    private final ConcurrentMap<String, YsrAppContext>
            yangSchemaStoreForRootOpParam;

    /*
     * Map for storing YANG schema nodes with respect to notifications.
     */
    private final ConcurrentMap<String, YsrAppContext>
            yangRootSchemaStoreForNotification;

    /*
     * Map for storing registered classes.
     */
    private final ConcurrentMap<String, Class<?>> registerClassStore;

    /*
     * Map for storing YANG file details.
     */
    private final ConcurrentMap<YangModuleIdentifier, String> yangFileStore;

    /*
     * Context of application which is registering with YMS.
     */
    private YsrAppContext ysrAppContext;

    /*
     * Context of application which is registering with YMS with multiple
     * revision.
     */
    private YsrAppContext ysrContextForSchemaStore;

    /*
     * Context of application which is registering with YMS with multiple
     * manager object.
     */
    private YsrAppContext ysrContextForAppStore;

    /*
     * Class loader of service application.
     */
    private ClassLoader classLoader;

    /**
     * YANG module library.
     */
    private final YangModuleLibrary library;

    /**
     * Creates an instance of default YANG schema registry.
     *
     * @param moduleId module set id of YSR module library
     */
    public DefaultYangSchemaRegistry(String moduleId) {
        appObjectStore = new ConcurrentHashMap<>();
        yangSchemaStore = new ConcurrentHashMap<>();
        yangSchemaStoreForRootInterface = new ConcurrentHashMap<>();
        yangSchemaStoreForRootOpParam = new ConcurrentHashMap<>();
        yangRootSchemaStoreForNotification = new ConcurrentHashMap<>();
        registerClassStore = new ConcurrentHashMap<>();
        yangFileStore = new ConcurrentHashMap<>();
        library = new DefaultYangModuleLibrary(moduleId);
    }


    @Override
    public void registerApplication(Object appObject, Class<?> serviceClass,
                                    YangNotificationExtendedService
                                            notificationExtendedService) {

        BundleContext bundleContext = getBundle(serviceClass)
                .getBundleContext();
        String jarPath = getJarPathFromBundleLocation(
                bundleContext.getBundle().getLocation(),
                bundleContext.getProperty(USER_DIRECTORY));
        // process application registration.
        processRegistration(serviceClass, appObject, jarPath);
        //process notification registration.
        processNotificationRegistration(serviceClass, appObject,
                                        notificationExtendedService);
    }

    /**
     * Process application registration.
     *
     * @param serviceClass service class
     * @param appObject    application object
     * @param jarPath      jar path
     */
    void processRegistration(Class<?> serviceClass, Object appObject,
                             String jarPath) {

        // set class loader for service class.
        setClassLoader(serviceClass.getClassLoader());

        //Check if service should be registered?
        if (appObject != null) {
            verifyApplicationRegistration(appObject, serviceClass);
        }
        //Add app class to registered service store.
        if (!registerClassStore.containsKey(serviceClass.getName())) {
            updateServiceClass(serviceClass);
        }

        // process storing operations.
        if (!verifyIfApplicationAlreadyRegistered(serviceClass)) {
            List<YangNode> curNodes =
                    processJarParsingOperations(jarPath);

            if (curNodes != null) {
                for (YangNode schemaNode : curNodes) {
                    //Process application context for registrations.
                    processApplicationContext(schemaNode);
                    //Update YANG file store.
                    updateYangFileStore(schemaNode, jarPath);
                    //Process module library operation for current node list.
                    processModuleLibrary(schemaNode);
                }
                //Set jar path for app context.
                ysrAppContext.jarPath(jarPath);
                ysrContextForSchemaStore.jarPath(jarPath);
                ysrContextForAppStore.jarPath(jarPath);
            }
        }

        //Verifies if object is updated for app store.
        updateApplicationObject(appObject, serviceClass);
    }

    /**
     * Verifies if service class should be registered or not.
     *
     * @param appObject application object
     * @param appClass  application class
     */
    private void verifyApplicationRegistration(Object appObject,
                                               Class<?> appClass) {
        Class<?> managerClass = appObject.getClass();
        Class<?>[] services = managerClass.getInterfaces();
        List<Class<?>> classes = new ArrayList<>();
        Collections.addAll(classes, services);
        if (!classes.contains(appClass)) {
            throw new RuntimeException("service class " + appClass.getName() +
                                               "is not being implemented by " +
                                               managerClass.getName());
        }
    }

    /**
     * Verifies if application is already registered with YMS.
     *
     * @param appClass application class
     * @return true if application already registered
     */
    private boolean verifyIfApplicationAlreadyRegistered(Class<?> appClass) {
        String simpleName = appClass.getSimpleName();
        String appName = appClass.getName();
        if (!appObjectStore.containsKey(appName)) {
            if (simpleName.contains(OP_PARAM)) {
                return yangSchemaStoreForRootOpParam
                        .containsKey(appName);
            } else {
                return yangSchemaStoreForRootInterface
                        .containsKey(appName);
            }
        }
        return true;
    }

    /**
     * Verifies if service is being implemented by some new object.
     *
     * @param appObject application's object
     * @param appClass  application's class
     */
    private void updateApplicationObject(Object appObject, Class<?> appClass) {
        YsrAppContext appContext =
                appObjectStore.get(appClass.getName());
        if (appContext != null) {
            YangSchemaNode schemaNode = appContext.curNode();
            String name = getInterfaceClassName(schemaNode);
            if (appContext.appObject() == null) {
                //update in application store.
                appContext.appObject(appObject);
                //Update app object for schema store for root interface.
                appContext = yangSchemaStoreForRootInterface.get(name);
                if (appContext != null) {
                    appContext.appObject(appObject);
                }
                // Update app object for schema store for root op param
                appContext = yangSchemaStoreForRootOpParam.get(name + OP_PARAM);
                if (appContext != null) {
                    appContext.appObject(appObject);
                }
            }
        }
    }

    @Override
    public void unRegisterApplication(Object managerObject,
                                      Class<?> serviceClass) {
        YangSchemaNode curNode = null;
        String serviceName = serviceClass.getName();

        //Check if service should be unregistered?
        if (managerObject != null) {
            verifyApplicationRegistration(managerObject, serviceClass);
        }
        //Remove registered class from store.
        registerClassStore.remove(serviceName);

        //check if service is in app store.
        if (appObjectStore.containsKey(serviceName)) {
            curNode = retrieveNodeForUnregister(serviceName, appObjectStore,
                                                managerObject);
        } else if (yangSchemaStoreForRootInterface.containsKey(serviceName)) {
            //check if service is in interface store.
            curNode = retrieveNodeForUnregister(serviceName,
                                                yangSchemaStoreForRootInterface,
                                                managerObject);
        } else if (yangSchemaStoreForRootOpParam.containsKey(serviceName)) {
            //check if service is in op param store.
            curNode = retrieveNodeForUnregister(serviceName,
                                                yangSchemaStoreForRootOpParam,
                                                managerObject);
        }
        if (curNode != null) {
            String javaName = getInterfaceClassName(curNode);
            removeFromYangSchemaStore(curNode);
            removeFromYangNotificationStore(curNode);
            removeFromAppSchemaStore(serviceName);
            removeFromYangSchemaNodeForRootInterface(javaName);
            removeFromYangSchemaNodeForRootOpParam(javaName);
            removeYangFileInfoFromStore(curNode);
            log.info(" service {} is unregistered.",
                     serviceClass.getSimpleName());
        } else {
            throw new RuntimeException(serviceClass.getSimpleName() +
                                               " service was not registered.");
        }
    }

    @Override
    public Object getRegisteredApplication(YangSchemaNode schemaNode) {
        if (schemaNode != null) {
            String name = getInterfaceClassName(schemaNode);
            if (yangSchemaStoreForRootInterface.containsKey(name)) {
                return yangSchemaStoreForRootInterface.get(name)
                        .appObject();
            }
            log.error("{} not found.", name);
        }
        return null;
    }

    @Override
    public YangSchemaNode getYangSchemaNodeUsingSchemaName(String schemaName) {
        return getSchemaNodeUsingSchemaNameWithRev(schemaName);
    }

    @Override
    public YangSchemaNode getYangSchemaNodeUsingAppName(String appName) {
        YsrAppContext appContext = appObjectStore.get(appName);
        if (appContext != null) {
            return appContext.curNode();
        }
        log.error("{} not found.", appName);
        return null;
    }

    @Override
    public YangSchemaNode
    getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(String name) {
        YsrAppContext appContext = yangSchemaStoreForRootInterface.get(name);
        if (appContext != null) {
            return appContext.curNode();
        }
        log.error("{} not found.", name);
        return null;
    }

    @Override
    public YangSchemaNode getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
            String name) {
        YsrAppContext appContext = yangSchemaStoreForRootOpParam.get(name);
        if (appContext != null) {
            return appContext.curNode();
        }
        log.error("{} not found.", name);
        return null;
    }

    @Override
    public YangSchemaNode getRootYangSchemaNodeForNotification(String name) {
        YsrAppContext appContext = yangRootSchemaStoreForNotification.get(name);
        if (appContext != null) {
            return appContext.curNode();
        }
        log.error("{} not found.", name);
        return null;
    }

    @Override
    public Class<?> getRegisteredClass(YangSchemaNode schemaNode,
                                       String appName) {
        String interfaceName = getInterfaceClassName(schemaNode);
        String serviceName = getServiceName(schemaNode);
        String defaultClass;
        if (schemaNode instanceof RpcNotificationContainer) {
            defaultClass = getOpParamClassName(schemaNode);
        } else {
            defaultClass = getDefaultClassName(schemaNode);
        }
        //If application class is registered.
        if (registerClassStore.containsKey(appName)) {
            return registerClassStore.get(appName);
        } else if (registerClassStore.containsKey(interfaceName)) {
            //If interface class is registered.
            return registerClassStore.get(interfaceName);
        } else if (registerClassStore.containsKey(serviceName)) {
            //If service class is registered.
            return registerClassStore.get(serviceName);
        } else if (registerClassStore.containsKey(defaultClass)) {
            //If default class is registered.
            return registerClassStore.get(defaultClass);
        }
        return null;
    }

    /**
     * Returns YANG file path for module identifier.
     *
     * @param moduleIdentifier module identifier
     * @return YANG file path for module identifier
     */
    public String getYangFile(YangModuleIdentifier moduleIdentifier) {
        if (yangFileStore.containsKey(moduleIdentifier)) {
            return yangFileStore.get(moduleIdentifier);
        }
        log.error("YANG files for corresponding module identifier {} not " +
                          "found", moduleIdentifier);
        return null;
    }

    /**
     * Updates service class store.
     *
     * @param serviceClass service class
     */
    void updateServiceClass(Class<?> serviceClass) {
        registerClassStore.put(serviceClass.getName(), serviceClass);
    }

    /**
     * Updates application object store.
     *
     * @param appName application name
     */
    private void updateAppObjectStore(String appName) {
        if (verifyClassExistence(appName)) {
            appObjectStore.put(appName, ysrContextForAppStore);
        }
    }

    /**
     * Updates YANG schema object store.
     *
     * @param schemaNode application's schema node
     */
    private void updateYangSchemaStore(YangSchemaNode schemaNode) {
        addSchemaNodeUsingSchemaNameWithRev(schemaNode);
    }

    /**
     * Updates YANG schema notification object store.
     *
     * @param name application's notification name
     */
    private void updateYangNotificationStore(String name) {
        if (verifyClassExistence(name)) {
            yangRootSchemaStoreForNotification.put(name, ysrAppContext);
        }
    }

    /**
     * Updates YANG schema object store for root interface file name.
     *
     * @param name name of generated interface file for root
     *             node
     */
    private void updateYangSchemaForRootInterfaceFileNameStore(String name) {
        if (verifyClassExistence(name)) {
            yangSchemaStoreForRootInterface.put(name, ysrAppContext);
        }
    }

    /**
     * Updates YANG schema object store  for root op param file name.
     *
     * @param name name of generated op param file for root node
     */
    private void updateYangSchemaForRootOpParamFileNameStore(String name) {
        if (verifyClassExistence(name)) {
            yangSchemaStoreForRootOpParam.put(name, ysrAppContext);
        }
    }

    /**
     * Updates yang file store for YANG node.
     *
     * @param node    YANG node
     * @param jarPath jar file path
     */
    private void updateYangFileStore(YangNode node, String jarPath) {
       //FIXME: fix when yang tools support for file name.
       //yangFileStore.put(getModuleIdentifier(node),
       //                   getYangFilePath(jarPath, node.getFileName()));
    }

    /**
     * Returns yang file path.
     *
     * @param jarPath          jar path
     * @param metaDataFileName name of yang file from metadata
     * @return yang file path
     */
    private String getYangFilePath(String jarPath, String metaDataFileName) {
       String[] metaData = metaDataFileName.split(SLASH);
       return jarPath + SLASH + metaData[metaData.length - 1];
    }

    /**
     * Process jar file for fetching YANG nodes.
     *
     * @param path jar file path
     * @return YANG schema nodes
     */
    private List<YangNode> processJarParsingOperations(String path) {
        //Deserialize data model and get the YANG node set.
        try {
            return parseJarFile(path + JAR, path);
        } catch (IOException e) {
            log.error(" failed to parse the jar file in path {} : {} ", path,
                      e.getMessage());
        }
        return null;
    }

    /**
     * Process an application an updates the maps for YANG schema registry.
     *
     * @param appNode application YANG schema nodes
     */
    void processApplicationContext(YangSchemaNode appNode) {

        String appName = getInterfaceClassName(appNode);

        //Create a new instance of ysr app context for each node.
        ysrAppContext = new YsrAppContext();
        ysrContextForSchemaStore = new YsrAppContext();
        ysrContextForAppStore = new YsrAppContext();

        //add cur node to app context.
        ysrAppContext.curNode(appNode);
        ysrContextForAppStore.curNode(appNode);

        //Updates maps wih schema nodes.
        updateAppObjectStore(getServiceName(appNode));

        // Updates schema store.
        updateYangSchemaStore(appNode);
        // update interface store.
        updateYangSchemaForRootInterfaceFileNameStore(appName);
        //update op param store.
        updateYangSchemaForRootOpParamFileNameStore(getOpParamClassName(appNode));
        //Checks if notification is present then update notification store map.
        String eventSubject = null;
        try {
            if (appNode.isNotificationPresent()) {
                eventSubject = getEventClassName(appNode);
            }
        } catch (DataModelException e) {
            log.error("failed to search notification from schema map : {}",
                      e.getLocalizedMessage());
        }
        if (eventSubject != null) {
            updateYangNotificationStore(eventSubject);
        }
        log.info("successfully registered this application {}{}", appName,
                 SERVICE);

    }

    /**
     * Returns jar path from bundle mvnLocationPath.
     *
     * @param mvnLocationPath mvnLocationPath of bundle
     * @return path of jar
     */
    private String getJarPathFromBundleLocation(String mvnLocationPath,
                                                String currentDirectory) {
        String path = currentDirectory + SYSTEM;
        String[] strArray = mvnLocationPath.split(MAVEN);
        String[] split = strArray[1].split(File.separator);
        String[] groupId = split[0].split(Pattern.quote(DELIMITER));

        return path + groupId[0] + SLASH + groupId[1] + SLASH + split[1] +
                SLASH + split[2] + SLASH + split[1] + HYPHEN + split[2];
    }

    /**
     * Returns de-serializes YANG data-model nodes.
     *
     * @param serializedFileInfo serialized File Info
     * @return de-serializes YANG data-model nodes
     */
    Set<YangSchemaNode> deSerializeDataModel(String serializedFileInfo) {

        Set<YangSchemaNode> nodes = new HashSet<>();
        Object readValue;
        try {
            FileInputStream fileInputStream =
                    new FileInputStream(serializedFileInfo);
            ObjectInputStream objectInputStream =
                    new ObjectInputStream(fileInputStream);
            readValue = objectInputStream.readObject();
            if (readValue instanceof Set<?>) {
                for (Object obj : (Set<?>) readValue) {
                    if (obj instanceof YangSchemaNode) {
                        nodes.add((YangSchemaNode) obj);
                    } else {
                        throw new RuntimeException(
                                "deserialize object is not an instance of " +
                                        "YANG schema node" + obj);
                    }
                }
            } else {
                throw new RuntimeException(
                        "deserialize object is not an instance of set of" +
                                "YANG schema node" + readValue);
            }
            objectInputStream.close();
            fileInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            log.error(" {} not found.: {}", serializedFileInfo,
                      e.getLocalizedMessage());
        }

        return nodes;
    }

    /**
     * Returns ysr app context.
     *
     * @return ysr app context
     */
    YsrAppContext ysrAppContext() {
        return ysrAppContext;
    }

    /**
     * Returns schema node based on the revision.
     *
     * @param name name of the schema node
     * @return schema node based on the revision
     */
    private YangSchemaNode getSchemaNodeUsingSchemaNameWithRev(String name) {
        YsrAppContext appContext;
        YangSchemaNode schemaNode;
        if (name.contains(AT)) {
            String[] revArray = name.split(AT);
            appContext = yangSchemaStore.get(revArray[0]);
            schemaNode = appContext.getSchemaNodeForRevisionStore(name);
            if (schemaNode != null) {
                return schemaNode;
            }
            return appContext.curNode();
        }
        if (yangSchemaStore.containsKey(name)) {
            appContext = yangSchemaStore.get(name);
            if (appContext != null) {
                Iterator<YangSchemaNode> iterator = appContext
                        .getYangSchemaNodeForRevisionStore().values()
                        .iterator();
                if (iterator.hasNext()) {
                    return appContext.getYangSchemaNodeForRevisionStore()
                            .values().iterator().next();
                } else {
                    return null;
                }
            }
        }
        log.error("{} not found.", name);
        return null;
    }

    /**
     * Adds schema node when different revision of node has received.
     *
     * @param schemaNode schema node
     */
    private void addSchemaNodeUsingSchemaNameWithRev(
            YangSchemaNode schemaNode) {

        String date = getDateInStringFormat(schemaNode);
        String name = schemaNode.getName();
        if (!date.equals(EMPTY_STRING)) {
            name = name + AT + date;
        }
        //check if already present.
        if (!yangSchemaStore.containsKey(schemaNode.getName())) {
            ysrContextForSchemaStore.curNode(schemaNode);
            //if revision is not present no need to add in revision store.
            ysrContextForSchemaStore
                    .addSchemaNodeWithRevisionStore(name, schemaNode);
            yangSchemaStore.put(schemaNode.getName(),
                                ysrContextForSchemaStore);
        } else {
            YsrAppContext appContext =
                    yangSchemaStore.get(schemaNode.getName());
            appContext.addSchemaNodeWithRevisionStore(name, schemaNode);
            appContext.curNode(schemaNode);
        }
    }

    /**
     * Returns date in string format.
     *
     * @param schemaNode schema node
     * @return date in string format
     */
    String getDateInStringFormat(YangSchemaNode schemaNode) {
        if (schemaNode != null) {
            if (((YangNode) schemaNode).getRevision() != null) {
                return new SimpleDateFormat(DATE_FORMAT)
                        .format(((YangNode) schemaNode).getRevision()
                                        .getRevDate());
            }
        }
        return EMPTY_STRING;
    }

    /**
     * Removes schema node from schema map.
     *
     * @param removableNode schema node which needs to be removed
     */
    private void removeSchemaNode(YangSchemaNode removableNode) {

        String name = removableNode.getName();
        String date = getDateInStringFormat(removableNode);

        if (!date.isEmpty()) {
            name = removableNode.getName() + AT +
                    getDateInStringFormat(removableNode);
        }
        YsrAppContext appContext = yangSchemaStore
                .get(removableNode.getName());
        if (appContext != null &&
                !appContext.getYangSchemaNodeForRevisionStore().isEmpty() &&
                appContext.getYangSchemaNodeForRevisionStore().size() != 1) {
            appContext.removeSchemaNodeForRevisionStore(name);
        } else {
            yangSchemaStore.remove(removableNode.getName());
        }
    }

    /**
     * Verifies if the manager object is already registered with notification
     * handler.
     *
     * @param serviceClass service class
     * @return true if the manager object is already registered with
     * notification handler
     */
    boolean verifyNotificationObject(Class<?> serviceClass) {
        YangSchemaNode schemaNode = null;
        String serviceName = serviceClass.getName();
        if (appObjectStore.containsKey(serviceName)) {
            schemaNode = getYangSchemaNodeUsingAppName(serviceName);
        } else if (yangSchemaStoreForRootInterface.containsKey(serviceName)) {
            schemaNode =
                    getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                            serviceName);
        } else if (yangSchemaStoreForRootOpParam.containsKey(serviceName)) {
            schemaNode = getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
                    serviceName);
        }

        if (schemaNode != null) {
            String name = getEventClassName(schemaNode);

            YsrAppContext appContext =
                    yangRootSchemaStoreForNotification.get(name);
            if (appContext != null && !appContext.isNotificationRegistered()) {
                appContext.setNotificationRegistered(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns schema node's generated interface class name.
     *
     * @param schemaNode schema node
     * @return schema node's generated interface class name
     */
    private String getInterfaceClassName(YangSchemaNode schemaNode) {
        return schemaNode.getJavaPackage() + PERIOD +
                getCapitalCase(schemaNode.getJavaClassNameOrBuiltInType());
    }

    /**
     * Returns schema node's generated op param class name.
     *
     * @param schemaNode schema node
     * @return schema node's generated op param class name
     */
    private String getOpParamClassName(YangSchemaNode schemaNode) {
        return getInterfaceClassName(schemaNode) + OP_PARAM;
    }

    /**
     * Returns schema node's generated op param class name.
     *
     * @param schemaNode schema node
     * @return schema node's generated op param class name
     */
    private String getDefaultClassName(YangSchemaNode schemaNode) {
        return schemaNode.getJavaPackage() + PERIOD + getCapitalCase(DEFAULT) +
                getCapitalCase(schemaNode.getJavaClassNameOrBuiltInType());
    }

    /**
     * Returns schema node's generated event class name.
     *
     * @param schemaNode schema node
     * @return schema node's generated event class name
     */
    private String getEventClassName(YangSchemaNode schemaNode) {
        return getInterfaceClassName(schemaNode).toLowerCase() + PERIOD +
                getCapitalCase(schemaNode.getJavaClassNameOrBuiltInType()) +
                EVENT_STRING;
    }

    /**
     * Returns schema node's generated service class name.
     *
     * @param schemaNode schema node
     * @return schema node's generated service class name
     */
    private String getServiceName(YangSchemaNode schemaNode) {
        return getInterfaceClassName(schemaNode) + SERVICE;
    }

    /**
     * Returns YSR application context for schema map.
     *
     * @return YSR application context for schema map
     */
    YsrAppContext ysrContextForSchemaStore() {
        return ysrContextForSchemaStore;
    }

    /**
     * Sets YSR application context for schema map.
     *
     * @param context YSR application context for
     *                schema map
     */
    void ysrContextForSchemaStore(YsrAppContext context) {
        ysrContextForSchemaStore = context;
    }

    /**
     * Returns YSR app context for application store.
     *
     * @return YSR app context for application store
     */
    YsrAppContext ysrContextForAppStore() {
        return ysrContextForAppStore;
    }

    /**
     * Retrieves schema node from the store and deletes jar file path.
     *
     * @param appName   application name
     * @param store     YSR stores
     * @param appObject applications object
     * @return schema node from the store
     */
    private YangSchemaNode retrieveNodeForUnregister(
            String appName,
            ConcurrentMap<String, YsrAppContext> store, Object appObject) {

        YsrAppContext curContext = store.get(appName);
        boolean isValidObject;
        if (curContext != null) {
            isValidObject = verifyAppObject(appObject, curContext.appObject());
            if (isValidObject) {
                YangSchemaNode curNode = curContext.curNode();
                //Delete all the generated ysr information in application's
                // package.
                removeYsrGeneratedTemporaryResources(curContext.jarPath(),
                                                     appName);
                return curNode;
            }
        }
        return null;
    }

    /**
     * Verifies the application object which needs to be unregistered.
     *
     * @param appObject current received application object
     * @param context   stored application object
     * @return true if objects are equal
     */
    private boolean verifyAppObject(Object appObject, Object context) {
        if (appObject != null && context != null) {
            return appObject.equals(context);
        }
        return appObject == null && context == null;
    }

    /**
     * Removes YSR generated temporary resources.
     *
     * @param rscPath resource path
     * @param appName application name
     */
    private void removeYsrGeneratedTemporaryResources(String rscPath,
                                                      String appName) {
        if (rscPath != null) {
            File jarPath = new File(rscPath);
            if (jarPath.exists()) {
                try {
                    deleteDirectory(jarPath);
                } catch (IOException e) {
                    log.error("failed to delete ysr resources for {} : {}",
                              appName, e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Removes from YANG schema store.
     *
     * @param curNode schema node
     */
    private void removeFromYangSchemaStore(YangSchemaNode curNode) {
        removeSchemaNode(curNode);
    }

    /**
     * Removes from YANG schema  notification store.
     *
     * @param curNode schema node
     */
    private void removeFromYangNotificationStore(YangSchemaNode curNode) {
        yangRootSchemaStoreForNotification
                .remove(getEventClassName(curNode));
    }

    /**
     * Removes from app store.
     *
     * @param appName application name
     */
    private void removeFromAppSchemaStore(String appName) {
        appObjectStore.remove(appName);
    }

    /**
     * Removes from interface store.
     *
     * @param appName application name
     */
    private void removeFromYangSchemaNodeForRootInterface(String appName) {
        yangSchemaStoreForRootInterface.remove(appName);
    }

    /**
     * Removes from op param store.
     *
     * @param appName application name
     */
    private void removeFromYangSchemaNodeForRootOpParam(String appName) {
        yangSchemaStoreForRootOpParam.remove(appName + OP_PARAM);
    }

    /**
     * Removes YANG file information from file store.
     *
     * @param schemaNode schema node
     */
    private void removeYangFileInfoFromStore(YangSchemaNode schemaNode) {
        yangFileStore.remove(getModuleIdentifier(schemaNode));
    }

    /**
     * Verifies if class with given name exists.
     *
     * @param appName application name
     * @return true if class exists
     */
    boolean verifyClassExistence(String appName) {
        try {
            classLoader.loadClass(appName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Process notification registration for manager class object.
     *
     * @param yangService yang service
     * @param yangManager yang manager
     * @param ynhService  notification extended service
     */
    private void processNotificationRegistration(
            Class<?> yangService, Object yangManager,
            YangNotificationExtendedService ynhService) {

        if (yangManager != null && yangManager instanceof ListenerService) {
            if (verifyNotificationObject(yangService)) {
                ynhService.registerAsListener(
                        (ListenerService) yangManager);
            }
        }
    }

    /**
     * Clears database for YSR.
     */
    public void flushYsrData() {
        appObjectStore.clear();
        yangSchemaStore.clear();
        yangRootSchemaStoreForNotification.clear();
        yangSchemaStoreForRootOpParam.clear();
        yangSchemaStoreForRootInterface.clear();
        registerClassStore.clear();
        yangFileStore.clear();
    }

    /**
     * Sets class loader of registered class.
     *
     * @param classLoader class loader of registered class
     */
    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Process module library for a registered service.
     *
     * @param node YANG schema nodes
     */
    private void processModuleLibrary(YangNode node) {
        YangModuleInformation moduleInformation =
                new DefaultYangModuleInformation(getModuleIdentifier(node),
                                                 node.getNameSpace());
        addSubModuleIdentifier(node, (
                DefaultYangModuleInformation) moduleInformation);
        //TODO: add feature list to module information.
        ((DefaultYangModuleLibrary) library)
                .addModuleInformation(moduleInformation);
    }

    /**
     * Adds sub module identifier.
     *
     * @param node        schema node
     * @param information module information
     */
    private void addSubModuleIdentifier(
            YangSchemaNode node, DefaultYangModuleInformation information) {
        List<YangInclude> includeList = new ArrayList<>();
        if (node instanceof YangModule) {
            includeList = ((YangModule) node).getIncludeList();
        } else if (node instanceof YangSubModule) {
            includeList = ((YangSubModule) node).getIncludeList();
        }
        for (YangInclude include : includeList) {
            information.addSubModuleIdentifiers(getModuleIdentifier(
                    include.getIncludedNode()));
        }
    }

    /**
     * Returns module identifier for schema node.
     *
     * @param schemaNode schema node
     * @return module identifier for schema node
     */
    private YangModuleIdentifier getModuleIdentifier(
            YangSchemaNode schemaNode) {
        return new DefaultYangModuleIdentifier(
                schemaNode.getName(), getDateInStringFormat(schemaNode));
    }

    /**
     * Returns module library.
     *
     * @return module library
     */
    public YangModuleLibrary getLibrary() {
        return library;
    }

}

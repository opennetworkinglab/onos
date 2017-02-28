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

import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yms.ysr.YangModuleIdentifier;
import org.onosproject.yms.ysr.YangModuleInformation;
import org.onosproject.yms.ysr.YangModuleLibrary;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static java.util.Collections.sort;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.parseJarFile;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.HYPHEN;
import static org.onosproject.yangutils.utils.UtilConstants.OP_PARAM;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.osgi.framework.FrameworkUtil.getBundle;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Representation of default YANG schema registry. Yang schema registry
 * provides interface to an application to register its YANG schema
 * with YMS. It provides YANG schema nodes to YDT, YNB and YSB.
 */
public class DefaultYangSchemaRegistry implements YangSchemaRegistry {

    private static final String SYSTEM = SLASH + "system" + SLASH;
    private static final String MAVEN = "mvn:";
    private static final String JAR = ".jar";
    private static final String USER_DIRECTORY = "user.dir";
    private static final String AT = "@";
    private static final String DATE_FORMAT = "yyyy-mm-dd";
    private static final String ONOS = "org.onosproject";
    private static final Logger log = getLogger(DefaultYangSchemaRegistry.class);

    /*
     * Map for storing app objects.
     */
    private final ConcurrentMap<String, Object> appObjectStore;

    /*
     * Map for storing YANG schema nodes.
     */
    private final ConcurrentMap<String, ConcurrentMap<String, YangSchemaNode>>
            yangSchemaStore;

    /*
     * Map for storing YANG schema nodes with respect to root's generated
     * interface file name.
     */
    private final ConcurrentMap<String, YangSchemaNode> interfaceNameKeyStore;

    /*
     * Map for storing YANG schema nodes root's generated op param file name.
     */
    private final ConcurrentMap<String, YangSchemaNode> opParamNameKeyStore;

    /*
     * Map for storing YANG schema nodes with respect to notifications.
     */
    private final ConcurrentMap<String, YangSchemaNode> eventNameKeyStore;

    /*
     * Map for storing YANG schema nodes with respect to app name.
     */
    private final ConcurrentMap<String, YangSchemaNode> appNameKeyStore;

    /*
     * Map for storing registered classes.
     */
    private final ConcurrentMap<String, Class<?>> registerClassStore;

    /*
     * Map for storing YANG file details.
     */
    private final ConcurrentMap<YangModuleIdentifier, String> yangFileStore;

    /**
     * Map for storing schema nodes with respect to namespace.
     */
    private final ConcurrentMap<String, YangSchemaNode> nameSpaceSchemaStore;

    private final ConcurrentMap<Object, Boolean> ynhRegistrationStore;
    private final ConcurrentMap<String, String> jarPathStore;

    /**
     * Creates an instance of default YANG schema registry.
     */
    public DefaultYangSchemaRegistry() {
        appObjectStore = new ConcurrentHashMap<>();
        yangSchemaStore = new ConcurrentHashMap<>();
        interfaceNameKeyStore = new ConcurrentHashMap<>();
        opParamNameKeyStore = new ConcurrentHashMap<>();
        eventNameKeyStore = new ConcurrentHashMap<>();
        registerClassStore = new ConcurrentHashMap<>();
        yangFileStore = new ConcurrentHashMap<>();
        appNameKeyStore = new ConcurrentHashMap<>();
        ynhRegistrationStore = new ConcurrentHashMap<>();
        jarPathStore = new ConcurrentHashMap<>();
        nameSpaceSchemaStore = new ConcurrentHashMap<>();
    }


    @Override
    public void registerApplication(Object appObject, Class<?> serviceClass) {
        synchronized (DefaultYangSchemaRegistry.class) {
            doPreProcessing(serviceClass, appObject);
            if (!verifyIfApplicationAlreadyRegistered(serviceClass)) {
                BundleContext context = getBundle(serviceClass).getBundleContext();
                if (context != null) {
                    Bundle[] bundles = context.getBundles();
                    Bundle bundle;
                    int len = bundles.length;
                    List<YangNode> curNodes;
                    String jarPath;
                    for (int i = len - 1; i >= 0; i--) {
                        bundle = bundles[i];
                        if (bundle.getSymbolicName().contains(ONOS)) {
                            jarPath = getJarPathFromBundleLocation(
                                    bundle.getLocation(), context.getProperty(USER_DIRECTORY));
                            curNodes = processJarParsingOperations(jarPath);
                            // process application registration.
                            if (curNodes != null && !curNodes.isEmpty()) {
                                jarPathStore.put(serviceClass.getName(), jarPath);
                                processRegistration(serviceClass, jarPath,
                                                    curNodes, appObject, false);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void unRegisterApplication(Object managerObject,
                                      Class<?> serviceClass) {
        synchronized (DefaultYangSchemaRegistry.class) {
            YangSchemaNode curNode;
            String serviceName = serviceClass.getName();

            //Check if service should be unregistered?
            if (managerObject != null) {
                verifyApplicationRegistration(managerObject, serviceClass);
            }
            //Remove registered class from store.
            registerClassStore.remove(serviceName);
            //check if service is in app store.
            curNode = appNameKeyStore.get(serviceName);
            if (curNode == null) {
                curNode = interfaceNameKeyStore.get(serviceName);
            }

            if (curNode != null) {
                removeSchemaNode(curNode);
                eventNameKeyStore.remove(getEventClassName(curNode));
                appObjectStore.remove(serviceName);
                interfaceNameKeyStore.remove(getInterfaceClassName(curNode));
                opParamNameKeyStore.remove(getOpParamClassName(curNode));
                yangFileStore.remove(getModuleIdentifier(curNode));
                appNameKeyStore.remove(serviceName);
                nameSpaceSchemaStore.remove(curNode.getNameSpace()
                                                    .getModuleNamespace());
                removeYsrGeneratedTemporaryResources(jarPathStore.get(serviceName),
                                                     serviceName);
                log.info(" service {} is unregistered.",
                         serviceClass.getSimpleName());
            } else {
                throw new RuntimeException(serviceClass.getSimpleName() +
                                                   " service was not registered.");
            }
        }
    }

    @Override
    public Object getRegisteredApplication(YangSchemaNode schemaNode) {
        Object obj = null;
        if (schemaNode != null) {
            String name = getServiceName(schemaNode);
            obj = appObjectStore.get(name);
            if (obj == null) {
                log.error("{} not found.", name);
            }
        }
        return obj;
    }

    @Override
    public YangSchemaNode getYangSchemaNodeUsingSchemaName(String schemaName) {
        return getSchemaNodeUsingSchemaNameWithRev(schemaName);
    }

    @Override
    public YangSchemaNode getYangSchemaNodeUsingAppName(String appName) {
        YangSchemaNode node = appNameKeyStore.get(appName);
        if (node == null) {
            log.error("{} not found.", appName);
        }
        return node;
    }

    @Override
    public YangSchemaNode
    getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(String name) {
        YangSchemaNode node = interfaceNameKeyStore.get(name);
        if (node == null) {
            log.error("{} not found.", name);
        }
        return node;
    }

    @Override
    public YangSchemaNode getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
            String name) {
        YangSchemaNode node = opParamNameKeyStore.get(name);
        if (node == null) {
            log.error("{} not found.", name);
        }
        return node;
    }

    @Override
    public YangSchemaNode getRootYangSchemaNodeForNotification(String name) {
        YangSchemaNode node = eventNameKeyStore.get(name);
        if (node == null) {
            log.error("{} not found.", name);
        }
        return node;
    }

    @Override
    public Class<?> getRegisteredClass(YangSchemaNode schemaNode) {
        String interfaceName = getInterfaceClassName(schemaNode);
        String serviceName = getServiceName(schemaNode);
        Class<?> regClass = registerClassStore.get(serviceName);
        if (regClass == null) {
            regClass = registerClassStore.get(interfaceName);
        }
        return regClass;
    }

    @Override
    public YangSchemaNode getSchemaWrtNameSpace(String nameSpace) {

        YangSchemaNode node = nameSpaceSchemaStore.get(nameSpace);
        if (node == null) {
            log.error("node with {} namespace not found.", nameSpace);
        }
        return node;
    }

    @Override
    public String getYangFile(YangModuleIdentifier moduleIdentifier) {
        String file = yangFileStore.get(moduleIdentifier);
        if (file == null) {
            log.error("YANG files for corresponding module identifier {} not " +
                              "found", moduleIdentifier);
        }
        return file;
    }

    @Override
    public boolean verifyNotificationObject(Object appObj, Class<?> service) {
        synchronized (DefaultYangSchemaRegistry.class) {
            YangSchemaNode node = appNameKeyStore.get(service.getName());
            if (node == null) {
                log.error("application is not registered with YMS {}",
                          service.getName());
                return false;
            }
            try {
                if (node.isNotificationPresent()) {
                    if (appObj != null) {
                        Boolean ifPresent = ynhRegistrationStore.get(appObj);
                        if (ifPresent == null) {
                            ynhRegistrationStore.put(appObj, true);
                            return true;
                        }
                    }
                }
            } catch (DataModelException e) {
                log.error("notification registration error: {} {}", e
                        .getLocalizedMessage(), e);
            }
            return false;
        }
    }

    @Override
    public void flushYsrData() {
        appObjectStore.clear();
        yangSchemaStore.clear();
        eventNameKeyStore.clear();
        opParamNameKeyStore.clear();
        interfaceNameKeyStore.clear();
        registerClassStore.clear();
        yangFileStore.clear();
        nameSpaceSchemaStore.clear();
    }

    @Override
    public void processModuleLibrary(String serviceName,
                                     YangModuleLibrary library) {
        synchronized (DefaultYangSchemaRegistry.class) {
            YangSchemaNode node = appNameKeyStore.get(serviceName);
            if (node != null) {
                YangModuleInformation moduleInformation =
                        new DefaultYangModuleInformation(getModuleIdentifier(node),
                                                         node.getNameSpace());
                addSubModuleIdentifier(node, (
                        DefaultYangModuleInformation) moduleInformation);
                //TODO: add feature list to module information.
                ((DefaultYangModuleLibrary) library)
                        .addModuleInformation(moduleInformation);
            }
        }
    }

    /**
     * Process service class.
     *
     * @param serviceClass service class
     * @param appObject    application object
     */

    void doPreProcessing(Class<?> serviceClass, Object appObject) {

        //Check if service should be registered?
        if (appObject != null) {
            verifyApplicationRegistration(appObject, serviceClass);
        }
        String name = serviceClass.getName();
        //Add app class to registered service store.
        if (!registerClassStore.containsKey(name)) {
            registerClassStore.put(name, serviceClass);
        }
    }

    void updateServiceClass(Class<?> service) {
        registerClassStore.put(service.getName(), service);
    }

    /**
     * Process application registration.
     *
     * @param service  service class
     * @param jarPath  jar path
     * @param nodes    YANG nodes
     * @param appObj   application object
     * @param isFromUt if registration is being called form unit test
     */
    void processRegistration(Class<?> service, String jarPath,
                             List<YangNode> nodes,
                             Object appObj, boolean isFromUt) {

        // process storing operations.
        YangNode schemaNode = findNodeWhichShouldBeReg(service.getName(), nodes);
        if (schemaNode != null) {
            if (appObj != null) {
                appObjectStore.put(service.getName(), appObj);
            }
            //Process application context for registrations.
            processApplicationContext(schemaNode, service.getName(), isFromUt);
            //Update YANG file store.
            updateYangFileStore(schemaNode, jarPath);
        }
    }

    /**
     * Returns the node for which corresponding class is generated.
     *
     * @param name  generated class name
     * @param nodes list of yang nodes
     * @return node for which corresponding class is generated
     */
    private YangNode findNodeWhichShouldBeReg(String name, List<YangNode> nodes) {
        for (YangNode node : nodes) {
            if (name.equals(getServiceName(node)) ||
                    name.equals(getInterfaceClassName(node))) {
                return node;
            }
        }
        return null;
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
        String appName = appClass.getName();
        return appObjectStore.containsKey(appName) ||
                interfaceNameKeyStore.containsKey(appName);
    }

    /**
     * Updates yang file store for YANG node.
     *
     * @param node    YANG node
     * @param jarPath jar file path
     */
    private void updateYangFileStore(YangNode node, String jarPath) {
        yangFileStore.put(getModuleIdentifier(node),
                          getYangFilePath(jarPath, node.getFileName()));
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
        String jar = path + JAR;
        try {
            File file = new File(jar);
            if (file.exists()) {
                return parseJarFile(path + JAR, path);
            }
        } catch (IOException e) {
            log.error(" failed to parse the jar file in path {} : {} ", path,
                      e.getMessage());
        }
        return null;
    }

    /**
     * Process an application an updates the maps for YANG schema registry.
     *
     * @param appNode  application YANG schema nodes
     * @param name     class name
     * @param isFormUt if method is being called from unit tests
     */
    private void processApplicationContext(YangSchemaNode appNode, String name,
                                           boolean isFormUt) {

        //Update map for which registrations is being called.
        appNameKeyStore.put(name, appNode);

        // Updates schema store.
        addToSchemaStore(appNode);
        // update interface store.
        interfaceNameKeyStore.put(getInterfaceClassName(appNode), appNode);

        //update op param store.
        opParamNameKeyStore.put(getOpParamClassName(appNode), appNode);

        //update namespaceSchema store.
        nameSpaceSchemaStore.put(appNode.getNameSpace().getModuleNamespace(), appNode);

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
            eventNameKeyStore.put(eventSubject, appNode);
        }
        if (!isFormUt) {
            log.info("successfully registered this application {}", name);
        }
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
        if (mvnLocationPath.contains(MAVEN)) {
            String[] strArray = mvnLocationPath.split(MAVEN);
            if (strArray[1].contains(File.separator)) {
                String[] split = strArray[1].split(File.separator);
                if (split[0].contains(PERIOD)) {
                    String[] groupId = split[0].split(Pattern.quote(PERIOD));
                    return path + groupId[0] + SLASH + groupId[1] + SLASH + split[1] +
                            SLASH + split[2] + SLASH + split[1] + HYPHEN + split[2];
                }
            }
        }
        return null;
    }

    /**
     * Returns schema node based on the revision.
     *
     * @param name name of the schema node
     * @return schema node based on the revision
     */
    private YangSchemaNode getSchemaNodeUsingSchemaNameWithRev(String name) {
        ConcurrentMap<String, YangSchemaNode> revMap;
        YangSchemaNode schemaNode;
        if (name.contains(AT)) {
            String[] revArray = name.split(AT);
            revMap = yangSchemaStore.get(revArray[0]);
            schemaNode = revMap.get(name);
            if (schemaNode == null) {
                log.error("{} not found.", name);
            }
            return schemaNode;
        }
        if (yangSchemaStore.containsKey(name)) {
            revMap = yangSchemaStore.get(name);
            if (revMap != null && !revMap.isEmpty()) {
                YangSchemaNode node = revMap.get(name);
                if (node != null) {
                    return node;
                }
                String revName = getLatestVersion(revMap);
                return revMap.get(revName);
            }
        }
        log.error("{} not found.", name);
        return null;
    }

    private String getLatestVersion(ConcurrentMap<String, YangSchemaNode> revMap) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, YangSchemaNode> entry : revMap.entrySet()) {
            keys.add(entry.getKey());
        }
        sort(keys);
        return keys.get(keys.size() - 1);
    }

    /**
     * Adds schema node when different revision of node has received.
     *
     * @param schemaNode schema node
     */
    private void addToSchemaStore(YangSchemaNode schemaNode) {

        String date = getDateInStringFormat(schemaNode);
        String name = schemaNode.getName();
        String revName = name;
        if (date != null) {
            revName = name + AT + date;
        }
        //check if already present.
        if (!yangSchemaStore.containsKey(name)) {
            ConcurrentMap<String, YangSchemaNode> revStore =
                    new ConcurrentHashMap<>();
            revStore.put(revName, schemaNode);
            yangSchemaStore.put(name, revStore);
        } else {
            yangSchemaStore.get(name).put(revName, schemaNode);
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
        return null;
    }

    /**
     * Removes schema node from schema map.
     *
     * @param removableNode schema node which needs to be removed
     */
    private void removeSchemaNode(YangSchemaNode removableNode) {
        String name = removableNode.getName();
        String revName = name;
        String date = getDateInStringFormat(removableNode);
        if (date != null) {
            revName = name + AT + date;
        }
        ConcurrentMap<String, YangSchemaNode> revMap = yangSchemaStore.get(name);
        if (revMap != null && !revMap.isEmpty() && revMap.size() != 1) {
            revMap.remove(revName);
        } else {
            yangSchemaStore.remove(removableNode.getName());
        }
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
     * Returns schema node's generated interface class name.
     *
     * @param schemaNode schema node
     * @return schema node's generated interface class name
     */
    String getInterfaceClassName(YangSchemaNode schemaNode) {
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
    String getServiceName(YangSchemaNode schemaNode) {
        return getInterfaceClassName(schemaNode) + SERVICE;
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
}
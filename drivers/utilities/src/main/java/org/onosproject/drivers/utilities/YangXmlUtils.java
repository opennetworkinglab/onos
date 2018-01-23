/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.drivers.utilities;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Util CLass for Yang models.
 * Clean abstraction to read, obtain and populate
 * XML from Yang models translated into XML skeletons.
 */
public class YangXmlUtils {

    public final Logger log = LoggerFactory
            .getLogger(getClass());

    private static YangXmlUtils instance = null;

    //no instantiation, single instance.
    protected YangXmlUtils() {

    }

    /**
     * Retrieves a valid XML configuration for a specific XML path for a single
     * instance of the Map specified key-value pairs.
     *
     * @param file   path of the file to be used.
     * @param values map of key and values to set under the generic path.
     * @return Hierarchical configuration containing XML with values.
     */
    public XMLConfiguration getXmlConfiguration(String file, Map<String, String> values) {
        InputStream stream = getCfgInputStream(file);
        XMLConfiguration cfg = loadXml(stream);
        XMLConfiguration complete = new XMLConfiguration();
        List<String> paths = new ArrayList<>();
        Map<String, String> valuesWithKey = new HashMap<>();
        values.keySet().forEach(path -> {
            List<String> allPaths = findPaths(cfg, path);
            String key = nullIsNotFound(allPaths.isEmpty() ? null : allPaths.get(0),
                                        "Yang model does not contain desired path");
            paths.add(key);
            valuesWithKey.put(key, values.get(path));
        });
        Collections.sort(paths, new StringLengthComparator());
        paths.forEach(key -> complete.setProperty(key, valuesWithKey.get(key)));
        addProperties(cfg, complete);
        return complete;
    }


    /**
     * Retrieves a valid XML configuration for a specific XML path for multiple
     * instance of YangElements objects.
     *
     * @param file     path of the file to be used.
     * @param elements List of YangElements that are to be set.
     * @return Hierachical configuration containing XML with values.
     */
    public XMLConfiguration getXmlConfiguration(String file, List<YangElement> elements) {
        InputStream stream = getCfgInputStream(file);
        HierarchicalConfiguration cfg = loadXml(stream);
        XMLConfiguration complete = new XMLConfiguration();
        Multimap<String, YangElement> commonElements = ArrayListMultimap.create();

        //saves the elements in a Multimap based on the computed key.
        elements.forEach(element -> {
            String completeKey = nullIsNotFound(findPath(cfg, element.getBaseKey()),
                                                "Yang model does not contain desired path");
            commonElements.put(completeKey, element);
        });

        //iterates over the elements and constructs the configuration
        commonElements.keySet().forEach(key -> {
            // if there is more than one element for a given path
            if (commonElements.get(key).size() > 1) {
                //creates a list of nodes that have to be added for that specific path
                ArrayList<ConfigurationNode> nodes = new ArrayList<>();
                //creates the nodes
                commonElements.get(key).forEach(element -> nodes.add(getInnerNode(element).getRootNode()));
                //computes the parent path
                String parentPath = key.substring(0, key.lastIndexOf("."));
                //adds the nodes to the complete configuration
                complete.addNodes(parentPath, nodes);
            } else {
                //since there is only a single element we can assume it's the first one.
                Map<String, String> keysAndValues = commonElements.get(key).stream().
                        findFirst().get().getKeysAndValues();
                keysAndValues.forEach((k, v) -> complete.setProperty(key + "." + k, v));
            }
        });
        addProperties(cfg, complete);
        return complete;
    }

    //Adds all the properties of the original configuration to the new one.
    private void addProperties(HierarchicalConfiguration cfg, HierarchicalConfiguration complete) {
        cfg.getKeys().forEachRemaining(key -> {
            String property = (String) cfg.getProperty(key);
            if (!"".equals(property)) {
                complete.setProperty(key, property);
            }
        });
    }

    protected InputStream getCfgInputStream(String file) {
        return getClass().getResourceAsStream(file);
    }

    /**
     * Reads a valid XML configuration and returns a Map containing XML field name.
     * and value contained for every subpath.
     *
     * @param cfg the Configuration to read.
     * @param path path of the information to be read.
     * @return list of elements containing baskey and map of key value pairs.
     */
    public List<YangElement> readXmlConfiguration(HierarchicalConfiguration cfg, String path) {
        List<YangElement> elements = new ArrayList<>();

        String key = nullIsNotFound(findPath(cfg, path), "Configuration does not contain desired path");

        getElements(cfg.configurationsAt(key), elements, key, cfg, path, key);
        return ImmutableList.copyOf(elements);
    }

    private void getElements(List<HierarchicalConfiguration> configurations,
                             List<YangElement> elements, String basekey,
                             HierarchicalConfiguration originalCfg, String path,
                             String originalKey) {
        //consider each sub configuration
        configurations.forEach(config -> {

            YangElement element = new YangElement(path, new HashMap<>());
            //for each of the keys of the sub configuration
            config.getKeys().forEachRemaining(key -> {
                //considers only one step ahead
                //if one step ahead has other steps calls self to analize them
                //else adds to yang element.
                if (key.split("\\.").length > 1) {
                    getElements(originalCfg.configurationsAt(basekey + "." + key.split("\\.")[0]),
                                elements, basekey + "." + key.split("\\.")[0], originalCfg, path,
                                originalKey);
                } else {
                    String replaced = basekey.replace(originalKey, "");
                    String partialKey = replaced.isEmpty() ? key : replaced.substring(1) + "." + key;
                    partialKey = partialKey.isEmpty() ? originalKey : partialKey;
                    //Adds values to the element with a subkey starting from the requeste path onwards
                    element.getKeysAndValues().put(partialKey, config.getProperty(key).toString());
                }
            });
            //if the element doesnt already exist
            if (!elements.contains(element) && !element.getKeysAndValues().isEmpty()) {
                elements.add(element);
            }
        });
    }

    /**
     * Single Instance of Yang utilities retriever.
     *
     * @return instance of YangXmlUtils
     */
    public static synchronized YangXmlUtils getInstance() {
        if (instance == null) {
            instance = new YangXmlUtils();
        }
        return instance;
    }

    /**
     * Return the string representation of the XMLConfig without header
     * and configuration element.
     *
     * @param cfg the XML to convert
     * @return the cfg string.
     */
    public String getString(XMLConfiguration cfg) {
        StringWriter stringWriter = new StringWriter();
        try {
            cfg.save(stringWriter);
        } catch (ConfigurationException e) {
            log.error("Cannot convert configuration", e.getMessage());
        }
        String xml = stringWriter.toString();
        xml = xml.substring(xml.indexOf("\n"));
        xml = xml.substring(xml.indexOf(">") + 1);
        return xml;
    }

    /**
     * Method to read an input stream into a XMLConfiguration.
     * @param xmlStream inputstream containing XML description
     * @return the XMLConfiguration object
     */
    public XMLConfiguration loadXml(InputStream xmlStream) {
        XMLConfiguration cfg = new XMLConfiguration();
        try {
            cfg.load(xmlStream);
            return cfg;
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException("Cannot load xml from Stream", e);
        }
    }

    //Finds all paths for a corresponding element
    private List<String> findPaths(HierarchicalConfiguration cfg, String path) {
        List<String> paths = new ArrayList<>();
        cfg.getKeys().forEachRemaining(key -> {
            if (key.equals(path)) {
                paths.add(key);
            }
            if (key.contains("." + path)) {
                paths.add(key);
            }
        });
        return paths;
    }

    //Finds the first parent path corresponding to an element.
    private String findPath(HierarchicalConfiguration cfg, String element) {
        Iterator<String> it = cfg.getKeys();
        while (it.hasNext()) {
            String key = it.next();
            String[] arr = key.split("\\.");
            for (int i = 0; i < arr.length; i++) {
                if (element.equals(arr[i])) {
                    String completeKey = "";
                    for (int j = 0; j <= i; j++) {
                        completeKey = completeKey + "." + arr[j];
                    }
                    return completeKey.substring(1);
                }
            }
        }
        return null;
    }

    //creates a node based on a single Yang element.
    private HierarchicalConfiguration getInnerNode(YangElement element) {
        HierarchicalConfiguration node = new HierarchicalConfiguration();
        node.setRoot(new HierarchicalConfiguration.Node(element.getBaseKey()));
        element.getKeysAndValues().forEach(node::setProperty);
        return node;
    }

    //String lenght comparator
    private class StringLengthComparator implements Comparator<String> {

        public int compare(String o1, String o2) {
            if (o2 == null && o1 == null) {
                return 0;
            }

            if (o1 == null) {
                return o2.length();
            }

            if (o2 == null) {
                return o1.length();
            }

            if (o1.length() != o2.length()) {
                return o1.length() - o2.length(); //overflow impossible since lengths are non-negative
            }
            return o1.compareTo(o2);
        }
    }

}

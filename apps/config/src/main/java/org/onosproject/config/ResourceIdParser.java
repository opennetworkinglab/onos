/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.config;

import java.util.Iterator;

import java.util.List;

import org.onosproject.yang.model.KeyLeaf;
import org.onosproject.yang.model.LeafListKey;
import org.onosproject.yang.model.ListKey;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceId;

/**
 * Utilities to work on the ResourceId.
 */
//FIXME add javadocs
public final class ResourceIdParser {

    public static final String ROOT = "root";
    public static final String NM_SEP = "#";
    public static final String VAL_SEP = "@";
    public static final String KEY_SEP = "$";
    public static final String EL_SEP = "|";
    public static final String VAL_CHK = "\\@";
    public static final String KEY_CHK = "\\$";
    public static final String NM_CHK = "\\#";
    public static final String EL_CHK = "\\|";


    private ResourceIdParser() {

    }

    public static ResourceId getParent(ResourceId path) {
        int last = path.nodeKeys().size();
        path.nodeKeys().remove(last - 1);
        return path;
    }

    public static NodeKey getInstanceKey(ResourceId path) {
        int last = path.nodeKeys().size();
        NodeKey ret = path.nodeKeys().get(last - 1);
        if (ret instanceof NodeKey) {
            return ret;
        } else {
            return null;
        }
    }

    public static NodeKey getMultiInstanceKey(ResourceId path) {
        int last = path.nodeKeys().size();
        NodeKey ret = path.nodeKeys().get(last - 1);
        if (ret instanceof ListKey) {
            return ret;
        } else {
            return null;
        }
    }

    public static String getNamespace(String nmspc) {
        String ret = null;
        if (nmspc.contains(ResourceIdParser.KEY_SEP)) {
            ret = nmspc.split(KEY_CHK)[0];
        } else if (nmspc.contains(ResourceIdParser.VAL_SEP)) {
            ret = nmspc.split(VAL_CHK)[0];
        } else {
            ret = nmspc;
        }
        return ret;
    }

    public static String getKeyVal(String nmspc) {
        String ret = null;
        if (nmspc.contains(ResourceIdParser.VAL_SEP)) {
            ret = nmspc.split(VAL_CHK)[1];
        }
        return ret;
    }


    public static String appendMultiInstKey(String path, String leaf) {
        return (path + leaf.substring(leaf.indexOf(KEY_SEP)));
    }

    public static String appendKeyLeaf(String path, String key) {
        return (path + EL_SEP + key);
    }

    public static String appendKeyLeaf(String path, KeyLeaf key) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(key.leafSchema().name());
        bldr.append(NM_SEP);
        bldr.append(key.leafSchema().namespace());
        bldr.append(NM_SEP);
        bldr.append(key.leafValue().toString());
        return (path + EL_SEP + bldr.toString());
    }

    public static String appendNodeKey(String path, NodeKey key) {
        return (path + EL_SEP + key.schemaId().name() + NM_SEP + key.schemaId().namespace());
    }

    public static String appendNodeKey(String path, String name, String nmSpc) {
        return (path + EL_SEP + name + NM_SEP + nmSpc);
    }

    public static String appendLeafList(String path, LeafListKey key) {
        return (path + VAL_SEP + key.asString());
    }

    public static String appendLeafList(String path, String val) {
        return (path + VAL_SEP + val);
    }

    public static String appendKeyList(String path, ListKey key) {
        StringBuilder bldr = new StringBuilder();
        for (KeyLeaf keyLeaf : key.keyLeafs()) {
            bldr.append(KEY_SEP);
            bldr.append(keyLeaf.leafSchema().name());
            bldr.append(NM_SEP);
            bldr.append(keyLeaf.leafSchema().namespace());
            bldr.append(NM_SEP);
            bldr.append(keyLeaf.leafValue().toString());
        }
        return (path + bldr.toString());
    }

    public static String parseNodeKey(NodeKey key) {
        if (key == null) {
            return null;
        }
        StringBuilder bldr = new StringBuilder();
        if (key instanceof LeafListKey) {
            parseLeafList((LeafListKey) key, bldr);
        } else if (key instanceof ListKey) {
            parseKeyList((ListKey) key, bldr);
        } else {
            parseNodeKey(key, bldr);
        }
        return bldr.toString();
    }

    public static String parseResId(ResourceId path) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(ROOT);
        if (path == null) {
            return bldr.toString();
        }
        List<NodeKey> nodeKeyList = path.nodeKeys();
        for (NodeKey key : nodeKeyList) {
            bldr.append(EL_SEP);
            if (key instanceof LeafListKey) {
                parseLeafList((LeafListKey) key, bldr);
            } else if (key instanceof ListKey) {
                parseKeyList((ListKey) key, bldr);
            } else {
                parseNodeKey(key, bldr);
            }
        }
        return bldr.toString();
    }

    private static void parseLeafList(LeafListKey key, StringBuilder bldr) {
        bldr.append(key.schemaId().name());
        bldr.append(NM_SEP);
        bldr.append(key.schemaId().namespace());
        bldr.append(NM_SEP);
        bldr.append(key.asString());
    }

    private static void parseKeyList(ListKey key, StringBuilder bldr) {
        bldr.append(key.schemaId().name());
        bldr.append(NM_SEP);
        bldr.append(key.schemaId().namespace());
        Iterator<KeyLeaf> iter = key.keyLeafs().iterator();
        KeyLeaf next;
        while (iter.hasNext()) {
            next = iter.next();
            bldr.append(KEY_SEP);
            bldr.append(next.leafSchema().name());
            bldr.append(NM_SEP);
            bldr.append(next.leafSchema().namespace());
            bldr.append(NM_SEP);
            bldr.append(next.leafValue().toString());
        }
    }

    private static void parseNodeKey(NodeKey key, StringBuilder bldr) {
        bldr.append(key.schemaId().name());
        bldr.append(NM_SEP);
        bldr.append(key.schemaId().namespace());
    }

    public static ResourceId getResId(List<String> dpath) {
        ResourceId.Builder resBldr = new ResourceId.Builder();
        Iterator<String> itr = dpath.iterator();
        itr.next();
        while (itr.hasNext()) {
            String name = itr.next();
            if (name.contains(VAL_SEP)) {
                resBldr.addLeafListBranchPoint(name.substring(0, name.indexOf(NM_SEP)),
                        name.substring(name.indexOf(NM_SEP) + 1, name.indexOf(VAL_SEP)),
                        name.substring(name.indexOf(VAL_SEP) + 1));
            } else if (name.contains(KEY_SEP)) {
                String[] keys = name.split(KEY_CHK);
                String[] nm = keys[0].split(NM_CHK);
                resBldr.addBranchPointSchema(nm[0], nm[1]);
                for (int i = 1; i < keys.length; i++) {
                    String key = keys[i];
                    String[] el = keys[i].split(NM_CHK);
                    if (el.length != 3) {
                        throw new FailedException("Malformed event subject, cannot parse");
                    }
                    resBldr.addKeyLeaf(el[0], el[1], el[2]);
                }
            } else {
                resBldr.addBranchPointSchema(name.substring(0, name.indexOf(NM_SEP)),
                        name.substring(name.indexOf(NM_SEP) + 1));
            }
        }
        return resBldr.build();
    }
}
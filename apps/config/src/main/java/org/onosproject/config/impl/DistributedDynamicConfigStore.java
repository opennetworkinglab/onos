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
package org.onosproject.config.impl;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigStore;
import org.onosproject.config.DynamicConfigStoreDelegate;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.config.ResourceIdParser;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTreeEvent;
import org.onosproject.store.service.DocumentTreeListener;
import org.onosproject.store.service.IllegalDocumentModificationException;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.NoSuchDocumentPathException;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.KeyLeaf;
import org.onosproject.yang.model.LeafListKey;
import org.onosproject.yang.model.LeafNode;
import org.onosproject.yang.model.ListKey;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.SchemaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.onosproject.config.DynamicConfigEvent.Type.NODE_ADDED;
import static org.onosproject.config.DynamicConfigEvent.Type.NODE_DELETED;
import static org.onosproject.config.DynamicConfigEvent.Type.NODE_UPDATED;
import static org.onosproject.config.DynamicConfigEvent.Type.UNKNOWN_OPRN;

/**
 * Implementation of the dynamic config store.
 */
@Beta
@Component(immediate = true)
@Service
public class DistributedDynamicConfigStore
        extends AbstractStore<DynamicConfigEvent, DynamicConfigStoreDelegate>
        implements DynamicConfigStore {
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    private AsyncDocumentTree<DataNode.Type> keystore;
    private ConsistentMap<String, LeafNode> objectStore;
    private final DocumentTreeListener<DataNode.Type> klistener = new InternalDocTreeListener();
    private final MapEventListener<String, LeafNode> olistener = new InternalMapListener();

    @Activate
    public void activateStore() {
        KryoNamespace.Builder kryoBuilder = new KryoNamespace.Builder()
                .register(KryoNamespaces.BASIC)
                .register(java.lang.Class.class)
                .register(DataNode.Type.class)
                .register(LeafNode.class)
                .register(InnerNode.class)
                .register(ResourceId.class)
                .register(NodeKey.class)
                .register(SchemaId.class)
                .register(LeafListKey.class)
                .register(ListKey.class)
                .register(KeyLeaf.class)
                .register(BigInteger.class)
                .register(java.util.LinkedHashMap.class);
        keystore = storageService.<DataNode.Type>documentTreeBuilder()
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withName("config-key-store")
                .withRelaxedReadConsistency()
                .buildDocumentTree();
        objectStore = storageService.<String, LeafNode>consistentMapBuilder()
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withName("config-object-store")
                .withRelaxedReadConsistency()
                .build();
        keystore.addListener(klistener);
        objectStore.addListener(olistener);
        log.info("DyanmicConfig Store Active");
    }

    @Deactivate
    public void deactivateStore() {
        keystore.removeListener(klistener);
        objectStore.removeListener(olistener);
        log.info("DyanmicConfig Store Stopped");
    }

    @Override
    public CompletableFuture<Boolean>
    addNode(ResourceId complete, DataNode node) {
        CompletableFuture<Boolean> eventFuture = CompletableFuture.completedFuture(true);
        List<NodeKey> nodeKeyList = complete.nodeKeys();
        NodeKey f = nodeKeyList.get(0);
        if (f.schemaId().name().compareTo("/") == 0) {
            nodeKeyList.remove(0);
        }
        String spath = ResourceIdParser.parseResId(complete);
        if (spath == null) {
            throw new FailedException("Invalid RsourceId, cannot create Node");
        }
        if (spath.compareTo(ResourceIdParser.ROOT) != 0) {
            if (completeVersioned(keystore.get(DocumentPath.from(spath))) == null) {
                throw new FailedException("Node or parent doesnot exist");
            }
        }
        spath = ResourceIdParser.appendNodeKey(spath, node.key());
        parseNode(spath, node);
        return eventFuture;
    }

    private void parseNode(String path, DataNode node) {
        if (completeVersioned(keystore.get(DocumentPath.from(path))) != null) {
            throw new FailedException("Requested node already present in the" +
                                              " store, please use an update method");
        }
        if (node.type() == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) {
            addLeaf(path, (LeafNode) node);
        } else if (node.type() == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE) {
            path = ResourceIdParser.appendLeafList(path, (LeafListKey) node.key());
            if (completeVersioned(keystore.get(DocumentPath.from(path))) != null) {
                throw new FailedException("Requested node already present in the" +
                                                  " store, please use an update method");
            }
            addLeaf(path, (LeafNode) node);
        } else if (node.type() == DataNode.Type.SINGLE_INSTANCE_NODE) {
            traverseInner(path, (InnerNode) node);
        } else if (node.type() == DataNode.Type.MULTI_INSTANCE_NODE) {
            path = ResourceIdParser.appendKeyList(path, (ListKey) node.key());
            if (completeVersioned(keystore.get(DocumentPath.from(path))) != null) {
                throw new FailedException("Requested node already present in the" +
                                                  " store, please use an update method");
            }
            traverseInner(path, (InnerNode) node);
        } else {
            throw new FailedException("Invalid node type");
        }
    }

    private void traverseInner(String path, InnerNode node) {
        addKey(path, node.type());
        Map<NodeKey, DataNode> entries = node.childNodes();
        if (entries.size() == 0) {
            return;
        }
        entries.forEach((k, v) -> {
            String tempPath;
            tempPath = ResourceIdParser.appendNodeKey(path, v.key());
            if (v.type() == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) {
                addLeaf(tempPath, (LeafNode) v);
            } else if (v.type() == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE) {
                tempPath = ResourceIdParser.appendLeafList(tempPath, (LeafListKey) v.key());
                addLeaf(tempPath, (LeafNode) v);
            } else if (v.type() == DataNode.Type.SINGLE_INSTANCE_NODE) {
                traverseInner(tempPath, (InnerNode) v);
            } else if (v.type() == DataNode.Type.MULTI_INSTANCE_NODE) {
                tempPath = ResourceIdParser.appendKeyList(tempPath, (ListKey) v.key());
                traverseInner(tempPath, (InnerNode) v);
            } else {
                throw new FailedException("Invalid node type");
            }
        });
    }

    private Boolean addLeaf(String path, LeafNode node) {
        objectStore.put(path, node);
        return addKey(path, node.type());
    }

    private Boolean addKey(String path, DataNode.Type type) {
        Boolean stat = false;
        CompletableFuture<Boolean> ret = keystore.create(DocumentPath.from(path), type);
        return complete(ret);
    }

    @Override
    public CompletableFuture<DataNode> readNode(ResourceId path, Filter filter) {
        CompletableFuture<DataNode> eventFuture = CompletableFuture.completedFuture(null);
        List<NodeKey> nodeKeyList = path.nodeKeys();
        NodeKey f = nodeKeyList.get(0);
        if (f.schemaId().name().compareTo("/") == 0) {
            nodeKeyList.remove(0);
        }
        String spath = ResourceIdParser.parseResId(path);
        DocumentPath dpath = DocumentPath.from(spath);
        DataNode.Type type = null;
        CompletableFuture<Versioned<DataNode.Type>> ret = keystore.get(dpath);
        type = completeVersioned(ret);
        if (type == null) {
            throw new FailedException("Requested node or some of the parents" +
                                              "are not present in the requested path");
        }
        DataNode retVal = null;
        if (type == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) {
            retVal = readLeaf(spath);
        } else if (type == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE) {
            retVal = readLeaf(spath);
        } else if (type == DataNode.Type.SINGLE_INSTANCE_NODE) {
            NodeKey key = ResourceIdParser.getInstanceKey(path);
            if (key == null) {
                throw new FailedException("Key type did not match node type");
            }
            DataNode.Builder superBldr = InnerNode
                    .builder(key.schemaId().name(), key.schemaId().namespace())
                    .type(type);
            readInner(superBldr, spath);
            retVal = superBldr.build();
        } else if (type == DataNode.Type.MULTI_INSTANCE_NODE) {
            NodeKey key = ResourceIdParser.getMultiInstanceKey(path);
            if (key == null) {
                throw new FailedException("Key type did not match node type");
            }
            DataNode.Builder superBldr = InnerNode
                    .builder(key.schemaId().name(), key.schemaId().namespace())
                    .type(type);
            for (KeyLeaf keyLeaf : ((ListKey) key).keyLeafs()) {
                //String tempPath = ResourceIdParser.appendKeyLeaf(spath, keyLeaf);
                //LeafNode lfnd = readLeaf(tempPath);
                superBldr.addKeyLeaf(keyLeaf.leafSchema().name(),
                                     keyLeaf.leafSchema().namespace(), String.valueOf(keyLeaf.leafValue()));
            }
            readInner(superBldr, spath);
            retVal = superBldr.build();
        } else {
            throw new FailedException("Invalid node type");
        }
        if (retVal != null) {
            eventFuture = CompletableFuture.completedFuture(retVal);
        } else {
            log.info("STORE: Failed to READ node");
        }
        return eventFuture;
    }

    private void readInner(DataNode.Builder superBldr, String spath) {
        CompletableFuture<Map<String, Versioned<DataNode.Type>>> ret = keystore.getChildren(
                DocumentPath.from(spath));
        Map<String, Versioned<DataNode.Type>> entries = null;
        entries = complete(ret);
        if ((entries != null) && (!entries.isEmpty())) {
            entries.forEach((k, v) -> {
                String[] names = k.split(ResourceIdParser.NM_CHK);
                String name = names[0];
                String nmSpc = ResourceIdParser.getNamespace(names[1]);
                String keyVal = ResourceIdParser.getKeyVal(names[1]);
                DataNode.Type type = v.value();
                String tempPath = ResourceIdParser.appendNodeKey(spath, name, nmSpc);
                if (type == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) {
                    superBldr.createChildBuilder(name, nmSpc, readLeaf(tempPath).value())
                            .type(type)
                            .exitNode();
                } else if (type == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE) {
                    String mlpath = ResourceIdParser.appendLeafList(tempPath, keyVal);
                    LeafNode lfnode = readLeaf(mlpath);
                    superBldr.createChildBuilder(name, nmSpc, lfnode.value())
                            .type(type)
                            .addLeafListValue(lfnode.value())
                            .exitNode();
                    //TODO this alone should be sufficient and take the nm, nmspc too
                } else if (type == DataNode.Type.SINGLE_INSTANCE_NODE) {
                    DataNode.Builder tempBldr = superBldr.createChildBuilder(name, nmSpc)
                            .type(type);
                    readInner(tempBldr, tempPath);
                } else if (type == DataNode.Type.MULTI_INSTANCE_NODE) {
                    DataNode.Builder tempBldr = superBldr.createChildBuilder(name, nmSpc)
                            .type(type);
                    tempPath = ResourceIdParser.appendMultiInstKey(tempPath, k);
                    String[] keys = k.split(ResourceIdParser.KEY_CHK);
                    for (int i = 1; i < keys.length; i++) {
                        //String curKey = ResourceIdParser.appendKeyLeaf(tempPath, keys[i]);
                        //LeafNode lfnd = readLeaf(curKey);
                        String[] keydata = keys[i].split(ResourceIdParser.NM_CHK);
                        tempBldr.addKeyLeaf(keydata[0], keydata[1], keydata[2]);
                    }
                    readInner(tempBldr, tempPath);
                } else {
                    throw new FailedException("Invalid node type");
                }
            });
        }
        superBldr.exitNode();
    }

    private LeafNode readLeaf(String path) {
        return objectStore.get(path).value();
    }

    private void parseForUpdate(String path, DataNode node) {
        if (node.type() == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) {
            addLeaf(path, (LeafNode) node);
        } else if (node.type() == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE) {
            path = ResourceIdParser.appendLeafList(path, (LeafListKey) node.key());
            addLeaf(path, (LeafNode) node);
        } else if (node.type() == DataNode.Type.SINGLE_INSTANCE_NODE) {
            traverseInner(path, (InnerNode) node);
        } else if (node.type() == DataNode.Type.MULTI_INSTANCE_NODE) {
            path = ResourceIdParser.appendKeyList(path, (ListKey) node.key());
            traverseInner(path, (InnerNode) node);
        } else {
            throw new FailedException("Invalid node type");
        }
    }

    @Override
    public CompletableFuture<Boolean> updateNode(ResourceId complete, DataNode node) {
        CompletableFuture<Boolean> eventFuture = CompletableFuture.completedFuture(true);
        List<NodeKey> nodeKeyList = complete.nodeKeys();
        NodeKey f = nodeKeyList.get(0);
        if (f.schemaId().name().compareTo("/") == 0) {
            nodeKeyList.remove(0);
        }
        String spath = ResourceIdParser.parseResId(complete);
        if (spath == null) {
            throw new FailedException("Invalid RsourceId, cannot update Node");
        }
        if (spath.compareTo(ResourceIdParser.ROOT) != 0) {
            if (completeVersioned(keystore.get(DocumentPath.from(spath))) == null) {
                throw new FailedException("Node or parent doesnot exist, cannot update");
            }
        }
        spath = ResourceIdParser.appendNodeKey(spath, node.key());
        parseForUpdate(spath, node);
        return eventFuture;
    }

    @Override
    public CompletableFuture<Boolean> nodeExist(ResourceId complete) {
        Boolean stat = true;
        List<NodeKey> nodeKeyList = complete.nodeKeys();
        NodeKey f = nodeKeyList.get(0);
        if (f.schemaId().name().compareTo("/") == 0) {
            nodeKeyList.remove(0);
        }
        String spath = ResourceIdParser.parseResId(complete);
        if (spath == null) {
            stat = false;
        } else if (completeVersioned(keystore.get(DocumentPath.from(spath))) == null) {
            stat = false;
        }
        return CompletableFuture.completedFuture(stat);
    }

    @Override
    public CompletableFuture<Boolean> replaceNode(ResourceId path, DataNode node) {
        throw new FailedException("Not yet implemented");
    }

    @Override
    public CompletableFuture<Boolean> deleteNode(ResourceId path) {
        throw new FailedException("Not yet implemented");
    }

    private void deleteInner(String spath) {
        CompletableFuture<Map<String, Versioned<DataNode.Type>>> ret = keystore.getChildren(
                DocumentPath.from(spath));
        Map<String, Versioned<DataNode.Type>> entries = null;
        entries = complete(ret);
        if ((entries != null) && (!entries.isEmpty())) {
            entries.forEach((k, v) -> {
                String[] names = k.split(ResourceIdParser.NM_CHK);
                String name = names[0];
                String nmSpc = ResourceIdParser.getNamespace(names[1]);
                String keyVal = ResourceIdParser.getKeyVal(names[1]);
                DataNode.Type type = v.value();
                String tempPath = ResourceIdParser.appendNodeKey(spath, name, nmSpc);
                if (type == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) {
                    removeLeaf(tempPath);
                } else if (type == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE) {
                    String mlpath = ResourceIdParser.appendLeafList(tempPath, keyVal);
                    removeLeaf(mlpath);
                } else if (type == DataNode.Type.SINGLE_INSTANCE_NODE) {
                    deleteInner(tempPath);
                } else if (type == DataNode.Type.MULTI_INSTANCE_NODE) {
                    tempPath = ResourceIdParser.appendMultiInstKey(tempPath, k);
                    deleteInner(tempPath);
                } else {
                    throw new FailedException("Invalid node type");
                }
            });
        }
        keystore.removeNode(DocumentPath.from(spath));
    }

    private void removeLeaf(String path) {
        keystore.removeNode(DocumentPath.from(path));
        objectStore.remove(path);
    }

    @Override
    public CompletableFuture<Boolean> deleteNodeRecursive(ResourceId path) {
        List<NodeKey> nodeKeyList = path.nodeKeys();
        NodeKey f = nodeKeyList.get(0);
        if (f.schemaId().name().compareTo("/") == 0) {
            nodeKeyList.remove(0);
        }
        String spath = ResourceIdParser.parseResId(path);
        if (spath == null) {
            throw new FailedException("Invalid RsourceId, cannot create Node");
        }
        if (spath.compareTo(ResourceIdParser.ROOT) == 0) {
            throw new FailedException("Cannot delete Root");
        }
        DocumentPath dpath = DocumentPath.from(spath);
        DataNode.Type type = null;
        CompletableFuture<Versioned<DataNode.Type>> ret = keystore.get(dpath);
        type = completeVersioned(ret);
        if (type == null) {
            throw new FailedException("Cannot delete, Requested node or some of the parents" +
                                              "are not present in the requested path");
        }
        DataNode retVal = null;
        if (type == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) {
            removeLeaf(spath);
        } else if (type == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE) {
            removeLeaf(spath);
        } else if (type == DataNode.Type.SINGLE_INSTANCE_NODE) {
            deleteInner(spath);
        } else if (type == DataNode.Type.MULTI_INSTANCE_NODE) {
            deleteInner(spath);
        } else {
            throw new FailedException("Invalid node type");
        }
        return CompletableFuture.completedFuture(true);
    }

    public class InternalDocTreeListener implements DocumentTreeListener<DataNode.Type> {
        @Override
        public void event(DocumentTreeEvent<DataNode.Type> event) {
            DynamicConfigEvent.Type type;
            ResourceId path;
            switch (event.type()) {
                case CREATED:
                    type = NODE_ADDED;
                    //log.info("NODE added in store");
                    break;
                case UPDATED:
                    //log.info("NODE updated in store");
                    type = NODE_UPDATED;
                    break;
                case DELETED:
                    //log.info("NODE deleted in store");
                    type = NODE_DELETED;
                    break;
                default:
                    //log.info("UNKNOWN operation in store");
                    type = UNKNOWN_OPRN;
            }
            path = ResourceIdParser.getResId(event.path().pathElements());
            notifyDelegate(new DynamicConfigEvent(type, path));
        }
    }

    public class InternalMapListener implements MapEventListener<String, LeafNode> {
        @Override
        public void event(MapEvent<String, LeafNode> event) {
            switch (event.type()) {
                case INSERT:
                    //log.info("NODE created in store");
                    break;
                case UPDATE:
                    //log.info("NODE updated in store");
                    break;
                case REMOVE:
                default:
                    //log.info("NODE removed in store");
                    break;
            }
        }
    }

    private <T> T complete(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (e == null) {
                throw new FailedException("Unknown Exception");
            } else {
                throw new FailedException(e.getCause().getMessage());
            }
        } catch (ExecutionException e) {
            if (e == null) {
                throw new FailedException("Unknown Exception");
            } else if (e.getCause() instanceof IllegalDocumentModificationException) {
                throw new FailedException("Node or parent doesnot exist or is root or is not a Leaf Node");
            } else if (e.getCause() instanceof NoSuchDocumentPathException) {
                throw new FailedException("Resource id does not exist");
            } else {
                throw new FailedException("Datastore operation failed");
            }
        }
    }

    private <T> T completeVersioned(CompletableFuture<Versioned<T>> future) {
        try {
            if (future.get() != null) {
                return future.get().value();
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FailedException(e.getCause().getMessage());
        } catch (ExecutionException e) {
            if (e == null) {
                throw new FailedException("Unknown Exception");
            } else if (e.getCause() instanceof IllegalDocumentModificationException) {
                throw new FailedException("Node or parent does not exist or is root or is not a Leaf Node");
            } else if (e.getCause() instanceof NoSuchDocumentPathException) {
                throw new FailedException("Resource id does not exist");
            } else {
                throw new FailedException("Datastore operation failed");
            }
        }
    }
}
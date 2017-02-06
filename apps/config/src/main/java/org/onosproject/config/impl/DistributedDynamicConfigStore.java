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
import org.onosproject.config.model.DataNode;
import org.onosproject.config.model.InnerNode;
import org.onosproject.config.model.LeafNode;
import org.onosproject.config.model.NodeKey;
import org.onosproject.config.model.ResourceId;
import org.onosproject.config.model.SchemaId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTreeEvent;
import org.onosproject.store.service.DocumentTreeListener;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the dynamic config store.
 */
@Component(immediate = true)
@Service
public class DistributedDynamicConfigStore
        extends AbstractStore<DynamicConfigEvent, DynamicConfigStoreDelegate>
        implements DynamicConfigStore {
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    private AsyncDocumentTree<DataNode.Type> keystore;
    private ConsistentMap<ResourceId, LeafNode> objectStore;
    private final DocumentTreeListener<DataNode.Type> klistener = new InternalDocTreeListener();
    private final MapEventListener<ResourceId, LeafNode> olistener = new InternalMapListener();

    @Activate
    public void activateStore() {
        KryoNamespace.Builder kryoBuilder = new KryoNamespace.Builder()
                .register(KryoNamespaces.BASIC)
                .register(String.class)
                .register(java.lang.Class.class)
                .register(DataNode.Type.class)
                .register(LeafNode.class)
                .register(InnerNode.class)
                .register(ResourceId.class)
                .register(NodeKey.class)
                .register(SchemaId.class)
                .register(java.util.LinkedHashMap.class);
        keystore = storageService.<DataNode.Type>documentTreeBuilder()
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withName("config-key-store")
                .withRelaxedReadConsistency()
                .buildDocumentTree();
        objectStore = storageService.<ResourceId, LeafNode>consistentMapBuilder()
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
    addNode(ResourceId path, DataNode node) {
        CompletableFuture<Boolean> eventFuture = CompletableFuture.completedFuture(false);
        Boolean stat = false;
        DocumentPath dpath  = DocumentPath.from(path.asString());
        log.info("STORE: dpath to parent {}", dpath);
        if (keystore.get(dpath).join() == null) {
            throw new FailedException("Some of the parents are not present in " +
                    "the requested path, please use a recursive create");
        }
        ResourceId cpath = path.builder()
                .addBranchPointSchema(node.key().schemaId().name(),
                                      node.key().schemaId().namespace()).build();
        dpath  = DocumentPath.from(cpath.asString());
        if (keystore.get(dpath).join() != null) {
            throw new FailedException("Requested node already present in the" +
                                              " store, please use an update method");
        }
        stat = checkNode(cpath, node);
        if (stat) {
            eventFuture = CompletableFuture.completedFuture(true);
        } else {
            log.info("STORE: FAILED to create node @ {}", path);
        }
        return eventFuture;
    }

    @Override
    public CompletableFuture<DataNode> readNode(ResourceId path, Filter filter) {
        CompletableFuture<DataNode> eventFuture = CompletableFuture.completedFuture(null);
        DocumentPath dpath = DocumentPath.from(path.asString());
        DataNode.Type type;
        type = keystore.get(dpath).join().value();
        if (type == null) {
            throw new FailedException("Requested node or some of the parents" +
                                              "are not present in the requested path");
        }
        DataNode retVal = null;
        //TODO handle single and multi instances differently
        if ((type == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) ||
                (type == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE)) {
            retVal = readLeaf(path);
        } else {
            int last = path.nodeKeys().size();
            NodeKey key = path.nodeKeys().get(last - 1);
            DataNode.Builder superBldr = new InnerNode.Builder(key.schemaId().name(),
                                          key.schemaId().namespace()).type(type);
            readInner(superBldr, path);
            retVal = superBldr.build();
        }
        if (retVal != null) {
            eventFuture = CompletableFuture.completedFuture(retVal);
        } else {
            log.info("STORE: FAILED to READ node @@@@");
        }
        return eventFuture;
    }

  @Override
  public CompletableFuture<Boolean>
  addRecursive(ResourceId path, DataNode node) {
      CompletableFuture<Boolean> eventFuture = CompletableFuture.completedFuture(false);
      Boolean stat = false;
      DocumentPath dpath  = DocumentPath.from(path.asString());
      //TODO need to check for each parent in the path and recursively create all missing
      /*if (keystore.get(dpath).join() == null) {
          //recursivley craete all missing aprents
      }*/
      if (keystore.get(dpath).join() != null) {
          throw new FailedException("Requested node already present " +
                                            "in the store, please use an update method");
      }
      //TODO single instance and multi instance need to be handled differently
      if ((node.type() == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) ||
              (node.type() == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE)) {
          stat = addLeaf(path, (LeafNode) node);
      } else {
          stat = (traverseInner(path, (InnerNode) node));
      }
      if (stat) {
          eventFuture = CompletableFuture.completedFuture(true);
      } else {
          log.info("STORE: FAILED to create node @@@@");
      }
      return eventFuture;
  }
    @Override
    public CompletableFuture<Boolean> updateNode(ResourceId path, DataNode node) {
        throw new FailedException("Not yet implemented");
    }
    @Override
    public CompletableFuture<Boolean>
    updateNodeRecursive(ResourceId path, DataNode node) {
        throw new FailedException("Not yet implemented");
    }
    @Override
    public CompletableFuture<Boolean>
    replaceNode(ResourceId path, DataNode node) {
        throw new FailedException("Not yet implemented");
    }
    @Override
    public CompletableFuture<Boolean>
    deleteNode(ResourceId path) {
        throw new FailedException("Not yet implemented");
    }
    @Override
    public CompletableFuture<Boolean>
    deleteNodeRecursive(ResourceId path) {
        throw new FailedException("Not yet implemented");
    }

    private Boolean addLeaf(ResourceId path, LeafNode node) {
        objectStore.put(path, node);
        return (keystore.create(DocumentPath.from(path.asString()), node.type()).join());
    }

    private Boolean addKey(ResourceId path, DataNode.Type type) {
        return (keystore.create(DocumentPath.from(path.asString()), type).join());
    }

    private Boolean checkNode(ResourceId path, DataNode node) {
        //TODO single instance and multi instance need to be handled differently
        if ((node.type() == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) ||
                (node.type() == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE)) {
            return (addLeaf(path, (LeafNode) node));
        } else if ((node.type() == DataNode.Type.SINGLE_INSTANCE_NODE) ||
                (node.type() == DataNode.Type.MULTI_INSTANCE_NODE)) {
            addKey(path, node.type());
            return (traverseInner(path, (InnerNode) node));
        } else {
            throw new FailedException("Node type should either be LEAF or INNERNODE");
        }
    }

    private LeafNode readLeaf(ResourceId path) {
        return objectStore.get(path).value();
    }

    private Boolean traverseInner(ResourceId path, InnerNode node) {
        addKey(path, node.type());
        Map<NodeKey, DataNode> entries = node.childNodes();
        if (entries.size() == 0) {
            throw new FailedException("Inner node cannot have empty children map");
        }
        entries.forEach((k, v) -> {
            ResourceId tempPath;
            try {
                tempPath = path.copyBuilder()
                        .addBranchPointSchema(k.schemaId().name(),
                                              k.schemaId().namespace())
                        .build();
            } catch (CloneNotSupportedException e) {
                throw new FailedException("ResourceId could not be cloned@@@@");
            }
            //TODO single instance and multi instance need to be handled differently
            if ((v.type() == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) ||
                    (v.type() == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE)) {
                addLeaf(tempPath, (LeafNode) v);
            } else if ((v.type() == DataNode.Type.SINGLE_INSTANCE_NODE) ||
                    (v.type() == DataNode.Type.MULTI_INSTANCE_NODE)) {
                traverseInner(tempPath, (InnerNode) v);
            } else {
                throw new FailedException("Node type should either be LEAF or INNERNODE");
            }
        });
        return true;
    }

    private void readInner(DataNode.Builder superBldr, ResourceId path) {
        Map<String, Versioned<DataNode.Type>> entries = keystore.getChildren(
                DocumentPath.from(path.asString())).join();
        if (entries.size() == 0) {
            throw new FailedException("Inner node cannot have empty children map");
        }
        entries.forEach((k, v) -> {
            ResourceId tempPath;
            String[] names = k.split("#");
            String name = names[0];
            String nmSpc = names[1];
            DataNode.Type type = v.value();
            try {
                tempPath = path.copyBuilder()
                        .addBranchPointSchema(name, nmSpc)
                        .build();
            } catch (CloneNotSupportedException e) {
                throw new FailedException("ResourceId could not be cloned@@@@");
            }
            //TODO single instance and multi instance need to be handled differently
            if ((type == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) ||
                    (type == DataNode.Type.MULTI_INSTANCE_LEAF_VALUE_NODE)) {
                superBldr.createChildBuilder(name, nmSpc, readLeaf(tempPath))
                        .type(type)
                        .exitNode();
            } else if ((type == DataNode.Type.SINGLE_INSTANCE_NODE) ||
                    (type == DataNode.Type.MULTI_INSTANCE_NODE)) {
                DataNode.Builder tempBldr = superBldr.createChildBuilder(name, nmSpc)
                        .type(type);
                readInner(tempBldr, tempPath);
            } else {
                throw new FailedException("Node type should either be LEAF or INNERNODE");
            }
        });
        superBldr.exitNode();
    }

    public class InternalDocTreeListener implements DocumentTreeListener<DataNode.Type> {
        @Override
        public void event(DocumentTreeEvent<DataNode.Type> event) {
            DynamicConfigEvent.Type type;
            DataNode node;
            ResourceId path;
            switch (event.type()) {
                case CREATED:
                    log.info("key created in store");
                    break;
                case UPDATED:
                    log.info("key updated in store");
                    break;
                case DELETED:
                    log.info("key deleted in store");
                    break;

                default:
            }
            //notify
        }
    }

    public class InternalMapListener implements MapEventListener<ResourceId, LeafNode> {
        @Override
        public void event(MapEvent<ResourceId, LeafNode> event) {
            switch (event.type()) {
                case INSERT:
                    log.info("OBJECT created in store");
                    break;
                case UPDATE:
                    log.info("OBJECT updated in store");
                    break;
                case REMOVE:
                default:
                    log.info("OBJECT removed in store");
                    break;
            }
            //notify
        }
    }
}
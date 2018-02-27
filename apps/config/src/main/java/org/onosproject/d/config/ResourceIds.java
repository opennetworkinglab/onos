/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.d.config;

import com.google.common.annotations.Beta;
import org.apache.commons.text.StringEscapeUtils;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.KeyLeaf;
import org.onosproject.yang.model.LeafListKey;
import org.onosproject.yang.model.LeafListKey.LeafListKeyBuilder;
import org.onosproject.yang.model.ListKey.ListKeyBuilder;
import org.onosproject.yang.model.ListKey;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.ResourceId.Builder;
import org.onosproject.yang.model.SchemaId;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility related to ResourceId.
 */
@Beta
public abstract class ResourceIds {

    private static final Logger log = getLogger(ResourceIds.class);

    /**
     * Root resource Id used by Yang Runtime.
     * (name: {@code "/"}, nameSpace: {@code null})
     */
    public static final ResourceId YRS_ROOT =
            ResourceId.builder().addBranchPointSchema("/", null).build();
    /**
     * Absolute ResourceId pointing at root node.
     * (name: {@link DeviceResourceIds#ROOT_NAME},
     *  nameSpace: {@link DeviceResourceIds#DCS_NAMESPACE})
     */
    public static final ResourceId ROOT_ID = ResourceId.builder()
            .addBranchPointSchema(DeviceResourceIds.ROOT_NAME,
                                  DeviceResourceIds.DCS_NAMESPACE)
            .build();

    /**
     * Builds the ResourceId of specified {@code node}.
     *
     * @param parent ResourceId of {@code node} parent
     * @param node   to create ResourceId.
     * @return ResourceId of {@code node}
     */
    public static ResourceId resourceId(ResourceId parent, DataNode node) {
        final ResourceId.Builder builder;
        if (parent == null) {
            builder = ResourceId.builder();
        } else {
            try {
                builder = parent.copyBuilder();
            } catch (CloneNotSupportedException e) {
                throw new IllegalArgumentException(e);
            }
        }

        SchemaId sid = node.key().schemaId();
        switch (node.type()) {
            case MULTI_INSTANCE_LEAF_VALUE_NODE:
                builder.addLeafListBranchPoint(sid.name(), sid.namespace(),
                                               ((LeafListKey) node.key()).asString());
                break;

            case MULTI_INSTANCE_NODE:
                builder.addBranchPointSchema(sid.name(), sid.namespace());
                for (KeyLeaf keyLeaf : ((ListKey) node.key()).keyLeafs()) {
                    builder.addKeyLeaf(keyLeaf.leafSchema().name(),
                                       keyLeaf.leafSchema().namespace(),
                                       keyLeaf.leafValAsString());
                }
                break;

            case SINGLE_INSTANCE_LEAF_VALUE_NODE:
            case SINGLE_INSTANCE_NODE:
                builder.addBranchPointSchema(sid.name(), sid.namespace());
                break;

            default:
                throw new IllegalArgumentException("Unknown type " + node);

        }

        return builder.build();
    }

    /**
     * Concats {@code path} after {@code prefix}.
     *
     * @param prefix path
     * @param path   to append after {@code path}
     * @return concatenated ResouceId
     */
    public static ResourceId concat(ResourceId prefix, ResourceId path) {
        checkArgument(!path.nodeKeys().contains(DeviceResourceIds.ROOT_NODE),
                      "%s was already absolute path", path);
        try {
            return prefix.copyBuilder().append(path).build();
        } catch (CloneNotSupportedException e) {
            log.error("Could not copy {}", path, e);
            throw new IllegalArgumentException("Could not copy " + path, e);
        }
    }


    /**
     * Returns {@code child} as relative ResourceId against {@code base}.
     *
     * @param base  ResourceId
     * @param child ResourceId to relativize
     * @return relative ResourceId
     */
    public static ResourceId relativize(ResourceId base, ResourceId child) {
        checkArgument(child.nodeKeys().size() >= base.nodeKeys().size(),
                      "%s path must be deeper than base prefix %s", child, base);
        @SuppressWarnings("rawtypes")
        Iterator<NodeKey> bIt = base.nodeKeys().iterator();
        @SuppressWarnings("rawtypes")
        Iterator<NodeKey> cIt = child.nodeKeys().iterator();
        while (bIt.hasNext()) {
            NodeKey<?> b = bIt.next();
            NodeKey<?> c = cIt.next();

            checkArgument(Objects.equals(b, c),
                          "%s is not a prefix of %s.\n" +
                                  "b:%s != c:%s",
                          base, child,
                          b, c);
        }

        return ResourceId.builder().append(child.nodeKeys().subList(base.nodeKeys().size(),
                                                                    child.nodeKeys().size())).build();
    }

    /**
     * Removes the root node from {@code path}.
     *
     * @param path given resource ID
     * @return resource ID without root node
     */
    public static ResourceId removeRootNode(ResourceId path) {
        if (!startsWithRootNode(path)) {
            return path;
        }

        return ResourceId.builder().append(path.nodeKeys().subList(1,
                                                                   path.nodeKeys().size())).build();
    }

    /**
     * Returns the resource ID of the parent data node pointed by {@code path}.
     *
     * @param path resource ID of the given data node
     * @return resource ID of the parent data node
     */
    public static ResourceId parentOf(ResourceId path) {
        try {
            return path.copyBuilder().removeLastKey().build();
        } catch (CloneNotSupportedException e) {
            log.error("Could not copy {}", path, e);
            throw new IllegalArgumentException("Could not copy " + path, e);
        }
    }

    /**
     * Tests if {@code child} starts with {@code prefix}.
     *
     * @param prefix expected
     * @param child  to test
     * @return true if {@code child} starts with {@code prefix}
     */
    public static boolean isPrefix(ResourceId prefix, ResourceId child) {

        return child.nodeKeys().size() >= prefix.nodeKeys().size() &&
                prefix.nodeKeys().equals(child.nodeKeys().subList(0, prefix.nodeKeys().size()));
    }

    /**
     * Tests if {@code path} starts with {@link DeviceResourceIds#ROOT_NAME}.
     *
     * @param path to test
     * @return true if {@code path} starts with {@link DeviceResourceIds#ROOT_NAME}
     */
    public static boolean startsWithRootNode(ResourceId path) {
        return !path.nodeKeys().isEmpty() &&
                DeviceResourceIds.ROOT_NAME.equals(path.nodeKeys().get(0).schemaId().name());
    }


    /**
     * Converts node-identifier element to a NodeKey.
     *
     * @param id to parse (node-identifier fragment between '/')
     * @return NodeKey (warning: returned namespace can be null, which should be interpreted as
     *         same as parent)
     */
    private static NodeKey toNodeKey(String id) {
        Pattern nodeId = Pattern.compile("^((?<prefix>[a-zA-Z_](?:[a-zA-Z0-9_.\\-]*)):)?"
                + "(?<identifier>[a-zA-Z_](?:[a-zA-Z0-9_.-]*))");

        Matcher nidMatcher = nodeId.matcher(id);
        if (!nidMatcher.find()) {
            throw new IllegalArgumentException("node identifier not found in " + id);
        }

        String prefix = nidMatcher.group("prefix");
        String identifier = nidMatcher.group("identifier");

        // key and val pattern is a bit loosened from RFC for simplicity
        Pattern preds = Pattern.compile("\\[\\s*(?<key>[^=\\s]+)\\s*=\\s*\\\"(?<val>[^\\]]+)\\\"\\s*\\]");
        Matcher predMatcher = preds.matcher(id);
        predMatcher.region(nidMatcher.end(), id.length());
        LeafListKeyBuilder llkb = null;
        ListKeyBuilder llb = null;
        while (predMatcher.find()) {
            String key = predMatcher.group("key");
            String val = predMatcher.group("val");
            if (key.equals(".")) {
                // LeafList
                if (llkb == null) {
                    llkb = new LeafListKeyBuilder();
                }
                llkb.schemaId(identifier, prefix)
                .value(val);
            } else {
                // ListKey
                if (llb == null) {
                    llb = new ListKeyBuilder();
                }
                llb.schemaId(identifier, prefix);
                Matcher m = nodeId.matcher(key);
                m.matches();
                llb.addKeyLeaf(m.group("identifier"), m.group("prefix"), val);
            }
        }
        if (llkb != null) {
            return llkb.build();
        } else if (llb != null) {
            return llb.build();
        } else {
            return NodeKey.builder().schemaId(identifier, prefix).build();
        }
    }


    /**
     * Add {@link #YRS_ROOT} prefix if not already.
     *
     * @param rid resource id
     * @return ResourceId starting from {@link #YRS_ROOT}
     */
    public static ResourceId prefixYrsRoot(ResourceId rid) {
        if (rid == null) {
            return YRS_ROOT;
        }

        if (isPrefix(YRS_ROOT, rid)) {
            return rid;
        }

        if (isPrefix(ROOT_ID, rid)) {
            return concat(YRS_ROOT, relativize(ROOT_ID, rid));
        }

        return concat(YRS_ROOT, rid);
    }

    /**
     * Add {@link #ROOT_ID} prefix if not already.
     *
     * @param rid resource id
     * @return ResourceId starting from {@link #ROOT_ID}
     */
    public static ResourceId prefixDcsRoot(ResourceId rid) {
        if (rid == null) {
            return ROOT_ID;
        }

        if (isPrefix(ROOT_ID, rid)) {
            return rid;
        }

        // test and replace YangRuntime root?
        if (isPrefix(YRS_ROOT, rid)) {
            return concat(ROOT_ID, relativize(YRS_ROOT, rid));
        }

        return concat(ROOT_ID, rid);
    }


    /**
     * Converts instance-identifier String into a ResourceId.
     *
     * @param input instance-identifier
     * @return Corresponding ResourceId relative to root or null if {@code iid} is '/'
     * Returned ResourceId will not have root NodeKey in it's path.
     * consider using {@link #prefixDcsRoot(ResourceId)},
     * {@link #prefixYrsRoot(ResourceId)} to add them.
     */
    public static ResourceId fromInstanceIdentifier(String input) {

        String[] nodes = input.split("/");
        List<NodeKey> nodeKeys = Arrays.stream(nodes)
            .filter(s -> !s.isEmpty())
            .map(ResourceIds::toNodeKey)
            .collect(Collectors.toList());

        if (nodeKeys.isEmpty()) {
            return null;
        }

        Builder builder = ResourceId.builder();

        // fill-in null (=inherit from parent) nameSpace
        String lastNamespace = null;
        for (NodeKey nodeKey : nodeKeys) {
            if (nodeKey.schemaId().namespace() != null) {
                lastNamespace = nodeKey.schemaId().namespace();
            }
            if (nodeKey instanceof LeafListKey) {
                builder.addLeafListBranchPoint(nodeKey.schemaId().name(),
                                               firstNonNull(nodeKey.schemaId().namespace(), lastNamespace),
                                               ((LeafListKey) nodeKey).value());

            } else if (nodeKey instanceof ListKey) {
                builder.addBranchPointSchema(nodeKey.schemaId().name(), lastNamespace);
                for (KeyLeaf kl : ((ListKey) nodeKey).keyLeafs()) {
                    builder.addKeyLeaf(kl.leafSchema().name(),
                                       firstNonNull(kl.leafSchema().namespace(), lastNamespace),
                                       kl.leafValue());
                }
            } else {
                builder.addBranchPointSchema(nodeKey.schemaId().name(), lastNamespace);
            }
        }
        return builder.build();
    }


    /**
     * Converts ResourceId to instance-identifier.
     *
     * @param rid to convert
     * @return instance-identifier
     *
     * @see <a href="https://tools.ietf.org/html/rfc7951#section-6.11">RFC 7951</a>
     * @see <a href="https://tools.ietf.org/html/rfc7950#section-14">RFC 7950 for ABNF</a>
     */
    public static String toInstanceIdentifier(ResourceId rid) {
        StringBuilder s = new StringBuilder();

        String lastNamespace = null;
        for (NodeKey nk : rid.nodeKeys()) {
            if (nk.schemaId().name().equals("/")) {
                // special handling for root nodeKey: skip it
                // YANG runtime root: null:/
                // DCS root: org.onosproject.dcs:/
                continue;
            }

            s.append('/');

            if (!Objects.equals(lastNamespace, nk.schemaId().namespace())) {
                s.append(nk.schemaId().namespace());
                s.append(':');
                lastNamespace = nk.schemaId().namespace();
            }
            s.append(nk.schemaId().name());

            if (nk instanceof LeafListKey) {
                LeafListKey llk = (LeafListKey) nk;
                s.append('[');
                s.append('.');

                s.append('=');

                s.append('"')
                 .append(StringEscapeUtils.escapeJson(llk.asString()))
                 .append('"');
                s.append(']');

            } else if (nk instanceof ListKey) {
                ListKey lk = (ListKey) nk;

                for (KeyLeaf kl : lk.keyLeafs()) {
                    s.append('[');

                    if (!Objects.equals(kl.leafSchema().namespace(), lastNamespace)) {
                        s.append(kl.leafSchema().namespace());
                        s.append(':');
                    }
                    s.append(kl.leafSchema().name());

                    s.append('=');

                    s.append('"')
                     .append(StringEscapeUtils.escapeJson(kl.leafValAsString()))
                     .append('"');
                    s.append(']');
                }
            } else {
                // normal NodeKey
                // nothing to do
            }
        }
        if (s.length() == 0) {
            return "/";
        }
        return s.toString();
    }

}

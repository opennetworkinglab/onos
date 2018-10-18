/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.odtn.utils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.AnnotatedNodeInfo;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultCompositeData;
import org.onosproject.yang.runtime.DefaultRuntimeContext;
import org.onosproject.yang.runtime.RuntimeContext;
import org.onosproject.yang.runtime.YangRuntimeService;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.annotations.Beta;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;

@Beta
@Component(immediate = true)
public class YangToolUtil {
    private static final Logger log = getLogger(YangToolUtil.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected YangRuntimeService yangRuntimeService;
    protected static YangRuntimeService yrs;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ModelConverter modelConverter;
    protected static ModelConverter converter;

    @Activate
    protected void activate() {
        yrs = yangRuntimeService;
        converter = modelConverter;
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    protected static void initStaticContext() {
        if (yrs == null) {
            yrs = DefaultServiceDirectory.getService(YangRuntimeService.class);
        }
        if (converter == null) {
            converter = DefaultServiceDirectory.getService(ModelConverter.class);
        }
    }

    /**
     * Converts XML Document into CharSequence.
     *
     * @param xmlInput to convert
     * @return CharSequence
     */
    public static CharSequence toCharSequence(Document xmlInput) {
        return toCharSequence(xmlInput, true);
    }

    /**
     * Converts XML Document into CharSequence.
     *
     * @param xmlInput to convert
     * @param omitXmlDecl or not
     * @return CharSequence
     */
    public static CharSequence toCharSequence(Document xmlInput, boolean omitXmlDecl) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            if (omitXmlDecl) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(xmlInput), new StreamResult(writer));
            return writer.getBuffer();
        } catch (TransformerException e) {
            log.error("Exception thrown", e);
            return null;
        }
    }

    /**
     * Converts JsonNode into CharSequence.
     *
     * @param jsonInput to convert
     * @return CharSequence
     */
    public static CharSequence toCharSequence(JsonNode jsonInput) {
        checkNotNull(jsonInput);
        ObjectMapper mapper = new ObjectMapper();
        // TODO following pretty printing option should be removed
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            return mapper.writerWithDefaultPrettyPrinter()
                         .writeValueAsString(jsonInput);
        } catch (JsonProcessingException e) {
            log.error("Exception thrown", e);
            return null;
        }
    }

    /**
     * Converts UTF-8 CompositeStream into CharSequence.
     *
     * @param utf8Input to convert
     * @return CharSequence
     */
    public static CharSequence toCharSequence(CompositeStream utf8Input) {
        StringBuilder s = new StringBuilder();
        try {
            CharStreams.copy(new InputStreamReader(utf8Input.resourceData(), UTF_8), s);
            return s;
        } catch (IOException e) {
            log.error("Exception thrown", e);
            return null;
        }
    }

    /**
     * Converts JSON CompositeStream into JsonNode.
     *
     * @param jsonInput to convert
     * @return JsonNode
     */
    public static JsonNode toJsonNode(CompositeStream jsonInput) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(jsonInput.resourceData());
        } catch (IOException e) {
            log.error("Exception thrown", e);
            return null;
        }
    }

    /**
     * Converts XML CompositeStream into XML Document.
     *
     * @param xmlInput to convert
     * @return Document
     */
    public static Document toDocument(CompositeStream xmlInput) {
        try {
            return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                .parse(new InputSource(new InputStreamReader(xmlInput.resourceData(), UTF_8)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Exception thrown", e);
            return null;
        }
    }

    /**
     * Converts XML source into XML Document.
     *
     * @param xmlInput to convert
     * @return Document
     */
    public static Document toDocument(CharSource xmlInput) {
        try {
            return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                .parse(new InputSource(xmlInput.openStream()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Exception thrown", e);
            return null;
        }
    }

    /**
     * Converts CompositeData into XML CompositeStream.
     *
     * @param input CompositeData to convert
     * @return XML CompositeStream
     */
    public static CompositeStream toXmlCompositeStream(CompositeData input) {
        initStaticContext();
        RuntimeContext yrtContext = new DefaultRuntimeContext.Builder()
                .setDataFormat("xml")
                // Following does not have any effect?
                //.addAnnotation(XMLNS_XC_ANNOTATION)
                .build();
        CompositeStream xml = yrs.encode(input, yrtContext);
        return xml;
    }

    /**
     * Converts CompositeData into JSON CompositeStream.
     *
     * @param input CompositeData to convert
     * @return JSON CompositeStream
     */
    public static CompositeStream toJsonCompositeStream(CompositeData input) {
        initStaticContext();
        RuntimeContext yrtContext = new DefaultRuntimeContext.Builder()
                .setDataFormat("JSON")
                .build();
        CompositeStream xml = yrs.encode(input, yrtContext);
        return xml;
    }

    /**
     * Converts ResourceData into CompositeData.
     *
     * @param input ResourceData to convert
     * @return CompositeData
     */
    public static CompositeData toCompositeData(ResourceData input) {
        CompositeData.Builder builder =
                DefaultCompositeData.builder();
        builder.resourceData(input);
        // remove, merge, replace, ...
        //builder.addAnnotatedNodeInfo(info)

        return builder.build();
    }

    /**
     * Converts ResourceData & AnnotatedNodeInfo into CompositeData.
     *
     * @param input ResourceData to convert
     * @param annotatedNodeInfos AnnotatedNodeInfoList to convert
     * @return CompositeData
     */
    public static CompositeData toCompositeData(
            ResourceData input,
            List<AnnotatedNodeInfo> annotatedNodeInfos) {
        CompositeData.Builder builder =
                DefaultCompositeData.builder();
        builder.resourceData(input);

        // Set AnnotationNodeInfo
        annotatedNodeInfos.stream()
                .forEach(a -> builder.addAnnotatedNodeInfo(a));

        return builder.build();
    }

    /**
     * Converts DataNode into ResourceData.
     *
     * @param resourceId pointing to parent of {@code dataNode}, YANG-wise.
     * @param dataNode to convert, must be InnerNode
     * @return ResourceData
     */
    public static ResourceData toResourceData(ResourceId resourceId, DataNode dataNode) {
        DefaultResourceData.Builder builder = DefaultResourceData.builder();
        builder.resourceId(checkNotNull(resourceId));
        if (dataNode instanceof InnerNode) {
            builder.addDataNode(dataNode);
        } else {
            log.error("Unexpected DataNode encountered {}", dataNode);
        }
        return builder.build();
    }

    /**
     * Converts ModelObject into a DataNode.
     *
     * @param input ModelOject
     * @return DataNode
     */
    public static DataNode toDataNode(ModelObject input) {
        // FIXME this converter will work with root-level nodes only.
        initStaticContext();
        ModelObjectData modelData = DefaultModelObjectData.builder()
                .addModelObject(input)
                .identifier(null)
                .build();

        ResourceData rnode = converter.createDataNode(modelData);
        if (rnode.dataNodes().isEmpty()) {
            log.error("input did not result in any datanode. {}", input);
            return null;
        }
        return rnode.dataNodes().get(0);
    }
}

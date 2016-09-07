package org.onosproject.driver.optical.extensions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.ExtensionTreatmentCodec;
import org.onosproject.driver.extensions.OplinkAttenuation;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for Oplink extensions.
 */
public class OplinkExtensionTreatmentCodec extends AbstractHandlerBehaviour
        implements ExtensionTreatmentCodec {

    private static final String TYPE = "type";
    private static final String MISSING_TYPE = "Missing extension type";
    private static final String UNSUPPORTED_TYPE = "Extension type is not supported: ";

    @Override
    public ExtensionTreatment decode(ObjectNode objectNode, CodecContext context) {
        if (objectNode == null || !objectNode.isObject()) {
            return null;
        }

        int typeInt = nullIsIllegal(objectNode.get(TYPE), MISSING_TYPE).asInt();
        ExtensionTreatmentType type = new ExtensionTreatmentType(typeInt);

        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OPLINK_ATTENUATION.type())) {
            return decodeTreatment(objectNode, context, OplinkAttenuation.class);
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_TYPE + type.toString());
        }
    }

    private <T extends ExtensionTreatment> ExtensionTreatment decodeTreatment(
            ObjectNode objectNode, CodecContext context, Class<T> entityClass) {
        if (context == null) {
            ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
            return serviceDirectory.get(CodecService.class).getCodec(entityClass).decode(objectNode, null);
        } else {
            return context.codec(entityClass).decode(objectNode, context);
        }
    }
}

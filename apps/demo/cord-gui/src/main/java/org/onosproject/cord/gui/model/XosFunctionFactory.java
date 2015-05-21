package org.onosproject.cord.gui.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility factory for operating on XOS functions.
 */
public class XosFunctionFactory extends JsonFactory {

    private static final String PARAMS = "params";

    private static final String LEVEL = "level";
    private static final String LEVELS = "levels";


    // URL Filtering Levels...
    private static final String PG = "PG";
    private static final String PG13 = "PG-13";
    private static final String R = "R";

    private static final String[] FILTER_LEVELS = { PG, PG13, R };
    private static final String DEFAULT_FILTER_LEVEL = PG;


    // no instantiation
    private XosFunctionFactory() {}

    /**
     * Produces the JSON representation of the given XOS function descriptor.
     *
     * @param xfd function descriptor
     * @return JSON encoding
     */
    public static ObjectNode toObjectNode(XosFunctionDescriptor xfd) {
        ObjectNode root = objectNode()
                .put(ID, xfd.id())
                .put(NAME, xfd.displayName())
                .put(DESC, xfd.description());
        root.set(PARAMS, paramsForXfd(xfd));
        return root;
    }

    private static JsonNode paramsForXfd(XosFunctionDescriptor xfd) {
        ParamStructFactory psf = PARAM_MAP.get(xfd);
        if (psf == null) {
            psf = DEF_PARAMS;
        }
        return psf.params();
    }

    // ==== handling different parameter structures...
    private static final Map<XosFunctionDescriptor, ParamStructFactory>
        PARAM_MAP = new HashMap<XosFunctionDescriptor, ParamStructFactory>();

    private static final ParamStructFactory DEF_PARAMS = new ParamStructFactory();
    static {
        PARAM_MAP.put(XosFunctionDescriptor.URL_FILTER, new UrlFilterParams());
    }

    // private parameter structure creator
    static class ParamStructFactory {
        ObjectNode params() {
            return objectNode();
        }
    }

    static class UrlFilterParams extends ParamStructFactory {
        @Override
        ObjectNode params() {
            ObjectNode result = objectNode();
            result.put(LEVEL, DEFAULT_FILTER_LEVEL);
            ArrayNode levels = arrayNode();
            for (String lvl: FILTER_LEVELS) {
                levels.add(lvl);
            }
            result.set(LEVELS, levels);
            return result;
        }
    }
}

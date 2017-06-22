package cz.inqool.arclib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

public class JsonHelper {

    /**
     * Performs deep merge of two json objects. If there are two equally named attributes in both objects, the source json has the priority
     * over the target json.
     * json.
     * @param target target json object
     * @param source source json object
     * @return resulting json object
     */
    public static JsonNode merge(final JsonNode target, final JsonNode source) {
        if (target instanceof ArrayNode && source instanceof ArrayNode) {
            // Both the target and source are array nodes, then append the
            // contents of the source array to the target array without
            // performing a deep copy. This is safe because either:
            // 1) Successive merges are also arrays and are appended.
            // 2) Successive merge(s) are not arrays and replace the array.
            // Note: this assumes that the target node is modifiable.
            ((ArrayNode) target).addAll((ArrayNode) source);
            return target;
        } else if (target instanceof ObjectNode && source instanceof ObjectNode) {
            // Both the target and source are object nodes, then recursively
            // merge the fields of the source node over the same fields in
            // the target node. Any unmatched fields from the source node are
            // simply added to the target node; this requires a deep copy
            // since subsequent merges may modify it.
            final Iterator<Map.Entry<String, JsonNode>> iterator = source.fields();
            while (iterator.hasNext()) {
                final Map.Entry<String, JsonNode> sourceFieldEntry = iterator.next();
                final JsonNode targetFieldValue = target.get(sourceFieldEntry.getKey());
                if (targetFieldValue != null) {
                    // Recursively merge the source field value into the target
                    // field value and replace the target value with the result.
                    final JsonNode newTargetFieldValue = merge(targetFieldValue, sourceFieldEntry.getValue());
                    ((ObjectNode) target).set(sourceFieldEntry.getKey(), newTargetFieldValue);
                } else {
                    // Add a deep copy of the source field to the target.
                    ((ObjectNode) target).set(sourceFieldEntry.getKey(), sourceFieldEntry.getValue().deepCopy());
                }
            }
            return target;
        } else {
            // The target and source nodes are of different types. Replace the
            // target node with the source node. This requires a deep copy of
            // the source node since subsequent merges may modify it.
            return source.deepCopy();
        }
    }

}

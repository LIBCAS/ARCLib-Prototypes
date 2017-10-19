package cz.cas.lib.arclib.solr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SolrDocument(solrCoreName = "arclib_xml")
public class ArclibXmlDocument {

    @Field
    @Indexed
    private String id;

    @Field
    @Indexed
    private String document;

    @Field("*")
    @Indexed
    @Dynamic
    private Map<String, Object> attributes = new HashMap<>();

    public void addAttribute(String attributeKey, Object newAttributeValue) {
        if (attributes.containsKey(attributeKey)) {
            Object oldAttrValue = attributes.get(attributeKey);
            if (oldAttrValue instanceof Set)
                ((HashSet) oldAttrValue).add(newAttributeValue);
            else {
                Set<Object> attributeValues = new HashSet<>();
                attributeValues.add(attributes.get(attributeKey));
                attributeValues.add(newAttributeValue);
                attributes.put(attributeKey, attributeValues);
            }
        } else
            attributes.put(attributeKey, newAttributeValue);
    }

    public void replaceAttribute(String attributeKey, Object oldAttributeValue, Object newAttributeValue) {
        if (attributes.containsKey(attributeKey)) {
            Object oldAttrValue = attributes.get(attributeKey);
            if (oldAttrValue instanceof Set) {
                Set<Object> attributeValues = ((HashSet) oldAttrValue);
                attributeValues.remove(oldAttributeValue);
                attributeValues.add(newAttributeValue);
                return;
            }
        }
        attributes.put(attributeKey, newAttributeValue);
    }
}
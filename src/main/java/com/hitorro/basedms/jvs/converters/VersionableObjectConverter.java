/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.basedms.jvs.converters;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.Container;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.jvs.ConversionContext;
import com.hitorro.basedms.jvs.ConversionException;
import com.hitorro.basedms.jvs.ConversionOptions;
import com.hitorro.basedms.jvs.DMSToJVSConverter;
import com.hitorro.basedms.jvs.content.ContentTextExtractor;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.JsonTypeSystem;
import com.hitorro.jsontypesystem.Type;
import com.hitorro.util.objects.EmbeddableDomainValue;

import java.util.Set;

/**
 * Default converter for VersionableObject to JVS format.
 * Maps all standard VersionableObject fields to JVS structure.
 */
public class VersionableObjectConverter implements DMSToJVSConverter<VersionableObject> {
    
    private static final String TYPE_NAME = "dm_versionable_object";
    private static final String DOMAIN_DMS = "dms";
    
    @Override
    public JVS convert(VersionableObject dmsObject, ConversionContext context) throws ConversionException {
        if (dmsObject == null) {
            throw new ConversionException("Cannot convert null VersionableObject");
        }
        
        ConversionOptions options = context.getOptions();
        
        // Get or create type
        Type type = getType(context);
        
        // Create JVS - if type is null, create without type and build JSON manually
        JVS jvs;
        if (type != null) {
            jvs = new JVS(type);
        } else {
            // Create JVS from raw JSON when type is not available
            com.fasterxml.jackson.databind.node.ObjectNode root = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            root.put("type", TYPE_NAME);
            jvs = new JVS(root);
            com.hitorro.basedms.Log.basedms.warn("Type '{}' not found in type system, creating JVS without type validation", TYPE_NAME);
        }
        
        try {
            // Map ID fields
            mapId(jvs, dmsObject);
            
            // Map times
            mapTimes(jvs, dmsObject);
            
            // Map metadata
            mapMetadata(jvs, dmsObject);
            
            // Map categories if enabled
            if (options.isIncludeCategories()) {
                mapCategories(jvs, dmsObject);
            }
            
            // Map version references if enabled
            if (options.isIncludeVersionReferences()) {
                mapVersionReferences(jvs, dmsObject);
            }
            
            // Map container references if enabled
            if (options.isIncludeContainerReferences()) {
                mapContainerReferences(jvs, dmsObject);
            }
            
            // Extract and map content if enabled
            if (options.isIncludeContent() && options.isExtractTextContent()) {
                mapContent(jvs, dmsObject, context.getContentExtractor());
            }
            
        } catch (Exception e) {
            throw new ConversionException("Error converting VersionableObject to JVS", e);
        }
        
        return jvs;
    }
    
    @Override
    public Class<VersionableObject> getTargetClass() {
        return VersionableObject.class;
    }
    
    /**
     * Map ID fields: id.did and id.domain
     */
    private void mapId(JVS jvs, VersionableObject obj) {
        try {
            jvs.set("id.did", obj.getGuid());
            jvs.set("id.domain", DOMAIN_DMS);
        } catch (Exception e) {
            // Log but continue
            com.hitorro.basedms.Log.basedms.error("Error mapping ID fields", e);
        }
    }
    
    /**
     * Map time fields: times.created, times.modified, times.authored
     */
    private void mapTimes(JVS jvs, VersionableObject obj) {
        try {
            if (obj.getCreationDate() != null) {
                jvs.set("times.created", obj.getCreationDate().getTime());
            }
            if (obj.getModifiedDate() != null) {
                jvs.set("times.modified", obj.getModifiedDate().getTime());
            }
            if (obj.getAuthoredDate() != null) {
                jvs.set("times.authored", obj.getAuthoredDate().getTime());
            }
        } catch (Exception e) {
            com.hitorro.basedms.Log.basedms.error("Error mapping time fields", e);
        }
    }
    
    /**
     * Map metadata fields: creator, realm, versionLabel, note
     */
    private void mapMetadata(JVS jvs, VersionableObject obj) {
        try {
            if (obj.getCreator() != null) {
                jvs.set("metadata.creator", obj.getCreator());
            }
            if (obj.getRealm() != null) {
                jvs.set("metadata.realm", obj.getRealm());
            }
            if (obj.getVersionLabel() != null) {
                jvs.set("metadata.versionLabel", obj.getVersionLabel());
            }
            if (obj.getNote() != null) {
                jvs.set("metadata.note", obj.getNote());
            }
        } catch (Exception e) {
            com.hitorro.basedms.Log.basedms.error("Error mapping metadata fields", e);
        }
    }
    
    /**
     * Map categories as array of {domain, value} objects
     */
    private void mapCategories(JVS jvs, VersionableObject obj) {
        try {
            Set<com.hitorro.util.core.valuemap.DomainValueIntf> categories = obj.getCategories();
            if (categories != null && !categories.isEmpty()) {
                ArrayNode categoriesArray = JsonNodeFactory.instance.arrayNode();
                
                for (com.hitorro.util.core.valuemap.DomainValueIntf dv : categories) {
                    ObjectNode categoryNode = JsonNodeFactory.instance.objectNode();
                    categoryNode.put("domain", dv.getDomain());
                    categoryNode.put("value", dv.getValue());
                    categoriesArray.add(categoryNode);
                }
                
                jvs.set("categories", categoriesArray);
            }
        } catch (Exception e) {
            com.hitorro.basedms.Log.basedms.error("Error mapping categories", e);
        }
    }
    
    /**
     * Map version references (parent version)
     */
    private void mapVersionReferences(JVS jvs, VersionableObject obj) {
        try {
            // Map parent version reference
            if (obj.getParentVersion() != null) {
                jvs.set("parent.did", obj.getParentVersion().getGuid());
            }
            
            // Optionally map canonical version reference
            if (obj.getCanonical() != null && obj.getCanonical() != obj) {
                jvs.set("canonical.did", obj.getCanonical().getGuid());
            }
        } catch (Exception e) {
            com.hitorro.basedms.Log.basedms.error("Error mapping version references", e);
        }
    }
    
    /**
     * Map container references
     */
    private void mapContainerReferences(JVS jvs, VersionableObject obj) {
        try {
            Container owningContainer = obj.getOwningContainer();
            if (owningContainer != null) {
                jvs.set("container.did", owningContainer.getGuid());
            }
        } catch (Exception e) {
            com.hitorro.basedms.Log.basedms.error("Error mapping container references", e);
        }
    }
    
    /**
     * Extract and map text content from Content objects
     */
    private void mapContent(JVS jvs, VersionableObject obj, ContentTextExtractor extractor) {
        try {
            Set<Content> contents = obj.getContents();
            if (contents == null || contents.isEmpty()) {
                com.hitorro.basedms.Log.basedms.debug("No contents found for document: {}", obj.getGuid());
                return;
            }
            
            com.hitorro.basedms.Log.basedms.debug("Found {} content objects for document: {}", 
                                                  contents.size(), obj.getGuid());
            
            StringBuilder fullText = new StringBuilder();
            
            for (Content content : contents) {
                com.hitorro.basedms.Log.basedms.debug("Processing content: hasStringValue={}, contentType={}",
                                                      content.hasStringValue(),
                                                      content.getContentType() != null ? content.getContentType().getMimeType() : "null");
                
                String text = extractor.extractText(content);
                if (text != null && !text.isEmpty()) {
                    com.hitorro.basedms.Log.basedms.debug("Extracted text length: {}", text.length());
                    if (fullText.length() > 0) {
                        fullText.append("\n\n");
                    }
                    fullText.append(text);
                } else {
                    com.hitorro.basedms.Log.basedms.debug("No text extracted from content");
                }
            }
            
            if (fullText.length() > 0) {
                jvs.set("fullText", fullText.toString());
                com.hitorro.basedms.Log.basedms.info("Mapped fullText with {} characters for document: {}", 
                                                     fullText.length(), obj.getGuid());
            } else {
                com.hitorro.basedms.Log.basedms.warn("No text extracted from any content for document: {}", obj.getGuid());
            }
            
        } catch (Exception e) {
            com.hitorro.basedms.Log.basedms.error("Error extracting content text", e);
        }
    }
    
    /**
     * Get the Type for this converter, creating it if necessary
     */
    private Type getType(ConversionContext context) {
        // Check cache first
        Type type = context.getTypeCache().get(TYPE_NAME);
        if (type != null) {
            return type;
        }
        
        // Try to get from global type system
        type = JsonTypeSystem.getMe().getType(TYPE_NAME);
        if (type != null) {
            context.getTypeCache().put(TYPE_NAME, type);
            return type;
        }
        
        // If not found, we'll need to create a basic type
        // For now, return null and let JVS handle it
        // In production, the type should be loaded from JSON config
        return null;
    }
}

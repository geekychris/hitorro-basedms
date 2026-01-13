package com.hitorro.base.objects;

import com.hitorro.util.typesystem.BaseType;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import jakarta.persistence.*;

/**
 * RenditionDescriptor - describes a rendition of content (images, videos, etc.)
 * 
 * A rendition is a transformed version of content, such as a thumbnail image,
 * a compressed video, or a format conversion.
 */
@Entity
@Table(name = "RenditionDescriptor")
@TypeClassMetaInfo(
    shortTypeName = "RD",  // Using "RD" as the short type name for RenditionDescriptor
    isView = false,
    isPersisted = true,
    schemaVersion = 1
)
public class RenditionDescriptor extends BaseType {
    public static final int SerializationVersion = 1;
    
    @Column(name = "m_name")
    private String m_name;
    
    @Column(name = "m_description")
    private String m_description;
    
    @Column(name = "m_width")
    private Integer m_width;
    
    @Column(name = "m_height")
    private Integer m_height;
    
    @Column(name = "m_format")
    private String m_format;
    
    public RenditionDescriptor() {
        super();
    }
    
    // Getters and Setters
    
    public String getName() {
        return m_name;
    }
    
    public void setName(String name) {
        this.m_name = name;
    }
    
    public String getDescription() {
        return m_description;
    }
    
    public void setDescription(String description) {
        this.m_description = description;
    }
    
    public Integer getWidth() {
        return m_width;
    }
    
    public void setWidth(Integer width) {
        this.m_width = width;
    }
    
    public Integer getHeight() {
        return m_height;
    }
    
    public void setHeight(Integer height) {
        this.m_height = height;
    }
    
    public String getFormat() {
        return m_format;
    }
    
    public void setFormat(String format) {
        this.m_format = format;
    }
    
    @Override
    public int getSerializationVersion() {
        return SerializationVersion;
    }
}

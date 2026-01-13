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
    
    @Column(name = "name")

    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "width")
    private Integer width;
    
    @Column(name = "m_height")
    private Integer height;
    
    @Column(name = "m_format")
    private String format;
    
    public RenditionDescriptor() {
        super();
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getWidth() {
        return width;
    }
    
    public void setWidth(Integer width) {
        this.width = width;
    }
    
    public Integer getHeight() {
        return height;
    }
    
    public void setHeight(Integer height) {
        this.height = height;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    @Override
    public int getSerializationVersion() {
        return SerializationVersion;
    }
}

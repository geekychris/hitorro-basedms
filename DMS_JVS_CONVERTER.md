# DMS to JSON Type System (JVS) Converter

## Overview

This module provides conversion utilities for transforming DMS (Document Management System) Hibernate-backed objects into JSON Type System (JVS) format. The converter supports VersionableObject and its subclasses, with extensible support for custom converters.

## Features

- **Automatic Conversion**: Convert DMS objects to JVS with a simple API call
- **Content Extraction**: Extract text from indexable Content objects
- **Extensible Registry**: Register custom converters for specialized DMS types
- **Thread-Safe**: All registry operations are thread-safe
- **Configurable**: Control what gets included in conversions via options
- **Type-Safe**: Full type support with generic converters

## Quick Start

### Basic Conversion

```java
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.jvs.DMSToJVSMapper;
import com.hitorro.jsontypesystem.JVS;

// Convert a single object
VersionableObject dmsObject = // ... get from database
JVS jvs = DMSToJVSMapper.convert(dmsObject);

// Access converted data
String docId = jvs.getString("id.did");
String domain = jvs.getString("id.domain");
Long createdTime = jvs.getLong("times.created");
```

### Batch Conversion

```java
List<VersionableObject> dmsObjects = // ... get from database
List<JVS> jvsList = DMSToJVSMapper.convertBatch(dmsObjects);
```

### Custom Options

```java
import com.hitorro.basedms.jvs.ConversionOptions;

ConversionOptions options = ConversionOptions.builder()
    .includeContent(true)
    .extractTextContent(true)
    .includeCategories(true)
    .includeVersionReferences(true)
    .build();

JVS jvs = DMSToJVSMapper.convert(dmsObject, options);
```

## Field Mapping

### VersionableObject → JVS Mapping

| DMS Field | JVS Field | Type | Notes |
|-----------|-----------|------|-------|
| `guid` | `id.did` | String | Document identifier |
| (constant) | `id.domain` | String | Always "dms" |
| `creationDate` | `times.created` | Long | Timestamp in milliseconds |
| `modifiedDate` | `times.modified` | Long | Timestamp in milliseconds |
| `authoredDate` | `times.authored` | Long | Timestamp in milliseconds |
| `creator` | `metadata.creator` | String | |
| `realm` | `metadata.realm` | String | |
| `versionLabel` | `metadata.versionLabel` | String | |
| `note` | `metadata.note` | String | |
| `categories` | `categories[]` | Array | Array of {domain, value} objects |
| `parentVersion.guid` | `parent.did` | String | Reference to parent version |
| `canonical.guid` | `canonical.did` | String | Reference to canonical version |
| `owningContainer.guid` | `container.did` | String | Reference to container |
| `contents` (text) | `fullText` | String | Concatenated text from all indexable content |

## Type Definitions

The module includes JSON type definitions for:

- `dm_versionable_object` - Main document type
- `dm_id` - ID structure with did and domain
- `dm_times` - Timestamp structure
- `dm_metadata` - Metadata fields
- `dm_category` - Category domain/value pair
- `dm_reference` - Reference to another document

Type definitions are located in `src/main/resources/jsontypes/dms/`.

## Custom Converters

### Creating a Custom Converter

```java
import com.hitorro.basedms.jvs.DMSToJVSConverter;
import com.hitorro.basedms.jvs.ConversionContext;
import com.hitorro.basedms.jvs.ConversionException;

public class MyDocumentConverter implements DMSToJVSConverter<MyDocument> {
    
    @Override
    public JVS convert(MyDocument dmsObject, ConversionContext context) 
            throws ConversionException {
        // Call base converter first
        DMSToJVSConverter<VersionableObject> baseConverter = 
            DMSToJVSConverterRegistry.getInstance().getConverter(VersionableObject.class);
        JVS jvs = baseConverter.convert(dmsObject, context);
        
        // Add custom fields
        jvs.set("customField", dmsObject.getCustomField());
        
        return jvs;
    }
    
    @Override
    public Class<MyDocument> getTargetClass() {
        return MyDocument.class;
    }
}
```

### Registering a Custom Converter

```java
DMSToJVSMapper.registerConverter(MyDocument.class, new MyDocumentConverter());
```

## Content Extraction

By default, the converter extracts text from Content objects with these MIME types:

- `text/plain`
- `text/html`
- `text/xml`
- `application/xml`
- `application/json`
- `text/csv`
- `text/markdown`

Additional text-based types (text/*) are also supported.

### Custom Content Extractor

```java
import com.hitorro.basedms.jvs.content.ContentTextExtractor;
import com.hitorro.basedms.jvs.ConversionContext;

ContentTextExtractor customExtractor = new MyContentExtractor();

ConversionContext context = ConversionContext.builder()
    .contentExtractor(customExtractor)
    .build();

JVS jvs = DMSToJVSMapper.convert(dmsObject, context);
```

## Configuration

Configuration file: `src/main/resources/dms-jvs-config.json`

```json
{
  "indexableContentTypes": [
    "text/plain",
    "text/html",
    "application/json"
  ]
}
```

## Architecture

### Core Components

1. **DMSToJVSMapper** - Main API facade
2. **DMSToJVSConverter** - Strategy interface for converters
3. **DMSToJVSConverterRegistry** - Thread-safe converter registry
4. **ConversionContext** - Provides context and resources for conversion
5. **ConversionOptions** - Configuration for conversion behavior
6. **ContentTextExtractor** - Interface for content text extraction
7. **VersionableObjectConverter** - Default converter implementation

### Package Structure

```
com.hitorro.basedms.jvs/
├── DMSToJVSMapper.java           - Main API
├── DMSToJVSConverter.java        - Converter interface
├── DMSToJVSConverterRegistry.java - Registry
├── ConversionContext.java        - Context
├── ConversionOptions.java        - Options
├── ConversionException.java      - Exception
├── content/
│   ├── ContentTextExtractor.java         - Extractor interface
│   └── DefaultContentTextExtractor.java  - Default implementation
└── converters/
    └── VersionableObjectConverter.java   - Default converter
```

## Integration Examples

### With Lucene Indexing

```java
import com.hitorro.index.indexer.JVSLuceneIndexWriter;

// Convert DMS objects to JVS
List<VersionableObject> dmsObjects = repository.findAll();
List<JVS> jvsList = DMSToJVSMapper.convertBatch(dmsObjects);

// Index in Lucene
try (JVSLuceneIndexWriter indexWriter = new JVSLuceneIndexWriter(config)) {
    indexWriter.indexDocuments(jvsList);
    indexWriter.commit();
}
```

### With MongoDB Storage

```java
import com.hitorro.mongo.JVSReactiveMongoClient;

// Convert and store in MongoDB
JVS jvs = DMSToJVSMapper.convert(dmsObject);

JVSReactiveMongoClient mongoClient = // ... create client
mongoClient.insertOne("documents", jvs).subscribe();
```

## Performance Considerations

- **Converter Lookup Caching**: Converters are cached after first lookup
- **Type Caching**: Type definitions are cached in ConversionContext
- **Shared Context**: Use same ConversionContext for batch operations
- **Lazy Content Loading**: Content is only loaded if options enable it

## Future Enhancements

- **Spring Integration**: Auto-registration via @DMSConverter annotation
- **Advanced Content Extraction**: Apache POI for Office docs, PDFBox for PDFs
- **Reverse Conversion**: JVS → DMS conversion
- **Reactive Streams**: Flux-based batch conversion
- **Type Generation**: Automatic type generation from DMS classes

## Examples

See test classes for more examples:
- `VersionableObjectConverterTest`
- `DMSToJVSMapperTest`
- `ContentTextExtractorTest`

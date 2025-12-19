# Hitorro Base DMS

This is the **hitorro-basedms** project, a standalone Document Management System with database persistence, content management, and workflow capabilities.

## Overview

The Hitorro Base DMS provides:

- **Document Management**: Version control, metadata management, content storage
- **Database Persistence**: Hibernate ORM integration with Derby database
- **Content Processing**: 
  - RSS/Atom feed handling
  - MP3 metadata extraction
  - Office document processing (Word, Excel, PowerPoint)
- **User Management**: Authentication, permissions, roles
- **Workflow & Job Scheduling**: Quartz-based job scheduling
- **Storage**: File system and S3 storage backends
- **SSH/SFTP Support**: Remote file transfer capabilities
- **Web Framework**: Apache Tapestry integration

## Project Structure

This is a standalone Maven JAR project.

```
hitorro-basedms/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/hitorro/base/objects/     # Domain objects
│   │   │   ├── com/hitorro/basedms/          # DMS services
│   │   │   └── ...
│   │   └── resources/
│   └── test/
├── pom.xml
└── README.md
```

## Building

```bash
mvn clean install
```

**Note:** This project currently has compilation issues due to dependencies on deleted modules (`hitorro-base` and `hitorro-util`). These need to be either:
1. Recreated as separate library projects
2. Refactored out of the codebase
3. Replaced with external library equivalents

## Known Issues

- References to `com.hitorro.util.*` packages (from deleted hitorro-util module)
- Missing type system utilities
- Missing base type classes

## Requirements

- Java 19 or higher
- Maven 3.6+

## Key Dependencies

- **Hibernate 6.4.4**: ORM framework
- **Apache Derby 10.17**: Embedded database
- **Apache Tapestry 5.4.3**: Web framework
- **Apache POI 5.2.5**: Office document processing
- **ROME 2.1.0**: RSS/Atom feed processing
- **JAudioTagger 3.0.1**: MP3 metadata extraction
- **Apache MINA 2.0.21**: Network application framework
- **JetS3t 0.9.4**: Amazon S3 integration
- **MongoDB Morphia 1.1.1**: MongoDB object mapping

## License

See LICENSE file for details.

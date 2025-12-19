# Hitorro BaseDMS Extraction Notes

## Extraction Date
December 19, 2025

## What Was Done

The `hitorro-basedms` module was successfully extracted from the `hitorro-parent` multi-module project into its own standalone Maven project.

### New Location
- **Old**: `/Users/chris/hitorro/hitorro/hitorro-parent/hitorro-basedms/`
- **New**: `/Users/chris/hitorro/hitorro-basedms/`

### Changes Made

1. **Copied entire module** to new standalone location
2. **Updated pom.xml**:
   - Removed parent reference to `hitorro-parent`
   - Changed from child module to standalone project
   - Updated version from 2.0 to 3.0.0
   - Added all necessary Maven properties (compiler source/target, encoding)
   - Commented out references to deleted dependencies (`hitorro-base`, `hitorro-util`)
   - Added missing dependencies:
     - `com.rometools:rome:2.1.0` (RSS/Atom feed processing)
     - `net.jthink:jaudiotagger:3.0.1` (MP3 metadata)
   - Updated Maven plugin versions
3. **Created documentation**:
   - README.md
   - .gitignore
   - EXTRACTION_NOTES.md (this file)
4. **Updated parent pom**: Removed `hitorro-basedms` from the modules list in `hitorro-parent/pom.xml`

## Package Structure

The project contains the following main packages:

```
com.hitorro.base.objects/          - Domain objects (User, Document, Container, etc.)
com.hitorro.base.service/          - Base services
com.hitorro.base.typesystem/       - Type system utilities
com.hitorro.basedms/               - DMS core services
  ├── auth/                        - Authentication & authorization
  ├── cache/                       - Caching layer
  ├── db/                          - Database utilities
  ├── job/                         - Job scheduling
  ├── queue/                       - Queue management
  ├── rss/                         - RSS/Atom feed handling
  ├── scheduler/                   - Quartz scheduler
  ├── session/                     - Session management
  ├── ssh/                         - SSH/SFTP support
  ├── transformer/                 - Content transformation
  ├── workflow/                    - Workflow engine
  └── ...
com.hitorro.basetext.indexer/      - Text indexing
com.hitorro.jsontypesystem/        - JSON type system
com.hitorro.util/                  - Utility classes
```

## Known Issues

### Compilation Problems

The project **does not currently compile** due to missing dependencies from deleted modules:

1. **hitorro-util module** (deleted):
   - Provides: `com.hitorro.util.typesystem.*`
   - Provides: `com.hitorro.util.core.*`
   - Provides: `com.hitorro.util.io.*`
   - Used by: Domain objects and many service classes

2. **hitorro-base module** (deleted):
   - Was a parent dependency
   - Some of its classes are now in this project under `com.hitorro.base.objects`
   - But those classes depend on `hitorro-util`

### Resolution Options

To make this project compile, you need to:

**Option A: Recreate Missing Modules**
- Create a new `hitorro-util` standalone project with the required utility classes
- Extract from the deleted modules in `/Users/chris/hitorro/hitorro/deleted/`

**Option B: Refactor Dependencies**
- Remove or replace code that depends on `hitorro-util`
- Find external library equivalents for utility functions
- Simplify the type system

**Option C: Hybrid Approach**
- Recreate minimal versions of required utility classes
- Merge them directly into this project (not recommended for long-term)

## Dependencies Summary

### Successfully Added
- ✅ ROME 2.1.0 (RSS/Atom feeds)
- ✅ JAudioTagger 3.0.1 (MP3 metadata)

### Needs Attention
- ❌ hitorro-util classes (type system, core utilities)
- ❌ hitorro-base classes (some moved here, but still depend on hitorro-util)

## Next Steps

1. Decide on approach for missing dependencies (A, B, or C above)
2. Either recreate `hitorro-util` or refactor code to remove dependencies
3. Fix compilation errors
4. Run tests to ensure functionality is preserved
5. Update any other modules that depended on `hitorro-basedms`

## Build Status

Current build status: **FAILS** (due to missing dependencies)

To attempt a build:
```bash
cd /Users/chris/hitorro/hitorro-basedms
mvn clean compile
```

Expected errors: Missing packages from `com.hitorro.util.*`

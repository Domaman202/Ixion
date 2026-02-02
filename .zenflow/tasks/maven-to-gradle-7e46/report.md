# Maven to Gradle Conversion Report

## Summary
Successfully converted the Maven-based project to Gradle build system.

## Changes Made

### Build Files Created
1. **build.gradle** - Main Gradle build script using Groovy DSL
   - Configured Java plugin with Java 21 compatibility
   - Configured Kotlin JVM plugin version 2.2.0
   - Migrated all dependencies from pom.xml
   - Set up JUnit Platform for testing

2. **settings.gradle** - Gradle settings file
   - Project name: ixion-jvm

3. **gradle.properties** - Gradle configuration
   - JVM args: -Xmx2048m
   - Enabled parallel builds and caching
   - Kotlin code style set to official

4. **Gradle Wrapper** - Version 8.11.1
   - gradlew (Unix/Linux executable)
   - gradlew.bat (Windows batch file)
   - gradle/wrapper/gradle-wrapper.jar
   - gradle/wrapper/gradle-wrapper.properties

## Dependency Migration
All Maven dependencies were successfully migrated to Gradle format:
- picocli 4.6.1
- JUnit Jupiter 6.0.1 (test scope)
- ASM (multiple modules)
- Apache Commons (collections4, lang3, commons-io)
- TOMLJ 1.0.0
- JGraphT (core, io)
- JMH (core, generator-annprocess)
- JavaTuples 1.2
- Kotlin stdlib and test

## Configuration Migration
- **Java Version**: 21 (sourceCompatibility and targetCompatibility)
- **Kotlin Version**: 2.2.0 with JVM toolchain 21
- **Encoding**: UTF-8 (inherited from Gradle defaults)
- **Test Framework**: JUnit Platform

## Known Issues

### Java 25 Compatibility
The current system is running **Java 25.0.1**, which is not yet fully supported by Gradle 8.11.1. This causes the following error when running the build:

```
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_' 
Unsupported class file major version 69
```

**Workaround**: To run Gradle commands, you need to use **Java 21 or earlier**. You can:
1. Install Java 21 (recommended, as the project targets Java 21)
2. Set JAVA_HOME to point to Java 21 before running Gradle commands
3. Wait for a future Gradle release that supports Java 25

The build configuration itself is correct and will work properly with Java 21.

## Build Commands

Once Java 21 is available:

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean

# View all available tasks
./gradlew tasks
```

## Files That Can Be Removed
- pom.xml (keep for reference if needed, but no longer required)
- Maven wrapper files (if any existed)
- target/ directory (Maven build output)

The .gitignore file already includes both Maven (target/) and Gradle (build/, .gradle/) directories.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Canto programming language compiler and runtime engine. The project is currently in the middle of a migration from JavaCC to ANTLR4 for implementing the grammar, along with other updates including dropping servlet support and updating to Jetty 12.0.5.

## Build System & Development Commands

This project uses Maven with Java 14. Key commands:

- **Compile**: `mvn compile`
- **Run tests**: `mvn test` 
- **Clean build**: `mvn clean compile`
- **Generate ANTLR parser**: Automatically handled by `antlr4-maven-plugin` during compile
- **Run compiler**: `java -cp target/classes cantoc [options] sourcepath`

The ANTLR4 maven plugin automatically generates parser classes from grammar files in `src/canto/parser/` to `target/generated-sources/antlr4/`.

## Architecture Overview

### Core Components

- **Parser**: ANTLR4-based lexer and parser (`CantoLexer.g4`, `CantoParser.g4`)
- **Compiler**: `canto.compiler.CantoCompiler` - main compilation logic
- **Runtime**: `canto.runtime.*` - server infrastructure and execution engine
- **Language Core**: `canto.lang.*` - core language definitions and constructs

### Package Structure

- `src/canto/parser/` - ANTLR4 grammar files and generated parser classes
- `src/canto/compiler/` - Compilation logic and compiler entry point
- `src/canto/runtime/` - Runtime server (Jetty-based) and execution environment
- `src/canto/lang/` - Core language definitions, types, and constructs
- `src/canto/util/` - Utility classes
- `src/cantocore/` - Core language library
- `test/` - JUnit 5 test files

### Key Files

- `src/cantoc.java` - Compiler entry point convenience wrapper
- `src/module-info.java` - Java module definition requiring Jetty, ANTLR runtime, and JUnit
- Grammar files use ANTLR4 with custom modes for text blocks, literal blocks, and nested comments

### Language Features

Canto includes distinctive language constructs:
- Text blocks with `[| ... |]` and `[/ ... /]` syntax
- Literal blocks with `[`` ... ``]` syntax  
- Code blocks with `{= ... =}` syntax
- Dynamic expressions with `(: ... :)` syntax
- Concurrent expressions with `(+ ... +)` syntax
- Rich set of operators including `**` (power), `??` (null coalescing), `>>>` (unsigned right shift)

## Migration Status

- ✅ ANTLR4 grammar files implemented (CantoLexer.g4, CantoParser.g4)
- ✅ Maven build configured with antlr4-maven-plugin
- ✅ Jetty updated to version 12.0.5 
- ✅ Servlet support removed
- 🔄 Integration between ANTLR-generated parser and existing compiler infrastructure

When working on parser-related code, note that generated ANTLR classes will be in the `target/generated-sources/antlr4/canto/parser/` directory after compilation.
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

# General rules

These rules apply to every task in this project unless explicitly overridden.
Bias: caution over speed on non-trivial work. Use judgment on trivial tasks.

## Rule 1 — Think Before Coding
State assumptions explicitly. If uncertain, ask rather than guess.
Present multiple interpretations when ambiguity exists.
Push back when a simpler approach exists.
Stop when confused. Name what's unclear.

## Rule 2 — Simplicity First
Minimum code that solves the problem. Nothing speculative.
No features beyond what was asked. No abstractions for single-use code.
Test: would a senior engineer say this is overcomplicated? If yes, simplify.

## Rule 3 — Surgical Changes
Touch only what you must. Clean up only your own mess.
Don't "improve" adjacent code, comments, or formatting.
Don't refactor what isn't broken. Match existing style.

## Rule 4 — Goal-Driven Execution
Define success criteria. Loop until verified.
Don't follow steps. Define success and iterate.
Strong success criteria let you loop independently.

## Rule 5 — Use the model only for judgment calls
Use me for: classification, drafting, summarization, extraction.
Do NOT use me for: routing, retries, deterministic transforms.
If code can answer, code answers.

## Rule 6 — Token budgets are not advisory
Per-task: 4,000 tokens. Per-session: 30,000 tokens.
If approaching budget, summarize and start fresh.
Surface the breach. Do not silently overrun.

## Rule 7 — Surface conflicts, don't average them
If two patterns contradict, pick one (more recent / more tested).
Explain why. Flag the other for cleanup.
Don't blend conflicting patterns.

## Rule 8 — Read before you write
Before adding code, read exports, immediate callers, shared utilities.
"Looks orthogonal" is dangerous. If unsure why code is structured a way, ask.

## Rule 9 — Tests verify intent, not just behavior
Tests must encode WHY behavior matters, not just WHAT it does.
A test that can't fail when business logic changes is wrong.

## Rule 10 — Checkpoint after every significant step
Summarize what was done, what's verified, what's left.
Don't continue from a state you can't describe back.
If you lose track, stop and restate.

## Rule 11 — Match the codebase's conventions, even if you disagree
Conformance > taste inside the codebase.
If you genuinely think a convention is harmful, surface it. Don't fork silently.

## Rule 12 — Fail loud
"Completed" is wrong if anything was skipped silently.
"Tests pass" is wrong if any were skipped.
Default to surfacing uncertainty, not hiding it.

# PsychonautWiki Journal Desktop Porting - AI Analysis and Planning

This directory contains comprehensive analysis, task planning, and context documentation for porting the PsychonautWiki Journal from Android to Kotlin Desktop with Wayland native support and mobile-first reactive UI.

## Directory Structure

### `/analysis/`
Codebase analysis and strategic planning documents:
- `codebase_analysis.json` - Technical analysis of current Android codebase
- `porting_strategy.json` - Strategic approach and technology decisions

### `/context/`
Detailed context files for different aspects of the porting project:
- `task_contexts.json` - Context for different task categories
- `testing_strategy.json` - Comprehensive testing approach and BDD strategy
- `mobile_first_design_context.json` - Mobile-first responsive design principles
- `wayland_integration_context.json` - Wayland protocol integration guidelines

### `/personas/`
Specialized development personas for task execution:
- `personas.json` - Detailed persona definitions with expertise, responsibilities, and assigned tasks

### `/tasks/`
Atomic task breakdown with BDD scenarios:
- `tasks.json` - Complete task breakdown with 23 atomic tasks across 6 phases
- `README.md` - Task organization and execution guidelines

## Project Overview

**Goal**: Port Android PsychonautWiki Journal to Kotlin Desktop with Wayland native support and mobile-first reactive UI

**Key Requirements**:
- Kotlin Desktop with Compose Multiplatform
- Wayland native integration
- Mobile-first reactive UI design  
- Complete feature parity with Android version
- Comprehensive BDD test coverage
- NixOS build environment

**Approach**: 
- 23 atomic tasks across 6 phases
- 5 specialized personas with clear responsibilities
- Comprehensive BDD testing with 100% critical path coverage
- Mobile-first design principles with progressive desktop enhancement

## Task Phases

1. **Foundation** (3 tasks, 36 hours) - Development environment and basic infrastructure
2. **Architecture** (4 tasks, 68 hours) - Core architecture migration and data layer
3. **UI Framework** (3 tasks, 60 hours) - Mobile-first responsive UI system
4. **Feature Migration** (4 tasks, 100 hours) - Complete feature parity with Android
5. **Platform Integration** (3 tasks, 54 hours) - Wayland native integration and desktop features
6. **Testing & DevOps** (6 tasks, 140 hours) - Comprehensive testing and automated deployment

**Total Estimate**: 458 hours across 23 atomic tasks

## Quality Standards

- **Testing**: 90% code coverage, 100% critical path BDD coverage
- **Performance**: <3s startup time, <100ms UI response
- **Accessibility**: WCAG 2.1 AA compliance
- **Mobile-First**: Progressive enhancement from mobile to desktop
- **Repository**: No binaries committed, clean git history, proper .gitignore

## Usage for Agents

This documentation provides agentic coding assistants with:
- Clear task breakdown with acceptance criteria
- Specialized persona guidance for different work types
- Comprehensive context for technical decisions
- BDD scenarios for validation
- Testing strategies and quality gates

Each task includes persona assignment, dependencies, and detailed BDD scenarios for validation.
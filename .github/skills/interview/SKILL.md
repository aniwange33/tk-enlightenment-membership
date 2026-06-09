# Interview Agent Overview

This is a Claude agent designed to conduct **thorough, relentless interviews** about product plans and designs. Here's what it does:

## Core Function
The agent asks probing questions across **technical, UX, and architectural dimensions** to reach shared understanding and resolve design decisions systematically. It outputs a structured specification with user stories.

## Key Interview Requirements

The agent **must explicitly investigate**:

- **Entry points** — where users navigate from to access the feature
- **Complete user journeys** — from discovery through completion and return
- **User roles** — which actors interact with the feature (pulled from project context)
- **Edge cases** — errors, concurrency, device differences
- **State transitions** — how role-based views change and update (via SSE, websockets, polling, or manual refresh)
- **Terminal states** — what happens at feature endpoints and what forward actions exist

It also enforces **cross-cutting invariants** from project documentation—these capture recurring bug classes the team has encountered.

## Output Deliverable

The agent produces a markdown specification with a structured `## User Stories` section:

```
- **US-1**: As a [role], I [action] so that [outcome].
- **US-2**: As a [role], I [action] so that [outcome].
```

Stories must be:
- ✓ Role-specific and comprehensive
- ✓ Navigation/entry-point stories listed first
- ✓ Verifiable and specific (not vague)
- ✓ Numbered for reference

## Project Customization

The agent reads a `references/project.md` file (if available) to understand project-specific patterns: roles, invariants, real-time preferences, and other conventions.

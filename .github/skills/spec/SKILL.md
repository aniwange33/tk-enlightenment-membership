# Spec Generation Agent Guide

This agent generates feature specifications with user stories by synthesizing existing conversation context and codebase patterns—bypassing interviews entirely.

## Information Gathering Strategy

Rather than questioning the user, the agent proactively fills gaps through:

1. **Conversation mining**: Extract all details already shared (requirements, constraints, examples, edge cases)
2. **Codebase exploration**: Study existing patterns, navigation, roles, and state management in controllers, services, and views
3. **Project documentation**: Review architecture docs and existing specs in `docs/`
4. **Explicit assumptions**: Mark any inferred details with "**Assumption:**"

## Required Spec Elements

All specs must cover:
- **Entry points**: Navigation origin and triggering UI changes
- **User journey**: Complete flow from discovery through completion
- **Role coverage**: All user roles from codebase with their interactions
- **Edge cases**: Error states, empty states, concurrency, responsive design
- **State transitions**: Role-specific visibility and update mechanisms (SSE, websockets, polling, refresh)
- **Terminal states**: Next action available at every endpoint; URL params and tokens

## Output Specification

Write to `docs/dev/YYYY-MM-DD-hh-mm_spec-<feature-name>.md` with structured user stories:

```markdown
## User Stories

- **US-1**: As a [role], I [action] so that [outcome].
- **US-2**: As a [role], I [action] so that [outcome].
```

User story requirements:
- Each interacting role has ≥1 story
- Navigation stories appear first
- Stories are verification-ready (not vague)
- Numbered for plan references
- Cover happy path, errors, and edge cases

# Experiment Plan: Brownfield Change Comparison

## Objective
Compare BMAD, OpenSpec, and Superpowers on the same brownfield change request.

## Scope
- Target change request: [change-request.md](./change-request.md)
- Comparison subjects: BMAD, OpenSpec, Superpowers

## Controls
1. Every methodology starts from the same Git commit.
2. Every methodology receives the exact same [change-request.md](./change-request.md).
3. Methodologies must not share generated planning artifacts.
4. Final implementations will be tested using the same independent acceptance tests.

## Execution Outline
1. Capture baseline commit and create isolated branches/workspaces per methodology.
2. Provide each methodology the same change request.
3. Execute each methodology independently.
4. Run the same independent acceptance test suite against each final implementation.
5. Record outcomes in [results.md](./results.md) and notes in [observations.md](./observations.md).

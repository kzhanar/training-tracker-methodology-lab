# Independent Training Expiration Acceptance Suite

This suite verifies the Training Expiration request through HTTP-visible behavior. It uses only Python's standard
library and does not import application classes or depend on a branch's implementation structure.

## Source of expectations

Expected behavior is derived from:

- `requirements.md` and `experiment/change-request.md`.
- The status window and integer-day validity decisions documented in all three methodology planning/specification
  artifacts.
- The exact-expiration decision selected for this suite: a completion remains valid through its expiration date;
  `EXPIRED` begins the following day.
- The shared decision that a missing completion alone is not expired and that a later completed, non-expired retake
  supersedes an older expired completion.

The methodologies did not agree on the exact-expiration boundary. The suite adopts the user-selected rule above,
rather than treating any one implementation as authoritative. Future-dated retakes are not used to decide the
expired-required listing because the methodology artifacts do not agree on that behavior.

## Run

Start one methodology branch's application using JDK 17, then run:

```powershell
$env:TRAINING_ACCEPTANCE_BASE_URL = "http://localhost:8080"
$env:TRAINING_ACCEPTANCE_PROFILE = "bmad" # bmad, openspec, or superpowers
python -m unittest discover -s experiment/acceptance -v
```

Run the exact same test file against each branch. Only the profile changes; it maps the feature API route and response
envelope to the shared observable contract. It does not change expected statuses or employee selection.

The suite captures one local date at startup and computes every completion date relative to it. Run the application
and suite on the same host/time zone, and avoid starting across local midnight. For a reproducible fixed date, run both
the application and the suite with their system clocks fixed to the same date before startup.

The feature request does not prescribe new endpoint paths or a response envelope for the status and expired-employee
features. The profiles use paths and envelope shapes documented in the methodology artifacts; they do not inspect or
assert internal implementation details.

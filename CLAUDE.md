# Project Conventions

## Git

- All merges into `main` must be **squash merge commits** so the history stays clean (one commit per
  branch/PR).
- Squash commit titles on `main` must start with the branch name, in the form
  `<branch-name>: <title>` (e.g. `01-openspec-explore-and-propose: Add bulk-hardware-orders change`).
  Set this title explicitly when merging, since GitHub does not add the branch name on its own.
- Do **not** create pull requests (e.g. via `gh pr create`).

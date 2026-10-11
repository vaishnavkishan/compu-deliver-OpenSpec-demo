# Overview

This repo is a worked example of [OpenSpec](https://openspec.dev/docs/installation) and
Spec-Driven Development (SDD): write and refine a spec first, then let it drive planning, task
breakdown, and implementation.

Read top to bottom — each step below corresponds to one branch, so you can follow the git history
alongside the writeup. Every step includes the raw slash-command prompt used.

## Prerequisites (branch name 00-openspec-initialize)

### 1. Install Node.js

OpenSpec requires Node.js 20.19.0 or higher.

```bash
node --version
```

### 2. Install the OpenSpec CLI

Docs: [Fission-AI/OpenSpec](https://github.com/Fission-AI/OpenSpec)

```bash
npm install -g @fission-ai/openspec@latest
openspec --version
```

### 3. Initialize OpenSpec in this repo

reference link [initialize-your-project](https://openspec.dev/docs/setup#initialize-your-project)

```bash
openspec init
```

Select Claude Code when prompted. Generates:

- `openspec/` — specs, changes, and project conventions
- `.claude/` — OpenSpec slash commands for Claude

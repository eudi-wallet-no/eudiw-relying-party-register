# JIRA ID in branch and PR title

Every task has a JIRA ID. **Always ask the user for the JIRA ID** if it is not provided;
do not assume or invent one. Use it in the PR title:

```
<JIRA-ID>: <PR title>
```

For example, `EUW-1234: Ny pr`. Without this, the `validate-pr-title` check fails in GitHub Actions.

The PR title (the part after the JIRA ID prefix) must be written in **Norwegian**.

## Branch names

Any new branch, whether created through `rename_branch` or directly with git, must be named
`<jira-id>`, for example `euw-1234`. Do not use a descriptive text suffix. If the branch already
exists, increment it: `euw-1234-2`, `euw-1234-3`, and so on.

# PR descriptions

Write naturally, briefly, and concretely. Avoid AI phrasing ("This PR introduces..."), emojis,
excessive structure, self-praise, and walls of summary text. One line is enough for trivial changes.
The PR description must be written in **Norwegian**.

Template:
```
## Hva og hvorfor
1-2 sentences: what changes and why.

## Endringer
- Key points, not every file or detail.
```
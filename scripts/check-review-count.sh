#!/usr/bin/env bash
# Reports whether a PR has enough approvals as the "Review count" status.
#   - Team leads (everyone in CODEOWNERS) need 1 approval from any teammate.
#   - Everyone else needs 2 approvals, and at least 1 must come from a lead.
# An approval only counts if the branch was up to date with main when the
# reviewer approved it, so every review covers the latest main.
#
# GitHub Actions sets GH_REPO, PR_NUMBER, PR_AUTHOR, and HEAD_SHA.

set -euo pipefail

leads="$(grep -o '@[A-Za-z0-9-]*' .github/CODEOWNERS | tr -d '@' | sort -u)"

is_lead() {
  grep -Fixq "$1" <<< "$leads"
}

if is_lead "$PR_AUTHOR"; then
  needed=1
  needs_lead=no
else
  needed=2
  needs_lead=yes
fi

# Get main's history and the commits the reviewers approved.
git fetch -q origin main "refs/pull/$PR_NUMBER/head"

# Each reviewer's latest decision counts, and only people on the repo count,
# because anyone can leave a review on a public repo.
approvals="$(gh api "repos/$GH_REPO/pulls/$PR_NUMBER/reviews?per_page=100" --jq '
  [.[] | select(.user != null and (.state == "APPROVED" or .state == "CHANGES_REQUESTED" or .state == "DISMISSED"))]
  | group_by(.user.login)
  | map(max_by(.submitted_at))
  | map(select(.state == "APPROVED" and (.author_association == "OWNER" or .author_association == "MEMBER" or .author_association == "COLLABORATOR")))
  | .[] | "\(.user.login) \(.commit_id) \(.submitted_at)"')"

counted=0
behind=0
lead_approved=no
while read -r reviewer commit submitted_at; do
  if [ -z "$reviewer" ]; then
    continue
  fi
  # Where main was when the reviewer approved, and whether the approved commit had it.
  main_then="$(git rev-list -1 --first-parent --before="$submitted_at" origin/main)"
  if [ -n "$main_then" ] && git merge-base --is-ancestor "$main_then" "$commit" 2> /dev/null; then
    counted=$((counted + 1))
    if is_lead "$reviewer"; then
      lead_approved=yes
    fi
  else
    behind=$((behind + 1))
  fi
done <<< "$approvals"

state=pending
if [ "$counted" -ge "$needed" ] && { [ "$needs_lead" = no ] || [ "$lead_approved" = yes ]; }; then
  state=success
fi

description="$counted of $needed approvals"
if [ "$needs_lead" = yes ] && [ "$lead_approved" = no ]; then
  description="$description, needs a lead"
fi
if [ "$behind" -gt 0 ]; then
  description="$description, $behind given while behind main"
fi

gh api "repos/$GH_REPO/statuses/$HEAD_SHA" \
  -f state="$state" \
  -f context="Review count" \
  -f description="$description" > /dev/null

echo "$PR_AUTHOR: $description ($state)"

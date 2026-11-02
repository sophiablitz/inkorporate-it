#!/usr/bin/env bash
# Reports how many approvals a PR has as the "Review count" status. Team leads
# need 1 approval and everyone else needs 2. The ruleset separately requires
# that one of those approvals comes from a lead.
#
# GitHub Actions sets GH_REPO, PR_NUMBER, PR_AUTHOR, and HEAD_SHA.

set -euo pipefail

needed=2
if grep -o '@[A-Za-z0-9-]*' .github/CODEOWNERS | tr -d '@' | grep -Fixq "$PR_AUTHOR"; then
  needed=1
fi

# Each reviewer's latest decision counts, and only people on the repo count,
# because anyone can leave a review on a public repo.
approvals="$(gh api "repos/$GH_REPO/pulls/$PR_NUMBER/reviews?per_page=100" --jq '
  [.[] | select(.user != null and (.state == "APPROVED" or .state == "CHANGES_REQUESTED" or .state == "DISMISSED"))]
  | group_by(.user.login)
  | map(max_by(.submitted_at))
  | map(select(.state == "APPROVED" and (.author_association == "OWNER" or .author_association == "MEMBER" or .author_association == "COLLABORATOR")))
  | length')"

state=pending
if [ "$approvals" -ge "$needed" ]; then
  state=success
fi

gh api "repos/$GH_REPO/statuses/$HEAD_SHA" \
  -f state="$state" \
  -f context="Review count" \
  -f description="$approvals of $needed approvals" > /dev/null

echo "$PR_AUTHOR has $approvals of $needed approvals: $state"

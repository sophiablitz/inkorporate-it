# Working on inkorporate-it

Nobody pushes directly to `main`. Every change goes on its own branch and gets
into `main` through a pull request (PR), which is a request for the team to
review your branch and merge it.

## First-time setup

Do this once. If you're on Windows, run these commands in Git Bash.

1. Accept your invitation at <https://github.com/sophiablitz/inkorporate-it/invitations>.
2. Copy the project to your computer and turn on the Git hooks:

   ```bash
   git clone https://github.com/sophiablitz/inkorporate-it.git
   ```

   ```bash
   cd inkorporate-it
   ```

   ```bash
   scripts/setup-hooks.sh
   ```

Open the `front-end` folder in Android Studio, not the top-level folder.

## Making a change

1. **Start from the latest `main`.**

   ```bash
   git switch main
   ```

   ```bash
   git pull
   ```

2. **Make a branch.** Start the name with your area, then a few words about the work.

   ```bash
   git switch -c front-end/login-screen
   ```

3. **Do the work.** For Android changes, run the app on the emulator and try
   what you changed.

4. **Commit.** If you changed the Android app, add `[emulator-tested]` to the
   message. By adding it, you're telling the team you ran this commit on the
   emulator.

   ```bash
   git add .
   ```

   ```bash
   git commit -m "Add login screen [emulator-tested]"
   ```

   Commits that only touch `back-end/`, `shared/`, or Markdown files don't need
   the flag.

5. **Push.** The first push of a branch needs the `-u` part. Git runs the tests
   before it pushes, which takes a couple of minutes. Afterward, a plain
   `git push` is enough.

   ```bash
   git push -u origin HEAD
   ```

6. **Open a PR.** Git prints a link after you push, or GitHub shows a
   **Compare & pull request** button on the repo page. Fill in the template.

7. **Get reviews.** On the PR's right side, under **Reviewers**, ask a teammate.
   GitHub adds the team leads automatically.

8. **Merge.** When the merge box is green, click **Merge pull request**, then
   **Delete branch**.

## What has to pass before a PR can merge

| Requirement | Who or what checks it |
| --- | --- |
| 2 approvals, including 1 from a team lead | Reviewers on GitHub |
| `scripts/test.sh` passes | **Test script** check |
| Android commits have `[emulator-tested]` | **Emulator flag** check |

You can't approve your own PR. If you push new commits after being approved, the
approvals are cleared, so ask your reviewers to look again.

## Reviewing a PR

Open the PR, go to **Files changed**, and leave comments on any line. Then click
**Review changes** and choose **Approve** or **Request changes**.

## Common problems

### Fixing a missing flag

If the problem is your most recent commit and it hasn't been pushed, reword it:

```bash
git commit --amend -m "Add login screen [emulator-tested]"
```

If you've already pushed, reword it the same way, then update your branch on
GitHub:

```bash
git push --force-with-lease
```

If the commit is older, the simplest fix is to combine your branch's commits into
one. Run the app on the emulator first, then replace the message below with your
own:

```bash
git reset --soft "$(git merge-base origin/main HEAD)"
```

```bash
git commit -m "Add login screen [emulator-tested]"
```

```bash
git push --force-with-lease
```

### `main` moved while you were working

Bring the latest `main` into your branch:

```bash
git fetch origin
```

```bash
git merge origin/main
```

If Git reports a conflict, open the files it lists, keep the right code, remove
the `<<<<<<<`, `=======`, and `>>>>>>>` lines, then run `git add .` and
`git commit`.

### A check failed on your PR

Click **Details** next to the check to see the log. Fix the problem, commit, and
push. The checks run again automatically.

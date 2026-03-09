# Maven Central Publishing

This repository publishes the release artifacts below to Maven Central:

- `dev.cominotti.java.evo:linter-core`
- `dev.cominotti.java.evo:linter-maven-plugin`
- `dev.cominotti.java.evo:linter-cli`

The human-facing CLI install path stays on GitHub Releases, where each tagged
release also uploads the ZIP and `tar.gz` distributions built from `linter-cli`.

## What Is Automated

The repository now automates the release-side mechanics below:

- `.github/workflows/release.yml` publishes to the Central Portal on `v*` tags
- `make central-preflight` checks the local prerequisites
- `make central-render-settings` writes `~/.m2/settings-central.xml` from env vars
- `make central-check-dns` validates the exact TXT record Sonatype will query
- `make central-upload-gh-secrets` pushes the required GitHub secrets with `gh`
- `make central-dry-run` exercises the release-only Maven profile without publishing

## Namespace Setup

The Sonatype namespace is the reversed-domain prefix, while the DNS proof uses
the normal domain name.

- Register namespace: `dev.cominotti`
- DNS ownership domain: `cominotti.dev`
- Published groupId root: `dev.cominotti.java.evo`

Official Sonatype references:

- `https://central.sonatype.org/publish/requirements/`
- `https://central.sonatype.org/register-a-namespace/`
- `https://central.sonatype.com/publishing/deployments`

## Manual Step 1: Create the Sonatype Central Portal Account

1. Open `https://central.sonatype.com/`.
2. Sign in with the account you want to use for releases.
3. Finish any required email verification or profile prompts.
4. Stay in the Central Portal for the next steps.

## Manual Step 2: Register the Namespace

1. In the Central Portal, open the namespace management area.
2. Create a namespace request for `dev.cominotti`.
3. Choose DNS verification.
4. When Sonatype shows the verification token, copy it exactly.
5. Add a TXT record on the exact domain `cominotti.dev` with that token.

DNS notes:

- Sonatype verifies the exact domain you claim ownership over.
- For this project, that means `cominotti.dev`, not `java.cominotti.dev` and not a GitHub Pages host.
- Keep the Portal tab open until DNS propagation is confirmed.

After you add the TXT record, verify it locally:

```bash
DOMAIN=cominotti.dev VERIFICATION_KEY='paste-the-token-here' make central-check-dns
```

If the script does not find the record yet, wait for DNS propagation and retry.

## Manual Step 3: Create a Portal Publishing Token

1. In the Central Portal, open the account settings or token management page.
2. Create a user token for publishing.
3. Copy the generated username and password immediately.
4. Export them in your shell:

   ```bash
   export MAVEN_CENTRAL_USERNAME='...'
   export MAVEN_CENTRAL_PASSWORD='...'
   ```

5. Render a local Maven settings file:

   ```bash
   make central-render-settings
   ```

This writes `~/.m2/settings-central.xml`. Use it for local deploys with:

```bash
mvn -s ~/.m2/settings-central.xml -B -ntp -Pcentral-release deploy
```

## Required GitHub Secrets

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_GPG_PRIVATE_KEY`
- `MAVEN_GPG_PASSPHRASE`

`GH_TOKEN` is provided automatically by GitHub Actions and is used to create or
update the GitHub Release after Central publishing completes.

## Manual Step 4: Prepare the GPG Signing Key

If you already have a release-signing key, reuse it. Otherwise:

1. Create a new GPG key:

   ```bash
   gpg --full-generate-key
   ```

2. Choose a modern key type such as ECC or RSA.
3. Use the release identity you want attached to Maven Central artifacts.
4. Set and remember a strong passphrase.
5. Confirm the key exists:

   ```bash
   gpg --list-secret-keys --keyid-format LONG
   ```

6. Export the armored private key for GitHub Actions:

   ```bash
   gpg --armor --export-secret-keys YOUR_KEY_ID > /tmp/java-evo-linter-gpg.asc
   export MAVEN_GPG_PASSPHRASE='your-passphrase'
   ```

## Manual Step 5: Upload the GitHub Secrets

First authenticate `gh` if you have not already:

```bash
gh auth login
```

Then export the required values:

```bash
export MAVEN_CENTRAL_USERNAME='...'
export MAVEN_CENTRAL_PASSWORD='...'
export MAVEN_GPG_PASSPHRASE='...'
```

Choose one of the two automated secret upload paths:

- If you already exported `MAVEN_GPG_PRIVATE_KEY`:

  ```bash
  export MAVEN_GPG_PRIVATE_KEY="$(cat /tmp/java-evo-linter-gpg.asc)"
  make central-upload-gh-secrets
  ```

- If you want the script to export directly from your local keyring:

  ```bash
  ./scripts/release/upload-github-secrets.sh --gpg-key-id YOUR_KEY_ID
  ```

The script uploads these exact secret names:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_GPG_PRIVATE_KEY`
- `MAVEN_GPG_PASSPHRASE`

## Manual Step 6: Run the Local Preflight

Run the automated checks:

```bash
make central-preflight
```

For a stricter release-tag validation:

```bash
./scripts/release/check-central-prereqs.sh --expect-tag v0.1.0 --require-credentials
```

This verifies the local Java runtime, Maven availability, GPG secret key
presence, project version, and whether Central credentials are discoverable.

## Release Flow

1. Verify the repository on JDK 21:

   ```bash
   mise exec -- mvn -B -ntp verify
   ```

2. Exercise the release-only profile without publishing:

   ```bash
   make central-dry-run
   ```

   If you want to test local signing as well:

   ```bash
   export MAVEN_GPG_PASSPHRASE='your-passphrase'
   mvn -s ~/.m2/settings-central.xml -B -ntp -Pcentral-release verify
   ```

3. Change the root version from `X.Y.Z-SNAPSHOT` to the final release version
   `X.Y.Z` in `pom.xml`.

4. Commit that version bump.

5. Create and push a version tag such as `v0.1.0`:

   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```

6. The tag push triggers `.github/workflows/release.yml`, which:

   - runs the full Maven lifecycle on JDK 21 with the `central-release` profile
   - signs the published artifacts with GPG
   - uploads the release bundle through Sonatype's Central Portal path
   - waits for Central publication
   - attaches the CLI archives to the GitHub Release

7. After the workflow succeeds:

   - confirm the deployment appears in the Central Portal
   - confirm the artifacts resolve from Maven Central
   - confirm the GitHub Release contains the CLI archives
   - bump the repo version back to the next `-SNAPSHOT`

## Local Dry Run

Use this before cutting a tag when you want to exercise the release-only Maven
attachments without publishing:

```bash
mvn -B -ntp -Pcentral-release verify
```

## Rollback Expectations

- Maven Central releases are immutable. After publication, do not expect to edit
  or replace the uploaded coordinates.
- If a release is bad, publish a new fixed version instead of attempting to
  overwrite the existing one.
- GitHub Release notes and attached CLI archives can be updated separately, but
  the Central artifacts must remain version-immutable.

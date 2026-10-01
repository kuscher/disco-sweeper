# Releasing Disco Sweeper

A release is a tag. Push `v<version>` and GitHub Actions (`.github/workflows/release.yml`) builds,
signs and publishes it. No machine needs the key file.

## Steps

1. On `main`: bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.
2. Add a `## <version> (<date>)` section to `CHANGELOG.md`. It becomes the GitHub release notes.
3. Write Google Play's "What's new" in `store-submission/listing/en-US/release-notes.txt` (at most
   500 characters).
4. Commit and push. Then tag that commit and push the tag:

   ```sh
   git tag v<version> && git push origin v<version>
   ```

5. When the workflow is done, open the draft on the closed-testing track in the Play Console and
   press **Send for review**. Nothing goes to review automatically.

The tag is `v` plus `versionName`, exactly: `v1.1` for `versionName = "1.1"`.

## What the tag does

- **Checks first:** `CHANGELOG.md` has a section for the version, `versionName` equals the tag, and
  the Play text is at most 500 characters.
- **Builds** the release APK and the Play bundle (.aab), signed with the release key, and runs
  `./ds sim`.
- **Checks the files** like `tools/release.sh`: the APK's and the bundle's certificate, the package
  and version, no permissions, no test hooks. Anything wrong stops the release.
- **GitHub:** a release `Disco Sweeper <version>` with `DiscoSweeper.apk` and `SHA256SUMS`. The
  README's download link points at `releases/latest/download/DiscoSweeper.apk`, so that name is
  fixed. The notes are the version's CHANGELOG section, then the install and verify text.
- **Google Play:** the bundle goes to the closed-testing track as a **draft**
  (`tools/play-upload.mjs`). What is live stays live. If that version code is on Play already, the
  step does nothing.

If a run fails after the GitHub release was made, fix the cause and re-run it (the whole run or only
the failed job): a release that exists is left as it is, and a version code that is on Play already
isn't uploaded twice.

## Dry run

**Run workflow** on the Actions tab (Release, branch `main`) does the same signed build and the
same checks, and publishes nothing: no GitHub release, and Play is only asked whether its key
works. Use it after changing the build or the workflow.

## The key

Every release must carry the Disco Sweeper release key (alias `discosweeper`), certificate SHA-256
`BA:4D:93:AA:D0:55:48:D8:02:5E:DE:97:B4:C2:D6:3E:1C:D6:CB:31:4A:99:F5:C1:06:96:22:9D:4B:9A:79:17`.
Android only installs an update over an app signed with the same key, and Google Play uses this key
too. The workflow refuses to publish anything signed with another one.

- The key and its password are secrets of the GitHub environment `release`
  (`SIGNING_KEYSTORE_B64`, `SIGNING_KEYSTORE_PASS`). The Play key is a secret of the environment
  `play` (`PLAY_SERVICE_ACCOUNT_JSON`). Only `main` and `v*` tags can use these environments; forks
  and pull requests never get them.
- The backup is in the owner's a private folder, with the password and a README.
- Agents never need the key file. It is never committed (`.gitignore` covers `*.jks` and
  `*.keystore`).

## By hand, on a machine that has the key

`tools/release.sh` still works where `~/.config/discosweeper/keystore.jks` and `keystore.pass`
exist. It builds the same `DiscoSweeper.apk`, `SHA256SUMS` and notes into
`executables/release-<version>/` and runs the same checks. It doesn't build the Play bundle
(`./gradlew :app:bundleRelease` does).

`tools/release.sh --publish` pushes the tag itself and creates the GitHub release. The tag starts
the workflow too: it leaves that release as it is, and still builds the bundle and puts it on Play
as a draft. Keep `--publish` for when Actions can't be used; the tag is the usual way.

## Checking a release by hand

```sh
apksigner verify --print-certs DiscoSweeper.apk | grep SHA-256
sha256sum -c SHA256SUMS
```

If `apksigner` isn't on PATH, it's in `$ANDROID_HOME/build-tools/<version>/`.

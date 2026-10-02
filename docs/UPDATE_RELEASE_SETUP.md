# Agent Lee Update Release Setup

Authority: LeeWay Standards / LEEWAY-UPDATE-v1.0

The unified Android updater retrieves only qualified, production-signed GitHub Release assets. The release workflow fails closed unless the repository has these GitHub Actions secrets:

- `LEEWAY_ANDROID_KEYSTORE_B64` — base64 of the LeeWay Android signing keystore.
- `LEEWAY_ANDROID_KEYSTORE_PASSWORD` — keystore password.
- `LEEWAY_ANDROID_KEY_ALIAS` — LeeWay signing alias.
- `LEEWAY_ANDROID_KEY_PASSWORD` — key password.

Never commit the keystore, passwords, or secret values to Git. Configure them only in the repository's Actions secrets UI or another authorized secret-management path.

On a successful `main` build, the workflow:
1. builds an unsigned release APK;
2. signs it with the protected LeeWay key;
3. verifies the signing certificate;
4. publishes `LeeWay-Agent-Lee.apk` as a GitHub Release asset;
5. calculates SHA-256, signer fingerprint, size, version and source commit;
6. writes `docs/download/LeeWay-Agent-Lee-latest.json`;
7. publishes only that small metadata file back to `main`.

The Android client verifies canonical repository, application ID, stable channel, SHA-256, version, manifest signer, and continuity with the currently installed signer before a staged update can become `READY_FOR_APPROVAL`.

Automatic retrieval is opt-in. Installation always requires an explicit user approval action.

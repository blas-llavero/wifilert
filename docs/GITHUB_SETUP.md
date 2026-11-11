# GitHub Actions APK setup — WIFILERT v0.1.2

1. Visit https://github.com/new while signed in to `blas-llavero`.
2. Enter `wifilert` as the repository name, select **Public**, and select **Add a README file** so the default `main` branch exists.
3. After creation, use the GitHub-connected assistant to upload all repository source files, including the approved logo and launcher icons. Alternatively use git on a computer. Upload **the actual extracted files**, including the `.github/workflows` folder; uploading a ZIP alone will not trigger a successful build.
4. The commit to `main` starts the **Android Debug APK** workflow. From the repository choose **Actions → Android Debug APK** and inspect the most recent run.
5. Once the checkmark is green, open that run and download the artifact **wifilert-debug-apk**. Extract it to obtain `app-debug.apk`.
6. On an Android device open Downloads, tap `app-debug.apk`, confirm installation from that source, and then open WIFILERT. Enable notifications and use the app's test button.

## Build and approval policy

- Debug APK only. No release signing and no Play Store publication.
- Only the owner-approved logo and derived icon sizes are included. The workflow refuses to build if any other images, videos or audio files are found, or if approved image hashes change.
- The UI has 10 translations. Test phone and watch notifications and the train detection before claiming production readiness.
- Never commit API tokens, credentials, personal location information or private files.
- If a workflow fails, inspect its logs under Actions and fix the reported cause before attempting another build.

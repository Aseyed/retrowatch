# GitHub Actions CI/CD

## Overview

The project includes a GitHub Actions workflow that automatically builds both Android applications on every push and pull request.

## Workflow File

**Location:** `.github/workflows/android.yml`

## What It Does

1. **Triggers:**
   - On push to `master` branch
   - On pull requests to `master` branch

2. **Builds:**
   - RetroWatch Android App
   - SmartGlasses Companion Android App

3. **Outputs:**
   - Uploads both APKs as artifacts
   - Artifacts available for 30 days
   - Can be downloaded from GitHub Actions page

## Workflow Steps

1. **Checkout** - Gets the latest code
2. **Set up JDK 17** - Configures Java environment
3. **Set up Android SDK** - Configures Android build tools
4. **Build RetroWatch** - Compiles RetroWatch app
5. **Build SmartGlasses** - Compiles SmartGlasses Companion app
6. **Upload APKs** - Saves APKs as downloadable artifacts

## Artifacts

After a successful build, you can download:

- **retrowatch-debug-apk** - RetroWatch app APK
- **smartglasses-companion-debug-apk** - SmartGlasses Companion APK

**Location:** GitHub Actions → Workflow run → Artifacts section

## Viewing Build Results

1. Go to **Actions** tab in GitHub
2. Click on the latest workflow run
3. View build logs
4. Download APKs from **Artifacts** section

## Manual Trigger

You can also manually trigger the workflow:

1. Go to **Actions** tab
2. Select **Android CI** workflow
3. Click **Run workflow**
4. Select branch and run

## Troubleshooting

**Build fails:**
- Check build logs in Actions tab
- Verify `settings.gradle` has correct module paths
- Ensure `local.properties` is not committed (should be in .gitignore)

**APK not found:**
- Check if build step succeeded
- Verify APK paths in workflow file
- Check build logs for errors

**SDK errors:**
- Workflow automatically sets up Android SDK
- If issues persist, check Android SDK version compatibility

## Customization

### Change Build Type

To build release APKs instead of debug:

```yaml
- name: Build RetroWatch App
  run: ./gradlew :android_apps:retrowatch:assembleRelease --stacktrace
```

### Add Signing

To sign APKs, add signing configuration:

```yaml
- name: Sign APK
  run: |
    jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
      -keystore ${{ secrets.KEYSTORE }} \
      android_apps/retrowatch/build/outputs/apk/release/app-release-unsigned.apk \
      ${{ secrets.KEY_ALIAS }}
```

### Build on Different Branches

Edit the `on:` section:

```yaml
on:
  push:
    branches: [ "master", "develop" ]
```

## Workflow Status Badge

Add to README.md:

```markdown
![Android CI](https://github.com/yourusername/retrowatch/workflows/Android%20CI/badge.svg)
```


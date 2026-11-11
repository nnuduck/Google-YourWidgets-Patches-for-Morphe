# Google YourWidgets Patches for Morphe

[English](#english) | [Русский](#русский)

---

## English

Compatibility, GmsCore/microG service redirect, and prompt template fallback patches for **Create My Widget** (`com.google.android.apps.yourwidget`), compatible with [Morphe](https://github.com/MorpheApp/morphe-patcher).

Repository: <https://github.com/nnuduck/Google-YourWidgets-Patches-for-Morphe>

### Target Application

| Property | Value |
| :--- | :--- |
| Package | `com.google.android.apps.yourwidget` |
| App name | Create My Widget |
| Supported version | `1.78.1014747048.5-release` (`285899`) |
| Format | Split APK bundle (`.apks`) |

### Included Patches

1. **`Bypass device compatibility check`**
   - Forces the `GEMINI_INTELLIGENCE_EXPERIENCE` runtime feature check to return `true`.
   - Removes `<uses-feature android:name="com.google.android.feature.GEMINI_INTELLIGENCE_EXPERIENCE">` from `AndroidManifest.xml`.
   - Removes the `hide_from_picker` flag from `res/xml/llm_widget_info.xml` (`android:widgetFeatures`) so the widget appears in the system/Pixel Launcher widget picker.
   - Option **`addLauncherShortcut`** (boolean, default `true`): adds a `MAIN` / `LAUNCHER` `<intent-filter>` to `LlmWidgetConfigActivity`.

2. **`Redirect services to GmsCore`**
   - Bypasses Google Play Services availability and Play signature validation checks.
   - Rewrites GMS, GSF, Phenotype, MobStore, and Google account (`com.google`) references to your configured microG/GmsCore vendor prefix (`gmsCoreVendorGroupId`).
   - Adds `<queries>`, `android.permission.GET_ACCOUNTS`, `<vendor>.gms.EXTENDED_ACCESS`, and `SPOOFED_PACKAGE_NAME` / `SPOOFED_PACKAGE_SIGNATURE` / `MICROG_PACKAGE_NAME` `<meta-data>` entries to `AndroidManifest.xml`.
   - Option **`gmsCoreVendorGroupId`** (string, default `"app.revanced"`): vendor package prefix of your installed GmsCore (`<vendor>.android.gms`).
   - Option **`spoofXAndroidCert`** (boolean, default `true`): dynamically hashes the original input APK certificate (SHA-1) and returns it from the `X-Android-Cert` (`PackageManager` and `SigningInfo`) helpers when running a non-root re-signed APK.

3. **`Skip prompt templates loading error`**
   - Skips the `PERMISSION_DENIED` latching and rethrow branches in `SuggestedWidgetPromptsDataServiceImpl` so the app falls back to its bundled static prompt templates instead of failing to populate the configurator carousel.

### Honest Limitations

> [!IMPORTANT]
> **These patches only bypass client-side restrictions.**
>
> - **Server-side region/account blocks (`PERMISSION_DENIED` / `ERROR_REASON_GEO_RESTRICTED`) are NOT bypassed.** `llmwidgets.pa.googleapis.com` validates eligibility on Google's servers (account country / Play region / server-side rollout). Changing your IP via VPN alone does not bypass an ineligible Google account region.
> - No global `Build.DEVICE` spoofing is performed, no debug trailer logging is added, and no forced locale override is included.

### Requirements & microG Setup

1. Install a compatible **microG / GmsCore** APK matching your `gmsCoreVendorGroupId` (by default ReVanced GmsCore, package `app.revanced.android.gms`).
2. Add your Google account inside microG Settings and grant Google authentication / Play services permissions to Create My Widget when prompted.
3. On Android with non-root re-signed packages, confirm Create My Widget holds `GET_ACCOUNTS` and `<vendor>.gms.EXTENDED_ACCESS` if your ROM restricts custom/runtime permissions:
   ```bash
   adb shell pm grant com.google.android.apps.yourwidget android.permission.GET_ACCOUNTS
   adb shell pm grant com.google.android.apps.yourwidget app.revanced.gms.EXTENDED_ACCESS
   ```
4. If microG prompts you to sign in cleanly inside `Create My Widget`, test with a dedicated secondary account if you prefer to isolate OAuth grants.

### Revoking Test Account OAuth Access

To revoke access granted through microG / this patched app after testing:
1. Open **Google Account settings** (`myaccount.google.com`) -> **Security** -> **Your connections to third-party apps & services** (and **Your devices**).
2. Remove the **Create My Widget** / microG OAuth grant and/or sign out of the registered Android microG device session.
3. Optionally remove the account from **microG Settings -> Google Accounts**.

### Building from Source

Morphe artifacts (`app.morphe:morphe-patches-gradle-plugin` and `app.morphe:morphe-patches-library`) are hosted on GitHub Packages (`https://maven.pkg.github.com/MorpheApp/registry`), which requires a GitHub username + read:packages Personal Access Token (or `mavenLocal()` if pre-installed):

```bash
export GITHUB_ACTOR="<your-github-username>"
export GITHUB_TOKEN="<your-github-pat-with-read-packages>"

./gradlew :patches:buildAndroid generatePatchesList
```

Or define `gpr.user` and `gpr.key` in `~/.gradle/gradle.properties`.

Generated outputs:
- Patch bundle: `patches/build/libs/patches-1.0.0.mpp`
- Patch metadata index: `patches-list.json`

### Patching via Morphe CLI

```bash
java -jar morphe-desktop-2.1.1-all.jar list-patches -p patches/build/libs/patches-1.0.0.mpp -v -o -d

java -jar morphe-desktop-2.1.1-all.jar patch \
  -p patches/build/libs/patches-1.0.0.mpp \
  --validate \
  -o output/yourwidget-patched.apks \
  "Create My Widget_1.78.1014747048.5-release.apks"
```

---

## Русский

Патчи совместимости, перенаправления сервисов на GmsCore/microG и офлайн-фолбэка шаблонов промптов для приложения **Create My Widget** (`com.google.android.apps.yourwidget`), совместимые с [Morphe](https://github.com/MorpheApp/morphe-patcher).

Репозиторий: <https://github.com/nnuduck/Google-YourWidgets-Patches-for-Morphe>

### Целевое приложение

| Параметр | Значение |
| :--- | :--- |
| Пакет | `com.google.android.apps.yourwidget` |
| Название | Create My Widget |
| Поддерживаемая версия | `1.78.1014747048.5-release` (`285899`) |
| Формат | Split APK (`.apks`) |

### Состав патчей

1. **`Bypass device compatibility check`**
   - Принудительно возвращает `true` в runtime-проверке фичи `GEMINI_INTELLIGENCE_EXPERIENCE`.
   - Удаляет `<uses-feature android:name="com.google.android.feature.GEMINI_INTELLIGENCE_EXPERIENCE">` из `AndroidManifest.xml`.
   - Удаляет флаг `hide_from_picker` из `res/xml/llm_widget_info.xml` (`android:widgetFeatures`), чтобы виджет отображался в системном списке виджетов и Pixel Launcher.
   - Опция **`addLauncherShortcut`** (по умолчанию `true`): добавляет `<intent-filter>` `MAIN` / `LAUNCHER` для `LlmWidgetConfigActivity`.

2. **`Redirect services to GmsCore`**
   - Обходит проверки наличия Google Play Services и проверки подписи GMS/Play Store.
   - Перенаправляет строки GMS, GSF, Phenotype, MobStore и аккаунтов (`com.google`) на выбранный пакет microG/GmsCore (`gmsCoreVendorGroupId`).
   - Добавляет в `AndroidManifest.xml` секцию `<queries>`, разрешения `GET_ACCOUNTS` и `<vendor>.gms.EXTENDED_ACCESS`, а также метаданные подмены подписи (`SPOOFED_PACKAGE_NAME`, `SPOOFED_PACKAGE_SIGNATURE`, `MICROG_PACKAGE_NAME`).
   - Опция **`gmsCoreVendorGroupId`** (по умолчанию `"app.revanced"`): префикс установленного microG/GmsCore (`<vendor>.android.gms`).
   - Опция **`spoofXAndroidCert`** (по умолчанию `true`): динамически вычисляет SHA-1 оригинального сертификата входного APK и подставляет его в заголовки `X-Android-Cert` (`PackageManager` и `SigningInfo`) при переподписи без root.

3. **`Skip prompt templates loading error`**
   - Пропускает обработку и повторный выброс `PERMISSION_DENIED` в корутине `SuggestedWidgetPromptsDataServiceImpl`, чтобы карусель подсказок загружалась из встроенных статических данных APK.

### Важные ограничения

> [!IMPORTANT]
> **Патчи обходят только клиентские проверки.**
>
> - **Серверная блокировка по стране/аккаунту (`PERMISSION_DENIED` / `ERROR_REASON_GEO_RESTRICTED`) НЕ обходится.** Сервер `llmwidgets.pa.googleapis.com` проверяет доступность функции для вашего Google-аккаунта/региона на стороне Google. Одного VPN на США недостаточно, если регион самого аккаунта не поддерживается.
> - В наборе нет подмены `Build.DEVICE`, отладочного логирования заголовков gRPC и принудительной подмены локали/часового пояса.

### Требования и настройка microG

1. Установите совместимый **microG / GmsCore** согласно `gmsCoreVendorGroupId` (по умолчанию ReVanced GmsCore с пакетом `app.revanced.android.gms`).
2. Добавьте Google-аккаунт в настройках microG и выдайте приложению доступ при первом входе.
3. При необходимости выдайте разрешения через `adb` (актуально для Android без root):
   ```bash
   adb shell pm grant com.google.android.apps.yourwidget android.permission.GET_ACCOUNTS
   adb shell pm grant com.google.android.apps.yourwidget app.revanced.gms.EXTENDED_ACCESS
   ```

### Как отозвать OAuth-доступ у тестового аккаунта

1. Откройте настройки **Google-аккаунта** (`myaccount.google.com`) -> **Безопасность** -> **Связи со сторонними приложениями и сервисами** (и **Ваши устройства**).
2. Удалите выданный доступ для **Create My Widget** / сеанса microG.
3. При желании удалите аккаунт из **Настройки microG -> Аккаунты Google**.

### Сборка из исходников

Для загрузки артефактов Morphe из GitHub Packages (`https://maven.pkg.github.com/MorpheApp/registry`) требуются учетные данные GitHub с правом `read:packages` (или заранее собранный `mavenLocal()`):

```bash
export GITHUB_ACTOR="<ваш-логин-github>"
export GITHUB_TOKEN="<github-pat-с-правом-read-packages>"

./gradlew :patches:buildAndroid generatePatchesList
```

Либо укажите `gpr.user` и `gpr.key` в `~/.gradle/gradle.properties`.

Готовые файлы:
- Бандл патчей: `patches/build/libs/patches-1.0.0.mpp`
- Список патчей: `patches-list.json`

### Лицензия / License

GPL-3.0 — см. [LICENSE](LICENSE) и [NOTICE](NOTICE) (соблюдены требования Section 7 по наименованию проекта и сохранению уведомлений Morphe).

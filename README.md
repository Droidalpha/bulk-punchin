# Delhivery Bulk Punch In — Android app project

This is a lightweight Android WebView wrapper for the official Delhivery Bulk Punch In webpage:

https://adhoc-manpower.delhivery.com/bulk-punch-in

## Build an APK

1. Install Android Studio on a computer.
2. Open Android Studio and choose **Open**.
3. Select this folder (`DelhiveryBulkPunchIn`).
4. Allow Android Studio to sync/download the Android Gradle plugin and SDK components.
5. From the menu, choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. When the build completes, select **locate** in the notification. The APK will be under `app/build/outputs/apk/debug/`.
7. Transfer the APK to your Android phone, open it, and allow installation from that source if prompted.

## Notes

- The app requires an internet connection and uses the website's existing login/access controls.
- This does not bypass Delhivery authentication, device policies, or website restrictions.
- This wrapper is not affiliated with or endorsed by Delhivery.
- App name is `Delhivery Bulk Punch In`; package ID is `com.delhivery.bulkpunchin`.

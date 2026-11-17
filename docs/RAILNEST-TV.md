# Railnest TV

Railnest TV is a standalone Android TV home app based on Aerial's app catalog and wallpaper engine. It updates the previous local preview in place, using the same application ID and signing key. The Aerial project history and license retain their original attribution.

Press Left from the first card in any row, or press Menu, to open the left navigation drawer. It has five destinations: Search, Home, Network settings, Desktop settings, and System settings. Press Right or Back to return to the apps.

Open **Desktop settings → Wallpaper** to select one of ten bundled images or an existing photo directly on the TV. **Set up from a phone or computer** displays a QR code and a local network address for selecting or uploading a photo.

The desktop settings page shows an explanation, current value, or wallpaper preview for the focused row. Press OK on a setting with multiple values to open its choice page.

Desktop settings also contains the home row and column layout, app name visibility, animation settings, hidden apps, and the home layout lock. The photo rotation interval appears while photo rotation is selected. The bundled image sources and licenses are in [wallpaper credits](../app/src/main/assets/figma-wallpapers/SOURCES.md).

Press Back from the first home screen or choose **Desktop settings → Start screensaver** to launch the system screensaver on supported devices. **Screensaver settings** opens the native configuration panel.

Choose **Desktop settings → Set Railnest TV as default Home** to open Android's Home app picker. The row shows whether the system resolves Home to Railnest TV. On the MiTV-AFMU1 tested here, the picker saves the selection, but the firmware still opens Google TV when pressing Home and after reboot. Standby also resumes the open app. You can choose Google TV again in the same picker.

Design reference: [Android TV design guidelines](https://developer.android.com/design/ui/tv).

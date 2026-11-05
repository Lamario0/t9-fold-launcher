# T9 Fold Launcher

A personal home-screen launcher for the Galaxy Z Fold 5 that looks and works like an old candybar phone.

- **Cover screen:** a phone with an LCD and a T9 keypad. Swipe left for the widget page.
- **Unfolded:** like an opened flip phone. The left half is the cover phone flipped face down (mirrored). The right half opens on a panel of office-phone speed keys; swipe left for the widget page.

## Keys

| Key | What it does |
| --- | --- |
| **2–9** | Find apps. In *Multi-tap* mode, tapping 5 shows J apps, again K, again L, a fourth time J+K+L. Pause, or press another key, to add the next letter. In *T9 words* mode, each press is one letter and any word in the name can match (6-2-7 finds Google **Map**s). |
| **0** | All apps, A–Z. |
| **1, \*, #** | Favourite apps or folders. Tap to open; hold to change. The first tap of an empty key asks you to pick one. |
| **Green** | Open the highlighted app. From the idle screen it shows recent apps. |
| **Red** | Back to the idle screen. |
| **Left soft key** | *Menu* (settings) on the idle screen. *Options* on an app: info, uninstall, hide, assign to a favourite key. |
| **Right soft key (C)** | Delete a letter / go back. Hold to clear everything. |
| **Up / Down** | Move the highlight. Hold to keep scrolling; it speeds up the longer you hold, and stops at the top or bottom of the list. |

Long-press any app in a list for its options. Swipe down anywhere to open notifications.

## Folders

Folders are lists on the LCD, like the folders on the old phones.

- **Make one:** open an app's *Options*, choose *Add to folder*, then *+ New folder*. You can also use *Menu → Folders → + New folder*. Type the name on the keypad, old-phone style: tap 4 twice for H, pause, then tap 4 three times for i. 0 is a space, C deletes, and *Save* (the left soft key or green key) finishes.
- **Find one:** folders are listed first on **0** (All apps). They also show up when you search, so typing G-A-M finds a "Games" folder.
- **Inside a folder:** just the folder's apps; the green key opens the highlighted one. *Options* on an app there includes *Remove from folder*. To tick several apps in or out at once, use the folder's *Add / remove apps* option.
- **Folder options** (*Options* or long-press on a folder): open, add or remove apps, rename, delete (the apps stay installed), or put the folder on key 1, \* or #.

## Phone styles

*Menu → Phone style* switches the look of both screens:

- **Nokia 3310** (the default): navy body, earpiece slots, yellow-green LCD, and wide oval keys in a smile. Under the screen sit the C key, the big blue Navi key on a silver arc, and the up/down scroll key. The Navi key opens Menu from the idle screen and opens or selects the highlighted row elsewhere; hold it for an app's options. The screen shows just the Navi key's label.
- **Razr silver / Razr pink**: metal flip with a slotted earpiece, black glass around a colour screen (blue or pink title bar), and a wide hinge. The flat keys are etched into a metal plate. Under the screen: two soft keys with a menu key between them, a web key (all apps) over the green send key, the round pad (top and bottom scroll, centre selects), and a message key (recent apps) over the red end key. A speaker grille sits in the chin.
- **Samsung flip**: silver clamshell with an oval earpiece, a framed colour screen with a blue title bar, and a barrel hinge with round bumpers. Under the screen: soft keys marked with three dots, a square pad with a blue *i* centre key, then send, C, and end keys. The pill keys have blue printing and sit in curved rows.
- **Motorola brick**: cream body with a dark face, antenna, speaker cup, BATT/ROAM lights, a red LED readout, and square keys. The function keys work as printed: Rcl (recent apps), Clr, Snd (red; opens), Fcn (menu/options), End, Vol (volume), and Lock (quick settings). Sto ▲ and Pwr ▼ scroll, since the original had no arrow keys.

## Settings (Menu)

Phone style, updates, search mode, key sounds (tones / clicks / silent), vibration, Android or retro status bar, the unfold animation, mirroring of the inside keypad, colour or LCD-style icons, favourite keys, hidden apps, and a shortcut to set the launcher as your home app.

## Speed keys (unfolded screen)

The right half of the unfolded screen has 16 programmable keys, like an office desk phone. Each has a plastic button, a status light, and a typed paper label.

- **Tap** a key (or its label) to use it. An empty key asks what it should do.
- **Hold** a key to change it. The choice appears on the phone's screen on the left: *Apps*, *Folders*, *App shortcuts*, *Quick actions*, or *Clear key*.
- **App shortcuts** are the extras apps offer on a long-press, such as a specific chat, "New tab", or directions home. They only appear while T9 Fold is your home app.
- **Quick actions:** Flashlight (a real on/off toggle; its light glows green while on), Wi-Fi, Internet, Bluetooth, Volume, Do not disturb, Notifications, Quick settings, Camera, and Settings. Android doesn't let apps switch Wi-Fi or Bluetooth directly, so those open the system panel or settings page.

The light is dim green on keys that are set, off on empty keys, and flashes amber when pressed.

## Widgets

The widget page is one swipe left of the dialer on the cover screen, and one swipe left of the speed keys when unfolded. Both show the same widgets. It starts blank. Tap **EDIT**, then **+ ADD WIDGET**. You can add any Android widget or a built-in retro one (pixel clock, calendar, battery). In edit mode, each widget can be moved up or down, made taller or shorter, or removed.

## Installing on the phone

Every push to `main` builds an APK with GitHub Actions and attaches it to a new release.

1. On the phone, sign in to github.com in the browser. The repo is private, so this is needed.
2. Open the repo's **Releases** page, take the newest **Build N**, and download `t9-fold-launcher.apk`.
3. Open the APK and allow your browser to install unknown apps if asked.
4. Press Home and choose **T9 Fold**, then **Always**. You can also use *Menu → Set as home app*, or *Settings → Apps → Choose default apps → Home app*.

Later builds install over the old one and keep your settings. The debug signing key is committed on purpose so this keeps working.

## Updating from inside the app

The launcher can update itself from this repo's Releases. Because the repo is private, it needs a read-only GitHub token, set up once.

1. On github.com, go to *Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token*.
2. Under *Repository access*, choose *Only select repositories* and pick **t9-fold-launcher**.
3. Under *Permissions → Repository permissions*, set **Contents** to **Read-only**. Leave everything else as it is.
4. Generate the token and copy it on the phone, for example by opening github.com in the phone's browser.
5. In the launcher, open *Menu → Updates → Paste token*.

After that, the launcher checks for a new build every few hours when you go to the home screen. You can also choose *Menu → Updates → Check now*. When a newer build exists, the idle screen shows **New build N! Tap**. Choose *Install build N*: the APK downloads, and Android asks "Update this app?". The first time, Android also asks you to allow T9 Fold to install apps; allow it, then choose Install again.

If your token expires, make a new one and paste it the same way. *Auto check* turns the background checks off; *Remove token* deletes the token from the phone.

## Building on a PC (optional)

```bash
git clone https://github.com/lamario0/t9-fold-launcher.git C:\dev\t9-fold-launcher
```

Open the folder in Android Studio, or run `gradlew assembleDebug`.

The pixel font is VT323 (SIL Open Font License; see `FONT_LICENSE_OFL.txt`).

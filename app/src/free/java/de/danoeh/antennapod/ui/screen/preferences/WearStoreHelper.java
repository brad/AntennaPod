package de.danoeh.antennapod.ui.screen.preferences;

class WearStoreHelper {
    static void setup(MainPreferencesFragment fragment) {
        fragment.findPreference(MainPreferencesFragment.PREF_SCREEN_WEAR_OS).setVisible(false);
    }
}

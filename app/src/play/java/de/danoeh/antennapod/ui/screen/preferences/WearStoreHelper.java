package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.wear.remote.interactions.RemoteActivityHelper;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import de.danoeh.antennapod.R;

class WearStoreHelper {
    static void setup(MainPreferencesFragment fragment) {
        fragment.findPreference(MainPreferencesFragment.PREF_SCREEN_WEAR_OS)
                .setOnPreferenceClickListener(preference -> {
                    openPlayStoreOnWatch(fragment);
                    return true;
                });
    }

    private static void openPlayStoreOnWatch(MainPreferencesFragment fragment) {
        RemoteActivityHelper remoteActivityHelper = new RemoteActivityHelper(fragment.requireContext(), Runnable::run);
        Wearable.getNodeClient(fragment.requireContext()).getConnectedNodes().addOnSuccessListener(nodes -> {
            if (nodes.isEmpty()) {
                Toast.makeText(fragment.requireContext(),
                        R.string.wearos_phone_not_reachable, Toast.LENGTH_SHORT).show();
                return;
            }
            for (Node node : nodes) {
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("market://details?id=" + fragment.requireContext().getPackageName()));
                remoteActivityHelper.startRemoteActivity(intent, node.getId());
            }
        });
    }
}

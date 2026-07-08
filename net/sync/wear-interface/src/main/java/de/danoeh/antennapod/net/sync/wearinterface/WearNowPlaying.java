package de.danoeh.antennapod.net.sync.wearinterface;

import de.danoeh.antennapod.model.feed.FeedItem;

public class WearNowPlaying {
    public final FeedItem item;
    public final boolean isPlaying;
    public final int volume;
    public final int maxVolume;
    public final String outputDevice;

    public WearNowPlaying(FeedItem item, boolean isPlaying, int volume, int maxVolume, String outputDevice) {
        this.item = item;
        this.isPlaying = isPlaying;
        this.volume = volume;
        this.maxVolume = maxVolume;
        this.outputDevice = outputDevice;
    }
}

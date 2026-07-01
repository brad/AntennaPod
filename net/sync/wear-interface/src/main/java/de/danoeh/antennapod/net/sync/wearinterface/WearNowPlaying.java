package de.danoeh.antennapod.net.sync.wearinterface;

import de.danoeh.antennapod.model.feed.FeedItem;

public class WearNowPlaying {
    public final FeedItem item;
    public final boolean isPlaying;
    public final float speed;
    public final int fastForwardSecs;
    public final int rewindSecs;
    public final int volume;
    public final int maxVolume;

    public WearNowPlaying(FeedItem item, boolean isPlaying, float speed, int fastForwardSecs, int rewindSecs, int volume, int maxVolume) {
        this.item = item;
        this.isPlaying = isPlaying;
        this.speed = speed;
        this.fastForwardSecs = fastForwardSecs;
        this.rewindSecs = rewindSecs;
        this.volume = volume;
        this.maxVolume = maxVolume;
    }
}

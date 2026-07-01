# Wear OS Companion App Roadmap

This document outlines the tasks required to build a high-quality Wear OS companion app for AntennaPod. Tasks are broken down into "Jules-size" items that can be completed in a single session.

## Phase 1: Player Experience & MVP (Current Focus)

### 1.1 Communication Layer Enhancements
- [x] **Add Skip/Seek Support to Data Layer**: Update `WearDataPaths.java` and `WearListenerService.java` on the phone to handle skip forward, skip backward, and seek-to-position messages.
- [x] **Implement Playback Speed Control**: Add paths and logic to get/set playback speed via the Wear OS DataLayer.
- [x] **Implement Volume Control**: Add paths and logic to sync and control phone volume from the watch.
- [x] **Enhance `WearNowPlaying` Model**: Update serialization to include current playback speed, skip intervals, and volume levels.

### 1.2 Modern Player UI
- [ ] **Create Dedicated Player Screen**: Implement a new `PlayerActivity` and `PlayerViewModel` to replace the basic `EpisodeDetailActivity`.
- [ ] **Add Media Control Buttons**: Implement a layout with Play/Pause, Skip Forward, Skip Backward, and Playback Speed buttons.
- [ ] **Progress Seeking UI**: Allow users to interact with the progress bar to seek within the episode.
- [ ] **Implement Volume Slider**: Add a UI component (or Digital Crown support) to adjust the phone's volume.
- [ ] **Visual Polish - Background Art**: Fetch and display the podcast/episode cover art as a blurred background or prominent thumbnail on the player screen.
- [ ] **Visual Polish - Typography & Layout**: Refine spacing and font sizes for a native Wear OS look and feel using Material3.

### 1.3 List & Browsing Enhancements
- [ ] **Thumbnails in Lists**: Update `ListItem.kt` to support displaying remote images (cached on watch) for subscriptions and episodes.
- [ ] **Sync Queue State**: Ensure the watch queue list updates immediately when the phone's queue changes (via DataLayer events).

## Phase 2: Integration & Convenience (Stretch)

### 2.1 Tiles & Complications
- [ ] **"Now Playing" Tile**: Create a Tile that shows the current episode and provides quick Play/Pause/Skip controls.
- [ ] **"Quick Access" Tile**: A Tile showing the top 3 items in the Queue or the most recent Downloads.
- [ ] **Playback Complication**: A watch face complication showing playback progress or a shortcut to the player.

### 2.2 System Integration
- [ ] **Ongoing Activity**: Integrate with Wear OS Ongoing Activities so the user can easily return to the player from the watch face.

## Phase 3: Standalone Playback (Future Stretch)

### 3.1 Technical Research
- [ ] **Research Standalone Playback**: Investigate Media3 integration on Wear OS for local playback and storage management for offline episodes.

### 3.2 Implementation
- [ ] **Offline Syncing**: Implement a mechanism to mark episodes for "Sync to Watch" and transfer the media files.
- [ ] **Local Media Player**: Build the local playback engine to allow listening without a phone connection.

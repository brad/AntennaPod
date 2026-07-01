package de.danoeh.antennapod.net.download.service.feed.local;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterfaceStub;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowMediaMetadataRetriever;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.robolectric.Shadows.shadowOf;

/**
 * Test local feeds handling in class LocalFeedUpdater.
 */
@RunWith(RobolectricTestRunner.class)
public class LocalFeedUpdaterTest {

    private static final String FEED_URL =
            "content://com.android.externalstorage.documents/tree/primary%3ADownload%2Flocal-feed";
    private static final String LOCAL_FEED_DIR1 = "src/test/assets/local-feed1";
    private static final String LOCAL_FEED_DIR2 = "src/test/assets/local-feed2";

    private Context context;

    @Before
    public void setUp() throws Exception {
        // Initialize environment
        context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);
        PlaybackPreferences.init(context);
        SynchronizationSettings.init(context);
        DownloadServiceInterface.setImpl(new DownloadServiceInterfaceStub());
        SynchronizationQueue.setInstance(Mockito.mock(SynchronizationQueue.class));

        // Initialize database
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

        mapDummyMetadata(LOCAL_FEED_DIR1);
        mapDummyMetadata(LOCAL_FEED_DIR2);
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypeMapping("mp3", "audio/mp3");
    }

    @After
    public void tearDown() {
        DBWriter.tearDownTests();
        PodDBAdapter.tearDownTests();
    }

    /**
     * Test adding a new local feed.
     */
    @Test
    public void testUpdateFeed_AddNewFeed() {
        // check for empty database
        List<Feed> feedListBefore = DBReader.getFeedList();
        assertThat(feedListBefore, is(empty()));

        callUpdateFeed(LOCAL_FEED_DIR2);

        // verify new feed in database
        verifySingleFeedInDatabaseAndItemCount(2);
        Feed feedAfter = verifySingleFeedInDatabase();
        assertEquals(FEED_URL, feedAfter.getDownloadUrl());
    }

    /**
     * Test adding further items to an existing local feed.
     */
    @Test
    public void testUpdateFeed_AddMoreItems() {
        // add local feed with 1 item (localFeedDir1)
        callUpdateFeed(LOCAL_FEED_DIR1);

        // now add another item (by changing to local feed folder localFeedDir2)
        callUpdateFeed(LOCAL_FEED_DIR2);

        verifySingleFeedInDatabaseAndItemCount(2);
    }

    /**
     * Test removing items from an existing local feed without a corresponding media file.
     */
    @Test
    public void testUpdateFeed_RemoveItems() {
        // add local feed with 2 items (localFeedDir1)
        callUpdateFeed(LOCAL_FEED_DIR2);

        // now remove an item (by changing to local feed folder localFeedDir1)
        callUpdateFeed(LOCAL_FEED_DIR1);

        verifySingleFeedInDatabaseAndItemCount(1);
    }

    /**
     * Test feed icon defined in the local feed media folder.
     */
    @Test
    public void testUpdateFeed_FeedIconFromFolder() {
        callUpdateFeed(LOCAL_FEED_DIR2);

        Feed feedAfter = verifySingleFeedInDatabase();
        assertThat(feedAfter.getImageUrl(), endsWith("%2Ffolder.png"));
    }

    /**
     * Test default feed icon if there is no matching file in the local feed media folder.
     */
    @Test
    public void testUpdateFeed_FeedIconDefault() {
        callUpdateFeed(LOCAL_FEED_DIR1);

        Feed feedAfter = verifySingleFeedInDatabase();
        assertThat(feedAfter.getImageUrl(), startsWith(Feed.PREFIX_GENERATIVE_COVER));
    }

    /**
     * Test default feed metadata.
     *
     * @see #mapDummyMetadata Title and PubDate are dummy values.
     */
    @Test
    public void testUpdateFeed_FeedMetadata() {
        callUpdateFeed(LOCAL_FEED_DIR1);

        Feed feed = verifySingleFeedInDatabase();
        List<FeedItem> feedItems = DBReader.getFeedItemList(feed, FeedItemFilter.unfiltered(),
                SortOrder.DATE_NEW_OLD, 0, Integer.MAX_VALUE);
        assertEquals("track1", feedItems.get(0).getTitle());
    }

    @Test
    public void testGetImageUrl_EmptyFolder() {
        String imageUrl = LocalFeedUpdater.getImageUrl(context, Collections.emptyList(), Uri.parse("content://tree/dummy"));
        assertThat(imageUrl, startsWith(Feed.PREFIX_GENERATIVE_COVER));
    }

    @Test
    public void testGetImageUrl_NoImageButAudioFiles() {
        List<FastDocumentFile> folder = Collections.singletonList(mockDocumentFile("audio.mp3", "audio/mp3"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, folder, Uri.parse("content://tree/dummy"));
        assertThat(imageUrl, startsWith(Feed.PREFIX_GENERATIVE_COVER));
    }

    @Test
    public void testGetImageUrl_PreferredImagesFilenames() {
        for (String filename : LocalFeedUpdater.PREFERRED_FEED_IMAGE_FILENAMES) {
            List<FastDocumentFile> folder = Arrays.asList(mockDocumentFile("audio.mp3", "audio/mp3"),
                    mockDocumentFile(filename, "image/jpeg")); // image MIME type doesn't matter
            String imageUrl = LocalFeedUpdater.getImageUrl(context, folder, Uri.parse("content://tree/dummy"));
            assertThat(imageUrl, endsWith(filename));
        }
    }

    @Test
    public void testGetImageUrl_OtherImageFilenameJpg() {
        List<FastDocumentFile> folder = Arrays.asList(mockDocumentFile("audio.mp3", "audio/mp3"),
                mockDocumentFile("my-image.jpg", "image/jpeg"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, folder, Uri.parse("content://tree/dummy"));
        assertThat(imageUrl, endsWith("my-image.jpg"));
    }

    @Test
    public void testGetImageUrl_OtherImageFilenameJpeg() {
        List<FastDocumentFile> folder = Arrays.asList(mockDocumentFile("audio.mp3", "audio/mp3"),
                mockDocumentFile("my-image.jpeg", "image/jpeg"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, folder, Uri.parse("content://tree/dummy"));
        assertThat(imageUrl, endsWith("my-image.jpeg"));
    }

    @Test
    public void testGetImageUrl_OtherImageFilenamePng() {
        List<FastDocumentFile> folder = Arrays.asList(mockDocumentFile("audio.mp3", "audio/mp3"),
                mockDocumentFile("my-image.png", "image/png"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, folder, Uri.parse("content://tree/dummy"));
        assertThat(imageUrl, endsWith("my-image.png"));
    }

    @Test
    public void testGetImageUrl_OtherImageFilenameUnsupportedMimeType() {
        List<FastDocumentFile> folder = Arrays.asList(mockDocumentFile("audio.mp3", "audio/mp3"),
                mockDocumentFile("my-image.svg", "image/svg+xml"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, folder, Uri.parse("content://tree/dummy"));
        assertThat(imageUrl, startsWith(Feed.PREFIX_GENERATIVE_COVER));
    }

    @Test
    public void testGetImageUrl_CoverJpgRecursive() {
        try (MockedStatic<FastDocumentFile> dfMock = Mockito.mockStatic(FastDocumentFile.class);
             MockedStatic<DocumentsContract> dcMock = Mockito.mockStatic(DocumentsContract.class)) {
            Uri treeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload");
            Uri subDocUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload/document/primary%3ADownload%2Fsub");

            dcMock.when(() -> DocumentsContract.getDocumentId(treeUri)).thenReturn("primary:Download");
            dcMock.when(() -> DocumentsContract.getDocumentId(subDocUri)).thenReturn("primary:Download/sub");

            dfMock.when(() -> FastDocumentFile.list(any(), eq(treeUri), eq("primary:Download")))
                    .thenReturn(Collections.singletonList(
                            mockDocumentFileWithUri("sub", DocumentsContract.Document.MIME_TYPE_DIR, subDocUri)));
            dfMock.when(() -> FastDocumentFile.list(any(), eq(treeUri), eq("primary:Download/sub")))
                    .thenReturn(Collections.singletonList(mockDocumentFile("cover.jpg", "image/jpeg")));

            String imageUrl = LocalFeedUpdater.getImageUrl(context, Collections.emptyList(), treeUri);
            assertThat(imageUrl, endsWith("cover.jpg"));
        }
    }

    @Test
    public void testGetImageUrl_CoverJpegCaseInsensitive() {
        try (MockedStatic<FastDocumentFile> dfMock = Mockito.mockStatic(FastDocumentFile.class);
             MockedStatic<DocumentsContract> dcMock = Mockito.mockStatic(DocumentsContract.class)) {
            Uri treeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload");
            dcMock.when(() -> DocumentsContract.getDocumentId(treeUri)).thenReturn("primary:Download");

            dfMock.when(() -> FastDocumentFile.list(any(), eq(treeUri), eq("primary:Download")))
                    .thenReturn(Collections.singletonList(mockDocumentFile("CoVeR.jPeG", "image/jpeg")));

            String imageUrl = LocalFeedUpdater.getImageUrl(context, Collections.emptyList(), treeUri);
            assertThat(imageUrl, endsWith("CoVeR.jPeG"));
        }
    }

    /**
     * Fill ShadowMediaMetadataRetriever with dummy duration and title.
     *
     * @param localFeedDir assets local feed folder with media files
     */
    private void mapDummyMetadata(@NonNull String localFeedDir) {
        for (String fileName : Objects.requireNonNull(new File(localFeedDir).list())) {
            String path = localFeedDir + '/' + fileName;
            ShadowMediaMetadataRetriever.addMetadata(path,
                    MediaMetadataRetriever.METADATA_KEY_DURATION, "10");
            ShadowMediaMetadataRetriever.addMetadata(path,
                    MediaMetadataRetriever.METADATA_KEY_TITLE, fileName);
            ShadowMediaMetadataRetriever.addMetadata(path,
                    MediaMetadataRetriever.METADATA_KEY_DATE, "20200601T222324");
        }
    }

    /**
     * Calls the method LocalFeedUpdater#tryUpdateFeed with the given local feed folder.
     *
     * @param localFeedDir assets local feed folder with media files
     */
    private void callUpdateFeed(@NonNull String localFeedDir) {
        try (MockedStatic<FastDocumentFile> dfMock = Mockito.mockStatic(FastDocumentFile.class)) {
            Uri folderUri = Uri.parse(FEED_URL);
            // mock external storage
            dfMock.when(() -> FastDocumentFile.list(any(), eq(folderUri))).thenReturn(mockLocalFolder(localFeedDir));
            dfMock.when(() -> FastDocumentFile.list(any(), any(), any())).thenReturn(Collections.emptyList());

            // call method to test
            Feed feed = new Feed(FEED_URL, null);
            feed.setPreferences(new de.danoeh.antennapod.model.feed.FeedPreferences(0, de.danoeh.antennapod.model.feed.FeedPreferences.AutoDownloadSetting.GLOBAL, de.danoeh.antennapod.model.feed.FeedPreferences.AutoDeleteAction.GLOBAL, de.danoeh.antennapod.model.feed.VolumeAdaptionSetting.OFF, de.danoeh.antennapod.model.feed.FeedPreferences.NewEpisodesAction.GLOBAL, null, null));

            for (FastDocumentFile f : mockLocalFolder(localFeedDir)) {
                shadowOf(context.getContentResolver()).registerInputStream(f.getUri(), new ByteArrayInputStream(new byte[0]));
            }

            try {
                LocalFeedUpdater.tryUpdateFeed(feed, context, folderUri, null);
            } catch (Exception e) {
                // Ignore. SynchronizationQueue is not mocked, but we can still check the database.
            }
        }
    }

    /**
     * Verify that the database contains exactly one feed and return that feed.
     */
    @NonNull
    private static Feed verifySingleFeedInDatabase() {
        List<Feed> feedListAfter = DBReader.getFeedList();
        assertEquals(1, feedListAfter.size());
        return feedListAfter.get(0);
    }

    /**
     * Verify that the database contains exactly one feed and the number of
     * items in the feed.
     *
     * @param expectedItemCount expected number of items in the feed
     */
    private static void verifySingleFeedInDatabaseAndItemCount(int expectedItemCount) {
        Feed feed = verifySingleFeedInDatabase();
        List<FeedItem> feedItems = DBReader.getFeedItemList(feed, FeedItemFilter.unfiltered(),
                SortOrder.DATE_NEW_OLD, 0, Integer.MAX_VALUE);
        assertEquals(expectedItemCount, feedItems.size());
    }

    /**
     * Create a DocumentFile mock object.
     */
    @NonNull
    private static FastDocumentFile mockDocumentFile(@NonNull String fileName, @NonNull String mimeType) {
        return mockDocumentFileWithUri(fileName, mimeType, Uri.parse("content://tree/dummy/document/" + fileName));
    }

    @NonNull
    private static FastDocumentFile mockDocumentFileWithUri(@NonNull String fileName, @NonNull String mimeType, @NonNull Uri uri) {
        return new FastDocumentFile(fileName, mimeType, uri, 0, 0);
    }

    private static List<FastDocumentFile> mockLocalFolder(String folderName) {
        List<FastDocumentFile> files = new ArrayList<>();
        for (File f : Objects.requireNonNull(new File(folderName).listFiles())) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(f.getPath());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            files.add(new FastDocumentFile(f.getName(), mimeType,
                    Uri.parse("content://tree/primary%3ADownload/document/primary%3ADownload%2F" + f.getName()), f.length(), f.lastModified()));
        }
        return files;
    }
}

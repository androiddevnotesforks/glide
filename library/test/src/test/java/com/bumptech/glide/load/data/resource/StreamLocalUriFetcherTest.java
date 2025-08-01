package com.bumptech.glide.load.data.resource;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.StreamLocalUriFetcher;
import com.bumptech.glide.load.data.mediastore.MediaStoreUtil;
import com.bumptech.glide.tests.ContentResolverShadow;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = ROBOLECTRIC_SDK,
    shadows = {ContentResolverShadow.class})
public class StreamLocalUriFetcherTest {
  @Mock private DataFetcher.DataCallback<InputStream> callback;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testLoadResource_returnsInputStream() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    Uri uri = Uri.parse("file://nothing");

    ContentResolver contentResolver = context.getContentResolver();
    ContentResolverShadow shadow = Shadow.extract(contentResolver);
    shadow.registerInputStream(uri, new ByteArrayInputStream(new byte[0]));

    StreamLocalUriFetcher fetcher =
        new StreamLocalUriFetcher(context.getContentResolver(), uri, false);
    fetcher.loadData(Priority.NORMAL, callback);
    verify(callback).onDataReady(ArgumentMatchers.<InputStream>isNotNull());
  }

  @Test
  public void testLoadResource_mediaUri_returnsFileDescriptor() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    Uri uri = Uri.parse("content://media");

    ContentResolver contentResolver = context.getContentResolver();

    AssetFileDescriptor assetFileDescriptor = mock(AssetFileDescriptor.class);
    FileInputStream inputStream = mock(FileInputStream.class);
    when(assetFileDescriptor.createInputStream()).thenReturn(inputStream);

    StreamLocalUriFetcher fetcher =
        new StreamLocalUriFetcher(
            context.getContentResolver(), uri, /* useMediaStoreApisIfAvailable */ true);

    try (MockedStatic<MediaStoreUtil> utils = Mockito.mockStatic(MediaStoreUtil.class)) {
      utils.when(MediaStoreUtil::isMediaStoreOpenFileApisAvailable).thenReturn(true);
      utils.when(() -> MediaStoreUtil.isMediaStoreUri(uri)).thenReturn(true);
      utils
          .when(() -> MediaStoreUtil.openAssetFileDescriptor(uri, contentResolver))
          .thenReturn(assetFileDescriptor);
      fetcher.loadData(Priority.NORMAL, callback);
      verify(callback).onDataReady(eq(inputStream));
    }
  }

  @Test
  public void testLoadResource_withNullInputStream_callsLoadFailed() {
    Context context = ApplicationProvider.getApplicationContext();
    Uri uri = Uri.parse("file://nothing");

    ContentResolver contentResolver = context.getContentResolver();
    ContentResolverShadow shadow = Shadow.extract(contentResolver);

    shadow.registerInputStream(uri, null /*inputStream*/);

    StreamLocalUriFetcher fetcher =
        new StreamLocalUriFetcher(
            context.getContentResolver(), uri, /* useMediaStoreApisIfAvailable */ false);
    fetcher.loadData(Priority.LOW, callback);

    verify(callback).onLoadFailed(isA(FileNotFoundException.class));
  }
}

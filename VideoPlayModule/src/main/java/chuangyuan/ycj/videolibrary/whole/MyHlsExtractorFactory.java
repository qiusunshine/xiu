package chuangyuan.ycj.videolibrary.whole;

import static com.google.android.exoplayer2.util.Assertions.checkNotNull;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor;
import com.google.android.exoplayer2.extractor.ts.Ac3Extractor;
import com.google.android.exoplayer2.extractor.ts.Ac4Extractor;
import com.google.android.exoplayer2.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory;
import com.google.android.exoplayer2.extractor.ts.MyTsExtractor;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.hls.HlsExtractorFactory;
import com.google.android.exoplayer2.source.hls.HlsTrackMetadataEntry;
import com.google.android.exoplayer2.source.hls.WebvttExtractor;
import com.google.android.exoplayer2.util.FileTypes;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.TimestampAdjuster;
import com.google.common.primitives.Ints;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import chuangyuan.ycj.videolibrary.video.ErrorListener;

public final class MyHlsExtractorFactory implements HlsExtractorFactory {
    private ErrorListener errorListener;

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }
    // Extractors order is optimized according to
    // https://docs.google.com/document/d/1w2mKaWMxfz2Ei8-LdxqbPs1VLe_oudB-eryXXw9OvQQ.
    private static final int[] DEFAULT_EXTRACTOR_ORDER =
            new int[] {
                    FileTypes.MP4,
                    FileTypes.WEBVTT,
                    FileTypes.TS,
                    FileTypes.ADTS,
                    FileTypes.AC3,
                    FileTypes.AC4,
                    FileTypes.MP3
            };

    @DefaultTsPayloadReaderFactory.Flags private final int payloadReaderFactoryFlags;
    private final boolean exposeCea608WhenMissingDeclarations;

    /**
     * Equivalent to {@link #MyHlsExtractorFactory(int, boolean) new
     * DefaultHlsExtractorFactory(payloadReaderFactoryFlags = 0, exposeCea608WhenMissingDeclarations =
     * true)}
     */
    public MyHlsExtractorFactory() {
        this(/* payloadReaderFactoryFlags= */ 0, /* exposeCea608WhenMissingDeclarations */ true);
    }

    /**
     * Creates a factory for HLS segment extractors.
     *
     * @param payloadReaderFactoryFlags Flags to add when constructing any {@link
     *     DefaultTsPayloadReaderFactory} instances. Other flags may be added on top of {@code
     *     payloadReaderFactoryFlags} when creating {@link DefaultTsPayloadReaderFactory}.
     * @param exposeCea608WhenMissingDeclarations Whether created {@link MyTsExtractor} instances should
     *     expose a CEA-608 track should the master playlist contain no Closed Captions declarations.
     *     If the master playlist contains any Closed Captions declarations, this flag is ignored.
     */
    public MyHlsExtractorFactory(
            int payloadReaderFactoryFlags, boolean exposeCea608WhenMissingDeclarations) {
        this.payloadReaderFactoryFlags = payloadReaderFactoryFlags;
        this.exposeCea608WhenMissingDeclarations = exposeCea608WhenMissingDeclarations;
    }

    @Override
    public MyBundledHlsMediaChunkExtractor createExtractor(
            Uri uri,
            Format format,
            @Nullable List<Format> muxedCaptionFormats,
            TimestampAdjuster timestampAdjuster,
            Map<String, List<String>> responseHeaders,
            ExtractorInput sniffingExtractorInput)
            throws IOException {
        @FileTypes.Type
        int formatInferredFileType = FileTypes.inferFileTypeFromMimeType(format.sampleMimeType);
        @FileTypes.Type
        int responseHeadersInferredFileType =
                FileTypes.inferFileTypeFromResponseHeaders(responseHeaders);
        @FileTypes.Type int uriInferredFileType = FileTypes.inferFileTypeFromUri(uri);

        // Defines the order in which to try the extractors.
        List<Integer> fileTypeOrder =
                new ArrayList<>(/* initialCapacity= */ DEFAULT_EXTRACTOR_ORDER.length);
        addFileTypeIfValidAndNotPresent(formatInferredFileType, fileTypeOrder);
        addFileTypeIfValidAndNotPresent(responseHeadersInferredFileType, fileTypeOrder);
        addFileTypeIfValidAndNotPresent(uriInferredFileType, fileTypeOrder);
        for (int fileType : DEFAULT_EXTRACTOR_ORDER) {
            addFileTypeIfValidAndNotPresent(fileType, fileTypeOrder);
        }

        // Extractor to be used if the type is not recognized.
        @Nullable Extractor fallBackExtractor = null;
        sniffingExtractorInput.resetPeekPosition();
        for (int i = 0; i < fileTypeOrder.size(); i++) {
            int fileType = fileTypeOrder.get(i);
            Extractor extractor =
                    checkNotNull(
                            createExtractorByFileType(fileType, format, muxedCaptionFormats, timestampAdjuster));
            if (sniffQuietly(extractor, sniffingExtractorInput)) {
                return new MyBundledHlsMediaChunkExtractor(extractor, format, timestampAdjuster, errorListener);
            }
            if (fallBackExtractor == null
                    && (fileType == formatInferredFileType
                    || fileType == responseHeadersInferredFileType
                    || fileType == uriInferredFileType
                    || fileType == FileTypes.TS)) {
                // If sniffing fails, fallback to the file types inferred from context. If all else fails,
                // fallback to Transport Stream. See https://github.com/google/ExoPlayer/issues/8219.
                fallBackExtractor = extractor;
            }
        }

        return new MyBundledHlsMediaChunkExtractor(
                checkNotNull(fallBackExtractor), format, timestampAdjuster, errorListener);
    }

    private static void addFileTypeIfValidAndNotPresent(
            @FileTypes.Type int fileType, List<Integer> fileTypes) {
        if (Ints.indexOf(DEFAULT_EXTRACTOR_ORDER, fileType) == -1 || fileTypes.contains(fileType)) {
            return;
        }
        fileTypes.add(fileType);
    }

    @SuppressLint("SwitchIntDef") // HLS only supports a small subset of the defined file types.
    @Nullable
    private Extractor createExtractorByFileType(
            @FileTypes.Type int fileType,
            Format format,
            @Nullable List<Format> muxedCaptionFormats,
            TimestampAdjuster timestampAdjuster) {
        switch (fileType) {
            case FileTypes.WEBVTT:
                return new WebvttExtractor(format.language, timestampAdjuster);
            case FileTypes.ADTS:
                return new AdtsExtractor();
            case FileTypes.AC3:
                return new Ac3Extractor();
            case FileTypes.AC4:
                return new Ac4Extractor();
            case FileTypes.MP3:
                return new Mp3Extractor(/* flags= */ 0, /* forcedFirstSampleTimestampUs= */ 0);
            case FileTypes.MP4:
                return createFragmentedMp4Extractor(timestampAdjuster, format, muxedCaptionFormats);
            case FileTypes.TS:
                return createTsExtractor(
                        payloadReaderFactoryFlags,
                        exposeCea608WhenMissingDeclarations,
                        format,
                        muxedCaptionFormats,
                        timestampAdjuster);
            default:
                return null;
        }
    }

    private static MyTsExtractor createTsExtractor(
            @DefaultTsPayloadReaderFactory.Flags int userProvidedPayloadReaderFactoryFlags,
            boolean exposeCea608WhenMissingDeclarations,
            Format format,
            @Nullable List<Format> muxedCaptionFormats,
            TimestampAdjuster timestampAdjuster) {
        @DefaultTsPayloadReaderFactory.Flags
        int payloadReaderFactoryFlags =
                DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM
                        | userProvidedPayloadReaderFactoryFlags;
        if (muxedCaptionFormats != null) {
            // The playlist declares closed caption renditions, we should ignore descriptors.
            payloadReaderFactoryFlags |= DefaultTsPayloadReaderFactory.FLAG_OVERRIDE_CAPTION_DESCRIPTORS;
        } else if (exposeCea608WhenMissingDeclarations) {
            // The playlist does not provide any closed caption information. We preemptively declare a
            // closed caption track on channel 0.
            muxedCaptionFormats =
                    Collections.singletonList(
                            new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_CEA608).build());
        } else {
            muxedCaptionFormats = Collections.emptyList();
        }
        @Nullable String codecs = format.codecs;
        if (!TextUtils.isEmpty(codecs)) {
            // Sometimes AAC and H264 streams are declared in TS chunks even though they don't really
            // exist. If we know from the codec attribute that they don't exist, then we can
            // explicitly ignore them even if they're declared.
            if (!MimeTypes.containsCodecsCorrespondingToMimeType(codecs, MimeTypes.AUDIO_AAC)) {
                payloadReaderFactoryFlags |= DefaultTsPayloadReaderFactory.FLAG_IGNORE_AAC_STREAM;
            }
            if (!MimeTypes.containsCodecsCorrespondingToMimeType(codecs, MimeTypes.VIDEO_H264)) {
                payloadReaderFactoryFlags |= DefaultTsPayloadReaderFactory.FLAG_IGNORE_H264_STREAM;
            }
        }

        return new MyTsExtractor(
                MyTsExtractor.MODE_HLS,
                timestampAdjuster,
                new DefaultTsPayloadReaderFactory(payloadReaderFactoryFlags, muxedCaptionFormats));
    }

    private static FragmentedMp4Extractor createFragmentedMp4Extractor(
            TimestampAdjuster timestampAdjuster,
            Format format,
            @Nullable List<Format> muxedCaptionFormats) {
        // Only enable the EMSG TrackOutput if this is the 'variant' track (i.e. the main one) to avoid
        // creating a separate EMSG track for every audio track in a video stream.
        return new FragmentedMp4Extractor(
                /* flags= */ isFmp4Variant(format) ? FragmentedMp4Extractor.FLAG_ENABLE_EMSG_TRACK : 0,
                timestampAdjuster,
                /* sideloadedTrack= */ null,
                muxedCaptionFormats != null ? muxedCaptionFormats : Collections.emptyList());
    }

    /** Returns true if this {@code format} represents a 'variant' track (i.e. the main one). */
    private static boolean isFmp4Variant(Format format) {
        Metadata metadata = format.metadata;
        if (metadata == null) {
            return false;
        }
        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry entry = metadata.get(i);
            if (entry instanceof HlsTrackMetadataEntry) {
                return !((HlsTrackMetadataEntry) entry).variantInfos.isEmpty();
            }
        }
        return false;
    }

    private static boolean sniffQuietly(Extractor extractor, ExtractorInput input)
            throws IOException {
        boolean result = false;
        try {
            result = extractor.sniff(input);
        } catch (EOFException e) {
            // Do nothing.
        } finally {
            input.resetPeekPosition();
        }
        return result;
    }
}

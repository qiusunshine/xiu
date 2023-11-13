package org.mozilla.xiu.browser.session;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSession.PermissionDelegate;
import org.mozilla.xiu.browser.R;

import java.util.Locale;

/**
 * 作者：By 15968
 * 日期：On 2023/11/13
 * 时间：At 10:09
 */

public class ExamplePermissionDelegate implements PermissionDelegate {
    public static final int REQUEST_PERMISSIONS = 2;
    private Activity activity;

    public ExamplePermissionDelegate(Activity activity) {
        this.activity = activity;
    }

    public int androidPermissionRequestCode = REQUEST_PERMISSIONS;
    private Callback mCallback;

    class ExampleNotificationCallback implements PermissionDelegate.Callback {
        private final PermissionDelegate.Callback mCallback;

        ExampleNotificationCallback(final PermissionDelegate.Callback callback) {
            mCallback = callback;
        }

        @Override
        public void reject() {
            //mShowNotificationsRejected = true;
            mCallback.reject();
        }

        @Override
        public void grant() {
            //mShowNotificationsRejected = false;
            mCallback.grant();
        }
    }

    class ExamplePersistentStorageCallback implements PermissionDelegate.Callback {
        private final PermissionDelegate.Callback mCallback;
        private final String mUri;

        ExamplePersistentStorageCallback(final PermissionDelegate.Callback callback, String uri) {
            mCallback = callback;
            mUri = uri;
        }

        @Override
        public void reject() {
            mCallback.reject();
        }

        @Override
        public void grant() {
            //mAcceptedPersistentStorage.add(mUri);
            mCallback.grant();
        }
    }

    public void onRequestPermissionsResult(final String[] permissions, final int[] grantResults) {
        if (mCallback == null) {
            return;
        }

        final Callback cb = mCallback;
        mCallback = null;
        for (final int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                // At least one permission was not granted.
                cb.reject();
                return;
            }
        }
        cb.grant();
    }

    @Override
    public void onAndroidPermissionsRequest(
            final GeckoSession session, final String[] permissions, final Callback callback) {
        if (Build.VERSION.SDK_INT >= 23) {
            // requestPermissions was introduced in API 23.
            mCallback = callback;
            activity.requestPermissions(permissions, androidPermissionRequestCode);
        } else {
            callback.grant();
        }
    }

    @Override
    public GeckoResult<Integer> onContentPermissionRequest(
            final GeckoSession session, final ContentPermission perm) {
        final int resId;
        switch (perm.permission) {
            case PERMISSION_GEOLOCATION:
                resId = R.string.request_geolocation;
                break;
            case PERMISSION_DESKTOP_NOTIFICATION:
                resId = R.string.request_notification;
                break;
            case PERMISSION_PERSISTENT_STORAGE:
                resId = R.string.request_storage;
                break;
            case PERMISSION_XR:
                resId = R.string.request_xr;
                break;
            case PERMISSION_AUTOPLAY_AUDIBLE:
            case PERMISSION_AUTOPLAY_INAUDIBLE:
//                if (!mAllowAutoplay.value()) {
//                    return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
//                } else {
                return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
            //}
            case PERMISSION_MEDIA_KEY_SYSTEM_ACCESS:
                resId = R.string.request_media_key_system_access;
                break;
            case PERMISSION_STORAGE_ACCESS:
                resId = R.string.request_storage_access;
                break;
            default:
                return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
        }
        final GeckoResult<Integer> res = new GeckoResult<>();
        final String title = activity.getString(resId, Uri.parse(perm.uri).getAuthority());
        new MaterialAlertDialogBuilder(activity)
                .setTitle("授权")
                .setMessage(title)
                .setPositiveButton("确定", (dialogInterface, i) -> {
                    res.complete(ContentPermission.VALUE_ALLOW);
                }).setNegativeButton("取消", (dialogInterface, i) -> {
                    res.complete(ContentPermission.VALUE_DENY);
                }).show();
        return res;
    }

    private String[] normalizeMediaName(final MediaSource[] sources) {
        if (sources == null) {
            return null;
        }
        String[] res = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            final int mediaSource = sources[i].source;
            final String name = sources[i].name;
            if (MediaSource.SOURCE_CAMERA == mediaSource) {
                if (name.toLowerCase(Locale.ROOT).contains("front")) {
                    res[i] = activity.getString(R.string.media_front_camera);
                } else {
                    res[i] = activity.getString(R.string.media_back_camera);
                }
            } else if (!name.isEmpty()) {
                res[i] = name;
            } else if (MediaSource.SOURCE_MICROPHONE == mediaSource) {
                res[i] = activity.getString(R.string.media_microphone);
            } else {
                res[i] = activity.getString(R.string.media_other);
            }
        }
        return res;
    }

    @Override
    public void onMediaPermissionRequest(
            final GeckoSession session,
            final String uri,
            final MediaSource[] video,
            final MediaSource[] audio,
            final MediaCallback callback) {
        // If we don't have device permissions at this point, just automatically reject the request
        // as we will have already have requested device permissions before getting to this point
        // and if we've reached here and we don't have permissions then that means that the user
        // denied them.
        if ((audio != null
                && ContextCompat.checkSelfPermission(
                activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
                || (video != null
                && ContextCompat.checkSelfPermission(
                activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)) {
            callback.reject();
            return;
        }

        final String host = Uri.parse(uri).getAuthority();
        final String title;
        if (audio == null) {
            title = activity.getString(R.string.request_video, host);
        } else if (video == null) {
            title = activity.getString(R.string.request_audio, host);
        } else {
            title = activity.getString(R.string.request_media, host);
        }
        String[] videoNames = normalizeMediaName(video);
        String[] audioNames = normalizeMediaName(audio);
        onMediaPrompt(session, title, video, audio, videoNames, audioNames, callback);
    }

    private Spinner addMediaSpinner(
            final Context context,
            final ViewGroup container,
            final MediaSource[] sources,
            final String[] sourceNames) {
        final ArrayAdapter<MediaSource> adapter =
                new ArrayAdapter<MediaSource>(context, android.R.layout.simple_spinner_item) {
                    private View convertView(final int position, final View view) {
                        if (view != null) {
                            final MediaSource item = getItem(position);
                            ((TextView) view).setText(sourceNames != null ? sourceNames[position] : item.name);
                        }
                        return view;
                    }

                    @Override
                    public View getView(final int position, View view, final ViewGroup parent) {
                        return convertView(position, super.getView(position, view, parent));
                    }

                    @Override
                    public View getDropDownView(final int position, final View view, final ViewGroup parent) {
                        return convertView(position, super.getDropDownView(position, view, parent));
                    }
                };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(sources);

        final Spinner spinner = new Spinner(context);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        container.addView(spinner);
        return spinner;
    }

    public void onMediaPrompt(
            final GeckoSession session,
            final String title,
            final MediaSource[] video,
            final MediaSource[] audio,
            final String[] videoNames,
            final String[] audioNames,
            final GeckoSession.PermissionDelegate.MediaCallback callback) {
        if (activity == null || (video == null && audio == null)) {
            callback.reject();
            return;
        }
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        final LinearLayout container = addStandardLayout(builder, title, /* msg */ null);

        final Spinner videoSpinner;
        if (video != null) {
            videoSpinner = addMediaSpinner(builder.getContext(), container, video, videoNames);
        } else {
            videoSpinner = null;
        }

        final Spinner audioSpinner;
        if (audio != null) {
            audioSpinner = addMediaSpinner(builder.getContext(), container, audio, audioNames);
        } else {
            audioSpinner = null;
        }

        builder
                .setNegativeButton(android.R.string.cancel, /* listener */ null)
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                final MediaSource video =
                                        (videoSpinner != null) ? (MediaSource) videoSpinner.getSelectedItem() : null;
                                final MediaSource audio =
                                        (audioSpinner != null) ? (MediaSource) audioSpinner.getSelectedItem() : null;
                                callback.grant(video, audio);
                            }
                        });

        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(final DialogInterface dialog) {
                        callback.reject();
                    }
                });
        dialog.show();
    }

    public void onMediaPrompt(
            final GeckoSession session,
            final String title,
            final MediaSource[] video,
            final MediaSource[] audio,
            final GeckoSession.PermissionDelegate.MediaCallback callback) {
        onMediaPrompt(session, title, video, audio, null, null, callback);
    }
    private int getViewPadding(final MaterialAlertDialogBuilder builder) {
        final TypedArray attr =
                builder
                        .getContext()
                        .obtainStyledAttributes(new int[] {android.R.attr.listPreferredItemPaddingLeft});
        final int padding = attr.getDimensionPixelSize(0, 1);
        attr.recycle();
        return padding;
    }
    private LinearLayout addStandardLayout(
            final MaterialAlertDialogBuilder builder, final String title, final String msg) {
        final ScrollView scrollView = new ScrollView(builder.getContext());
        final LinearLayout container = new LinearLayout(builder.getContext());
        final int horizontalPadding = getViewPadding(builder);
        final int verticalPadding = (msg == null || msg.isEmpty()) ? horizontalPadding : 0;
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(
                /* left */ horizontalPadding, /* top */ verticalPadding,
                /* right */ horizontalPadding, /* bottom */ verticalPadding);
        scrollView.addView(container);
        builder.setTitle(title).setMessage(msg).setView(scrollView);
        return container;
    }

    private MaterialAlertDialogBuilder createStandardDialog(
            final MaterialAlertDialogBuilder builder,
            final GeckoSession.PromptDelegate.BasePrompt prompt,
            final GeckoResult<GeckoSession.PromptDelegate.PromptResponse> response) {
        builder.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(final DialogInterface dialog) {
                        if (!prompt.isComplete()) {
                            response.complete(prompt.dismiss());
                        }
                    }
                });
        return builder;
    }

}
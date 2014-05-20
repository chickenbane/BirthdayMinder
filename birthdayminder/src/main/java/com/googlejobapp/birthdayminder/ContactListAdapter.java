package com.googlejobapp.birthdayminder;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by joey.tsai on 5/12/2014.
 */
public class ContactListAdapter extends CursorAdapter {
    private static final String TAG = "ContactListAdapter";

    private final LayoutInflater mInflater;
    private final ContentResolver mResolver;
    private final ContactPhotoCache mPhotoCache;

    public ContactListAdapter(Context context) {
        super(context, null, 0);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResolver = context.getContentResolver();
        mPhotoCache = new ContactPhotoCache();
    }

    @Override
    public long getItemId(int position) {
        final BirthdayCursor cursor = (BirthdayCursor) getCursor();
        return cursor.getBirthdayListRow(position).mId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = newView(null, null, parent);
        } else {
            v = convertView;
        }

        final BirthdayCursor cursor = (BirthdayCursor) getCursor();
        final BirthdayListRow row = cursor.getBirthdayListRow(position);
        bindView(v, row);
        return v;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View view = mInflater.inflate(R.layout.contact_row, parent, false);
        ContactRow row = new ContactRow();
        row.qcBadge = (QuickContactBadge) view.findViewById(R.id.quickbadge);
        row.tvName = (TextView) view.findViewById(R.id.textViewName);
        row.tvDays = (TextView) view.findViewById(R.id.textViewDays);
        row.tvDate = (TextView) view.findViewById(R.id.textViewDate);
        row.tvAge = (TextView) view.findViewById(R.id.textViewAge);
        view.setTag(row);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        throw new IllegalStateException("getView doesn't call this anymore.");
    }

    public void bindView(final View view, BirthdayListRow birthdayRow) {
        ContactRow row = (ContactRow) view.getTag();

        final String formattedBirthday = DateUtils.formatDateTime(null,
                birthdayRow.mBirthday.mNextBirthday, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);
        final String daysAway = birthdayRow.mBirthday.mDaysAway + "d";

        String contactAge = birthdayRow.mBirthday.getNextBirthdayAgeFormatted();

        QuickContactBadge badge = row.qcBadge;
        badge.assignContactUri(birthdayRow.mUri);
        String thumbUri = birthdayRow.mThumbUri;
        Bitmap bitmap = null;

        if (mPhotoCache.fetchContactPhoto(thumbUri)) {
            bitmap = mPhotoCache.getContactPhoto(thumbUri);
            if (bitmap == null) {
                ContactPhotoTask task = new ContactPhotoTask(badge, mPhotoCache, mResolver);
                task.execute(thumbUri);
            }
        }

        if (bitmap == null) {
            badge.setImageToDefault();
        } else {
            badge.setImageBitmap(bitmap);
        }

        row.tvName.setText(birthdayRow.mName);
        row.tvDays.setText(daysAway);
        row.tvDate.setText(formattedBirthday);
        row.tvAge.setText(contactAge);
    }

    private static class ContactRow {
        QuickContactBadge qcBadge;
        TextView tvName;
        TextView tvDays;
        TextView tvDate;
        TextView tvAge;
    }

    private static Bitmap loadContactPhotoThumbnail(ContentResolver resolver, String photoData) {
        if (photoData == null) {
            return null;
        }

        InputStream is = null;
        try {
            Uri thumbUri = Uri.parse(photoData);
            is = resolver.openInputStream(thumbUri);
            if (is != null) {
                return BitmapFactory.decodeStream(is);
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found for photoData=" + photoData);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Can't close the input stream", e);
                }
            }
        }
        return null;
    }

    private static class ContactPhotoCache {
        private final LruCache<String, Bitmap> mCache;
        private final Set<String> mHasNoContactPhoto;

        public ContactPhotoCache() {
            // Get max available VM memory, exceeding this amount will throw an
            // OutOfMemory exception. Stored in kilobytes as LruCache takes an
            // int in its constructor.
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/4th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 4;

            mCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };

            mHasNoContactPhoto = Collections.synchronizedSet(new HashSet<String>());
        }

        public Bitmap getContactPhoto(String thumbUri) {
            return mCache.get(thumbUri);
        }

        public boolean fetchContactPhoto(String thumbUri) {
            return thumbUri != null && !mHasNoContactPhoto.contains(thumbUri);
        }

        public void putContactPhoto(String uri, Bitmap bitmap) {
            if (bitmap == null) {
                mHasNoContactPhoto.add(uri);
            } else {
                synchronized (mCache) {
                    if (mCache.get(uri) == null) {
                        mCache.put(uri, bitmap);
                    }
                }
            }
        }
    }

    private static class ContactPhotoTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;
        private final ContactPhotoCache mCache;
        private final ContentResolver mResolver;

        public ContactPhotoTask(ImageView imageView, ContactPhotoCache photoCache, ContentResolver contentResolver) {
            mImageViewReference = new WeakReference<ImageView>(imageView);
            mCache = photoCache;
            mResolver = contentResolver;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String uri = params[0];
            final Bitmap bitmap = loadContactPhotoThumbnail(mResolver, uri);
            mCache.putContactPhoto(uri, bitmap);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mImageViewReference != null && bitmap != null) {
                final ImageView imageView = mImageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}

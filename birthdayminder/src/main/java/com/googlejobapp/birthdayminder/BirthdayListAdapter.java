package com.googlejobapp.birthdayminder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
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
public class BirthdayListAdapter extends CursorAdapter {
    private static final String TAG = "BirthdayListAdapter";

    private static final String[] PROJECTION = {
            ContactsContract.Data._ID, // 0
            ContactsContract.Data.LOOKUP_KEY,  // 1
            ContactsContract.Data.DISPLAY_NAME_PRIMARY, // 2
            ContactsContract.CommonDataKinds.Event.START_DATE, // 3
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI, // 4
    };

    private static final String SELECTION = ContactsContract.Data.MIMETYPE + " = ? AND "
            + ContactsContract.CommonDataKinds.Event.TYPE + " = ?";
    private static final String[] SELECTION_ARGS = {ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            "" + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY};

    private static final int INDEX_CONTACT_ID = 0;
    private static final int INDEX_LOOKUP_KEY = 1;
    private static final int INDEX_CONTACT_NAME = 2;
    private static final int INDEX_BIRTHDATE = 3;
    private static final int INDEX_THUMBNAIL_URI = 4;

    private final LayoutInflater mInflater;
    private final ContentResolver mResolver;
    private final ContactPhotoCache mPhotoCache;

    public BirthdayListAdapter(Context context) {
        super(context, null, 0);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResolver = context.getContentResolver();
        mPhotoCache = new ContactPhotoCache();
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
    public void bindView(final View view, final Context context,
                         final Cursor cursor) {
        ContactRow row = (ContactRow) view.getTag();

        final ContactBirthday birthday = ContactBirthday
                .createContactBirthday(cursor.getString(INDEX_BIRTHDATE));
        final String formattedBirthday = DateUtils.formatDateTime(null,
                birthday.getNextBirthday(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);
        final String daysAway = birthday.getDaysAway() + "d";

        String contactAge = birthday.getNextBirthdayAgeFormatted();

        long id = cursor.getLong(INDEX_CONTACT_ID);
        String lookup = cursor.getString(INDEX_LOOKUP_KEY);
        Uri uri = ContactsContract.Contacts.getLookupUri(id, lookup);

        QuickContactBadge badge = row.qcBadge;
        badge.assignContactUri(uri);
        String thumbUri = cursor.getString(INDEX_THUMBNAIL_URI);
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

        row.tvName.setText(cursor.getString(INDEX_CONTACT_NAME));
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

    protected static CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context, ContactsContract.Data.CONTENT_URI, PROJECTION,
                SELECTION, SELECTION_ARGS, null);
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

package com.googlejobapp.birthdayminder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

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

    private LayoutInflater mInflater;
    private ContentResolver mResolver;

    public BirthdayListAdapter(Context context) {
        super(context, null, 0);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResolver = context.getContentResolver();
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

        String thumbUri = cursor.getString(INDEX_THUMBNAIL_URI);
        row.qcBadge.setImageToDefault();
        if (thumbUri != null) {
            BitmapWorkerTask task = new BitmapWorkerTask(context, row.qcBadge);
            task.execute(thumbUri);
        }

//        Bitmap imageBitmap = null;
//        if (cursor instanceof BirthdayCursor) {
//            imageBitmap = ((BirthdayCursor) cursor).getContactBitmap();
//        } else {
//            Log.e(TAG, "Cursor is not a BirthdayCursor!");
//        }

        row.qcBadge.assignContactUri(uri);
//        if (imageBitmap == null) {
//            row.qcBadge.setImageToDefault();
//        } else {
//            row.qcBadge.setImageBitmap(imageBitmap);
//        }

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

    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final ContentResolver mResolver;

        public BitmapWorkerTask(Context context, ImageView imageView) {
            mResolver = context.getContentResolver();
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            String uri = params[0];
            return loadContactPhotoThumbnail(mResolver, uri);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private static class BirthdayCursor extends CursorWrapper {
        private final Map<String, Bitmap> mBitmaps;
        public BirthdayCursor(Cursor cursor) {
            super(cursor);
            mBitmaps = new HashMap<String, Bitmap>();
        }

        public void loadContactBitmap(ContentResolver resolver) {
            String thumbUri = getString(INDEX_THUMBNAIL_URI);
            final Bitmap imageBitmap = loadContactPhotoThumbnail(resolver, thumbUri);
            mBitmaps.put(thumbUri, imageBitmap);
        }

        public Bitmap getContactBitmap() {
            String thumbUri = getString(INDEX_THUMBNAIL_URI);
            return mBitmaps.get(thumbUri);
        }
    }
}

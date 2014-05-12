package com.googlejobapp.birthdayminder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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

        row.qcBadge.assignContactUri(uri);
        row.qcBadge.setImageBitmap(loadContactPhotoThumbnail(thumbUri));

        row.tvName.setText(cursor.getString(INDEX_CONTACT_NAME));
        row.tvDays.setText(daysAway);
        row.tvDate.setText(formattedBirthday);
        row.tvAge.setText(contactAge);
    }

    /**
     * Load a contact photo thumbnail and return it as a Bitmap,
     * resizing the image to the provided image dimensions as needed.
     *
     * @param photoData photo ID Prior to Honeycomb, the contact's _ID value.
     *                  For Honeycomb and later, the value of PHOTO_THUMBNAIL_URI.
     * @return A thumbnail Bitmap, sized to the provided width and height.
     * Returns null if the thumbnail is not found.
     */
    private Bitmap loadContactPhotoThumbnail(String photoData) {
        if (photoData == null) {
            Log.d(TAG, "photoData is null");
            return null;
        }
        Log.d(TAG, "photoData is cool");
        // Creates an asset file descriptor for the thumbnail file.
        AssetFileDescriptor afd = null;
        InputStream is = null;
        // try-catch block for file not found
        try {
            // Creates a holder for the URI.
            Uri thumbUri = Uri.parse(photoData);


        /*
         * Retrieves an AssetFileDescriptor object for the thumbnail
         * URI
         * using ContentResolver.openAssetFileDescriptor
         */
            afd = mResolver.openAssetFileDescriptor(thumbUri, "r");
            is = mResolver.openInputStream(thumbUri);
        /*
         * Gets a file descriptor from the asset file descriptor.
         * This object can be used across processes.
         */
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            // Decode the photo file and return the result as a Bitmap
            // If the file descriptor is valid
            if (fileDescriptor == null) {
                Log.d(TAG, "Bleh, fileDescriptor is null");
            }

            if (is != null) {
                return BitmapFactory.decodeStream(is);
            }

            if (fileDescriptor != null) {
                Log.d(TAG, "Decoding bitmap!");
                return BitmapFactory.decodeFileDescriptor(
                        fileDescriptor, null, null);
            }
            // If the file isn't found
        } catch (FileNotFoundException e) {
            /*
             * Handle file not found errors
             */

        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        Log.d(TAG, "I hate this function");
        return null;
    }

    private static class ContactRow {
        QuickContactBadge qcBadge;
        TextView tvName;
        TextView tvDays;
        TextView tvDate;
        TextView tvAge;
    }

    protected static CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context, ContactsContract.Data.CONTENT_URI, PROJECTION,
                SELECTION, SELECTION_ARGS, null);
    }
}

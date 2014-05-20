package com.googlejobapp.birthdayminder;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * Created by joey.tsai on 5/19/2014.
 */
public class BirthdayDao {

    private static final String[] PROJECTION = {
            ContactsContract.Data._ID, // 0
            ContactsContract.Data.LOOKUP_KEY,  // 1
            ContactsContract.Data.DISPLAY_NAME_PRIMARY, // 2
            ContactsContract.CommonDataKinds.Event.START_DATE, // 3
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI, // 4
    };

    private static final int INDEX_CONTACT_ID = 0;
    private static final int INDEX_LOOKUP_KEY = 1;
    private static final int INDEX_CONTACT_NAME = 2;
    private static final int INDEX_BIRTHDATE = 3;
    private static final int INDEX_THUMBNAIL_URI = 4;

    private static final String SELECTION = ContactsContract.Data.MIMETYPE + " = ? AND "
            + ContactsContract.CommonDataKinds.Event.TYPE + " = ?";
    private static final String[] SELECTION_ARGS = {ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            "" + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY};

    static public BirthdayContact createBirthdayListRow(Cursor cursor) {
        final ContactBirthday birthday = ContactBirthday
                .createContactBirthday(cursor.getString(INDEX_BIRTHDATE));
        long id = cursor.getLong(INDEX_CONTACT_ID);
        String lookup = cursor.getString(INDEX_LOOKUP_KEY);
        String thumbUri = cursor.getString(INDEX_THUMBNAIL_URI);
        String name = cursor.getString(INDEX_CONTACT_NAME);
        Uri uri = ContactsContract.Contacts.getLookupUri(id, lookup);

        return new BirthdayContact(id, uri, thumbUri, birthday, name);
    }

    static protected BirthdayCursorLoader createCursorLoader(Context context) {
        return new BirthdayCursorLoader(context);
    }

    static private class BirthdayCursorLoader extends CursorLoader {

        public BirthdayCursorLoader(Context context) {
            super(context, ContactsContract.Data.CONTENT_URI, PROJECTION,
                    SELECTION, SELECTION_ARGS, null);
        }

        @Override
        public Cursor loadInBackground() {
            final Cursor cursor = super.loadInBackground();
            BirthdayCursor birthdayCursor = new BirthdayCursor(cursor);
            birthdayCursor.fillCache();
            return birthdayCursor;
        }
    }
}

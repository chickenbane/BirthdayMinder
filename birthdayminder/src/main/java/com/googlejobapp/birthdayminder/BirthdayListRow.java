package com.googlejobapp.birthdayminder;

import android.net.Uri;

/**
 * Created by joey.tsai on 5/19/2014.
 */
public class BirthdayListRow {
    private final long mId;
    private final Uri mUri;
    private final String mThumbUri;
    private final ContactBirthday mBirthday;
    private final String mName;

    public BirthdayListRow(long id, Uri uri, String thumbUri, ContactBirthday birthday, String name) {
        mId = id;
        mUri = uri;
        mThumbUri = thumbUri;
        mBirthday = birthday;
        mName = name;
    }

    public long getId() {
        return mId;
    }

    public Uri getUri() {
        return mUri;
    }

    public String getThumbUri() {
        return mThumbUri;
    }

    public ContactBirthday getBirthday() {
        return mBirthday;
    }

    public String getName() {
        return mName;
    }
}

package com.googlejobapp.birthdayminder;

import android.net.Uri;

/**
 * Created by joey.tsai on 5/19/2014.
 */
public class BirthdayContact implements Comparable<BirthdayContact> {
    protected final long mId;
    protected final Uri mUri;
    protected final String mThumbUri;
    protected final ContactBirthday mBirthday;
    protected final String mName;

    public BirthdayContact(long id, Uri uri, String thumbUri, ContactBirthday birthday, String name) {
        mId = id;
        mUri = uri;
        mThumbUri = thumbUri;
        mBirthday = birthday;
        mName = name;
    }

    @Override
    public int compareTo(BirthdayContact another) {
        int me = mBirthday.mDaysAway;
        int her = another.mBirthday.mDaysAway;
        if (me < her) {
            return -1;
        } else if (me > her) {
            return 1;
        }
        return 0;
    }
}

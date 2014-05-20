package com.googlejobapp.birthdayminder;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by joey.tsai on 5/16/2014.
 */
public class BirthdayCursor extends CursorWrapper {
    private List<BirthdayContact> mRows;
    public BirthdayCursor(Cursor cursor) {
        super(cursor);
    }

    public void fillCache() {
        mRows = new ArrayList<BirthdayContact>();
        final Cursor cursor = getWrappedCursor();
        while (cursor.moveToNext()) {
            mRows.add(BirthdayDao.createBirthdayListRow(cursor));
        }

        Collections.sort(mRows);
    }

    public BirthdayContact getBirthdayContact(int position) {
        return mRows.get(position);
    }

    @Override
    public boolean moveToPosition(int position) {
        throw new IllegalStateException("CursorAdapter.getView doesn't call this anymore.");
    }
}

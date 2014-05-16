package com.googlejobapp.birthdayminder;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;

public class ContactListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

	private static final String TAG = "ContactListFragment";


	private BirthdayListAdapter mAdapter;

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = new BirthdayListAdapter(getActivity());
		setListAdapter(mAdapter);

		getListView().setOnItemClickListener(this);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		return BirthdayListAdapter.createCursorLoader(getActivity());
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mAdapter.swapCursor(null);

	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {

		final Cursor cursor = ((CursorAdapter) parent.getAdapter()).getCursor();
		cursor.moveToPosition(position);
//		final long contactId = cursor.getLong(INDEX_CONTACT_ID);
//		final String lookupKey = cursor.getString(INDEX_LOOKUP_KEY);
//		final Uri contactUri = Contacts.getLookupUri(contactId, lookupKey);
//
//		Log.v(TAG, "Contact URI: " + contactUri);
		Log.v(TAG, "num=" + cursor.getCount());
	}

}

package com.googlejobapp.birthdayminder;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ContactListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

	private static final String TAG = "ContactListFragment";
	private static final String[] PROJECTION = { Data._ID, Data.LOOKUP_KEY,
			Data.DISPLAY_NAME_PRIMARY, Event.START_DATE };

	private static final String SELECTION = Data.MIMETYPE + " = ? AND "
			+ Event.TYPE + " = ?";
	private static final String[] SELECTION_ARGS = { Event.CONTENT_ITEM_TYPE,
			"" + Event.TYPE_BIRTHDAY };

	private static final int INDEX_CONTACT_ID = 0;
	private static final int INDEX_LOOKUP_KEY = 1;
	private static final int INDEX_CONTACT_NAME = 2;
	private static final int INDEX_BIRTHDATE = 3;

	private SimpleCursorAdapter mAdapter;

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = new ContactListCursorAdapter(getActivity());
		setListAdapter(mAdapter);

		getListView().setOnItemClickListener(this);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		return new CursorLoader(getActivity(), Data.CONTENT_URI, PROJECTION,
				SELECTION, SELECTION_ARGS, null);
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
		final long contactId = cursor.getLong(INDEX_CONTACT_ID);
		final String lookupKey = cursor.getString(INDEX_LOOKUP_KEY);
		final Uri contactUri = Contacts.getLookupUri(contactId, lookupKey);

		Log.v(TAG, "Contact URI: " + contactUri);
		Log.v(TAG, "num=" + cursor.getCount());
	}

	private static class ContactListCursorAdapter extends SimpleCursorAdapter {
		public ContactListCursorAdapter(final Context context) {
			super(context, R.layout.contact_row, null, new String[] {
					Data.DISPLAY_NAME_PRIMARY, Event.START_DATE }, new int[] {
					R.id.textViewName, R.id.textViewDays }, 0);
		}

		@Override
		public void bindView(final View view, final Context context,
				final Cursor cursor) {
			ContactRow row = (ContactRow) view.getTag();
			if (row == null) {
				row = new ContactRow();
				row.tvName = (TextView) view.findViewById(R.id.textViewName);
				row.tvDays = (TextView) view.findViewById(R.id.textViewDays);
				row.tvDate = (TextView) view.findViewById(R.id.textViewDate);
				row.tvAge = (TextView) view.findViewById(R.id.textViewAge);
				view.setTag(row);
			}

			final ContactBirthday birthday = ContactBirthday
					.createContactBirthday(cursor.getString(INDEX_BIRTHDATE));
			final String formattedBirthday = DateUtils.formatDateTime(null,
					birthday.getNextBirthday(), DateUtils.FORMAT_SHOW_DATE);
			final String daysAway = birthday.getDaysAway() + " days";

			final Integer age = birthday.getNextBirthdayAge();
			String contactAge;
			if (age == null) {
				contactAge = "-";
			} else {
				contactAge = age.toString();
			}

			row.tvName.setText(cursor.getString(INDEX_CONTACT_NAME));
			row.tvDays.setText(daysAway);
			row.tvDate.setText(formattedBirthday);
			row.tvAge.setText(contactAge);
		}
	}

	private static class ContactRow {
		TextView tvName;
		TextView tvDays;
		TextView tvDate;
		TextView tvAge;
	}

}

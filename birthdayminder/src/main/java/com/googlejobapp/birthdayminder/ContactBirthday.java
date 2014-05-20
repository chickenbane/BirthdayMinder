package com.googlejobapp.birthdayminder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.text.format.Time;
import android.util.Log;

/**
 * It appears there are two date formats for the birthday in my Google contacts.
 * It also appears to be unrelated to the date format set on my phone. If a year
 * is supplied, it's: yyyy-MM-dd if no year is supplied, it's --MM-dd
 * 
 * TODO the no year format with a 02-29 leap-year birthdate is a bug
 */
public class ContactBirthday {
	private static final String TAG = "ContactBirthday";

	private static final SimpleDateFormat BIRTHDAY_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd", Locale.US);
	private static final SimpleDateFormat BIRTHDAY_NO_YEAR_FORMAT = new SimpleDateFormat(
			"--MM-dd", Locale.US);

	private final Date mBirthDate;

    protected final long mNextBirthday;
    protected final int mDaysAway;
    protected final String mNextAge;

	public static ContactBirthday createContactBirthday(final String contactDate) {
		if (contactDate == null) {
			return null;
		}

		final Date birthDate;
		final boolean hasYear = !contactDate.startsWith("--");
		try {
			if (hasYear) {
				birthDate = BIRTHDAY_FORMAT.parse(contactDate);
			} else {
				birthDate = BIRTHDAY_NO_YEAR_FORMAT.parse(contactDate);
			}
		} catch (final ParseException e) {
			Log.e(TAG, "Can't parse date string=" + contactDate, e);
			return null;
		}

		return new ContactBirthday(birthDate, hasYear);
	}

	public ContactBirthday(final Date birthDate, boolean hasYear) {
        mBirthDate = birthDate;
        mNextBirthday = calcNextBirthday(birthDate);
        mDaysAway = calcDaysAway(mNextBirthday);
        mNextAge = calcNextAge(hasYear, birthDate);
    }

    static private long calcNextBirthday(Date birthDate) {
        final Calendar now = Calendar.getInstance();
        final int thisYear = now.get(Calendar.YEAR);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(birthDate);
        calendar.set(Calendar.YEAR, thisYear);

        if (now.after(calendar)) {
            calendar.set(Calendar.YEAR, thisYear + 1);
        }
        return calendar.getTimeInMillis();
    }

    static private int calcDaysAway(long nextBirthday) {
        final long now = System.currentTimeMillis();
        final Time nowTime = new Time();
        nowTime.set(now);
        final int today = Time.getJulianDay(now, nowTime.gmtoff);

        final Time birthdayTime = new Time();
        birthdayTime.set(nextBirthday);
        final int future = Time.getJulianDay(nextBirthday,
                birthdayTime.gmtoff);

        return future - today;
    }

    static private String calcNextAge(boolean hasYear, Date birthdate) {
        if (!hasYear) {
            return "-";
        }

        final Calendar now = Calendar.getInstance();
        final int thisYear = now.get(Calendar.YEAR);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(birthdate);
        final int birthYear = calendar.get(Calendar.YEAR);
        final int years = thisYear - birthYear;
        calendar.set(Calendar.YEAR, thisYear);
        int age;
        if (now.after(calendar)) {
            age = years + 1;
        } else {
            age = years;
        }
        return String.valueOf(age);
    }
}

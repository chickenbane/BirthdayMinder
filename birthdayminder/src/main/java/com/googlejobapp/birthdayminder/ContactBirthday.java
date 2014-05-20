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

	private final boolean mHasYear;
	private final Date mBirthDate;

    private final long mNextBirthday;
    private final int mDaysAway;

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

		return new ContactBirthday(hasYear, birthDate);
	}

	public ContactBirthday(final boolean hasYear, final Date birthDate) {
        mHasYear = hasYear;
        mBirthDate = birthDate;

        mNextBirthday = calcNextBirthday(birthDate);
        mDaysAway = calcDaysAway(mNextBirthday);
    }

    public long getNextBirthday() {
        return mNextBirthday;
    }

    public int getDaysAway() {
        return mDaysAway;
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

	public Integer getNextBirthdayAge() {
		if (!mHasYear) {
			return null;
		}

		final Calendar now = Calendar.getInstance();
		final int thisYear = now.get(Calendar.YEAR);
		final Calendar birthdate = Calendar.getInstance();
		birthdate.setTime(mBirthDate);
		final int birthYear = birthdate.get(Calendar.YEAR);
		final int years = thisYear - birthYear;
		birthdate.set(Calendar.YEAR, thisYear);

		if (now.after(birthdate)) {
			return years + 1;
		}
		return years;
	}

    public String getNextBirthdayAgeFormatted() {
        final Integer age = getNextBirthdayAge();
        String contactAge;
        if (age == null) {
            contactAge = "-";
        } else {
            contactAge = age.toString();
        }
        return contactAge;
    }
}

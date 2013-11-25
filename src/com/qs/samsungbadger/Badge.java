package com.qs.samsungbadger;

import java.io.ByteArrayOutputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

interface BadgeColumns {
    public static final String ID = "_id";
    public static final String PACKAGE = "package";
    public static final String CLASS = "class";
    public static final String BADGE_COUNT = "badgecount";
    public static final String ICON = "icon";
}

public final class Badge implements BadgeColumns, Parcelable {
    private static final Uri CONTENT_URI = Uri.parse("content://com.sec.badge/apps");

    private static final int CONTENT_ID_COLUMN = 0;
    private static final int CONTENT_PACKAGE_COLUMN = 1;
    private static final int CONTENT_CLASS_COLUMN = 2;
    private static final int CONTENT_BADGE_COUNT_COLUMN = 3;
    private static final int CONTENT_ICON_COLUMN = 4;

    private static final String[] CONTENT_PROJECTION = {
        ID, PACKAGE, CLASS, BADGE_COUNT, ICON
    };

    private static final String BADGE_SELECTION = BadgeColumns.PACKAGE + "=?";

    public Uri mBaseUri;
    public long mId;
    public String mPackage;
    public String mClass;
    public int mBadgeCount;
    public byte[] mIcon;

    public Badge() {
        mBaseUri = CONTENT_URI;
    }

    private boolean isSaved() {
        return mId <= 0;
    }

    private void restore(Cursor c) {
        mId = c.getLong(CONTENT_ID_COLUMN);
        mPackage = c.getString(CONTENT_PACKAGE_COLUMN);
        mClass = c.getString(CONTENT_CLASS_COLUMN);
        mBadgeCount = c.getInt(CONTENT_BADGE_COUNT_COLUMN);
        mIcon = c.getBlob(CONTENT_ICON_COLUMN);
    }

    private ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(PACKAGE, mPackage);
        cv.put(CLASS, mClass);
        cv.put(BADGE_COUNT, mBadgeCount);
        cv.put(ICON, mIcon);
        return cv;
    }

    /**
     * Returns true if badging is supported on this device.<br>
     * 
     * <p>If this is the first time checking a blocking query will be made
     * to the BadgeProvider. All subsequent checks will be read from a shared preferences
     * key.</p>
     * 
     * @param context
     * @return true if badging is supported, false otherwise
     */
    public static boolean isBadgingSupported(Context context) {
	    Preferences prefs = Preferences.getPreferences(context);
	    final int isSupported = prefs.getIsBadgingSupported();

	    // This indicates we haven't checked before so check now
	    if (isSupported == -1) {
	        Cursor c = context.getContentResolver().query(CONTENT_URI,
	    		    null, null, null, null);

	        if (c == null) {
	            prefs.setIsBadgingSupported(false);
	            return false;
	        } else {
	            c.close();
	            return true;
	        }
	    } else {
	        return isSupported == 1;
	    }
	}

    /**
     * Returns the badge associated with our application.<br>
     * 
     * <p>Null is returned if we do not have a badge registered
     * with the BadgeProvider (ie: first time run) or if badging is not supported on
     * this device.<br><br>
     * 
     * <b>THIS OPERATION IS BLOCKING. DO NOT RUN ON UI THREAD.</b></p>
     * 
     * @param context
     * @return the badge object or null
     */
    public static Badge getBadge(Context context) {
        if (!isBadgingSupported(context)) return null;

        Cursor c = context.getContentResolver().query(CONTENT_URI,
            CONTENT_PROJECTION, BADGE_SELECTION,
            new String[] {context.getPackageName()}, null);

        try {
            // No badge registered for our app yet
            if (!c.moveToFirst()) {
                return null;
            }

            Badge b = new Badge();
            b.restore(c);
            return b;
        } finally {
            c.close();
        }
    }

    /**
     * Converts the icon byte array into a Bitmap.
     * 
     * @return the icon as a bitmap or null
     */
    public Bitmap getIcon() {
        // Nothing to do
        if (mIcon == null || mIcon.length == 0) return null;

        return BitmapFactory.decodeByteArray(mIcon, 0, mIcon.length);
    }

    /**
     * Sets the icon from a Bitmap.
     * 
     * @param icon the icon to use
     */
    public void setIcon(Bitmap icon) {
        if (icon == null) return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        mIcon = stream.toByteArray();
    }

    /**
     * Sets the icon from a Drawable.
     * 
     * @param icon the icon to use
     */
    public void setIcon(Drawable icon) {
        if (icon == null) return;

    	BitmapDrawable bitDw = ((BitmapDrawable) icon);
    	setIcon(bitDw.getBitmap());
    }

    /**
     * Creates a badge record in the Badge provider.
     * 
     * <p>Throws {@link UnsupportedOperationException} if this Badge instance
     * has already been saved OR if {@link #isBadgingSupported(Context)} returns false.
     * The field {@link Badge#mId} will be set with the new id if successful.<br><br>
     * 
     * <b>THIS OPERATION IS BLOCKING. DO NOT RUN ON UI THREAD.</b></p>
     * 
     * @param context
     * @return the url of the newly created row
     */
    public Uri save(Context context) {
        if (isSaved() || !isBadgingSupported(context)) {
            throw new UnsupportedOperationException();
        }

        Uri res = context.getContentResolver().insert(mBaseUri, toContentValues());
        mId = Long.parseLong(res.getPathSegments().get(1));
        return res;
    }

    /**
     * Updates the record of this Badge instance with the BadgeProvider.
     * 
     * <p>Throws {@link UnsupportedOperationException} if this Badge instance has
     * not been saved yet (You can't update if you haven't saved) OR if
     * {@link #isBadgingSupported(Context)} returns false.<br><br>
     * 
     * <b>THIS OPERATION IS BLOCKING. DO NOT RUN ON UI THREAD.</b></p>
     * 
     * @param context
     * @return true if update was successful, false otherwise.
     */
    public boolean update(Context context) {
        if (!isSaved() || !isBadgingSupported(context)) {
            throw new UnsupportedOperationException();
        }

        final int rows = context.getContentResolver().update(mBaseUri,
            toContentValues(), null, null);
        return rows > 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<Badge> CREATOR
            = new Parcelable.Creator<Badge>() {
        @Override
        public Badge createFromParcel(Parcel in) {
            return new Badge(in);
        }

        @Override
        public Badge[] newArray(int size) {
            return new Badge[size];
        }
    };

    /**
     * Supports Parcelable
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mPackage);
        dest.writeString(mClass);
        dest.writeInt(mBadgeCount);
        dest.writeByteArray(mIcon);
    }

    /**
     * Supports Parcelable
     */
    public Badge(Parcel in) {
        mBaseUri = Badge.CONTENT_URI;
        mId = in.readLong();
        mPackage = in.readString();
        mClass = in.readString();
        mBadgeCount = in.readInt();
        mIcon = in.createByteArray();
    }
}

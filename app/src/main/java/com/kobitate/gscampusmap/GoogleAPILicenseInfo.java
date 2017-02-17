package com.kobitate.gscampusmap;

import android.content.Context;

import com.google.android.gms.common.GoogleApiAvailability;

import de.psdev.licensesdialog.licenses.License;

/**
 * Created by kobi on 2/17/17.
 */

public class GoogleAPILicenseInfo extends License {
	@Override
	public String getName() {
		return "Google Maps Android API Notices";
	}

	@Override
	public String readSummaryTextFromResources(Context context) {
		return readFullTextFromResources(context);
	}

	@Override
	public String readFullTextFromResources(Context context) {
		return GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(context);
	}

	@Override
	public String getVersion() {
		return "January 27, 2017";
	}

	@Override
	public String getUrl() {
		return "https://developers.google.com/maps/terms";
	}
}

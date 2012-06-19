package com.pixplicity.example.facebookthief;

import android.content.Intent;
import android.net.Uri;

public class TrapperResult {

	public final String line;
	public final String interest;

	public TrapperResult(String line, String token) {
		this.line = line;
		this.interest = token;
	}

	public Intent getIntent() {
		if (interest == null) {
			return null;
		}
		return new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://graph.facebook.com/me?access_token="
						+ interest));
	}

}

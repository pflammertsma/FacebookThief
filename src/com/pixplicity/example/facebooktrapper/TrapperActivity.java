package com.pixplicity.example.facebooktrapper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;

public class TrapperActivity extends Activity {

	protected static final int MAX_ITEMS = 200;

	protected static final boolean USE_SERVICE = true;

	protected static final String TAG = "FacebookTrapper";

	private static Thread mThread;

	private ToggleButton mBtn;
	private ListView mList;

	private ArrayAdapter<TrapperResult> mListAdapter;

	protected TrapperService mService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mBtn = (ToggleButton) findViewById(R.id.toggleButton1);
		mBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (USE_SERVICE) {
					Intent serviceIntent = new Intent(TrapperActivity.this,
							TrapperService.class);
					if (mBtn.isChecked()) {
						final ProgressDialog pd = new ProgressDialog(
								TrapperActivity.this);
						pd.setMessage("Starting service...");
						pd.show();
						pd.setCancelable(false);
						new Thread() {
							@Override
							public void run() {
								do {
									mService = TrapperService
											.getInstance(TrapperActivity.this);
									if (mService != null) {
										break;
									}
									try {
										Thread.sleep(500);
									} catch (InterruptedException e) {
									}
								} while (true);
								TrapperActivity.this
										.runOnUiThread(new Runnable() {
											@Override
											public void run() {
												pd.dismiss();
											}
										});
							}
						}.start();
						startService(serviceIntent);
					} else {
						if (mService != null) {
							mService.stop();
						}
						stopService(serviceIntent);
					}
				} else {
					if (mBtn.isChecked()) {
						if (mThread != null) {
							mThread.interrupt();
						}
						mThread = new TrapperThread(TrapperActivity.this) {
							@Override
							public void run() {
								showNotification(
										0, "Facebook Trapper running",
										null);
								doInBackground(mList, mListAdapter);
								showNotification(
										-1, "Facebook Trapper stopped",
										null);
								mBtn.post(new Runnable() {
									@Override
									public void run() {
										mBtn.setChecked(false);
									}
								});
							};
						};
						mThread.start();
					} else if (mThread != null) {
						mThread.interrupt();
					}
				}
			}
		});
		mListAdapter = new TrapperListAdapter(this);
		mList = (ListView) findViewById(R.id.listView1);
		mList.setAdapter(mListAdapter);
		mList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				TrapperResult result = mListAdapter.getItem(position);
				Intent intent = result.getIntent();
				if (intent != null) {
					startActivity(intent);
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (USE_SERVICE) {
			mService = TrapperService
					.getInstance(TrapperActivity.this);
			if (mService != null && mService.isAlive()) {
				mBtn.setChecked(true);
			} else {
				mBtn.setChecked(false);
			}
		} else {
			if (mThread != null && mThread.isAlive()) {
				mBtn.setChecked(true);
			} else {
				mBtn.setChecked(false);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (USE_SERVICE && mService != null) {
			mService.setActivity(null);
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (mThread != null && mThread.isAlive()) {
			mBtn.setChecked(true);
		}
	}

	protected ListView getList() {
		return mList;
	}

}
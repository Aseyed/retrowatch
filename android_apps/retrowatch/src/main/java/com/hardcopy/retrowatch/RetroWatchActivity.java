/*
 * Copyright (C) 2014 The Retro Watch - Open source smart watch project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hardcopy.retrowatch;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.hardcopy.retrowatch.contents.objects.CPObject;
import com.hardcopy.retrowatch.contents.objects.ContentObject;
import com.hardcopy.retrowatch.contents.objects.FilterObject;
import com.hardcopy.retrowatch.service.RetroWatchService;
import com.hardcopy.retrowatch.utils.Constants;
import com.hardcopy.retrowatch.utils.Logs;
import com.hardcopy.retrowatch.utils.RecycleUtils;
import com.hardcopy.retrowatch.utils.Utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.material.tabs.TabLayout;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class RetroWatchActivity extends AppCompatActivity implements IFragmentListener {

    // Debugging
    private static final String TAG = "RetroWatchActivity";
    
    private static final long REMOTE_REFRESH_DELAY = 5*1000;
    private static final long CONTENTS_REFRESH_TIME = 60*1000;
	
	// Context, System
	private Context mContext;
	private RetroWatchService mService;
	private Utils mUtils;
	private ActivityHandler mActivityHandler;
	private boolean mIsServiceBound = false;
	
	// Global
	private boolean mStopService = false;
	
	// UI stuff
	private FragmentManager mFragmentManager;
	private RetroWatchFragmentAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;
	private TabLayout mTabLayout;
	
	private ImageView mImageBT = null;
	private TextView mTextStatus = null;

	// Refresh timer
	private Timer mRefreshTimer = null;
	
	
	/*****************************************************
	 * 
	 *	 Overrided methods
	 *
	 ******************************************************/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Setup global exception handler to catch startup crashes
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
				Log.e(TAG, "CRASH: " + paramThrowable.getMessage(), paramThrowable);
				// Try to show a toast before dying (might not work if main thread is dead)
				// System.exit(2); // Let the system handle it
			}
		});

		try {
			Log.d(TAG, "onCreate: Starting...");
			
			//----- System, Context
			mContext = this;//.getApplicationContext();
			mActivityHandler = new ActivityHandler();
			
			// IMPORTANT: Request permissions FIRST before initializing full UI
			Log.d(TAG, "onCreate: Checking permissions...");
			if (!checkPermissions()) {
				Log.d(TAG, "onCreate: Permissions needed");
				try {
					// Show simple permission request screen
					Log.d(TAG, "onCreate: Setting content view to activity_permission_request");
					setContentView(R.layout.activity_permission_request);
					
					Log.d(TAG, "onCreate: Finding status_text");
					mTextStatus = (TextView) findViewById(R.id.status_text);
					if (mTextStatus == null) {
						Log.e(TAG, "mTextStatus is null in activity_permission_request");
					}
					
					Log.d(TAG, "onCreate: Requesting app permissions");
					requestAppPermissions();
				} catch (Exception e) {
					Log.e(TAG, "Error in permission request setup: " + e.getMessage(), e);
					Toast.makeText(this, "Setup Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			} else {
				// Permissions already granted, initialize normally
				Log.d(TAG, "onCreate: Permissions granted, initializing UI");
				initializeUI();
			}
			Log.d(TAG, "onCreate: Completed");
		} catch (Exception e) {
			Log.e(TAG, "CRASH in onCreate: " + e.getMessage(), e);
			Toast.makeText(this, "Crash in onCreate: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void initializeUI() {
		try {
			Log.d(TAG, "Step 1: Setting content view");
			setContentView(R.layout.activity_retro_watch);

			Log.d(TAG, "Step 2: Loading utilities");
			// Load static utilities
			mUtils = new Utils(mContext);
			
			Log.d(TAG, "Step 3: Setting up action bar");
			// Set up the action bar
			final ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setDisplayShowTitleEnabled(true);
			}

			Log.d(TAG, "Step 4: Creating fragment adapter");
			// Create the adapter that will return a fragment for each of the primary sections of the app.
			mFragmentManager = getSupportFragmentManager();
			mSectionsPagerAdapter = new RetroWatchFragmentAdapter(mFragmentManager, mContext, this);

			Log.d(TAG, "Step 5: Setting up ViewPager");
			// Set up the ViewPager with the sections adapter.
			mViewPager = (ViewPager) findViewById(R.id.pager);
			mViewPager.setAdapter(mSectionsPagerAdapter);

			Log.d(TAG, "Step 6: Setting up TabLayout");
			// Set up the Material TabLayout
			mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
			
			Log.d(TAG, "Step 7: Adding tabs");
			// Add tabs
			if (mSectionsPagerAdapter != null) {
				for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
					mTabLayout.addTab(mTabLayout.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)));
				}
			}
			
			Log.d(TAG, "Step 8: Connecting TabLayout with ViewPager");
			// Connect TabLayout with ViewPager
			mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
				@Override
				public void onTabSelected(TabLayout.Tab tab) {
					if (mViewPager != null) {
						mViewPager.setCurrentItem(tab.getPosition());
					}
				}
				
				@Override
				public void onTabUnselected(TabLayout.Tab tab) {
				}
				
				@Override
				public void onTabReselected(TabLayout.Tab tab) {
				}
			});
			
			// Update TabLayout when ViewPager is swiped
			if (mViewPager != null && mTabLayout != null) {
				mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
			}

			Log.d(TAG, "Step 9: Setting up status views");
			// Setup views
			mImageBT = (ImageView) findViewById(R.id.status_title);
			if (mImageBT != null) {
				mImageBT.setImageDrawable(ContextCompat.getDrawable(mContext, android.R.drawable.presence_invisible));
			}
			mTextStatus = (TextView) findViewById(R.id.status_text);
			if (mTextStatus != null) {
				mTextStatus.setText(getResources().getString(R.string.bt_state_init));
			}
			
			Log.d(TAG, "initializeUI completed successfully");
		} catch (Exception e) {
			Log.e(TAG, "ERROR in initializeUI: " + e.getMessage(), e);
			throw e; // Re-throw to be caught by caller
		}
	}
	
	private static final int REQUEST_CODE_PERMISSIONS = 101;

	private boolean checkPermissions() {
		if (Build.VERSION.SDK_INT >= 31) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		
		if (Build.VERSION.SDK_INT >= 33) {
			 if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				 return false;
			 }
		}

		if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 30) {
			 if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				 return false;
			 }
		}
		
		return true;
	}

	private void requestAppPermissions() {
		ArrayList<String> permissions = new ArrayList<>();
		if (Build.VERSION.SDK_INT >= 31) {
			permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
			permissions.add(Manifest.permission.BLUETOOTH_SCAN);
		}
		if (Build.VERSION.SDK_INT >= 33) {
			permissions.add(Manifest.permission.POST_NOTIFICATIONS);
		}
		if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 30) {
			 permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
			 permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
		}
		
		if (!permissions.isEmpty()) {
			ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
		} else {
			doStartService();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_CODE_PERMISSIONS) {
			// Check if Bluetooth permissions were granted
			boolean bluetoothGranted = true;
			if (Build.VERSION.SDK_INT >= 31) {
				bluetoothGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
			}
			
			if (bluetoothGranted) {
				// Permissions granted, initialize UI and start service
				try {
					Log.d(TAG, "Initializing UI after permissions granted");
					initializeUI();
					Log.d(TAG, "UI initialized successfully, starting service");
					doStartService();
					Log.d(TAG, "Service start requested");
				} catch (Exception e) {
					Log.e(TAG, "ERROR during initialization: " + e.getMessage(), e);
					Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			} else {
				// Permissions denied, show explanation
				Toast.makeText(this, "Bluetooth permissions are required. Please grant permissions in Settings.", Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public synchronized void onStart() {
		super.onStart();
		
		if(mRefreshTimer != null) {
			mRefreshTimer.cancel();
		}
	}
	
	@Override
	public synchronized void onResume() {
		super.onResume();
		
		// Start service when activity resumes and permissions are granted
		// Only if UI is already initialized (mService might be null on first run)
		if (mViewPager != null && mService == null && checkPermissions()) {
			doStartService();
		}
	}
	
	@Override
	public synchronized void onPause() {
		super.onPause();
	}
	
	@Override
	public void onStop() {
		// Stop the timer
		if(mRefreshTimer != null) {
			mRefreshTimer.cancel();
			mRefreshTimer = null;
		}
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopContentUpdate();
		finalizeActivity();
	}
	
	@Override
	public void onLowMemory (){
		super.onLowMemory();
		// onDestroy is not always called when applications are finished by Android system.
		finalizeActivity();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.retro_watch, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_scan) {
			// Launch the DeviceListActivity to see devices and do scan
			doScan();
			return true;
		} else if (id == R.id.action_noti_settings) {
			// Launch notification settings screen
			setNotificationAccess();
			return true;
		} else if (id == R.id.action_refresh) {
			// Refresh every contents
			refreshContentObjects();
			return true;
		} else if (id == R.id.action_send_all) {
			// Send all available contents to watch
			mService.reserveRemoteUpdate(100);
			return true;
		}
		/* Disabled:
		if (id == R.id.action_discoverable) {
			// Disabled: Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		if (id == R.id.action_settings) {
			// Disabled: 
		}
		*/
		return false;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();		// TODO: Disable this line to run below code
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		// This prevents reload after configuration changes
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void OnFragmentCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
		switch(msgType) {
		case IFragmentListener.CALLBACK_REQUEST_FILTERS:
			getFiltersAll();
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_ADD_FILTER:
			int id = Constants.RESPONSE_ADD_FILTER_FAILED;
			FilterObject filterObj = null;
			if(mService != null && arg4 != null) {
				filterObj = (FilterObject) arg4;
				id = mService.addFilter(filterObj);
			} else {
				break;
			}
			
			if(id > Constants.RESPONSE_ADD_FILTER_FAILED) {
				FiltersFragment frg = (FiltersFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_FILTERS);
				frg.addFilter(filterObj);
				
				ArrayList<ContentObject> contents = mService.refreshContentObjectList();
				MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				frg2.deleteMessageAll();
				frg2.addMessageAll(contents);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_EDIT_FILTER:
			int id2 = Constants.RESPONSE_EDIT_FILTER_FAILED;
			FilterObject filterObject = null;
			if(mService != null && arg4 != null) {
				filterObject = (FilterObject) arg4;
				id2 = mService.editFilter(filterObject);
			} else {
				break;
			}
			
			if(id2 > Constants.RESPONSE_EDIT_FILTER_FAILED) {
				FiltersFragment frg = (FiltersFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_FILTERS);
				frg.editFilter(filterObject);
				
				ArrayList<ContentObject> contents = mService.refreshContentObjectList();
				MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				frg2.deleteMessageAll();
				frg2.addMessageAll(contents);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_DELETE_FILTER:
			if(mService != null && arg4 != null) {
				FilterObject filter = (FilterObject) arg4;
				if(mService.deleteFilter(filter.mId) > Constants.RESPONSE_DELETE_FILTER_FAILED) {
					FiltersFragment frg = (FiltersFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_FILTERS);
					frg.deleteFilter(filter.mId);
					
					ArrayList<ContentObject> contents = mService.refreshContentObjectList();
					MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
					frg2.deleteMessageAll();
					frg2.addMessageAll(contents);
				}
			}
			break;

		case IFragmentListener.CALLBACK_REQUEST_DELETE_PACKAGE_FILTER:
			if(mService != null && arg4 != null) {
				FilterObject filter = (FilterObject) arg4;
				if(mService.deleteFilter(filter.mType, filter.mOriginalString) > Constants.RESPONSE_DELETE_FILTER_FAILED) {
					FiltersFragment frg = (FiltersFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_FILTERS);
					frg.deleteFilter(filter.mType, filter.mOriginalString);
					
					ArrayList<ContentObject> contents = mService.refreshContentObjectList();
					MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
					frg2.deleteMessageAll();
					frg2.addMessageAll(contents);
				}
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_CLOCK_STYLE:
			int clockStyle = arg0;
			if(mService != null) {
				mService.sendClockStyle(clockStyle);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_SHOW_INDICATOR:
			int indicator = arg0;
			if(mService != null) {
				mService.showIndicator(indicator);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_SET_EMAIL_ADDRESS:
			if(mService != null) {
				mService.setGmailAddress(arg2);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_RUN_IN_BACKGROUND:
			if(mService != null) {
				mService.startServiceMonitoring();
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_SET_TCP_HOST:
			if(mService != null && arg2 != null) {
				mService.setTcpHost(arg2);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_SET_TCP_PORT:
			if(mService != null) {
				mService.setTcpPort(arg0);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_CONNECT:
			if(mService != null) {
				mService.connectDevice();
				Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_DISCONNECT:
			if(mService != null) {
				mService.disconnectDevice();
				Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_SEND_CLOCK:
			if(mService != null) {
				mService.sendClockData();
				Toast.makeText(this, "Clock data sent", Toast.LENGTH_SHORT).show();
			}
			break;
			
		default:
			break;
		}
	}
	

	
	/*****************************************************
	 * 
	 *	Private classes
	 *
	 ******************************************************/
	
	/**
	 * Service connection
	 */
	private ServiceConnection mServiceConn = new ServiceConnection() {
		
		public void onServiceConnected(ComponentName className, IBinder binder) {
			try {
				Logs.d(TAG, "Activity - Service connected");
				Log.d(TAG, "onServiceConnected: Getting service instance");
				
				mService = ((RetroWatchService.RetroWatchServiceBinder) binder).getService();
				mIsServiceBound = true;
				
				// Activity couldn't work with mService until connections are made
				// So initialize parameters and settings here, not while running onCreate()
				Log.d(TAG, "onServiceConnected: Calling initialize()");
				initialize();
				Log.d(TAG, "onServiceConnected: Initialization complete");
			} catch (Exception e) {
				Log.e(TAG, "ERROR in onServiceConnected: " + e.getMessage(), e);
				Toast.makeText(RetroWatchActivity.this, "Error connecting to service: " + e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "onServiceDisconnected");
			mService = null;
			mIsServiceBound = false;
		}
	};
	
	private void doStartService() {
		try {
			Logs.d(TAG, "# Activity - doStartService()");
			Log.d(TAG, "Starting RetroWatchService...");
			Intent serviceIntent = new Intent(this, RetroWatchService.class);
			// On Android 8+ prefer foreground-service start to avoid background-start restrictions.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				ContextCompat.startForegroundService(this, serviceIntent);
			} else {
				startService(serviceIntent);
			}
			Log.d(TAG, "Binding to RetroWatchService...");
			bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "Service started and bind requested");
		} catch (IllegalStateException e) {
			// Can happen on modern Android if a foreground service start is disallowed in current app state.
			Log.e(TAG, "IllegalStateException in doStartService: " + e.getMessage(), e);
			Toast.makeText(this, "Unable to start service: " + e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Log.e(TAG, "ERROR in doStartService: " + e.getMessage(), e);
			Toast.makeText(this, "Error starting service: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void doStopService() {
		Logs.d(TAG, "# Activity - doStopService()");
		mService.finalizeService();
		stopService(new Intent(this, RetroWatchService.class));
	}
	
	/**
	 * Initialization / Finalization
	 */
	private void initialize() {
		try {
			Logs.d(TAG, "# Activity - initialize()");
			Log.d(TAG, "initialize: Setting up service");
			mService.setupService(mActivityHandler);
			
			Log.d(TAG, "initialize: Checking Bluetooth status");
			// If BT is not on, request that it be enabled.
			// RetroWatchService.setupBT() will then be called during onActivityResult
			if(!mService.isBluetoothEnabled()) {
				Log.d(TAG, "initialize: Bluetooth not enabled, requesting");
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
			}
			
			Log.d(TAG, "initialize: Refreshing content objects");
			// Get messages(notifications)
			refreshContentObjects();
			
			Log.d(TAG, "initialize: Getting filters");
			// Get filters
			getFiltersAll();
			
			Log.d(TAG, "initialize: Reserving content update");
			// Reserve refresh timer
			reserveContentUpdate(5000);
			
			Log.d(TAG, "initialize: Getting BLE status");
			// Get current connection status (Result will be delivered on Handler)
			mService.getBleStatus();
			
			Log.d(TAG, "initialize: Completed successfully");
		} catch (SecurityException e) {
			Log.e(TAG, "SecurityException in initialize: " + e.getMessage(), e);
			Toast.makeText(this, "Bluetooth permission error: " + e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Log.e(TAG, "ERROR in initialize: " + e.getMessage(), e);
			Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void finalizeActivity() {
		Logs.d(TAG, "# Activity - finalizeActivity()");
		
		if(mStopService)
			doStopService();
		
		if (mIsServiceBound) {
			try {
				unbindService(mServiceConn);
			} catch (IllegalArgumentException e) {
				// Defensive: avoid crashing if system already unbound us or bind never completed.
				Log.w(TAG, "Service not bound when unbinding: " + e.getMessage());
			} finally {
				mIsServiceBound = false;
			}
		}

		RecycleUtils.recursiveRecycle(getWindow().getDecorView());
		System.gc();
	}
	
	/**
	 * Launch the DeviceListActivity to see devices and do scan
	 */
	private void doScan() {
		Intent intent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
	}
	
	/**
	 * Launch notification settings screen
	 */
	private void setNotificationAccess() {
		Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
		startActivity(intent);
	}
	
	/**
	 * Ensure this device is discoverable by others
	 */
	private void ensureDiscoverable() {
		if (mService.getBluetoothScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(intent);
		}
	}
	
	private void refreshContentObjects() {
		if(mService != null) {
			// WARNING: This makes remote sync.
			// mService.sendGetAllNotificationsSignal();		// Delete cached notifications and set refresh signal
			ArrayList<ContentObject> contents = mService.refreshContentObjectList();	// Get cached contents
			
			MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
			frg2.deleteMessageAll();
			frg2.addMessageAll(contents);
		}
	}
	
	/**
	 * Reserve message list(notifications) refresh. Default is once a minute.
	 * @param delay		delay before fist execution
	 */
	private void reserveContentUpdate(long delay) {
		if(mRefreshTimer != null)
			mRefreshTimer.cancel();
		mRefreshTimer = new Timer();
		mRefreshTimer.schedule(new RefreshTimerTask(), delay, CONTENTS_REFRESH_TIME);
	}
	
	/**
	 * Stop the refresh timer
	 */
	private void stopContentUpdate() {
		if(mRefreshTimer != null)
			mRefreshTimer.cancel();
		mRefreshTimer = null;
	}
	
	
	/*****************************************************
	 * 
	 *	Public classes
	 *
	 ******************************************************/
	
	/**
	 * Receives result from external activity
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Logs.d(TAG, "onActivityResult " + resultCode);
		
		switch(requestCode) {
		case Constants.REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Attempt to connect to the device
				if(address != null && mService != null)
					mService.connectDevice(address);
			}
			break;
			
		case Constants.REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a BT session
				mService.setupBT();
			} else {
				// User did not enable Bluetooth or an error occured
				Logs.e(TAG, "BT is not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
			}
			break;
		}	// End of switch(requestCode)
	}
	
	private void getFiltersAll() {
		if(mService != null) {
			ArrayList<FilterObject> filterList = mService.getFiltersAll();
			FiltersFragment frg = (FiltersFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_FILTERS);
			frg.addFilterAll(filterList);
		}
	}
	
	/*
	private void getRssAll() {
		if(mService != null) {
			ArrayList<CPObject> cpoList = mService.getRssAll();
			RssFragment frg = (RssFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_RSS);
			frg.addRssAll(cpoList);
		}
	}*/
	
	
	
	/*****************************************************
	 * 
	 *	Handler, Callback, Sub-classes
	 *
	 ******************************************************/
	
	public class ActivityHandler extends Handler {
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what) {
			// BT state message
			case Constants.MESSAGE_BT_STATE_INITIALIZED:
				mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
						getResources().getString(R.string.bt_state_init));
				mImageBT.setImageDrawable(ContextCompat.getDrawable(mContext, android.R.drawable.presence_invisible));
				break;
			case Constants.MESSAGE_BT_STATE_LISTENING:
				mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
						getResources().getString(R.string.bt_state_wait));
				mImageBT.setImageDrawable(ContextCompat.getDrawable(mContext, android.R.drawable.presence_invisible));
				break;
			case Constants.MESSAGE_BT_STATE_CONNECTING:
				mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
						getResources().getString(R.string.bt_state_connect));
				mImageBT.setImageDrawable(ContextCompat.getDrawable(mContext, android.R.drawable.presence_away));
				break;
			case Constants.MESSAGE_BT_STATE_CONNECTED:
				if(mService != null) {
					String deviceName = mService.getDeviceName();
					if(deviceName != null) {
						mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
								getResources().getString(R.string.bt_state_connected) + " " + deviceName);
						mImageBT.setImageDrawable(ContextCompat.getDrawable(mContext, android.R.drawable.presence_online));
					}
				}
				break;
			case Constants.MESSAGE_BT_STATE_ERROR:
				mTextStatus.setText(getResources().getString(R.string.bt_state_error));
				mImageBT.setImageDrawable(ContextCompat.getDrawable(mContext, android.R.drawable.presence_busy));
				break;
			
			// BT Command status
			case Constants.MESSAGE_CMD_ERROR_NOT_CONNECTED:
				mTextStatus.setText(getResources().getString(R.string.bt_cmd_sending_error));
				mImageBT.setImageDrawable(ContextCompat.getDrawable(mContext, android.R.drawable.presence_busy));
				break;
				
			////////////////////////////////////////////
			// Contents changed
			////////////////////////////////////////////
			case Constants.MESSAGE_ADD_NOTIFICATION:
			{
				ContentObject obj = (ContentObject)msg.obj;
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null)
					frg.addMessage(obj);
				break;
			}
			
			case Constants.MESSAGE_DELETE_NOTIFICATION:
			{
				int _id = msg.arg1;
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null)
					frg.deleteMessage(_id);
				break;
			}
			
			case Constants.MESSAGE_GMAIL_UPDATED:
			{
				ContentObject obj = null;
				if(msg.obj != null) {
					obj = (ContentObject)msg.obj;
				}
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null) {
					frg.deleteMessageByTypeAndName(ContentObject.CONTENT_TYPE_MESSAGING, ContentObject.GMAIL_PACKAGE_NAME);
					frg.addMessage(obj);
				}
				break;
			}
			
			// Disable: this case is deprecated
			case Constants.MESSAGE_SMS_RECEIVED:
			{
				ContentObject obj = null;
				if(msg.obj != null) {
					obj = (ContentObject)msg.obj;
				}
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null) {
					frg.deleteMessageByTypeAndName(ContentObject.CONTENT_TYPE_MESSAGING, ContentObject.SMS_PACKAGE_NAME);
					frg.addMessage(obj);
				}
				break;
			}
			
			case Constants.MESSAGE_CALL_STATE_RECEIVED:
			case Constants.MESSAGE_RF_STATE_RECEIVED:
			{
				ContentObject obj = null;
				if(msg.obj != null) {
					obj = (ContentObject)msg.obj;
				}
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null) {
					if(msg.what == Constants.MESSAGE_CALL_STATE_RECEIVED)
						frg.deleteMessageByTypeAndName(ContentObject.CONTENT_TYPE_EMERGENCY, ContentObject.TELEPHONY_CALL_PACKAGE_NAME);
					else
						frg.deleteMessageByTypeAndName(ContentObject.CONTENT_TYPE_EMERGENCY, ContentObject.TELEPHONY_RF_PACKAGE_NAME);
					frg.addMessage(obj);
				}
				break;
			}
			
			case Constants.MESSAGE_FEED_UPDATED:
			{
				ArrayList<ContentObject> feedList = null;
				if (msg.obj instanceof ArrayList<?>) {
					// Avoid unchecked casts: copy only the expected element type.
					feedList = new ArrayList<>();
					for (Object o : (ArrayList<?>) msg.obj) {
						if (o instanceof ContentObject) {
							feedList.add((ContentObject) o);
						}
					}
				}
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null) {
					frg.deleteMessageByType(ContentObject.CONTENT_TYPE_FEED);
					if(feedList != null && feedList.size() > 0)
						frg.addMessageAll(feedList);
				}
				break;
			}
			
			default:
				break;
			}
			
			super.handleMessage(msg);
		}
	}	// End of class ActivityHandler
	
    /**
     * Auto-refresh Timer
     */
	private class RefreshTimerTask extends TimerTask {
		public RefreshTimerTask() {}
		
		public void run() {
			mActivityHandler.post(new Runnable() {
				public void run() {
					refreshContentObjects();
				}
			});
		}
	}
	
}

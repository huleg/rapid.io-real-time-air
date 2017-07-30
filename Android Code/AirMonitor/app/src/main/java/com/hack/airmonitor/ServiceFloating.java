package com.hack.airmonitor;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.rapid.Rapid;
import io.rapid.RapidError;

public class ServiceFloating extends Service {
	public String TAG = "ServiceFloating";
	public static  int ID_NOTIFICATION = 2018;

	private WindowManager windowManager;
    private ImageView chatHead;
	private PopupWindow pwindo;

	boolean mHasDoubleClicked = false;
	long lastPressTime;

	ArrayList<String> myArray;
	ArrayList<PInfo> apps;
	List listCity;

	private String mDeviceAddress;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override 
	public void onCreate() {
		super.onCreate();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


		RetrievePackages getInstalledPackages = new RetrievePackages(getApplicationContext());
		apps = getInstalledPackages.getInstalledApps(false);
		myArray = new ArrayList<String>();

		for(int i=0 ; i<apps.size() ; ++i) {
			myArray.add(apps.get(i).appname);
		}

		listCity = new ArrayList();
		for(int i=0 ; i<apps.size() ; ++i) {
			listCity.add(apps.get(i));
		}

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		chatHead = new ImageView(this);
		
		chatHead.setImageResource(R.drawable.floating2);


		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);

		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = 0;
		params.y = 100;

		windowManager.addView(chatHead, params);

		try {
			chatHead.setOnTouchListener(new View.OnTouchListener() {
				private WindowManager.LayoutParams paramsF = params;
				private int initialX;
				private int initialY;
				private float initialTouchX;
				private float initialTouchY;

				@Override public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:

						// Get current time in nano seconds.
						long pressTime = System.currentTimeMillis();


						// If double click...
						if (pressTime - lastPressTime <= 300) {
							createNotification();
							ServiceFloating.this.stopSelf();
							mHasDoubleClicked = true;
						}
						else {     // If not double click....
							mHasDoubleClicked = false;
						}
						lastPressTime = pressTime; 
						initialX = paramsF.x;
						initialY = paramsF.y;
						initialTouchX = event.getRawX();
						initialTouchY = event.getRawY();
						break;
					case MotionEvent.ACTION_UP:
						break;
					case MotionEvent.ACTION_MOVE:
						paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
						paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
						windowManager.updateViewLayout(chatHead, paramsF);
						break;
					}
					return false;
				}
			});
		} catch (Exception e) {
			// TODO: handle exception
		}

		chatHead.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				initiatePopupWindow(chatHead);
				//				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
				//				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				//				getApplicationContext().startActivity(intent);
			}
		});

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent.hasExtra("DeviceAddress"))
		{
			mDeviceAddress = intent.getExtras().getString("DeviceAddress");
			final BluetoothManager bluetoothManager =
					(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = bluetoothManager.getAdapter();

			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
			connectToDevice(device);
		}
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}



	private void initiatePopupWindow(View anchor) {
		if (mGatt != null) {
			mGatt.close();
			mGatt = null;
		}

		this.stopSelf();
	}

	public void createNotification(){
		Intent notificationIntent = new Intent(getApplicationContext(), ServiceFloating.class);
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, notificationIntent, 0);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.floating2).setTicker("Click to start launcher").setWhen(System.currentTimeMillis())
                .setContentTitle("Start launcher")
                .setContentText("Click to start launcher");

		NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ID_NOTIFICATION, builder.build());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (chatHead != null) windowManager.removeView(chatHead);
	}


	private BluetoothGatt mGatt;
	private BluetoothAdapter mBluetoothAdapter;

	// BLE Services
	public static final UUID MPU_SERVICE_UUID = UUID.fromString("fef431b0-51e0-11e7-9598-0800200c9a67");
	// BLE Characteristics
	public static final UUID X_ACCEL_CHARACTERISTICS_UUID = UUID.fromString("fef431b0-51e0-11e7-9598-0800200c9a66");
	// BLE Descriptors
	public static final UUID X_ACCEL_DESCRIPTOR_UUID = UUID.fromString("fef431b0-51e0-11e7-9598-0800200c9a66");

	public void connectToDevice(BluetoothDevice device) {
		if (mGatt == null) {
			Log.d("connectToDevice", "connecting to device: "+device.toString());
			mGatt = device.connectGatt(this, false, gattCallback);
		}
	}

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {


		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			Log.i("onConnectionStateChange", "Status: " + status);
			switch (newState) {
				case BluetoothProfile.STATE_CONNECTED:
					Log.i("gattCallback", "STATE_CONNECTED");
					gatt.discoverServices();
					break;
				case BluetoothProfile.STATE_DISCONNECTED:
					Log.e("gattCallback", "STATE_DISCONNECTED");
					Log.i("gattCallback", "reconnecting...");
					BluetoothDevice mDevice = gatt.getDevice();
					mGatt = null;
					connectToDevice(mDevice);
					break;
				default:
					Log.e("gattCallback", "STATE_OTHER");
			}

		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			mGatt = gatt;
			List<BluetoothGattService> services = gatt.getServices();
			Log.i("onServicesDiscovered", services.toString());

			BluetoothGattCharacteristic characteristic = mGatt.getService(MPU_SERVICE_UUID).getCharacteristic(X_ACCEL_CHARACTERISTICS_UUID);
			mGatt.setCharacteristicNotification(characteristic, true);


			for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
				Log.i("descriptor", descriptor.getUuid().toString());

				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				mGatt.writeDescriptor(descriptor);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
										 BluetoothGattCharacteristic
												 characteristic, int status) {
			Log.i("onCharacteristicRead", "read");
			Log.i("onCharacteristicRead", characteristic.getStringValue(0));
			byte[] value=characteristic.getValue();
			String v = new String(value);
			Log.i("onCharacteristicRead", "Value: " + v);
			//gatt.disconnect();
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
											final BluetoothGattCharacteristic
													characteristic) {
			int flag = characteristic.getProperties();
			int format = -1;
			if ((flag & 0x01) != 0) {
				format = BluetoothGattCharacteristic.FORMAT_UINT16;
				Log.d(TAG, "Heart rate format UINT16.");
			} else {
				format = BluetoothGattCharacteristic.FORMAT_UINT8;
				Log.d(TAG, "Heart rate format UINT8.");
			}
			Log.i("onCharacteristicChanged", characteristic.getIntValue(format, 0).toString());

			Rapid.getInstance().collection("airdata", AirData.class).newDocument()
					.mutate(new AirData(new Location(37.7736687, -122.4185968), characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0), 0, 0, 0))
					.onSuccess(() -> {
						Log.d(TAG, "Success");
					})
					.onError(error -> {
						switch(error.getType()){
							case TIMEOUT:
								Log.e("foobar", "timeout");
								break; // mutation timed out
							case PERMISSION_DENIED:
								Log.e("foobar", "PERMISSION_DENIED");
								break; // access control related error
						}
					});

			chatHead.post(new Runnable() {
				@Override
				public void run() {
					Integer temp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
					if (temp < 50) {
						chatHead.setImageResource(R.mipmap.lvl1);
					} else if (temp >= 50 && temp < 60) {
						chatHead.setImageResource(R.mipmap.lvl2);
					} else if (temp >= 60 && temp < 75) {
						chatHead.setImageResource(R.mipmap.lvl3);
					} else if (temp >= 75 && temp < 90) {
						chatHead.setImageResource(R.mipmap.lvl4);
					} else if (temp >= 90 && temp < 120) {
						chatHead.setImageResource(R.mipmap.lvl5);
					} else {
						chatHead.setImageResource(R.mipmap.lvl6);
					}
				}
			});
		}
	};

	static class AirData{
		Location location;
		int air;
		int temp;
		int humidity;
		int light;

		public AirData(Location location, int air, int temp, int humidity, int light) {
			this.location = location;
			this.air = air;
			this.temp = temp;
			this.humidity = humidity;
			this.light = light;
		}
	}

	static class Location
	{
		double latitude;
		double longitude;

		public Location(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}

}
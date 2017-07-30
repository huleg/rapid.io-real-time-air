package com.hack.airmonitor;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Handler;
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

public class ServiceFloatingBackup extends Service {
	public String TAG = "ServiceFloating";
	public static  int ID_NOTIFICATION = 2018;

	private WindowManager windowManager;
    private ImageView chatHead;
	private PopupWindow pwindo;

	boolean mHasDoubleClicked = false;
	long lastPressTime;
	private Boolean _enable = true;

	ArrayList<String> myArray;
	ArrayList<PInfo> apps;
	List listCity;

	private Context mainContext=this;
	BluetoothLeService mBluetoothLeService;
	private String mDeviceAddress;
	private static BluetoothGattCharacteristic mSCharacteristic, mModelNumberCharacteristic;//, mSerialPortCharacteristic, mCommandCharacteristic;
	public boolean mConnected = false;
	public BlunoLibrary.connectionStateEnum mConnectionState = BlunoLibrary.connectionStateEnum.isNull;
	private Handler mHandler= new Handler();
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

	//public static final String SerialPortUUID="00001800-0000-1000-8000-00805f9b34fb";
	//public static final String CommandUUID="00001801-0000-1000-8000-00805f9b34fb";
	public static final String ModelNumberStringUUID="fef431b0-51e0-11e7-9598-0800200c9a66";


	private int mBaudrate=9600;	//set the default baud rate to 115200
	private String mPassword="AT+PASSWOR=DFRobot\r\n";


	private String mBaudrateBuffer = "AT+CURRUART="+mBaudrate+"\r\n";

	private Runnable mConnectingOverTimeRunnable=new Runnable(){

		@Override
		public void run() {
			if(mConnectionState== BlunoLibrary.connectionStateEnum.isConnecting)
				mConnectionState= BlunoLibrary.connectionStateEnum.isToScan;
			onConectionStateChange(mConnectionState);
			mBluetoothLeService.close();
		}};

	private Runnable mDisonnectingOverTimeRunnable=new Runnable(){

		@Override
		public void run() {
			if(mConnectionState== BlunoLibrary.connectionStateEnum.isDisconnecting)
				mConnectionState= BlunoLibrary.connectionStateEnum.isToScan;
			onConectionStateChange(mConnectionState);
			mBluetoothLeService.close();
		}};

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
		
		if(prefs.getString("ICON", "floating2").equals("floating3")){
			chatHead.setImageResource(R.drawable.floating3);
		} else if(prefs.getString("ICON", "floating2").equals("floating4")){
			chatHead.setImageResource(R.drawable.floating4);
		} else if(prefs.getString("ICON", "floating2").equals("floating5")){
			chatHead.setImageResource(R.drawable.floating5);
		} else if(prefs.getString("ICON", "floating2").equals("floating5")){
			chatHead.setImageResource(R.drawable.floating2);
		}

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
							ServiceFloatingBackup.this.stopSelf();
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
				_enable = false;
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
			Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
			bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
		}
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	private void connectToBluetooth()
	{
		mainContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService.connect(mDeviceAddress)) {
			Log.d(TAG, "Connect request success");
			mConnectionState= BlunoLibrary.connectionStateEnum.isConnecting;
			onConectionStateChange(mConnectionState);
			mHandler.postDelayed(mConnectingOverTimeRunnable, 10000);
		}
		else {
			Log.d(TAG, "Connect request fail");
			mConnectionState= BlunoLibrary.connectionStateEnum.isToScan;
			onConectionStateChange(mConnectionState);
			initiatePopupWindow(null);
		}
	}


	private void initiatePopupWindow(View anchor) {
		if(mBluetoothLeService!=null) {
			mHandler.removeCallbacks(mDisonnectingOverTimeRunnable);
			mBluetoothLeService.close();
			mainContext.unbindService(mServiceConnection);
			mBluetoothLeService = null;
		}
		mainContext.unregisterReceiver(mGattUpdateReceiver);
		this.stopSelf();
	}

	// Code to manage Service lifecycle.
	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			Log.d("ServiceFloating", "mServiceConnection onServiceConnected");
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				ServiceFloatingBackup.this.stopSelf();
			}
			else {
				connectToBluetooth();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			System.out.println("mServiceConnection onServiceDisconnected");
			mBluetoothLeService = null;
		}
	};

	public void createNotification(){
		Intent notificationIntent = new Intent(getApplicationContext(), ServiceFloatingBackup.class);
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


	public void onConectionStateChange(BlunoLibrary.connectionStateEnum theConnectionState) {
		switch (theConnectionState) {											//Four connection state
			case isConnected:
				Log.e("Foobar", "Connected");
				//buttonScan.setText("Connected");
				break;
			case isConnecting:
				Log.e("Fobar", "Connecting");
				break;
			case isToScan:
				Log.e("Fobar", "Scan");
				break;
			case isScanning:
				Log.e("Fobar", "Scanning");
				break;
			case isDisconnecting:
				Log.e("Fobar", "isDisconnecting");
				break;
			default:
				break;
		}
	}

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@SuppressLint("DefaultLocale")
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			Log.d("receiver", "mGattUpdateReceiver->onReceive->action="+action);
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				mHandler.removeCallbacks(mConnectingOverTimeRunnable);

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				mConnectionState = BlunoLibrary.connectionStateEnum.isToScan;
				onConectionStateChange(mConnectionState);
				mHandler.removeCallbacks(mDisonnectingOverTimeRunnable);
				mBluetoothLeService.close();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
				for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
					System.out.println("ACTION_GATT_SERVICES_DISCOVERED  "+
							gattService.getUuid().toString());
				}
				getGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				Log.e("Doh", intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				if(mSCharacteristic==mModelNumberCharacteristic)
				{

					mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, false);
					/*
					mSCharacteristic=mCommandCharacteristic;
					mSCharacteristic.setValue(mPassword);
					mBluetoothLeService.writeCharacteristic(mSCharacteristic);
					mSCharacteristic.setValue(mBaudrateBuffer);

					mBluetoothLeService.writeCharacteristic(mSCharacteristic);
					mSCharacteristic=mSerialPortCharacteristic;
					*/
					//mSCharacteristic.setValue(mBaudrateBuffer);
					//mBluetoothLeService.writeCharacteristic(mSCharacteristic);
					mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);
					mConnectionState = BlunoLibrary.connectionStateEnum.isConnected;
					onConectionStateChange(mConnectionState);
				}
				else{
					onSerialReceived(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				}


				System.out.println("displayData "+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

//            	mPlainProtocol.mReceivedframe.append(intent.getStringExtra(BluetoothLeService.EXTRA_DATA)) ;
//            	System.out.print("mPlainProtocol.mReceivedframe:");
//            	System.out.println(mPlainProtocol.mReceivedframe.toString());


			}
		}
	};


	private void getGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		String uuid = null;
		mModelNumberCharacteristic=null;
		//mSerialPortCharacteristic=null;
		//mCommandCharacteristic=null;
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			uuid = gattService.getUuid().toString();
			System.out.println("displayGattServices + uuid="+uuid);

			List<BluetoothGattCharacteristic> gattCharacteristics =
					gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas =
					new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				uuid = gattCharacteristic.getUuid().toString();
				System.out.println("uuid  "+uuid);

				if(uuid.equals(ModelNumberStringUUID)){
					mModelNumberCharacteristic=gattCharacteristic;
					System.out.println("mModelNumberCharacteristic  "+mModelNumberCharacteristic.getUuid().toString());
				}
				/*
				else if(uuid.equals(SerialPortUUID)){
					mSerialPortCharacteristic = gattCharacteristic;
					System.out.println("mSerialPortCharacteristic  "+mSerialPortCharacteristic.getUuid().toString());
//                    updateConnectionState(R.string.comm_establish);
				}
				else if(uuid.equals(CommandUUID)){
					mCommandCharacteristic = gattCharacteristic;
					System.out.println("mSerialPortCharacteristic  "+mSerialPortCharacteristic.getUuid().toString());
//                    updateConnectionState(R.string.comm_establish);
				}*/

			}
			mGattCharacteristics.add(charas);
		}
/*
		if (mModelNumberCharacteristic==null || mSerialPortCharacteristic==null || mCommandCharacteristic==null) {
			Toast.makeText(mainContext, "Please select DFRobot devices", Toast.LENGTH_SHORT).show();
			mConnectionState = BlunoLibrary.connectionStateEnum.isToScan;
			onConectionStateChange(mConnectionState);
		}
		else {
			mSCharacteristic=mModelNumberCharacteristic;
			mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);
			mBluetoothLeService.readCharacteristic(mSCharacteristic);
		}
*/

		mSCharacteristic=mModelNumberCharacteristic;
		mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);
		mBluetoothLeService.readCharacteristic(mSCharacteristic);
	}

	public void onSerialReceived(String theString) {
		Log.e("received", theString);
	}

	private IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}
}
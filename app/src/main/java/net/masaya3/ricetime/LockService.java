package net.masaya3.ricetime;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import net.masaya3.ricetime.control.LockView;
import net.masaya3.ricetime.control.LockView.OnUnlockListener;
import net.masaya3.ricetime.data.BeaconInfo;


/**
 * 
 * @author 雅也
 *
 */
public class LockService extends Service implements LeScanCallback, 
													OnUnlockListener, 
													Runnable{

	
	/** Beacon情報 */
	//private static String BEACON_UUID = "00000000-20DD-1001-B000-001C4DEF2E9A";
	//private static int MAJOR_KEY = 1;
	//private static int MINOR_LOCK_KEY = 0;
	//private static int MINOR_UNLOCK_KEY = 1;
	
	/** */
	private BluetoothAdapter mBluetoothAdapter;
	/** */
	private BluetoothManager bluetoothManager;
	
	/** */
	private ComponentName mComponentName;
	/** */
	private DevicePolicyManager mDevicePolicyManager;
	
	/** */
	private LockView mLockView;
	
	/** */
	private WindowManager mWindowManager = null;

	/** */
	private static int MIN_DELAY_TIME = 500;
	
	/** */
	private static int SCAN_SLEEP = 2000;
	
	/** */
	private Thread mThread; 

	/** */
	private Handler mHandler;
	
	/** */
	private long lockTime = 0;
	
	/** */
	private BeaconInfo mBeaconInfo;
	
    /**
     * サービス作成時（一度だけ呼び出される）
     */
    @Override
	public void onCreate() {
    	super.onCreate();
    	Log.d("Service", "onStart");

    	mWindowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
    	
		bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);	
		
		mBeaconInfo = BeaconInfo.load(getApplicationContext());
		
		//BLEの検知開始
		mBluetoothAdapter = bluetoothManager.getAdapter();
		
		mHandler = new Handler(Looper.getMainLooper());
		
		mDevicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
		mComponentName = new ComponentName(this, MainDeviceReceiver.class);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BOOT_COMPLETED);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);		
		
        //レシーバの登録
        registerReceiver(mBroadcastReceiver, filter);
        
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentTitle(this.getString(R.string.notification_title));
		    builder.setContentText(this.getString(R.string.notification_message));
		    builder.setSmallIcon(R.drawable.icon);
		    
		Notification notification = builder.build();
		    
    	NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    	manager.notify(1, notification);
    	startForeground(1, notification);    		
    	
    	lockTime = 0;
    	mLockView = null;
    	
		mThread = new Thread(this);
		mThread.start();
    }
	
	/**
	 * サービスコール時
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.d("WidgetService", "onStartCommand");
    	super.onStartCommand(intent, flags, startId);
    	
    	if(Utils.isLock(this) && mLockView == null){
    		lock();
    	}
    	
		return START_STICKY;
	}
	
	/**
	 * 
	 */
	@Override
	public void run() {
        while (mThread != null) {
        	if(mBluetoothAdapter == null){
        		break;
        	}
        	mBluetoothAdapter.startLeScan(this);
        
            try {
            	if(mLockView != null){
        			mHandler.post(new Runnable(){

        				@Override
        				public void run() {
        					mLockView.update();
        				}});            		
            	}
            	
            	
                Thread.sleep(SCAN_SLEEP);

            	if(mBluetoothAdapter == null){
            		break;
            	}
                
                mBluetoothAdapter.stopLeScan(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}
	
	/** */
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			
			//On時
			if(action.equals(Intent.ACTION_BOOT_COMPLETED) || 
				action.equals(Intent.ACTION_SCREEN_ON)){
				Log.d("Receive", "BOOT or SCREEN_ON");
				
		        if(Utils.isLock(context)){
		        	lock();
		        }
		        else{
		        	unlock();
		        }
			}
			
			//Off時
			if(action.equals(Intent.ACTION_SCREEN_OFF)){
				Log.d("Receive", "SCREEN_OFF");
			}
		}};
    
    /**
     * サービスバインド時
     */
	@Override
	public IBinder onBind(Intent in) {
		return null;
	}
	
	/**
	 * サービス削除時
	 */
	@Override
	public void onDestroy(){
		Log.d("Service", "onDestroy");		
		super.onDestroy();

		//ブロードキャスト
		unregisterReceiver(mBroadcastReceiver);	

		//通知常駐の削除
		stopForeground(true);
		
		mThread = null;
		
		//サービス停止しても、実行中の場合は再度起動する
		/*
		if(Utils.isStart(getApplicationContext())){
			startService(new Intent(this, LockService.class));
		}
		*/
		return;
	}

		
	/**
	 * 
	 */
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        final String uuid = Utils.intToHex2(scanRecord[9] & 0xff) 
                + Utils.intToHex2(scanRecord[10] & 0xff)
                + Utils.intToHex2(scanRecord[11] & 0xff)
                + Utils.intToHex2(scanRecord[12] & 0xff)
                + "-"
                + Utils.intToHex2(scanRecord[13] & 0xff)
                + Utils.intToHex2(scanRecord[14] & 0xff)
                + "-"
                + Utils.intToHex2(scanRecord[15] & 0xff)
                + Utils.intToHex2(scanRecord[16] & 0xff)
                + "-"
                + Utils.intToHex2(scanRecord[17] & 0xff)
                + Utils.intToHex2(scanRecord[18] & 0xff)
                + "-"
                + Utils.intToHex2(scanRecord[19] & 0xff)
                + Utils.intToHex2(scanRecord[20] & 0xff)
                + Utils.intToHex2(scanRecord[21] & 0xff)
                + Utils.intToHex2(scanRecord[22] & 0xff)
                + Utils.intToHex2(scanRecord[23] & 0xff)
                + Utils.intToHex2(scanRecord[24] & 0xff);
        
        if(!mBeaconInfo.uuid.equals(uuid)){
        	return;
        }
        
        final int major = Integer.parseInt(Utils.intToHex2(scanRecord[25] & 0xff) +  Utils.intToHex2(scanRecord[26] & 0xff));
        final int minor = Integer.parseInt(Utils.intToHex2(scanRecord[27] & 0xff) +  Utils.intToHex2(scanRecord[28] & 0xff));
        

        if(Utils.isLock(this) && 
        		mBeaconInfo.unlock_major == major &&
        		mBeaconInfo.unlock_minor == minor){
        	unlock();
        }
        else if(!Utils.isLock(this) &&       		
        		mBeaconInfo.lock_major == major &&
        		mBeaconInfo.lock_minor == minor){
        	if(!lock()){
        		return;
        	}
        	
            if (mDevicePolicyManager.isAdminActive(mComponentName)) {

            	mDevicePolicyManager.lockNow();
        	}
        }
	}
	/**
	 * 
	 */
	@Override
	public void onUnlock() {
		unlock();
		//パスワードの解除
		Utils.setPassword(getApplicationContext(), "");
	}
	
	/**
	 * 
	 */
	private synchronized boolean lock(){

		if(mLockView != null){
			return false;
		}
		
		long time = System.currentTimeMillis() - lockTime;
		if(time <  MIN_DELAY_TIME){
			return false;
		}

		try{
			Utils.lock(this);

			mLockView = new LockView(LockService.this);
		    mLockView.setOnUnlockListener(LockService.this);
			    	
			// ロック画面表示用のパラメータ
			final LayoutParams params = new LayoutParams();
					
			params.width = LayoutParams.MATCH_PARENT;
			params.height = LayoutParams.MATCH_PARENT;
			params.type = LayoutParams.TYPE_SYSTEM_ERROR;
			//params.type = LayoutParams.TYPE_SYSTEM_ERROR;
			params.format = PixelFormat.TRANSLUCENT;
			params.flags = LayoutParams.FLAG_LAYOUT_INSET_DECOR| 
							LayoutParams.FLAG_LAYOUT_IN_SCREEN| 
							LayoutParams.FLAG_LAYOUT_NO_LIMITS|
							LayoutParams.FLAG_FULLSCREEN ;       	
			mHandler.post(new Runnable(){
				@Override
				public void run() {						
					mWindowManager.addView(mLockView, params);
			}});

		}
		catch(Exception e){
			e.printStackTrace();
			mLockView = null;
		}

		return true;
	}
	
	/**
	 * 
	 */
	private synchronized void unlock(){
		
		if(mLockView != null){
			mHandler.post(new Runnable(){
				@Override
				public void run() {					
					mWindowManager.removeView(mLockView);
					mLockView = null;
				}});
		}
    	Utils.unlock(this);	
    	
		lockTime  = System.currentTimeMillis();

	}
}

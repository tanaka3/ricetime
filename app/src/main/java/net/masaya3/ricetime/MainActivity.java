package net.masaya3.ricetime;
import net.masaya3.ricetime.data.BeaconInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;

import android.text.InputType;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity{
	
	private static final int RESULT_DEVICE_ADMIN = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private TextView mControlText;
	private AlertDialog mDialog;
	
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {	
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);
        
        
        //NFC起動時は、データの登録を行う
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
        	setupNFC(getIntent());
        }        

		//登録済みの場合
		if(!BeaconInfo.isRegist(getApplicationContext())){    
			BeaconInfo beaconInfo = new BeaconInfo();
			beaconInfo.save(getApplicationContext());
		}        
        
        setupView();
        
        if(!setupBluetooth()){
        	return;
        }

        if(!setupAdminActive()){
        	return;
        }
        
        mControlText.setEnabled(true);

    }

    @Override
    protected void onNewIntent(Intent intent) {

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
        	setupNFC(intent);
        }
    }

	/**
	 * 
	 * @return
	 */
	private boolean setupBluetooth(){
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
         
        if (adapter == null || !adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean setupAdminActive(){
		DevicePolicyManager mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName mCN = new ComponentName(this, MainDeviceReceiver.class);
        if (!mDPM.isAdminActive(mCN)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);  
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mCN);
            startActivityForResult(intent, RESULT_DEVICE_ADMIN);
            return false;
        }
        
        return true;
	}
	
	/**
	 * 
	 */
	private void setupView(){
		
        mControlText = (TextView)this.findViewById(R.id.controlText);
        
        mControlText.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				
				//未設定の場合
        		if(!BeaconInfo.isRegist(getApplicationContext())){
        			setupSwitch();
        			return;
        		}
        		
				if(Utils.isStart(getApplicationContext())){
					stop();
				}
				else{				
					start();
				}
			}}); 
        
        mControlText.setText(Utils.isStart(getApplicationContext()) ? R.string.service_stop : R.string.service_start);		
	}
	
	/**
	 * 
	 */
	private void startLock(){
		Intent in = new Intent(getApplicationContext(), LockService.class);
		getApplicationContext().startService(in);
		Utils.start(getApplicationContext());
		
		mControlText.setText(R.string.service_stop);		
	}
	
	/**
	 * 
	 */
	private void stopLock(){
		
    	Intent in = new Intent(getApplicationContext(), LockService.class);
    	getApplicationContext().stopService(in);   			
		Utils.stop(getApplicationContext());
		
		mControlText.setText(R.string.service_start);	
	}

	/**
	 * 
	 */
	private void setupNFC(Intent intent){
		
		if(intent == null){
			return;
		}
		
		if(mDialog != null && mDialog.isShowing()){
			return;
		}
		
        NdefMessage[] messages = getNdefMessages(intent);
        final BeaconInfo beaconInfo = BeaconInfo.parse(messages);
        
        if(beaconInfo != null){

        	//現在の設定と同じ場合は何もしない
        	
        	if(!Utils.isStart(getApplicationContext())){
        		
        		//登録済みの場合
        		if(BeaconInfo.isRegist(getApplicationContext())){
        			BeaconInfo nowInfo = BeaconInfo.load(getApplicationContext());
        			
        			if(!nowInfo.equals(beaconInfo)){
	        	   		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        	   		builder.setTitle(getString(R.string.setting_nfc_update_title));
	        	        builder.setMessage(getString(R.string.setting_nfc_update_message));
	        	        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								beaconInfo.save(getApplicationContext());
							}
						});
	        	        
	        	        builder.setNegativeButton(getString(R.string.cancel), null);
	        	        mDialog = builder.create();
	        	        mDialog.show();
        			}	
        			else{
	        	   		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        	   		builder.setTitle(getString(R.string.setting_nfc_registed_title));
	        	        builder.setMessage(getString(R.string.setting_nfc_registed_message));
	        	        builder.setPositiveButton(getString(R.string.ok), null);
	        	        mDialog = builder.create();
	        	        mDialog.show();     				
        			}
        		}
        		//未登録の場合
        		else{

        	   		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	   		builder.setTitle(getString(R.string.setting_nfc_title));
        	        builder.setMessage(getString(R.string.setting_nfc_message));
        	        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener(){

						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							beaconInfo.save(getApplicationContext());
						}
					});	        	        
        	        builder.setNegativeButton(getString(R.string.cancel), null);
        	        mDialog = builder.create();
        	        mDialog.show();

        		}
        	}
        }
        setIntent(new Intent());
	}
	
	/**
	 * 
	 */
	private void setupSwitch(){
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.setting_input_title));
        builder.setMessage(getString(R.string.setting_input_message));
        builder.setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.setting, null);
        
        final EditText idText = (EditText) view.findViewById(R.id.idText);
        final EditText keyText = (EditText) view.findViewById(R.id.keyText);
        builder.setView(view);

		builder.setPositiveButton(getString(R.string.ok), null);	
        builder.setNegativeButton(getString(R.string.cancel), null);

        mDialog = builder.create();
        mDialog.show();
		Button buttonOK = mDialog.getButton( DialogInterface.BUTTON_POSITIVE );
		buttonOK.setOnClickListener( new OnClickListener()
		{
			public void onClick( View v )
			{
				String id = idText.getText().toString();
				String key = keyText.getText().toString();
				
				BeaconInfo info = BeaconInfo.parse(id, key);
				
            	if(info != null){
            		mDialog.dismiss();	            		
            		info.save(getApplicationContext());
            		start();
            	}
            	//error
            	else{
            		view.findViewById(R.id.errorText).setVisibility(View.VISIBLE);
            	}
			}
		});			
	}
	/**
	 * 
	 */
	private void start(){
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.password_title));
        builder.setMessage(getString(R.string.password_message));
        builder.setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.lock_password, null);
        
        final EditText passwordText = (EditText) view.findViewById(R.id.passwordText);
        final CheckBox showPasswordCheck = (CheckBox) view.findViewById(R.id.showPasswordCheck);
        showPasswordCheck.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				} else {
					passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				}
			}});
        
        builder.setView(view);
        

		builder.setPositiveButton(getString(R.string.ok), null);	
	            
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int idx) {
            	startLock();
            }});

        mDialog = builder.create();
        mDialog.show();        
		Button buttonOK = mDialog.getButton( DialogInterface.BUTTON_POSITIVE );
		buttonOK.setOnClickListener( new OnClickListener()
		{
			public void onClick( View v )
			{
            	if(passwordText.getText().toString().length() > 0){
            		Utils.setPassword(getApplicationContext(), passwordText.getText().toString());
            		startLock();
            		mDialog.dismiss();	                		
            	}
            	else{
            		view.findViewById(R.id.errorText).setVisibility(View.VISIBLE);
            	}
			}
		});		

	}

	/**
	 * 
	 */
	private void stop(){
		
		//パスワードない場合は、ロック解除
		if(Utils.getPassword(getApplicationContext()).equals("")){
			stopLock();
			return;
		}
		
		//パスワードがある場合は、入力させる
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.password_title));
        builder.setMessage(getString(R.string.password_message));
        builder.setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.unlock_password, null);
        
        final EditText passwordText = (EditText) view.findViewById(R.id.passwordText);
        final CheckBox showPasswordCheck = (CheckBox) view.findViewById(R.id.showPasswordCheck);
        showPasswordCheck.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				} else {
					passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				}
			}});
        
        builder.setView(view);
		builder.setPositiveButton(getString(R.string.ok), null);		            
        builder.setNegativeButton(getString(R.string.cancel), null);
        
        mDialog = builder.create();
        mDialog.show();        
		Button buttonOK = mDialog.getButton( DialogInterface.BUTTON_POSITIVE );
		buttonOK.setOnClickListener( new OnClickListener()
		{
			public void onClick( View v )
			{
				String password = Utils.getPassword(getApplicationContext());
            	if(password.equals(passwordText.getText().toString())){
           		
            		Utils.setPassword(getApplicationContext(), "");
            		stopLock();
            		mDialog.dismiss();	                		
            	}
            	else{
            		view.findViewById(R.id.errorText).setVisibility(View.VISIBLE);
            	}
			}
		});

	}

	
	/**
	 * 
	 */
	private void clearSetting(){
		
		if(mDialog != null && mDialog.isShowing()){
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.clear_setting_title));
        builder.setMessage(getString(R.string.clear_setting_message));
		builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {				
				//登録状態の解除
				BeaconInfo.unRegist(getApplicationContext());
			}
		});
		
        builder.setNegativeButton(getString(R.string.cancel), null);	
        mDialog = builder.create();
        mDialog.show();
	}
	
	/**
	 * 
	 */
    @Override
    public void onResume(){
    	Log.d("MainActivity", "onResume");
    	super.onResume();  
    }
    
    /**
     * 
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
        	
       		if (resultCode == Activity.RESULT_OK) {
       			
                if(!setupAdminActive()){
                	return;
                }    
      	        mControlText.setEnabled(true);
		        
       		}
       		else{
           		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		        builder.setTitle(this.getString(R.string.bluetooth_error_title));
		        builder.setMessage(this.getString(R.string.bluetooth_error_message));
		        builder.setPositiveButton(this.getString(R.string.ok), null);
		        mDialog = builder.create();
		        mDialog.show();       			

       		}

        	break;
       	case RESULT_DEVICE_ADMIN:
       		if (resultCode == Activity.RESULT_OK) {
      	        mControlText.setEnabled(true);
       		}
       		else{
    			
           		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		        builder.setTitle(this.getString(R.string.device_admin_error_title));
		        builder.setMessage(this.getString(R.string.device_admin_error_message));
		        builder.setPositiveButton(this.getString(R.string.ok), null);
		        mDialog = builder.create();
		        mDialog.show();          			
         			
       		}
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    /**
     * 
     */
    private NdefMessage[] getNdefMessages(Intent intent) {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[] {};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {
                    record
                });
                msgs = new NdefMessage[] {
                    msg
                };
            }
        }
        
        return msgs;
    }
    
    /**
     * 
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
    	MenuItem settingMenu = menu.findItem(R.id.clearSetting);
    	
    	settingMenu.setEnabled(!Utils.isStart(getApplicationContext()));
    	
    	return super.onPrepareOptionsMenu(menu);
    }
    
    /**
     * 
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clearSetting:
            	clearSetting();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

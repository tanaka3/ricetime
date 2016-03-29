package net.masaya3.ricetime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LockReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		// サービス呼ぶだけ
		if(!Utils.isStart(context)){
			return;
		}
		
		Intent service = new Intent(context, LockService.class);
		context.startService(service);			
	}
}
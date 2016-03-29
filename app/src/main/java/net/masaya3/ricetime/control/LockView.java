package net.masaya3.ricetime.control;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import net.masaya3.ricetime.R;
import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * 
 * @author 雅也
 *
 */
public class LockView extends FrameLayout implements OnTouchListener{

	/**
	 * 
	 * @author 雅也
	 *
	 */
	public interface OnUnlockListener{
		public void onUnlock();
	}
	/** */
	public OnUnlockListener mOnUnlockListener;
	
	/**
	 * 
	 * @param listenr
	 */
	public void setOnUnlockListener(OnUnlockListener listenr){
		this.mOnUnlockListener = listenr;
	}
	
	/** */
	private TextView mTimeText;
	
	/** */
	private TextView mDateText;
	
	/** */
	private long mStartTime;
	
	
	private int mMode = 0;
	/**
	 * 
	 * @param context
	 */
	public LockView(Context context) {
		super(context);
		
		setupView();
	}
	

	/**
	 * 
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch(event.getAction()){
		
		case MotionEvent.ACTION_DOWN:
			
			WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
			Display disp = wm.getDefaultDisplay();
			Point size = new Point();
			

			disp.getSize(size);


			int wsplit = size.x > size.y ? 6 : 5;
			int hsplit = size.x < size.y ? 6 : 5;
			
			if(size.x == size.y){
				wsplit = hsplit = 5;
			}
			
			int touch_width = size.x / wsplit;
			int touch_height = size.y / hsplit;

			
			switch(mMode){
			case 0:
				//右上をタッチした場合に有効とする
				
				if(touch_width  * (wsplit -1) <= event.getX() && 
					event.getX() <= touch_width * wsplit &&
					touch_height  * 0 <= event.getY() && 
					event.getY() <= touch_height * 1){
					mMode++;
					mStartTime = System.currentTimeMillis();
				}
				
				break;
			case 1:
				//左下をタッチした場合に有効とする
				if(touch_width  * 0 <= event.getX() && 
					event.getX() <= touch_width * 1 &&
					touch_height  * (hsplit-1) <= event.getY() && 
					event.getY() <= touch_height * hsplit){
					mMode++;
				}
				else{
					mMode = 0;
				}
				
				break;
			case 2:
				//左上をタッチした場合に有効とする
				if(touch_width  * 0 <= event.getX() && 
					event.getX() <= touch_width * 1 &&
					touch_height  * 0 <= event.getY() && 
					event.getY() <= touch_height * 1){
					mMode++;
				}
				else{
					mMode = 0;
				}
				break;
			case 3:
				
				long time = System.currentTimeMillis() - mStartTime;
				//右下をタッチした場合に有効とする(合計4秒以内）
				if( time < 4000 &&
					touch_width  *  (wsplit -1) <= event.getX() && 
					event.getX() <= touch_width * wsplit &&
					touch_height  * (hsplit-1) <= event.getY() && 
					event.getY() <= touch_height * hsplit){
					
					mMode = 0;
					
					if(mOnUnlockListener != null){
						mOnUnlockListener.onUnlock();
					}
	
				}
				else{
					mMode = 0;
				}				
				
				break;
			}
			
			break;
		}
		
		return false;
	}

	/**
	 * 
	 */
	private void setupView(){
		LayoutInflater inflater = LayoutInflater.from(this.getContext());
        
		View view =  inflater.inflate(R.layout.lock, null, false);
		this.addView((view));
        
		view.setOnTouchListener(this);
        

        mTimeText = (TextView)this.findViewById(R.id.timeText);        
        mDateText = (TextView)this.findViewById(R.id.dateText);
        
        update();
	}

	/**
	 * 
	 * @param now
	 */
	public void update(){
		Calendar now = Calendar.getInstance();
		
        SimpleDateFormat timeFormat = new SimpleDateFormat(getContext().getString(R.string.lock_time_format), Locale.JAPANESE);    
        mTimeText.setText(timeFormat.format(now.getTime()));
        
        SimpleDateFormat dateFormat = new SimpleDateFormat(getContext().getString(R.string.lock_date_format), Locale.JAPANESE); 
        mDateText.setText(dateFormat.format(now.getTime()));
	}

}

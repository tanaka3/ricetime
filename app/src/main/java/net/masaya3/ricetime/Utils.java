package net.masaya3.ricetime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;



/**
 * Widget情報
 */
public class Utils{
	public static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";  
	public static final String DATE_PATTERN_SHORT = "yyyyMMdd"; 
	 
	 //Date日付型をString文字列型へ変換
	public static String date2string(Date date) {  
	    SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);  
	    return sdf.format(date);  
	}  
	 
	public static String date2string(Date date, String format) {  
	    SimpleDateFormat sdf = new SimpleDateFormat(format);  
	    return sdf.format(date);  
	} 
	
	public static Date string2date(String value) {
	    if ( value == null || value == "" )
	        return null;

	    // 日付フォーマットを作成
	    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
	    // 日付の厳密チェックを指定
	    dateFormat.setLenient(false);
	    try {
	        // 日付値を返す
	        return dateFormat.parse(value);
	    } catch ( ParseException e ) {
	        // 日付値なしを返す
	        return null;
	    } finally {
	        dateFormat = null;
	    }
	}
	 
	 
	//intデータを 2桁16進数に変換するメソッド
	public static String intToHex2(int i) {
	    char hex_2[] = {Character.forDigit((i>>4) & 0x0f,16),Character.forDigit(i&0x0f, 16)};
	    String hex_2_str = new String(hex_2);
	    return hex_2_str.toUpperCase();
	}

	public static synchronized boolean isStart(Context context){
		SharedPreferences shared = context.getSharedPreferences("status", Context.MODE_PRIVATE);
		if(!shared.contains("service")){
			return false;
		}
		
		return shared.getBoolean("service", false);
	}
	
	public static synchronized void start(Context context){
		Editor eEditor =context.getSharedPreferences("status", Context.MODE_PRIVATE).edit();
		eEditor.putBoolean("service", true);
		eEditor.commit(); 		
	}
	
	public static  synchronized void stop(Context context){
		Editor eEditor =context.getSharedPreferences("status", Context.MODE_PRIVATE).edit();
		eEditor.putBoolean("service", false);
		eEditor.commit();	  
	}
	

	public static synchronized boolean isLock(Context context){
		SharedPreferences shared = context.getSharedPreferences("status", Context.MODE_PRIVATE);
		if(!shared.contains("lock")){
			return false;
		}
		
		return shared.getBoolean("lock", false);
	}	
	
	public static synchronized void lock(Context context){
		Editor eEditor = context.getSharedPreferences("status", Context.MODE_PRIVATE).edit();
		eEditor.putBoolean("lock", true);
		eEditor.commit(); 		
	}
	
	public static  synchronized void unlock(Context context){
		Editor eEditor = context.getSharedPreferences("status", Context.MODE_PRIVATE).edit();
		eEditor.putBoolean("lock", false);
		eEditor.commit();	  
	}

	/**
	 * 
	 * @param context
	 * @param password
	 */
	public static synchronized void setPassword(Context context, String password){
		Editor eEditor = context.getSharedPreferences("status", Context.MODE_PRIVATE).edit();
		eEditor.putString("password", password);
		eEditor.commit(); 		
	}
	
	/**
	 * 
	 * @param context
	 * @return
	 */
	public static  synchronized String getPassword(Context context){
		SharedPreferences shared = context.getSharedPreferences("status", Context.MODE_PRIVATE);
		if(!shared.contains("password")){
			return "";
		}
		
		return shared.getString("password", "");
	}
	
    /**
     * 
     * @param context
     * @return
     */
    public static synchronized boolean isRegist(Context context){
        SharedPreferences shared = context.getSharedPreferences("status", Context.MODE_PRIVATE);
        return shared.contains("regist");
    }
}

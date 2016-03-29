package net.masaya3.ricetime.data;


import net.masaya3.ricetime.Utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

/**
 * 
 * @author tanaka
 *
 */
public class BeaconInfo {

	private static final String UUID_KEY = "uuid_key";
	private static final String LOCK_MAJOR_KEY = "lock_major_key";
    private static final String LOCK_MINOR_KEY = "lock_minor_key";	
    private static final String UNLOCK_MAJOR_KEY = "unlock_minor_key";	
    private static final String UNLOCK_MINOR_KEY = "unlock_minor_key";	
    private static final String REGIST = "regist";
    
	private static final String DEFAULT_UUID = "00000000-20DD-1001-B000-001C4DEF2E9A";
	private static final int DEFAULT_LOCK_MAJOR = 1;
    private static final int DEFAULT_LOCK_MINOR = 0;	
    private static final int DEFAULT_UNLOCK_MAJOR = 1;	
    private static final int DEFAULT_UNLOCK_MINOR = 1;	

	/** */
	public String uuid = DEFAULT_UUID;
	/** */
	public int lock_major = DEFAULT_LOCK_MAJOR;
	/** */	
	public int lock_minor = DEFAULT_LOCK_MINOR;

	/** */
	public int unlock_major = DEFAULT_UNLOCK_MAJOR;
	/** */	
	public int unlock_minor = DEFAULT_UNLOCK_MINOR;
	
	/**
	 * 
	 */
	public BeaconInfo(){

	}
	
	/**
	 * 
	 * @param info
	 * @return
	 */
	public boolean equals(BeaconInfo info){
		
		if(!uuid.equalsIgnoreCase(info.uuid)){
			return false;
		}
		
		if(lock_major != info.lock_major){
			return false;
		}
		
		if(lock_minor != info.lock_minor){
			return false;
		}

		if(unlock_major != info.unlock_major){
			return false;
		}

		if(unlock_minor != info.unlock_minor){
			return false;
		}
		
		return true;	
	}
	
	/**
	 * 
	 * @param messages
	 * @return
	 */
	public static BeaconInfo parse(NdefMessage[] messages){
		
		if(messages == null){
			return null;
		}
		
		
		if(messages.length == 0){
			return null;
		}
		
		NdefMessage message = messages[0];
		
		NdefRecord[] records = message.getRecords();
		if(records.length < 2){
			return null;
		}
		
		NdefRecord record = records[1];

		byte[] bytes =record.getPayload();
		if(bytes.length != 24){
			return null;
		}
		
		BeaconInfo info = new BeaconInfo();
		
		info.uuid =  Utils.intToHex2(bytes[0] & 0xff) 
				                + Utils.intToHex2(bytes[1] & 0xff)
				                + Utils.intToHex2(bytes[2] & 0xff)
				                + Utils.intToHex2(bytes[3] & 0xff)
				                + "-"
				                + Utils.intToHex2(bytes[4] & 0xff)
				                + Utils.intToHex2(bytes[5] & 0xff)
				                + "-"
				                + Utils.intToHex2(bytes[6] & 0xff)
				                + Utils.intToHex2(bytes[7] & 0xff)
				                + "-"
				                + Utils.intToHex2(bytes[8] & 0xff)
				                + Utils.intToHex2(bytes[9] & 0xff)
				                + "-"
				                + Utils.intToHex2(bytes[10] & 0xff)
				                + Utils.intToHex2(bytes[11] & 0xff)
				                + Utils.intToHex2(bytes[12] & 0xff)
				                + Utils.intToHex2(bytes[13] & 0xff)
				                + Utils.intToHex2(bytes[14] & 0xff)
				                + Utils.intToHex2(bytes[15] & 0xff);
		
		info.lock_major = Integer.parseInt(Utils.intToHex2(bytes[16] & 0xff) +  Utils.intToHex2(bytes[17] & 0xff));
		info.lock_minor = Integer.parseInt(Utils.intToHex2(bytes[18] & 0xff) +  Utils.intToHex2(bytes[19] & 0xff));
		info.unlock_major = Integer.parseInt(Utils.intToHex2(bytes[20] & 0xff) +  Utils.intToHex2(bytes[21] & 0xff));
		info.unlock_minor = Integer.parseInt(Utils.intToHex2(bytes[22] & 0xff) +  Utils.intToHex2(bytes[23] & 0xff));

		return info;
	}
	
	/**
	 * 
	 * @param id
	 * @param key
	 * @return
	 */
	public static BeaconInfo parse(String id, String key){
		
		if(id == null || key == null){
			return null;
		}
		
		if(id.length() != DEFAULT_UUID.length()){
			return null;
		}
		
		String[] baseIds = id.split("-");
		String[] defaultIds= id.split("-");
		
		if(baseIds.length != defaultIds.length){
			return null;
		}
		
		for(int i=0; i<defaultIds.length; i++){
			if(baseIds[i].length() != defaultIds[i].length()){
				return null;
			}
		}
		
		BeaconInfo info = new BeaconInfo();
		info.uuid = id;
		
		String keys[] = key.split("-");
		if(keys.length != 4){
			return null;
		}
		try{
			info.lock_major = Integer.parseInt(keys[0]);
			info.lock_minor = Integer.parseInt(keys[1]);
			info.unlock_major = Integer.parseInt(keys[2]);
			info.unlock_minor = Integer.parseInt(keys[3]);
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
		
		return info;
	}

    /**
     *
     * @param context
     * @param type
     */
    public synchronized void save(Context context){

        SharedPreferences.Editor eEditor =context.getSharedPreferences("beacon", Context.MODE_PRIVATE).edit();

        eEditor.putString(UUID_KEY, uuid);
        eEditor.putInt(LOCK_MAJOR_KEY, lock_major);
        eEditor.putInt(LOCK_MINOR_KEY, lock_minor);
        eEditor.putInt(UNLOCK_MAJOR_KEY, unlock_major);
        eEditor.putInt(UNLOCK_MINOR_KEY, unlock_minor);
        eEditor.putBoolean(REGIST, true);
        eEditor.commit();
    }
    
    /**
     * 
     * @param context
     * @return
     */
    public static boolean isRegist(Context context){
        SharedPreferences shared = context.getSharedPreferences("beacon", Context.MODE_PRIVATE);
        if(!shared.contains(REGIST)){
        	return false;
        }
        
        return shared.getBoolean(REGIST, false);
    }
     
    /**
     * 
     * @param context
     */
    public  static synchronized void unRegist(Context context){
        SharedPreferences.Editor eEditor =context.getSharedPreferences("beacon", Context.MODE_PRIVATE).edit();
        eEditor.putBoolean(REGIST, false); 	
        eEditor.commit();        
    }
    
     /**
     *
     * @param context
     * @param type
     */
    public static synchronized BeaconInfo load(Context context){
       BeaconInfo info = new BeaconInfo();

       SharedPreferences shared = context.getSharedPreferences("beacon", Context.MODE_PRIVATE);
       if(!shared.contains(REGIST)){
    	   return info;
       }
       info.uuid = getSharedString(shared, BeaconInfo.UUID_KEY, DEFAULT_UUID);
       info.lock_major = getSharedInt(shared, LOCK_MAJOR_KEY, DEFAULT_LOCK_MAJOR);
       info.lock_minor = getSharedInt(shared, LOCK_MINOR_KEY, DEFAULT_LOCK_MINOR);
       info.unlock_major = getSharedInt(shared, UNLOCK_MAJOR_KEY, DEFAULT_UNLOCK_MAJOR);
       info.unlock_minor = getSharedInt(shared, UNLOCK_MINOR_KEY, DEFAULT_UNLOCK_MINOR);
       
       return info;
    }
    
    /**
     * 
     * @param shared
     * @param key
     * @param default_value
     * @return
     */
    private static int getSharedInt(SharedPreferences shared , String key, int default_value){
	
        if(shared == null){
	        return default_value;
	    }
	
	    return shared.getInt(key, default_value);
    }
	  
    /**
     * 
     * @param shared
     * @param key
     * @param default_value
     * @return
     */
    private static String getSharedString(SharedPreferences shared , String key, String default_value){
	
    	if(shared == null){
	        return default_value;
	    }
	    return shared.getString(key, default_value);
    }
}

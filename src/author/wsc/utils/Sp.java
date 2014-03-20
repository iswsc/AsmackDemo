package author.wsc.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import author.wsc.entity.LoginVo;

public class Sp {
	
	private static Sp instance;
	private Context context;
	private final String USERNAME = "username";
	private final String PASSWORD = "password";
	private final String HOST = "host";
	private final String SERVERNAME = "servername";
	private final String PORT = "port";
	private Sp(Context context){
		this.context = context;
	}
	
	public static Sp getInstance(Context context){
		if(instance == null){
			instance = new Sp(context);
		}
		return instance;
	}
	
	
	public void saveServer(String username ,String password ,String host,String servername,String port){
		Editor editor = context.getSharedPreferences("config", 0).edit();
		editor.putString(USERNAME, username);
		editor.putString(PASSWORD, password);
		editor.putString(HOST, host);
		editor.putString(SERVERNAME, servername);
		editor.putString(PORT, port);
		editor.commit();
	}
	public LoginVo getServer(){
		SharedPreferences sp =  context.getSharedPreferences("config", 0);
		LoginVo vo = new LoginVo();
		vo.setUsername(sp.getString(USERNAME, ""));
		vo.setPassword(sp.getString(PASSWORD, ""));
		vo.setHostname(sp.getString(HOST, ""));
		vo.setServername(sp.getString(SERVERNAME, ""));
		vo.setPort(sp.getString(PORT, ""));
		return vo;
	}
}

package author.wsc.entity;

import java.io.Serializable;

import android.text.TextUtils;
import author.wsc.xmppmanager.XmppUtils;

public class ChatMsg implements Serializable{ 
	
	private int type;
	private String username;
	private String msg;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getJid(){
		if(!TextUtils.isEmpty(username)){
			return username + "@" +XmppUtils.SERVER_NAME;
		}
		return username;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
}

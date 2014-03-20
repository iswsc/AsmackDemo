package author.wsc.entity;

import org.jivesoftware.smack.packet.Presence;

/**
 * 用户信息
 * 
 * @author by_wsc
 * @email wscnydx@gmail.com
 * @date 日期：2013-9-1 时间：下午10:26:33
 */
public class UserInfoBase {

	private String jid;
	private String username;
	private String name;
	private String email;
	private String nickname;
	private int status;// 0 在线 1为忙碌 2为离开 3为隐身或离线
	public String getJid() {
		return jid;
	}
	public void setJid(String jid) {
		this.jid = jid;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public int getStatus() {
		return status;
	}
	/**
	 * 设置在线状态
	 * @param presence
	 * @author by_wsc
	 * @email wscnydx@gmail.com
	 * @date 日期：2013-9-1 时间：下午10:33:37
	 */
	public void setStatus(Presence presence) {
		Presence.Mode mode = presence.getMode();
		if(mode == null){//如果mode为空 则用Presence.Type.available 判断是不否在线
			if(Presence.Type.available.equals(presence.getType())){
				this.status = 0;
			}else{
				this.status = 3;
			}
		}else if(Presence.Mode.xa.equals(mode)){
			this.status = 3;
		}else if(Presence.Mode.dnd.equals(mode)){
			this.status =  2;
		}else if(Presence.Mode.away.equals(mode)){
			this.status = 1;
		}else{
			this.status = 0;
		}
	}
	
	@Override
	public String toString() {
		return "jid = " + jid + ",name = " + name
				+ ",email = " + email + ",username = " + username;
	}
	
}

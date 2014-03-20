package author.wsc.utils;

import author.wsc.xmppmanager.XmppUtils;

/**
 * 工具类
 * 
 * @author by_wsc
 * @email wscnydx@gmail.com
 * @date 日期：2013-4-19 时间：下午11:39:09
 */
public class Utils {

	
	/**
	 * 根据jid获取用户名
	 * @param jid
	 * @return
	 * @author by_wsc
	 * @email wscnydx@gmail.com
	 * @date 日期：2013-4-19 时间：下午11:38:49
	 */
	public static String getJidToUsername(String jid){
		
		return jid.split("@")[0];
	}
	
	public static String getUserNameToJid(String username){
		return username + "@" + XmppUtils.SERVER_NAME;
	}
}

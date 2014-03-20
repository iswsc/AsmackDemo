package author.wsc.utils;

import org.jivesoftware.smack.XMPPException;

import android.os.Handler;
import author.wsc.activity.MainActivity;
import author.wsc.xmppmanager.XmppUtils;

/**
 * xmpp动作
 * 
 * @author by_wsc
 * @email wscnydx@gmail.com
 * @date 日期：2013-4-17 时间：下午11:06:04
 */
public class XmppRunnable extends Thread {

	private Handler loginHandler;
	private int TYPE = 0;
	public static final int LOGIN = 1000;
	private int loginCode = 200;//成功
	private String[] up;
	
	/**
	 * 登录专用
	 * @param handler
	 * @param type
	 * @param up
	 */
	public XmppRunnable(Handler handler,int type,final String[] up){
		loginHandler = handler;
		TYPE = type;
		this.up = up;
	this.start();
	}
	
	@Override
	public void run() {
		switch (TYPE) {
		case LOGIN:
			loginHandler.sendEmptyMessage(MainActivity.DIALOG_SHOW);
			String username = up[0];
			String password = up[1];
			try {
				XmppUtils.getInstance().createConnection();
				XmppUtils.getInstance().getConnection().login(username, password, XmppUtils.RESOURCE);
			} catch (XMPPException ex) {
				ex.printStackTrace();
				if (ex.getXMPPError() != null) {
					loginCode = ex.getXMPPError().getCode();
				}
				//这里可能还有一些是没有写出来
				switch (loginCode) {
				case 409://重复登录
					loginCode = XmppUtils.LOGIN_ERROR_REPEAT;
					break;
				case 502://
					loginCode = XmppUtils.LOGIN_ERROR_NET;
					break;
				case 401://认证错误
					loginCode = XmppUtils.LOGIN_ERROR_PWD;
					break;
				default://未知
					loginCode = XmppUtils.LOGIN_ERROR;
					break;
				}
			} catch (Exception e){
				loginCode = XmppUtils.LOGIN_ERROR_NET;
			}
			Logs.w(XmppRunnable.class, "loginCode = " + loginCode);
			loginHandler.sendEmptyMessage(MainActivity.DIALOG_CANCLE);
			loginHandler.sendEmptyMessage(loginCode);
			break;

		}
	}

}

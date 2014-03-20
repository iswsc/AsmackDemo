package author.wsc.activity;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Registration;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import author.wsc.utils.MyToast;
import author.wsc.xmppmanager.XmppUtils;

import com.wscnydx.xmppdemo.R;

/**
 * 注册页面
 * 
 * @author by_wsc
 * @email wscnydx@gmail.com
 * @date 日期：2013-4-15 时间：下午11:32:29
 */
public class RegisterActivity extends Activity implements OnClickListener, TextWatcher{
	
	private EditText host;
	private EditText servername;
	private EditText port;
	private EditText username;
	private EditText password;
	private EditText repeatPassword;
	private Button register;
	
	Thread registerThread;
	private final int ERROR_CONN = 4001;
	private final int ERROR_REGISTER = 4002;
	private final int ERROR_REGISTER_REPEATUSER = 4003;
	private final int SUCCESS = 201;
	private final int LOADING = 202;
	
	ProgressDialog dialog;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			
			case ERROR_CONN:
				
				MyToast.showToast(RegisterActivity.this, "与服务器连接失败，请稍后再试！");
				
				break;
				
			case ERROR_REGISTER:
				
				MyToast.showToast(RegisterActivity.this, "注册失败，请重试");
				
				break;
			case ERROR_REGISTER_REPEATUSER:
				
				MyToast.showToast(RegisterActivity.this, "账号已存在！");
				
				break;
			case SUCCESS:
				
				MyToast.showToast(RegisterActivity.this, "注册成功！");
				SystemClock.sleep(1000);
				finish();
				break;
			case LOADING:
				
				dialog = new ProgressDialog(RegisterActivity.this);
				dialog.setMessage("loading...");
				dialog.setOnDismissListener(new OnDismissListener() {
					
					@Override
					public void onDismiss(DialogInterface dialog) {
						register.setEnabled(true);
							try {
								registerThread.interrupt();
							} catch (Exception e) {
								e.printStackTrace();
							}
					}
				});
				dialog.show();
				register.setEnabled(false);
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register);
		
		
		
		findView();
		setListener();
	}
	private void findView() {
		host = (EditText) findViewById(R.id.et_register_host);
		servername = (EditText) findViewById(R.id.et_register_servername);
		port = (EditText) findViewById(R.id.et_register_port);
		username = (EditText) findViewById(R.id.et_register_user);
		password = (EditText) findViewById(R.id.et_register_pass1);
		repeatPassword= (EditText) findViewById(R.id.et_register_pass2);
		register = (Button) findViewById(R.id.bt_regedit);
		
	}
	private void setListener() {
		register.setOnClickListener(this);
		username.addTextChangedListener(this);
		password.addTextChangedListener(this);
		repeatPassword.addTextChangedListener(this);
		
	}
	@Override
	public void onClick(View v) {
		//两次密码一致
		if(TextUtils.equals(password.getText().toString().trim(), 
				      repeatPassword.getText().toString().trim())){
			XmppUtils.SERVER_HOST = host.getText().toString().trim();
			XmppUtils.SERVER_NAME = servername.getText().toString().trim();
			XmppUtils.SERVER_PORT = Integer.parseInt(port.getText().toString().trim());
			registerThread = new Thread(){
				public void run() {
					try {
						XmppUtils.getInstance().createConnection();
					} catch (Exception e) {
						mHandler.sendEmptyMessage(ERROR_CONN);
						e.printStackTrace();
						return;
					}
					try {
					Registration registration = new Registration();
					registration.setType(IQ.Type.SET);
					registration.setTo(XmppUtils.SERVER_NAME);
					registration.setUsername(username.getText().toString().trim());
					registration.setPassword(password.getText().toString().trim());
					PacketFilter filter = new AndFilter(new PacketIDFilter(registration.getPacketID()),new PacketTypeFilter(IQ.class));
					PacketCollector collector = XmppUtils.getInstance().getConnection().createPacketCollector(filter); 
					XmppUtils.getInstance().getConnection().sendPacket(registration); 
					Packet response = collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
					collector.cancel();
			        if (response == null) {
			            throw new XMPPException("No response from server on status set.");
			        }
			        if (response.getError() != null) {
			            throw new XMPPException(response.getError());
			        }
					} catch (XMPPException e) {
						if(e.getXMPPError() != null && e.getXMPPError().getCode() == 409){
							mHandler.sendEmptyMessage(ERROR_REGISTER_REPEATUSER);
						}else{
							
							mHandler.sendEmptyMessage(ERROR_REGISTER);
						}
						e.printStackTrace();
						return;
					}
					mHandler.sendEmptyMessage(SUCCESS);
					
				};
			};
			registerThread.start();
		}else{
			MyToast.showToast(this, "两次密码不一致");
		}
	}
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		
	}
	@Override
	public void afterTextChanged(Editable s) {
		//用户名密码不能为空
		if(username.getText().toString().trim().length() > 0
		   &&	
		   password.getText().toString().trim().length() > 0
		   &&
		   repeatPassword.getText().toString().trim().length() > 0){
			register.setEnabled(true);
		}else{
			register.setEnabled(false);
		}
	}

}

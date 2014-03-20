package author.wsc.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import author.wsc.entity.LoginVo;
import author.wsc.service.XmppService;
import author.wsc.utils.Logs;
import author.wsc.utils.MyToast;
import author.wsc.utils.Sp;
import author.wsc.utils.XmppRunnable;
import author.wsc.xmppmanager.XmppUtils;

import com.wscnydx.xmppdemo.R;

/**
 * 
 * @author by_wsc
 * @email wscnydx@gmail.com
 * @date 日期：2013-4-15 时间：下午10:28:33
 */
public class MainActivity extends Activity implements TextWatcher, OnClickListener {

	private EditText username;
	private EditText password;
	private EditText host;
	private EditText port;
	private EditText servername;
	private Button login;
	private Button register;
	public static final int DIALOG_SHOW = 0;
	public static final int DIALOG_CANCLE = 1;
	ProgressDialog dialog ;
	private Handler loginHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case XmppUtils.LOGIN_ERROR:
				MyToast.showToast(MainActivity.this, "登录失败");
				break;
			case XmppUtils.LOGIN_ERROR_NET:
				MyToast.showToast(MainActivity.this, "连接服务器失败");
				break;
			case XmppUtils.LOGIN_ERROR_PWD:
				MyToast.showToast(MainActivity.this, "密码错误");
				break;
			case XmppUtils.LOGIN_ERROR_REPEAT:
				MyToast.showToast(MainActivity.this, "重复登录");
				break;
			case 200:
				MyToast.showToast(MainActivity.this, "登录成功");
				Intent intentService = new Intent(MainActivity.this,XmppService.class);
				startService(intentService);
				Intent intent = new Intent(MainActivity.this,FriendListActivity.class);
				startActivity(intent);
				finish();
				break;

			case DIALOG_SHOW:
				dialog = new ProgressDialog(MainActivity.this);
				dialog.setMessage("Loding");
				dialog.show();
				break;
			case DIALOG_CANCLE:
				if(dialog != null){
					dialog.dismiss();
					dialog = null;
				}
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		findView();
		
		setListener();
		
		LoginVo vo = Sp.getInstance(this).getServer();
		
		username.setText(vo.getUsername());
		password.setText(vo.getPassword());
		host.setText(vo.getHostname());
		servername.setText(vo.getServername());
		port.setText(vo.getPort());
	}

	private void setListener() {
		username.addTextChangedListener(this);
		password.addTextChangedListener(this);
		login.setOnClickListener(this);
		register.setOnClickListener(this);
	}

	private void findView() {
		username = (EditText) findViewById(R.id.et_user);
		password = (EditText) findViewById(R.id.et_pass);
		host = (EditText) findViewById(R.id.et_host);
		port = (EditText) findViewById(R.id.et_port);
		servername = (EditText) findViewById(R.id.et_servername);
		login = (Button) findViewById(R.id.bt_login);
		register = (Button) findViewById(R.id.bt_regedit);
		
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public void afterTextChanged(Editable s) {
		if(username.getText().toString().trim().length() > 0
		   &&
		   password.getText().toString().trim().length() > 0){
			login.setEnabled(true);
		}else{
			login.setEnabled(false);
		}
			
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		switch (v.getId()) {
		
		case R.id.bt_login://登录按钮
			
			try {
				XmppUtils.SERVER_HOST = host.getText().toString();
				XmppUtils.SERVER_NAME = servername.getText().toString();
				XmppUtils.SERVER_PORT = Integer.parseInt(port.getText().toString());
				if(TextUtils.isEmpty(XmppUtils.SERVER_HOST)
				|| TextUtils.isEmpty(XmppUtils.SERVER_NAME)
				|| XmppUtils.SERVER_PORT < 0 ){
					MyToast.showToast(this, "请检查输入信息是否有误,或为空");	
					return;
				}
			} catch (Exception e) {
				MyToast.showToast(this, "请检查输入信息是否有误");
				return;
			}
			
			Logs.i(MainActivity.class, "host = " + XmppUtils.SERVER_HOST);
			Logs.i(MainActivity.class, "servername = " + XmppUtils.SERVER_NAME);
			Logs.i(MainActivity.class, "port = " + XmppUtils.SERVER_PORT);
			
			final String [] up = new String[]{username.getText().toString().trim(),
											  password.getText().toString().trim()};
			Sp.getInstance(this).saveServer(username.getText().toString().trim(), password.getText().toString().trim(),
					XmppUtils.SERVER_HOST , 
					XmppUtils.SERVER_NAME, 
					String.valueOf(XmppUtils.SERVER_PORT));
			
			new XmppRunnable(loginHandler, XmppRunnable.LOGIN, up);
			break;

		case R.id.bt_regedit://注册按钮
			
			intent = new Intent(this, RegisterActivity.class);
			startActivity(intent);
			break;
		}
		
		intent = null;
	}
	
}

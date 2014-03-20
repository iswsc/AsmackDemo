package author.wsc.activity;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import author.wsc.db.DbHelper;
import author.wsc.entity.ChatMsg;
import author.wsc.entity.FriendInfo;
import author.wsc.utils.Logs;
import author.wsc.utils.MyToast;
import author.wsc.xmppmanager.XmppUtils;

import com.wscnydx.xmppdemo.R;

/**
 * 聊天窗口
 * 
 * @author by_wsc
 */
public class Chat extends Activity {

	private static final String TAG = "Chat";

	public static final int SEND_MSG = 1;
	public static final int RECEIVER_MSG = 2;
	private static final int NOTIF_MSG = 3;

	private static final int TYPE_SIZE = 3;// listview中有多少布局

	private EditText chat_msg;
	private Button send_msg;

	private ListView chat_list;
	private ChatListAdapter adapter;
	private FriendInfo info;
	private FriendInfo myInfo;

	private List<ChatMsg> chatMsgList = new ArrayList<ChatMsg>();
	private ChatMsg msg_me;
	private ChatMsg msg_other;

	private LayoutInflater inflater;
	
	private MessageReceiver receiver;
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case SEND_MSG:

				break;
			case RECEIVER_MSG:

				break;
			case NOTIF_MSG:
				adapter.notifyDataSetChanged();
				chat_list.setSelection(chatMsgList.size());// 设置移动到最后一行
				break;

			}
			super.handleMessage(msg);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.chat);

		if (getIntent().getExtras() != null
				&& getIntent().getExtras().getSerializable("info") != null) {
			info = (FriendInfo) getIntent().getExtras().getSerializable("info");
		} else {
			MyToast.showToast(this, "获取好友信息失败");
			finish();
		}

		chat_msg = (EditText) findViewById(R.id.chat_msg);
		send_msg = (Button) findViewById(R.id.send_msg);
		chat_list = (ListView) findViewById(R.id.chat_list);
		((TextView) findViewById(R.id.title)).setText(info.getUsername());
		inflater = LayoutInflater.from(this);
		adapter = new ChatListAdapter();
		chatMsgList = DbHelper.getInstance(this).getChatMsgByUserName(info.getUsername());
		chat_list.setAdapter(adapter);
		chat_list.setSelection(chatMsgList.size());
		send_msg.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String sendmsg = chat_msg.getText().toString().trim();
				try {
					Message message = new Message(info.getJid(), Type.chat);
					message.setBody(sendmsg);
					XmppUtils.getInstance().getConnection().sendPacket(message);
				} catch (Exception e1) {
					e1.printStackTrace();
					Toast.makeText(Chat.this, "没有连网", Toast.LENGTH_SHORT).show();
				}
				msg_me = new ChatMsg();
				chat_msg.setText("");
				msg_me.setMsg(sendmsg);
				msg_me.setType(SEND_MSG);
				msg_me.setUsername(info.getUsername());
				chatMsgList.add(msg_me);
				DbHelper.getInstance(Chat.this).saveChatMsg(msg_me);
				msg_me = null;
				sendmsg = null;
				mHandler.sendEmptyMessage(NOTIF_MSG);
			}
		});

		chat_msg.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (TextUtils.isEmpty(s.toString())) {
					send_msg.setEnabled(false);
				} else {
					send_msg.setEnabled(true);
				}

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				if (TextUtils.isEmpty(s.toString())) {
					send_msg.setEnabled(false);
				} else {
					send_msg.setEnabled(true);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (TextUtils.isEmpty(s.toString())) {
					send_msg.setEnabled(false);
				} else {
					send_msg.setEnabled(true);
				}
			}
		});

		receiver = new MessageReceiver();
		IntentFilter filter = new IntentFilter("msg_in");
		registerReceiver(receiver, filter);
		
	}

	class ChatListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return chatMsgList.size();
		}

		@Override
		public ChatMsg getItem(int position) {
			return chatMsgList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {

			return chatMsgList.get(position).getType();
		}

		@Override
		public int getViewTypeCount() {
			return TYPE_SIZE;// size要大于布局个数
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Holder holder = null;
			int type = getItemViewType(position);
			if (convertView == null) {
				switch (type) {
				case SEND_MSG:
					convertView = inflater.inflate(R.layout.chat_me, null);
					holder = new Holder();
					holder.msg = (TextView) convertView
							.findViewById(R.id.msg_me);
					convertView.setTag(holder);
					Logs.i(Chat.class, "chat_me");
					break;
				case RECEIVER_MSG:
					convertView = inflater.inflate(R.layout.chat_other, null);
					holder = new Holder();
					holder.msg = (TextView) convertView
							.findViewById(R.id.msg_other);
					holder.icon = (Button) convertView
							.findViewById(R.id.header_icon);
					convertView.setTag(holder);
					Logs.i(Chat.class, "chat_other");
					break;
				}
			} else {
				holder = (Holder) convertView.getTag();
			}
			ChatMsg chatMsg = chatMsgList.get(position);

			switch (type) {
			case SEND_MSG:
				holder.msg.setText(chatMsg.getMsg());

				break;
			case RECEIVER_MSG:
				holder.msg.setText(chatMsg.getMsg());
				holder.icon.setBackgroundResource(R.drawable.h091);
				break;
			}

			return convertView;
		}

		class Holder {
			TextView msg;
			Button icon;
		}
	}

	/*class MyChatManagerListener implements ChatManagerListener {

		@Override
		public void chatCreated(org.jivesoftware.smack.Chat chat,
				boolean createdLocally) {
			chat.addMessageListener(new MyMessageListener());

		}
	}

	class MyMessageListener implements MessageListener {

		@Override
		public void processMessage(org.jivesoftware.smack.Chat chat,
				Message message) {
			Logs.i(Chat.class, message.getBody());
			Logs.i(Chat.class, message.getFrom());

			msg_other = new ChatMsg();
			String receivermsg = message.getBody().trim();
			msg_other.setMsg(receivermsg);
			msg_other.setType(RECEIVER_MSG);
			chatMsgList.add(msg_other);
			msg_other = null;
			receivermsg = null;
			mHandler.sendEmptyMessage(NOTIF_MSG);
		}

	}
*/
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
	
	private class MessageReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			if("msg_in".equals(intent.getAction())){
				ChatMsg chatMsg = (ChatMsg) intent.getExtras().getSerializable("msg_in");
				if(info.getUsername().equals(chatMsg.getUsername())){
					
					chatMsgList.add(chatMsg);
					mHandler.sendEmptyMessage(NOTIF_MSG);
				}
			}
		}
		
	}
	
}

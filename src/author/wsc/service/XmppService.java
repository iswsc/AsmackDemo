package author.wsc.service;

import java.util.Collection;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.Presence.Mode;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import author.wsc.activity.Chat;
import author.wsc.activity.FriendListActivity;
import author.wsc.db.DbHelper;
import author.wsc.entity.ChatMsg;
import author.wsc.entity.FriendInfo;
import author.wsc.utils.Logs;
import author.wsc.utils.MyToast;
import author.wsc.utils.Utils;
import author.wsc.xmppmanager.XmppUtils;

public class XmppService extends Service {

	XMPPConnection connection;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			ChatMsg chatMsg = (ChatMsg) msg.obj;
			MyToast.showToast(FriendListActivity.friendListActivity, "用户【" + chatMsg.getUsername() + "】，body = " + chatMsg.getMsg());
		};
	};
	@Override
	public void onCreate() {
		
		try {
			connection = XmppUtils.getInstance().getConnection();
			connection.addPacketListener(new PacketListener() {
				
				@Override
				public void processPacket(Packet packet) {
					if(packet instanceof Message){//信息监听
						Message msg = (Message) packet;
						if(Message.Type.chat.equals(msg.getType())){//是一对一聊天信息
							if(!TextUtils.isEmpty(msg.getBody())){
								Logs.i(XmppService.class, "收到一条信息 xml = " + msg.toXML());
								Intent intent = new Intent();
								intent.setAction("msg_in");
								ChatMsg chatMsg = new ChatMsg();
								chatMsg.setMsg(msg.getBody());
								chatMsg.setType(2);
								chatMsg.setUsername(Utils.getJidToUsername(msg.getFrom()));
								DbHelper.getInstance(XmppService.this).saveChatMsg(chatMsg);
								intent.putExtra("msg_in", chatMsg);
								android.os.Message message = new android.os.Message();
								message.obj = chatMsg;
								mHandler.sendMessage(message);
								sendBroadcast(intent);
								
							}
							
						}else if(Message.Type.groupchat.equals(msg.getType())){//多人聊天
							
						}
					}else if(packet instanceof Presence){
						Presence presence = (Presence) packet;
							Logs.i(XmppService.class, "收到一条状态 xml = " + presence.toXML());
							
					}
				}
			}, null);
		} catch (XMPPException e) {
			
			e.printStackTrace();
		}
		
		try {
			connection.getRoster().addRosterListener(new RosterListener() {
				
				//好友在线状态改变
				@Override
				public void presenceChanged(Presence presence) {
					
					Logs.i(XmppService.class, "presenceChanged  username = " + Utils.getJidToUsername(presence.getFrom())
												+ " ,在线状态 = " + presence.getMode());
					
				}
				
				//好友内容更新了
				@Override
				public void entriesUpdated(Collection<String> addresses) {
					
					for(String add : addresses){
						Logs.i(XmppService.class, " updaye = " + add);
					}
				}
				
				//删除好友
				@Override
				public void entriesDeleted(Collection<String> addresses) {
					for(String add : addresses){
						Logs.i(XmppService.class, " del = " + add);
						if(FriendListActivity.adapter != null){
							FriendListActivity.friendListActivity.deleteFriend(Utils.getJidToUsername(add));
						}
					}
					
				}
				//添加好友
				@Override
				public void entriesAdded(Collection<String> addresses) {
					for(String add : addresses){
						Logs.i(XmppService.class, " add = " + add);
						Presence response = new Presence(Presence.Type.subscribed);
						response.setTo(add);
						response.setMode(Mode.chat); // 用户状态
						connection.sendPacket(response);

						String[] mGroupName = { "Friends" };
						try {
							createEntry(add, Utils.getJidToUsername(add), mGroupName);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		connection.addConnectionListener(new ConnectionListener() {
			
			@Override
			public void reconnectionSuccessful() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void reconnectionFailed(Exception e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void reconnectingIn(int seconds) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void connectionClosedOnError(Exception e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void connectionClosed() {
				
			}
		});
		
	}
	
	@Override
	public void onDestroy() {
		Logs.i(XmppService.class, "onDestroy");
		super.onDestroy();
	}
	
	/**
	 * 邀请人向被邀请人发出一个添加好友的消息
	 * 
	 * @param user
	 *            被邀请人的email
	 * @param name
	 *            被邀请人的昵称
	 * @param groups
	 *            要添加的群组
	 * @param callMsg
	 *            招呼消息
	 * @param isInvaite
	 *            该数据是否为邀请
	 * @throws XMPPException
	 * @author by_wsc
	 * @email wscnydx@gmail.com
	 * @date 日期：2013-4-20 时间：上午3:54:13
	 */
	public void createEntry(String user, String name, String[] groups) throws XMPPException {
		if (TextUtils.isEmpty(user) || groups == null || groups.length == 0) {
			return;
		}
		// ----------- 加上昵称
		name = user.substring(0, user.indexOf("@"));
		// -------------
		RosterPacket rosterPacket = new RosterPacket();
		rosterPacket.setType(IQ.Type.SET);
		RosterPacket.Item item = new RosterPacket.Item(user, name);
		if (groups != null) {
			for (String group : groups) {
				if (!TextUtils.isEmpty(group)) {
					item.addGroupName(group);
				}
			}
		}
		rosterPacket.addRosterItem(item);
		PacketCollector collector = connection.createPacketCollector(
				new PacketIDFilter(rosterPacket.getPacketID()));
		connection.sendPacket(rosterPacket);
		IQ response = (IQ) collector.nextResult(SmackConfiguration
				.getPacketReplyTimeout());
		collector.cancel();
		if (response == null) {
			throw new XMPPException("No response from the server.");
		} else if (response.getType() == IQ.Type.ERROR) {
			throw new XMPPException(response.getError());
		}

		Presence presencePacket = new Presence(Presence.Type.subscribe);
		presencePacket.setTo(user);
		connection.sendPacket(presencePacket);

		Presence response2 = new Presence(Presence.Type.available);
		response2.setTo(user);
		response2.setMode(Mode.chat);
		connection.sendPacket(response);
		FriendInfo fInfo = new FriendInfo();
		fInfo.setUsername(Utils.getJidToUsername(user));
		android.os.Message message = new android.os.Message();
		message.what = FriendListActivity.ADD_FRIEND;
		message.obj = fInfo;
		FriendListActivity.friendListActivity.mHandler.sendMessage(message);
	}
	
}

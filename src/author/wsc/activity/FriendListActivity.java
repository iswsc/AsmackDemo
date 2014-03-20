package author.wsc.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import author.wsc.entity.FriendInfo;
import author.wsc.entity.GroupInfo;
import author.wsc.entity.UserInfoBase;
import author.wsc.service.XmppService;
import author.wsc.utils.Logs;
import author.wsc.utils.Utils;
import author.wsc.xmppmanager.XmppUtils;

import com.wscnydx.xmppdemo.R;

import de.measite.smack.AndroidDebugger;

/**
 * 好友列表
 * 
 * @author by_wsc
 * @email wscnydx@gmail.com
 * @date 日期：2013-4-18 时间：上午12:35:35
 */
public class FriendListActivity extends Activity implements OnGroupClickListener, OnChildClickListener {
	private static final String TAG = "FriendListActivity";
	private LayoutInflater mChildInflater;
	private ExpandableListView listView;
	private List<GroupInfo> groupList;
	private List<FriendInfo> friendList;
	public static MyAdapter adapter;
	public static FriendListActivity friendListActivity;
	FriendInfo friendInfo;
	GroupInfo groupInfo;

	private ProgressDialog dialog;
	
	public final static int NOTIF_UI = 1000;
	private final int DIALOG_SHOW = 1001;
	private final int DIALOG_CANCLE = 1002;
	public static final int ADD_FRIEND = 1003;
	
	public Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case NOTIF_UI:
				adapter.notifyDataSetChanged();
				break;
			case DIALOG_SHOW:
				dialog = new ProgressDialog(FriendListActivity.this);
				dialog.setMessage("loading");
				dialog.show();
				break;
			case DIALOG_CANCLE:
				if(dialog != null){
					dialog.dismiss();
					dialog = null;
				}
				break;
			case ADD_FRIEND:
				FriendInfo friendInfo = (FriendInfo) msg.obj;
				Logs.i(FriendListActivity.class, "add friend username = " + friendInfo.getUsername());
				groupList.get(0).getFriendInfoList().add(friendInfo);
				adapter.notifyDataSetChanged();
				break;
			
			}
		};
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		friendListActivity = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.friendlist);
		listView = (ExpandableListView) findViewById(R.id.contact_list_view);
		registerForContextMenu(listView);
		AndroidDebugger.printInterpreted = true;
		
		
		try {
			
			loadFriend();
		} catch (Exception e) {
			
			e.printStackTrace();
			Intent intent = new Intent(this,MainActivity.class);
			startActivity(intent);
			finish();
			Toast.makeText(this, "出错啦",0).show();
			return;
		}
		adapter = new MyAdapter(this);
		listView.setAdapter(adapter);
		listView.setOnGroupClickListener(this);
		listView.setOnChildClickListener(this);
		listView.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				return false;
			}
		});
		new Thread(){
			public void run() {
				XmppUtils.getInstance().sendOnLine();
			};
		}.start();
		
		listView.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				
				return false;
			}
		});
	}

	public void loadFriend() {
		try {

			XMPPConnection conn = XmppUtils.getInstance().getConnection();
			Roster roster = conn.getRoster();
			Collection<RosterGroup> groups = roster.getGroups();

			groupList = new ArrayList<GroupInfo>();
			
			

			for (RosterGroup group : groups) {
				groupInfo = new GroupInfo();
				friendList = new ArrayList<FriendInfo>();
				groupInfo.setGroupName(group.getName());
				Collection<RosterEntry> entries = group.getEntries();
				for (RosterEntry entry : entries) {
					if("both".equals(entry.getType().name())){//只添加双边好友 
						friendInfo = new FriendInfo();
						friendInfo.setUsername(Utils.getJidToUsername(entry.getUser()));
						friendList.add(friendInfo);
						friendInfo = null;
						
					}
				}
				groupInfo.setFriendInfoList(friendList);
				groupList.add(groupInfo);
				groupInfo = null;
			}
		if(groupList.isEmpty()){
			groupInfo = new GroupInfo();
			groupInfo.setGroupName("Friends");
			groupInfo.setFriendInfoList(new ArrayList<FriendInfo>());
			groupList.add(groupInfo);
			groupInfo = null;
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		adapter.notifyDataSetChanged();
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		XmppUtils.getInstance().closeConn();
		Intent intent = new Intent(this, XmppService.class);
		stopService(intent);
//		DbHelper.getInstance(this).closeDb();
		friendListActivity = null;
		super.onDestroy();
	}
	public class MyAdapter extends BaseExpandableListAdapter {
		Context context;
		public MyAdapter(Context context){
			mChildInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		class FriendHolder{
			TextView name;
			ImageView iv;
		}
		
		@Override
		public int getGroupCount() {
			// TODO Auto-generated method stub
			return groupList.size();
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			// TODO Auto-generated method stub
			return groupList.get(groupPosition).getFriendInfoList().size();
		}

		@Override
		public GroupInfo getGroup(int groupPosition) {
			
			return groupList.get(groupPosition);
		}
		public GroupInfo getGroup(String groupName) {
			GroupInfo groupInfo = null;
			if(getGroupCount() > 0){
				for(int i = 0,j = getGroupCount();i< j;i++){
					GroupInfo holder = (GroupInfo) getGroup(i);
					if(TextUtils.isEmpty(holder.getGroupName())){
						groupList.remove(holder);
					}else{
						if(holder.getGroupName().equals(groupInfo)){
							groupInfo = holder;
						}
					}
				}
			}
			return groupInfo;
		}

		@Override
		public FriendInfo getChild(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return groupList.get(groupPosition).getFriendInfoList()
					.get(childPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			// TODO Auto-generated method stub
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return childPosition;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			FriendHolder holder;
			if(convertView == null){
				holder = new FriendHolder();
				
				convertView = mChildInflater.inflate(R.layout.friend_group_item,null);
				holder.name =  (TextView) convertView.findViewById(R.id.friend_group_list_name);
				holder.iv = (ImageView) convertView.findViewById(R.id.friend_group_list_icon);
				convertView.setTag(holder);
			}else{
				holder = (FriendHolder) convertView.getTag();
			}
			String groupname = groupList.get(groupPosition).getGroupName();
			holder.name.setText(groupname);
			if(isExpanded){
				holder.iv.setBackgroundResource(R.drawable.sc_group_expand);
			}else{
				holder.iv.setBackgroundResource(R.drawable.sc_group_unexpand);
				
			}
			
			return convertView;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			FriendHolder holder;
				if(convertView == null){
					holder = new FriendHolder();
					convertView = mChildInflater.inflate(R.layout.friend_child_item,null);
					holder.name =  (TextView) convertView.findViewById(R.id.friend_nickname);
					convertView.setTag(holder);
				}else{
					holder = (FriendHolder) convertView.getTag();
				}
				FriendInfo groupname = groupList.get(groupPosition).getFriendInfoList().get(childPosition);
				holder.name.setText(groupname.getUsername());
				if(isLastChild){
					listView.setItemChecked(groupPosition, true);
				}
				return convertView;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return true;
		}

	}

	@Override
	public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {
		
		return false;
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		FriendInfo info = groupList.get(groupPosition).getFriendInfoList().get(childPosition);
		Intent intent = new Intent(this,Chat.class);
		intent.putExtra("info", info);
		startActivity(intent);
		return false;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(Menu.NONE, Menu.FIRST + 1, 1,"添加好友").setIcon(R.drawable.itheima);
		menu.add(Menu.NONE, Menu.FIRST + 2, 1,"退出登录").setIcon(R.drawable.itheima);
		menu.add(Menu.NONE, Menu.FIRST + 3, 1,"获取好友").setIcon(R.drawable.itheima);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST + 1:
			View view = View.inflate(this, R.layout.dialog, null);
			final PopupWindow mPopupWindow = new PopupWindow(view, LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT, true);

			mPopupWindow.setWindowLayoutMode(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT);

			mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
			mPopupWindow.showAtLocation(((Activity) this).getWindow()
					.getDecorView(), Gravity.CENTER, 0, 0);

			mPopupWindow.setAnimationStyle(R.style.animationmsg);
			mPopupWindow.setFocusable(true);
			mPopupWindow.setTouchable(true);
			mPopupWindow.setOutsideTouchable(true);
			mPopupWindow.update();
			final EditText addFriend = (EditText) view.findViewById(R.id.addfriend);
//			final EditText addGroup = (EditText) view.findViewById(R.id.addgroup);
			Button sure = (Button) view.findViewById(R.id.sure);
			Button cancle = (Button) view.findViewById(R.id.cancle);
			sure.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final String username = addFriend.getText().toString();
//					final String group = addGroup.getText().toString().trim();
					Logs.i(FriendListActivity.class, "input username = " + username);
					new Thread(){
						public void run() {
							try {
								mHandler.sendEmptyMessage(DIALOG_SHOW);							// jid = q3@wscnydx
								XmppUtils.getInstance().getConnection().getRoster().createEntry(Utils.getUserNameToJid(username), username, new String[]{/*"".equals(group)? */"Friends"/* : group*/});
								mHandler.sendEmptyMessage(DIALOG_CANCLE);
							} catch (Exception e) {
								mHandler.sendEmptyMessage(DIALOG_CANCLE);
								
								e.printStackTrace();
							}
						};
					}.start();
					mPopupWindow.dismiss();
				}
				
			});
			cancle.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mPopupWindow.dismiss();
					
				}
			});
			break;
		case Menu.FIRST + 2:
			
			finish();
			
			break;
		case Menu.FIRST + 3:
			
			try {
				List<UserInfoBase> list = XmppUtils.getInstance().searchUser("@", true, true, true);
				
				for (UserInfoBase infoBase : list) {
					Logs.d(FriendListActivity.class, infoBase.toString());
				}
			} catch (XMPPException e) {
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
			}
			
		break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * 长按事件
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if(menuInfo instanceof ExpandableListView.ExpandableListContextMenuInfo){
			
			ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
			
			int type = ExpandableListView.getPackedPositionType(info.packedPosition);
			
			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
				
				int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
				int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				final FriendInfo dInfo = groupList.get(groupPos).getFriendInfoList().get(childPos);
				builder.setTitle("删除【" + dInfo.getNickname() + "】好友？");
				builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {


					@Override
					public void onClick(DialogInterface dialog, int which) {
						
							new Thread(){
								public void run() {
									try {
										mHandler.sendEmptyMessage(DIALOG_SHOW);
										RosterEntry entry  = XmppUtils.getInstance().getConnection().getRoster().getEntry(dInfo.getJid());
										XmppUtils.getInstance().getConnection().getRoster().removeEntry(entry);
										deleteFriend(dInfo.getUsername());
										mHandler.sendEmptyMessage(DIALOG_CANCLE);
										mHandler.sendEmptyMessage(NOTIF_UI);
									} catch (Exception e) {
										mHandler.sendEmptyMessage(DIALOG_CANCLE);
										e.printStackTrace();
									}
								};
							}.start();
					}
				});
				builder.create();
				builder.show();
			}
		}
	}
	public void deleteFriend(String username){
		for (int i = 0; i < groupList.size(); i++) {
			for (int j = 0; j < groupList.get(i).getFriendInfoList().size(); j++) {
				FriendInfo friendInfo = groupList.get(i).getFriendInfoList().get(j);
				if(username.equals(friendInfo.getUsername())){
					groupList.get(i).getFriendInfoList().remove(friendInfo);
					mHandler.sendEmptyMessage(NOTIF_UI);
					return;
				}
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		Intent i = new Intent(Intent.ACTION_MAIN);
		// i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.addCategory(Intent.CATEGORY_HOME);
		startActivity(i);
	}
	
}

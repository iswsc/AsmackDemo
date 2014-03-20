package author.wsc.xmppmanager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.ReportedData.Column;
import org.jivesoftware.smackx.ReportedData.Row;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.BytestreamsProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import author.wsc.entity.FriendInfo;
import author.wsc.entity.UserInfoBase;

public class XmppUtils {

	public static String SERVER_HOST = "192.168.0.103";//你openfire服务器所在的ip
	public static  String SERVER_NAME = "wsc";//设置openfire时的服务器名
	public static int    SERVER_PORT = 5222;//服务端口 可以在openfire上设置
	public static String SERVER_SEARCH =  "search." + SERVER_NAME;
	public static final String RESOURCE = "wsc";
	public static final int LOGIN_ERROR_REPEAT = 409;//重复登录
	public static final int LOGIN_ERROR_NET = 502;//服务不可用
	public static final int LOGIN_ERROR_PWD = 401;//密码错误 或其他
	public static final int LOGIN_ERROR= 404;//未知错误
	private static XMPPConnection conn;
	private static XmppUtils instance;
	private static FriendInfo currFriendInfo;
	public static XmppUtils getInstance(){
		
		if(instance == null){
			instance = new XmppUtils();
		}
		return instance;
	}
	
	/**
	 * 创建XMPP连接实例
	 * 
	 * @return
	 * @throws org.jivesoftware.smack.XMPPException
	 */
	public void createConnection() throws XMPPException {
		if (null == conn || !conn.isAuthenticated()) {
			XMPPConnection.DEBUG_ENABLED = true;//开启DEBUG模式
			//配置连接
			ConnectionConfiguration config = new ConnectionConfiguration(
					SERVER_HOST, SERVER_PORT,
					SERVER_NAME);
			config.setReconnectionAllowed(true);
			config.setSendPresence(true);
			config.setSASLAuthenticationEnabled(true);
			
			
			conn = new XMPPConnection(config);
			conn.connect();//连接到服务器
			//配置 各种Provider 如果不配置 则会无法解析数据
			configureConnection(ProviderManager.getInstance());

		}
	}
	
	public XMPPConnection getConnection()throws XMPPException{
		return conn;
	}
	
	/**
	 * 是否已经登录
	 * @return
	 * @author by_wsc
	 * @email wscnydx@gmail.com
	 * @date 日期：2013-4-17 时间：下午11:03:14
	 */
	public boolean isLogin(){
		if(conn == null) return false;//连接未生成
		else if(!conn.isConnected()) return false;//连接未生效
		else if(!conn.isAuthenticated()) return false;//连接未认证
		return true;
	}
	
	public void sendOnLine(){
		if(conn == null || !conn.isConnected() || !conn.isAuthenticated())
			throw new RuntimeException("连接有问题");
		
		Presence presence = new Presence(Type.available);
		presence.setMode(Mode.chat);
		conn.sendPacket(presence);
		
	}
	
	/**
	 * 关闭XmppConnection连接
	 * 
	 * @author by_wsc
	 * @email wscnydx@gmail.com
	 * @date 日期：2013-4-19 时间：下午10:23:09
	 */
	public void closeConn() {
		if (null != conn && conn.isConnected()) {
			Presence pres = new Presence(Presence.Type.unavailable);
			conn.disconnect(pres);
			conn = null;
				
		}
	}
	
	/**
	 * xmpp配置
	 * @param pm
	 * @author by_wsc
	 * @email wscnydx@gmail.com
	 * @date 日期：2013-4-15 时间：下午11:02:20
	 */
	private void configureConnection(ProviderManager pm) {
		// Private Data Storage
		pm.addIQProvider("query", "jabber:iq:private",
				new PrivateDataManager.PrivateDataIQProvider());

		// Time
		try {
			pm.addIQProvider("query", "jabber:iq:time",
					Class.forName("org.jivesoftware.smackx.packet.Time"));
		} catch (Exception e) {
			e.printStackTrace();
			// Logs.v(TAG,
			// "Can't load class for org.jivesoftware.smackx.packet.Time");
		}

		// Roster Exchange
		pm.addExtensionProvider("x", "jabber:x:roster",
				new RosterExchangeProvider());

		// Message Events
		pm.addExtensionProvider("x", "jabber:x:event",
				new MessageEventProvider());

		// Chat State
		pm.addExtensionProvider("active",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("composing",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("paused",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("inactive",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("gone",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		// XHTML
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",
				new XHTMLExtensionProvider());

		// Group Chat Invitations
		pm.addExtensionProvider("x", "jabber:x:conference",
				new GroupChatInvitation.Provider());

		// Service Discovery # Items //解析房间列表
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());

		// Service Discovery # Info //某一个房间的信息
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());

		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());

		// MUC User
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());

		// MUC Admin
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new MUCAdminProvider());

		// MUC Owner
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new MUCOwnerProvider());

		// Delayed Delivery
		pm.addExtensionProvider("x", "jabber:x:delay",
				new DelayInformationProvider());

		// Version
		try {
			pm.addIQProvider("query", "jabber:iq:version",
					Class.forName("org.jivesoftware.smackx.packet.Version"));
		} catch (ClassNotFoundException e) {
			// Not sure what's happening here.
		}
		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
				new OfflineMessageRequest.Provider());

		// Offline Message Indicator
		pm.addExtensionProvider("offline",
				"http://jabber.org/protocol/offline",
				new OfflineMessageInfo.Provider());

		// Last Activity
		pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());

		// User Search
		pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());

		// SharedGroupsInfo
		pm.addIQProvider("sharedgroup",
				"http://www.jivesoftware.org/protocol/sharedgroup",
				new SharedGroupsInfo.Provider());

		// JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses",
				"http://jabber.org/protocol/address",
				new MultipleAddressesProvider());
		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si",
				new StreamInitiationProvider());

		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
				new BytestreamsProvider());

		// pm.addIQProvider("open", "http://jabber.org/protocol/ibb",
		// new IBBProviders.Open());
		//
		// pm.addIQProvider("close", "http://jabber.org/protocol/ibb",s
		// new IBBProviders.Close());
		//
		// pm.addExtensionProvider("data", "http://jabber.org/protocol/ibb",
		// new IBBProviders.Data());

		// Privacy
		pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());

		pm.addIQProvider("command", "http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider());
		pm.addExtensionProvider("malformed-action",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.MalformedActionError());
		pm.addExtensionProvider("bad-locale",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadLocaleError());
		pm.addExtensionProvider("bad-payload",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadPayloadError());
		pm.addExtensionProvider("bad-sessionid",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadSessionIDError());
		pm.addExtensionProvider("session-expired",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.SessionExpiredError());
		
	}
	
	
	/**
	 * 搜索好友
	 * @param searchContent 搜索内容
	 * @param searchUsername 是否搜索用户名
	 * @param searchName 是否搜索姓名
	 * @param searchemail 是否搜索邮箱
	 * @return
	 * @throws XMPPException
	 * @author by_wsc
	 * @email wscnydx@gmail.com
	 * @date 日期：2013-9-1 时间：下午4:32:12
	 */
	public List<UserInfoBase> searchUser(String searchContent,boolean searchUsername,boolean searchName,boolean searchemail) throws XMPPException{
		
		String value1 = searchUsername ? "1" : "0";
		String value2 = searchName ? "1" : "0";
		String value3 = searchemail ? "1" : "0";
		final String searchXml = "<query xmlns=\"jabber:iq:search\"><x xmlns=\"jabber:x:data\" type=\"submit\">" +
				"<field var=\"FORM_TYPE\" type=\"hidden\">" +
				"<value>jabber:iq:search</value></field>" +
				"<field var=\"search\" type=\"text-single\"><value>" + StringUtils.escapeForXML(searchContent == null ? "" : searchContent) + "</value></field>" +
				"<field var=\"Username\" type=\"boolean\"><value>" + value1 + "</value></field>" +
				"<field var=\"Name\" type=\"boolean\"><value>" + value2 + "</value></field>" +
				"<field var=\"Email\" type=\"boolean\"><value>" + value3 + "</value></field></x></query>";
		
		IQ iq = new IQ() {
			
			@Override
			public String getChildElementXML() {
				return searchXml;
			}
		};
		PacketCollector collector = conn.createPacketCollector(new PacketIDFilter(iq.getPacketID()));
		iq.setTo(SERVER_SEARCH);
		iq.setType(IQ.Type.SET);
	        conn.sendPacket(iq);

	        IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

	        // Cancel the collector.
	        collector.cancel();
	        if (response == null) {
	            throw new XMPPException("No response from server on status set.");
	        }

	        ReportedData reportedData = ReportedData.getReportedDataFrom(response);
	        List<UserInfoBase> list = new ArrayList<UserInfoBase>();
	        Iterator<Row> iterator = reportedData.getRows();
	        while(iterator.hasNext()){
	        	Row rows = iterator.next();
	        	Iterator<String> iterator2 = rows.getValues("jid");
	        	UserInfoBase userInfoBase = new UserInfoBase();
	        	while(iterator2.hasNext()){
	        		String jid = iterator2.next();
	        		System.out.println("jid = " + jid);
	        		userInfoBase.setJid(jid);
	        	}
	        	Iterator<String> iterator3 = rows.getValues("username");
	        	int i = 0;
	        	while(iterator3.hasNext()){
	        		String username = iterator3.next();
	        		System.out.println("username = " + username);
	        		userInfoBase.setUsername(username);
	        		i++;
	        	}
	        	i = 0;
	        	Iterator<String> iterator4 = rows.getValues("email");
	        	while(iterator4.hasNext()){
	        		String email = iterator4.next();
	        		System.out.println("email = " + email);
	        		userInfoBase.setEmail(email);
	        		i++;
	        	}
	        	i = 0;
	        	Iterator<String> iterator5 = rows.getValues("name");
	        	while(iterator5.hasNext()){
	        		String name = iterator5.next();
	        		System.out.println("name = " + name);
	        		userInfoBase.setName(name);
	        		i++;
	        	}
	        	list.add(userInfoBase);
	        }
	        return list;
	}
	
}

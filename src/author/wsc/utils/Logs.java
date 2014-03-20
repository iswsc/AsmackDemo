package author.wsc.utils;

/**
 * 自定义LOG <font color="red">所有LOG日志必须用这个类,不用系统的</font>
 * 
 * @author by_wsc
 * @email wscnydx@gmail.com
 * @date 日期：2013-4-17 时间：下午11:32:54
 */
public class Logs {

	public static final int VERBOSE = 2;

	public static final int DEBUG = 3;

	public static final int INFO = 4;

	public static final int WARN = 5;

	public static final int ERROR = 6;

	public static final int ASSERT = 7;

	/**
	 * 
	 * @param tag
	 * @param msg
	 * @param show
	 * @author wsc
	 */
	public static void v(Class clazz, String msg) {
		if (ASSERT > VERBOSE)
			android.util.Log.v(clazz.toString(), msg);
	}

	public static void d(Class clazz, String msg) {
		if (ASSERT > DEBUG)
			android.util.Log.d(clazz.toString(), msg);
	}

	public static void i(Class clazz, String msg) {
		if (ASSERT > INFO )
			android.util.Log.i(clazz.toString(), msg);
	}

	public static void w(Class clazz, String msg) {
		if (ASSERT > WARN )
			android.util.Log.w(clazz.toString(), msg);
	}

	public static void e(Class clazz, String msg) {
		if (ASSERT > ERROR)
			android.util.Log.e(clazz.toString(), msg);
	}
}

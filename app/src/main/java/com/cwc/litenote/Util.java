package com.cwc.litenote;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;

public class Util 
{
    SharedPreferences mPref_vibration;
    Context mContext;
    Activity mActivity;
    String mEMailString;
    private static DB mDb;
    static String NEW_LINE = "\r" + System.getProperty("line.separator");

	static int STYLE_DEFAULT = 1;
    
	static int ACTIVITY_CREATE = 0;
    static int ACTIVITY_VIEW_NOTE = 1;
    static int ACTIVITY_EDIT_NOTE = 2;
    static int ACTIVITY_IMPORT = 3;
    static int ACTIVITY_SELECT_PICTURE = 4;
    
    static boolean DEBUG_MODE = false; 
    static boolean RELEASE_MODE = !DEBUG_MODE;
    //set mode
//    static boolean CODE_MODE = RELEASE_MODE;
    static boolean CODE_MODE = DEBUG_MODE;
    
    int defltBgClr;
    int defltTxtClr;

    // style
    // 0,2,4,6,8: dark background, 1,3,5,7,9: light background
	static int[] mBG_ColorArray = new int[]{Color.rgb(34,34,34), //#222222
											Color.rgb(255,255,255),
											Color.rgb(38,87,51), //#265733
											Color.rgb(186,249,142),
											Color.rgb(87,38,51),//#572633
											Color.rgb(249,186,142),
											Color.rgb(38,51,87),//#263357
											Color.rgb(142,186,249),
											Color.rgb(87,87,51),//#575733
											Color.rgb(249,249,140)};
	static int[] mText_ColorArray = new int[]{Color.rgb(255,255,255),
											  Color.rgb(0,0,0),
											  Color.rgb(255,255,255),
											  Color.rgb(0,0,0),
											  Color.rgb(255,255,255),
											  Color.rgb(0,0,0),
											  Color.rgb(255,255,255),
											  Color.rgb(0,0,0),
											  Color.rgb(255,255,255),
											  Color.rgb(0,0,0)};

    
    public Util(){};
    
	public Util(FragmentActivity activity) {
		mContext = activity;
		mActivity = activity;
	}
	
	public Util(Context context) {
		mContext = context;
	}
	
	// set vibration time
	void vibrate()
	{
		mPref_vibration = mContext.getSharedPreferences("vibration", 0);
    	if(mPref_vibration.getString("KEY_ENABLE_VIBRATION","yes").equalsIgnoreCase("yes"))
    	{
			Vibrator mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
			if(mPref_vibration.getString("KEY_VIBRATION_TIME","25") != "")
			{
				int vibLen = Integer.valueOf(mPref_vibration.getString("KEY_VIBRATION_TIME","25"));
				mVibrator.vibrate(vibLen); //length unit is milliseconds
				System.out.println("vibration len = " + vibLen);
			}
    	}
	}
	
	// save to SD card: for checked pages
	String saveToSdCard(String filename, List<Boolean> checkedArr,boolean enableToast)
	{   
		//first row text
		String data ="";
		//get data from DB
		if(checkedArr == null)
			data = queryDB(data,null);// all pages
		else
			data = queryDB(data,checkedArr);
		
		// sent data
		data = addRssVersionAndChannel(data);
		mEMailString = data;
		
		writeToSdCardFile(data,filename);
		
		return mEMailString;
	}
	
	// save to SD card: for NoteView class
	String saveStringToSdCard(String filename, String curString)
	{   
		//sent data
		String data = "";
		data = data.concat(curString);
		mEMailString = data;
		
		writeToSdCardFile(data,filename);
		
		return mEMailString;
	}
	
	
	void writeToSdCardFile(String data,String filename)
	{
	    // SD card path + "/" + directory path
	    String dirString = Environment.getExternalStorageDirectory().toString() + 
	    		              "/" + 
	    		              Util.getAppName(mContext);
	    
		File dir = new File(dirString);
		if(!dir.isDirectory())
			dir.mkdir();
		File file = new File(dir, filename);
		file.setReadOnly();
		
//		FileWriter fw = null;
//		try {
//			fw = new FileWriter(file);
//		} catch (IOException e1) {
//			System.out.println("_FileWriter error");
//			e1.printStackTrace();
//		}
//		BufferedWriter bw = new BufferedWriter(fw);
		
		BufferedWriter bw = null;
		OutputStreamWriter osw = null;
		
		int BUFFER_SIZE = 8192;
		try {
			osw = new OutputStreamWriter(new FileOutputStream(file.getPath()), "UTF-8");
			bw = new BufferedWriter(osw,BUFFER_SIZE);
			
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		try {
			bw.write(data);
			bw.flush();
			osw.close();
			bw.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
    /**
     * Query current data base
     * @param checkedArr 
     * 
     */
    String queryDB(String data, List<Boolean> checkedArr)
    {
    	String curData = data;
    	
		String strFinalPageViewed_tableId = Util.getPref_lastTimeView_NotesTableId((Activity) mContext);
        DB.setNotesTableId(strFinalPageViewed_tableId);
    	
    	mDb = new DB(mContext);
    	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
    	int tabCount = DB.getTabsCount();
    	mDb.doClose();
    	for(int i=0;i<tabCount;i++)
    		
    	{
    		// null: all pages
        	if((checkedArr == null ) || ( checkedArr.get(i) == true  ))
    		{
	        	// set Sent string Id
				List<Long> rowArr = new ArrayList<Long>();
        		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
				DB.setNotesTableId(String.valueOf(DB.getTab_NotesTableId(i)));
				mDb.doClose();
				
        		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
	    		for(int k=0;k<mDb.getNotesCount();k++)
	    		{
    				rowArr.add(k,(long) mDb.getNoteId(k));
	    		}
	    		mDb.doClose();
	    		curData = curData.concat(getSendStringWithXmlTag(rowArr));
    		}
    	}
    	return curData;
    	
    }
    
    // get current time string
    static String getCurrentTimeString()
    {
		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONDAY)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		
//		int hour = cal.get(Calendar.HOUR);//12h 
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		int mSec = cal.get(Calendar.MILLISECOND);
		
		String strTime = year 
				+ "-" + String.format(Locale.US,"%02d", month)
				+ "-" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "_" + String.format(Locale.US,"%02d", hour)
				+ "-" + String.format(Locale.US,"%02d", min)
				+ "-" + String.format(Locale.US,"%02d", sec) 
				+ "_" + String.format(Locale.US,"%03d", mSec);
//		System.out.println("time = "+  strTime );
		return strTime;
    }
    
    // get time string
    static String getTimeString(Long time)
    {
		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONDAY)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		int hour = cal.get(Calendar.HOUR);//12h 
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		
		String strTime = year 
				+ "-" + String.format(Locale.US,"%02d", month)
				+ "-" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "    " + String.format(Locale.US,"%02d", hour)
				+ ":" + String.format(Locale.US,"%02d", min)
				+ ":" + String.format(Locale.US,"%02d", sec) ;
//		System.out.println("time = "+  strTime );
		
		return strTime;
    }
    
//    void deleteAttachment(String mAttachmentFileName)
//    {
//		// delete file after sending
//		String attachmentPath_FileName = Environment.getExternalStorageDirectory().getPath() + "/" +
//										 mAttachmentFileName;
//		File file = new File(attachmentPath_FileName);
//		boolean deleted = file.delete();
//		if(deleted)
//			System.out.println("delete file is OK");
//		else
//			System.out.println("delete file is NG");
//    }
    

	void markCurrent(DialogInterface dlgIntf)
	{
		mDb = new DB(mActivity);
	    ListView listView = ((AlertDialog) dlgIntf).getListView();
	    final ListAdapter originalAdapter = listView.getAdapter();
	    final int style = Util.getCurrentPageStyle(mActivity);
        CheckedTextView textViewDefault = new CheckedTextView(mActivity) ;
        defltBgClr = textViewDefault.getDrawingCacheBackgroundColor();
        defltTxtClr = textViewDefault.getCurrentTextColor();

	    listView.setAdapter(new ListAdapter()
	    {
	        @Override
	        public int getCount() {
	            return originalAdapter.getCount();
	        }
	
	        @Override
	        public Object getItem(int id) {
	            return originalAdapter.getItem(id);
	        }
	
	        @Override
	        public long getItemId(int id) {
	            return originalAdapter.getItemId(id);
	        }
	
	        @Override
	        public int getItemViewType(int id) {
	            return originalAdapter.getItemViewType(id);
	        }
	
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            View view = originalAdapter.getView(position, convertView, parent);
	            //set CheckedTextView in order to change button color
	            CheckedTextView textView = (CheckedTextView)view;
	            mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
	            if(DB.getTab_NotesTableId(position) == Integer.valueOf(DB.getNotesTableId()))
	            {
		            textView.setTypeface(null, Typeface.BOLD_ITALIC);
		            textView.setBackgroundColor(mBG_ColorArray[style]);
		            textView.setTextColor(mText_ColorArray[style]);
			        if(style%2 == 0)
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
			        else
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_light);
	            }
	            else
	            {
		        	textView.setTypeface(null, Typeface.NORMAL);
		            textView.setBackgroundColor(defltBgClr);
		            textView.setTextColor(defltTxtClr);
		            textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
	            }
				mDb.doClose();
	            return view;
	        }

	        @Override
	        public int getViewTypeCount() {
	            return originalAdapter.getViewTypeCount();
	        }

	        @Override
	        public boolean hasStableIds() {
	            return originalAdapter.hasStableIds();
	        }
	
	        @Override
	        public boolean isEmpty() {
	            return originalAdapter.isEmpty();
	        }

	        @Override
	        public void registerDataSetObserver(DataSetObserver observer) {
	            originalAdapter.registerDataSetObserver(observer);
	
	        }
	
	        @Override
	        public void unregisterDataSetObserver(DataSetObserver observer) {
	            originalAdapter.unregisterDataSetObserver(observer);
	
	        }
	
	        @Override
	        public boolean areAllItemsEnabled() {
	            return originalAdapter.areAllItemsEnabled();
	        }
	
	        @Override
	        public boolean isEnabled(int position) {
	            return originalAdapter.isEnabled(position);
	        }
	    });
	}
	
	// get App name
	static public String getAppName(Context context)
	{
		return context.getResources().getString(R.string.app_name);
	}
	
	// get style
	static public int getNewPageStyle(Context context)
	{
		SharedPreferences mPref_style;
		mPref_style = context.getSharedPreferences("style", 0);
		return mPref_style.getInt("KEY_STYLE",STYLE_DEFAULT);
	}
	
	
	// set button color
	static String[] mItemArray = new String[]{"1","2","3","4","5","6","7","8","9","10"};
    public static void setButtonColor(RadioButton rBtn,int iBtnId)
    {
    	if(iBtnId%2 == 0)
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_dark);
    	else
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_light);
		rBtn.setBackgroundColor(Util.mBG_ColorArray[iBtnId]);
		rBtn.setText(mItemArray[iBtnId]);
		rBtn.setTextColor(Util.mText_ColorArray[iBtnId]);
    }
	
    // get current page style
	static public int getCurrentPageStyle(Context context)
	{
		int style = 0;
		mDb = new DB(context);
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		style = mDb.getTabStyle(TabsHostFragment.mCurrentTabIndex);
		mDb.doClose();
		
		return style;
	}

	// get style count
	static public int getStyleCount()
	{
		return mBG_ColorArray.length;
	}
	

	// set notes table id of last time view
	public static void setPref_lastTimeView_NotesTableId(Activity act, int id )
	{
//		SharedPreferences mPref = act.getSharedPreferences("last_time_view", 0);
//        mPref.edit().putString("KEY_LAST_TIME_VIEW_NOTES_TABLE_ID",
//        										String.valueOf(id))
//        							.commit(); //note: point to table
		
	  SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
      String keyPrefix = "DRAWER";
      mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
      int notesTableId =  DB.getTab_NotesTableId(TabsHostFragment.mCurrentTabIndex);
	  mDb.doClose();
	  String keyName = keyPrefix.concat(String.valueOf(DrawerActivity.mCurrentDrawerIndex));
      pref.edit().putInt(keyName, notesTableId).commit();
		
	}
	
	// get notes table id of last time view
	public static String getPref_lastTimeView_NotesTableId(Activity act)
	{
	       // get final viewed table Id
//		SharedPreferences mPref = act.getSharedPreferences("last_time_view", 0);
//		String id = mPref.getString("KEY_LAST_TIME_VIEW_NOTES_TABLE_ID","1");
		
		SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
		String keyPrefix = "DRAWER";
		String keyName = keyPrefix.concat(String.valueOf(DrawerActivity.mCurrentDrawerIndex));
		int notesTableId = pref.getInt(keyName, 1); // notes table Id: default is 1
		return String.valueOf(notesTableId);
		
//		return id;
	}
	
	// get Send String with XML tag
	static String getSendStringWithXmlTag(List<Long> rowArr)
	{
        String PAGE_TAG_B = "<page>";
        String TAB_TAG_B = "<tabname>";
        String TAB_TAG_E = "</tabname>";
        String TITLE_TAG_B = "<title>";
        String TITLE_TAG_E = "</title>";
        String BODY_TAG_B = "<body>";
        String BODY_TAG_E = "</body>";
        String PAGE_TAG_E = "</page>";
        
        String sentString = NEW_LINE;

    	// when page has tab name only, no notes
    	if(rowArr.size() == 0)
    	{
        	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        	sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
	        sentString = sentString.concat(NEW_LINE + TAB_TAG_B + DB.getCurrentTabTitle() + TAB_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + TITLE_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + BODY_TAG_B +  BODY_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + PAGE_TAG_E );
    		sentString = sentString.concat(NEW_LINE);
    		mDb.doClose();
    	}
    	else
    	{
	        for(int i=0;i< rowArr.size();i++)
	        {
	        	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		    	Cursor cursorNote = mDb.queryNote(rowArr.get(i));
		    	
		        String strTitleEdit = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB.KEY_NOTE_TITLE));
		        
		        String strBodyEdit = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB.KEY_NOTE_BODY));
		    	
		        int mark = cursorNote.getInt(cursorNote.getColumnIndexOrThrow(DB.KEY_NOTE_MARKING));
		        String srtMark = (mark == 1)? "[s]":"[n]";
		        
		        if(i==0)
		        {
		        	sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
		        	sentString = sentString.concat(NEW_LINE + TAB_TAG_B + DB.getCurrentTabTitle() + TAB_TAG_E );
		        }
		        
		        sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + srtMark + strTitleEdit + TITLE_TAG_E);
		        sentString = sentString.concat(NEW_LINE + BODY_TAG_B + strBodyEdit + BODY_TAG_E);
		    	sentString = sentString.concat(NEW_LINE);
		    	if(i==rowArr.size()-1)
		        	sentString = sentString.concat(NEW_LINE +  PAGE_TAG_E);
		    		
		    	mDb.doClose();
	        }
    	}
    	return sentString;
	}
	
	// add RSS tag
	public static String addRssVersionAndChannel(String str)
	{
        String RSS_TAG_B = NEW_LINE + "<rss version=\"2.0\">";
        String RSS_TAG_E = "</rss>";
        String CHANNEL_TAG_B = "<channel>";
        String CHANNEL_TAG_E = "</channel>";
        
        String data = RSS_TAG_B + CHANNEL_TAG_B;
        data = data.concat(str);
		data = data.concat(CHANNEL_TAG_E + RSS_TAG_E);
		
		return data;
	}

	// trim XML tag
	public String trimXMLtag(String string) {
		string = string.replace("<rss version=\"2.0\">","");
		string = string.replace("<channel>","");
		string = string.replace("<page>","");
		string = string.replace("<tabname>","--- Page: ");
		string = string.replace("</tabname>"," ---");
		string = string.replace("<title>","Title: ");
		string = string.replace("</title>","");
		string = string.replace("<body>","Body: ");
		string = string.replace("</body>","");
		string = string.replace("[s]","");
		string = string.replace("[n]","");
		string = string.replace("</page>"," ");
		string = string.replace("</channel>","");
		string = string.replace("</rss>","");
		string = string.trim();
		return string;
	}
	
	static String getDisplayNameByUri(Uri uri, Activity activity)
	{
//		String audioID = null, title, artist;
		String display_name;

        String[] proj = { 
//        	      MediaStore.Audio.Media._ID, 
//                MediaStore.Audio.Media.ARTIST,
//                MediaStore.Audio.Media.TITLE,
//                MediaStore.Audio.Media.DATA, 
                	MediaStore.Audio.Media.DISPLAY_NAME,
//                MediaStore.Audio.Media.DURATION
                };
        
                Cursor cursor = activity.getContentResolver().query(uri, proj, null, null, null);

                cursor.moveToFirst(); //reset the cursor
                int col_index=-1;
                do
                {
//                  col_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
//                  title = cursor.getString(col_index);
//                  col_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
//                  artist = cursor.getString(col_index);
                  col_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                  display_name = cursor.getString(col_index);
                }while(cursor.moveToNext());
                cursor.close();
//                System.out.println("artist_name = " + title);
//                System.out.println("artist_band = " + artist);
                System.out.println("display_name = " + display_name);
                return display_name;
	}
	
	static boolean isUriExist(String uriString, Activity activity)
	{
		Uri imageUri = Uri.parse(uriString);
		boolean bFileExist = false;
		try
		{
			activity.getContentResolver().openInputStream(imageUri);
			bFileExist = true;
		}
		catch (Exception e) 
		{
	    }				
		
		return bFileExist;
	}
	
	static boolean isUriExist(String uriString, Context context)
	{
		Uri imageUri = Uri.parse(uriString);
		boolean bFileExist = false;
		try
		{
			context.getContentResolver().openInputStream(imageUri);
			bFileExist = true;
		}
		catch (Exception e) 
		{
	    }				
		
		return bFileExist;
	}
}

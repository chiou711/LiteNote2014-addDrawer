package com.cwc.litenote;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

public class TabsHostFragment extends Fragment 
{
    public static final String ARG_PLANET_NUMBER = "planet_number";
    
    static FragmentTabHost mTabHost; 
    static int mTabCount;
	static String TAB_SPEC_PREFIX = "tab";
	static String TAB_SPEC;
	boolean bTabNameByDefault = true; 
	// for DB
	static DB mDb;
	private static Cursor mTabCursor;
	
	static SharedPreferences mPref_FinalPageViewed;
	private static SharedPreferences mPref_delete_warn;
	static int mFinalPageViewed_TabIndex;
	public static int mCurrentTabIndex;
	private static ArrayList<String> mTabIndicator_ArrayList = new ArrayList<String>();
	static int mFirstExist_TabId =0;
	static int mLastExist_TabId =0;
	static HorizontalScrollView mHorScrollView;
    private static String[] mDrawerTitles = new String[DB.DEFAULT_DRAWER_COUNT];

	public TabsHostFragment(){};
	
	@Override
	public void onCreate(final Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
    	System.out.println("TabsHostFragment / _Create");
        
        if(Util.CODE_MODE == Util.RELEASE_MODE) 
        {
        	OutputStream nullDev = new OutputStream() 
            {
                public  void    close() {}
                public  void    flush() {}
                public  void    write(byte[] b) {}
                public  void    write(byte[] b, int off, int len) {}
                public  void    write(int b) {}
            }; 
            System.setOut( new PrintStream(nullDev));
        }
        
        //Log.d below can be disabled by applying proguard
        //1. enable proguard-android-optimize.txt in project.properties
        //2. be sure to use newest version to avoid build error
        //3. add the following in proguard-project.txt
        /*-assumenosideeffects class android.util.Log {
        public static boolean isLoggable(java.lang.String, int);
        public static int v(...);
        public static int i(...);
        public static int w(...);
        public static int d(...);
        public static int e(...);
    	}
        */
        Log.d("test log tag","start app"); 
    } 

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	System.out.println("TabsHostFragment / _onCreateView");
    	
    	View rootView = inflater.inflate(R.layout.activity_main, container, false);
    	
    	//select drawer
        int i = getArguments().getInt(ARG_PLANET_NUMBER);
//        String planet = getResources().getStringArray(R.array.planets_array)[i];
        String planet = mDrawerTitles[i];
        getActivity().setTitle(planet);
        
        setRootView(rootView);
        
        setTabHost();
        setTab(getActivity());
        return rootView;
    }	
    
    static View mRootView;
	private void setRootView(View rootView) {
		mRootView = rootView;
	}
	
	private static View getRootView()
	{
		return mRootView;
	}

	
	/**
	 * set tab host
	 * 
	 */
	protected void setTabHost()
	{
		// declare tab widget
        TabWidget tabWidget = (TabWidget) getRootView().findViewById(android.R.id.tabs);
        
        // declare linear layout
        LinearLayout linearLayout = (LinearLayout) tabWidget.getParent();
        
        // set horizontal scroll view
        HorizontalScrollView horScrollView = new HorizontalScrollView(getActivity());
        horScrollView.setLayoutParams(new FrameLayout.LayoutParams(
								            FrameLayout.LayoutParams.MATCH_PARENT,
								            FrameLayout.LayoutParams.WRAP_CONTENT));
        linearLayout.addView(horScrollView, 0);
        linearLayout.removeView(tabWidget);
        
        horScrollView.addView(tabWidget);
        horScrollView.setHorizontalScrollBarEnabled(true); //set scroll bar
        horScrollView.setHorizontalFadingEdgeEnabled(true); // set fading edge
        mHorScrollView = horScrollView;

		// tab host
        mTabHost = (FragmentTabHost)getRootView().findViewById(android.R.id.tabhost);
        
        //for android-support-v4.jar
        //mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent); 
        
        //add frame layout for android-support-v13.jar
        //Note: must use getChildFragmentManager() for nested fragment
        mTabHost.setup(getActivity(), getChildFragmentManager(), android.R.id.tabcontent);
	}
	
	static void setTab(Activity activity)
	{
        //set tab indicator
    	setTabIndicator(activity);
    	
    	// set tab listener
    	setTabChangeListener(activity);
    	setTabEditListener(activity);
	}
	
	/**
	 * set tab indicator
	 * 
	 */
	protected static void setTabIndicator(final Activity activity)
	{
        // get final viewed table Id
		String tblId = Util.getPref_lastTimeView_NotesTableId(activity);
		
        System.out.println("TabsHost / setTabIndicator / strFinalPageViewed_tableId = " + tblId);
		Context context = activity.getApplicationContext();

		DB.setNotesTableId(tblId);
		mDb = new DB(context);
		
		// insert when table is empty, activated only for the first time 
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		if(DB.getTabsCount() == 0)
		{//??? need to change the items?
			String tabs_table = DB.getCurrentTabsTableName();
			mDb.insertTab(tabs_table,"N1",1,"N1",1,0);
			mDb.insertTab(tabs_table,"N2",2,"N2",2,1); 
			mDb.insertTab(tabs_table,"N3",3,"N3",3,2); 
			mDb.insertTab(tabs_table,"N4",4,"N4",4,3); 
			mDb.insertTab(tabs_table,"N5",5,"N5",5,4); 
		}
		mDb.doClose();
		
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		mTabCursor = DB.mTabCursor;
		mTabCount = DB.getTabsCount();

		// get first tab id and last tab id
		int i = 0;
		while(i < mTabCount)
    	{
    		mTabIndicator_ArrayList.add(i,DB.getTabTitle(i));  
    		
			if(mTabCursor.isFirst())
			{
				mFirstExist_TabId = mDb.getTabId(i) ;
			}
			if(mTabCursor.isLast())
			{
				mLastExist_TabId = mDb.getTabId(i) ;
			}
			i++;
    	}
    	
		// get final view table id of last time
		for(int iPosition =0;iPosition<mTabCount;iPosition++)
		{
			if(Integer.valueOf(tblId) == 
					DB.getTab_NotesTableId(iPosition))
			{
				mFinalPageViewed_TabIndex = iPosition;	// starts from 0
			}
		}
		mDb.doClose();
		
	
    	//add tab
//        mTabHost.getTabWidget().setStripEnabled(true); // enable strip
        i = 0;
        while(i < mTabCount)
        {
        	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
            TAB_SPEC = TAB_SPEC_PREFIX.concat(String.valueOf(mDb.getTabId(i)));
        	mDb.doClose();
            mTabHost.addTab(mTabHost.newTabSpec(TAB_SPEC)
									.setIndicator(mTabIndicator_ArrayList.get(i)),
							NoteFragment.class, //interconnection
							null);
            
            //set round corner and background color
            mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
            int style = mDb.getTabStyle(i);
            mDb.doClose();
    		switch(style)
    		{
    			case 0:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_0);
    				break;
    			case 1:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_1);
    				break;
    			case 2:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_2);
    				break;
    			case 3:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_3);
    				break;
    			case 4:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_4);
    				break;	
    			case 5:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_5);
    				break;	
    			case 6:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_6);
    				break;	
    			case 7:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_7);
    				break;	
    			case 8:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_8);
    				break;		
    			case 9:
    				mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_9);
    				break;		
    			default:
    				break;
    		}
    		
    		
            //set text color
	        TextView tv = (TextView) mTabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
		    if((style%2) == 1)
    		{	
		        tv.setTextColor(Color.argb(255,0,0,0));
    		}
           	else
           	{
		        tv.setTextColor(Color.argb(255,255,255,255));
           	}
        	
            // set tab text center
	    	int tabCount = mTabHost.getTabWidget().getTabCount();
	    	for (int j = 0; j < tabCount; j++) {
	    	    final View view = mTabHost.getTabWidget().getChildTabViewAt(j);
	    	    if ( view != null ) {
	    	        //  get title text view
	    	        final View textView = view.findViewById(android.R.id.title);
	    	        if ( textView instanceof TextView ) {
	    	            ((TextView) textView).setGravity(Gravity.CENTER);
	    	            ((TextView) textView).setSingleLine(true);
	    	            ((TextView) textView).setPadding(6, 0, 6, 0);
	    	            ((TextView) textView).setMinimumWidth(96);
	    	            ((TextView) textView).setMaxWidth(UtilImage.getScreenWidth(activity)/2);
	    	            textView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
	    	        }
	    	    }
	    	}
	    	i++;
        }
        
        setTabMargin(activity);

		mCurrentTabIndex = mFinalPageViewed_TabIndex;
		
		//set background color to selected tab 
		mTabHost.setCurrentTab(mCurrentTabIndex); 
        
		// scroll to last view
        mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
		        mPref_FinalPageViewed = activity.getSharedPreferences("last_time_view", 0);
		        int scrollX = mPref_FinalPageViewed.getInt("KEY_LAST_TIME_VIEW_SCROLL_X", 0);
	        	mHorScrollView.scrollTo(scrollX, 0);
	            openFragmentByTabSpec(mTabHost.getCurrentTabTag(),activity);
	        } 
	    });
        
	}
	

	static void setTabMargin(Activity activity)
	{
    	mTabHost.getTabWidget().setShowDividers(TabWidget.SHOW_DIVIDER_MIDDLE);
        mTabHost.getTabWidget().setDividerDrawable(R.drawable.ic_tab_divider);
    	
        TabWidget tabWidget = (TabWidget) getRootView().findViewById(android.R.id.tabs);
        
        LinearLayout.LayoutParams tabWidgetLayout;
        for (int j = 0; j < mTabCount; j++) 
        {
        	tabWidgetLayout = (LinearLayout.LayoutParams) tabWidget.getChildAt(j).getLayoutParams();
        	int oriLeftMargin = tabWidgetLayout.leftMargin;
        	int oriRightMargin = tabWidgetLayout.rightMargin;
        	
        	// fix right edge been cut issue when single one note
        	if(mTabCount == 1)
        		oriRightMargin = 0;
        	
        	if (j == 0) {
        		tabWidgetLayout.setMargins(0, 2, oriRightMargin, 5);
        	} else if (j == (mTabCount - 1)) {
        		tabWidgetLayout.setMargins(oriLeftMargin, 2, 0, 5);
        	} else {
        		tabWidgetLayout.setMargins(oriLeftMargin, 2, oriRightMargin, 5);
        	}
        }
        tabWidget.requestLayout();
	}
	
	
	/**
	 * set tab change listener
	 * 
	 */
	protected static void setTabChangeListener(final Activity activity)
	{
        // set on tab changed listener
	    mTabHost.setOnTabChangedListener(new OnTabChangeListener()
	    {
			@Override
			public void onTabChanged(String tabSpec)
			{
				openFragmentByTabSpec(tabSpec,activity);
			}
		}
	    );    
	}
	
	static void openFragmentByTabSpec(String tabSpec,Activity activity)
	{
		System.out.println("_openFragmentByTabSpec");
		// get scroll X
		int scrollX = mHorScrollView.getScrollX();
		
		//update final page currently viewed: scroll x
        mPref_FinalPageViewed = activity.getSharedPreferences("last_time_view", 0);
		mPref_FinalPageViewed.edit().putInt("KEY_LAST_TIME_VIEW_SCROLL_X",scrollX).commit();
		
		for(int i=0;i<mTabCount;i++)
		{
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			int iTabId = mDb.getTabId(i);
			mDb.doClose();
			int notesTblId = DB.getTab_NotesTableId(i);
			TAB_SPEC = TAB_SPEC_PREFIX.concat(String.valueOf(iTabId)); // TAB_SPEC starts from 1
	    	
			if(TAB_SPEC.equals(tabSpec) )
	    	{
	    		mCurrentTabIndex = i;
	    		//update final page currently viewed: tab Id
	    		Util.setPref_lastTimeView_NotesTableId(activity,notesTblId);
		    	mTabHost.setCurrentTab(mCurrentTabIndex); 
	    		
	    		DB.setNotesTableId(String.valueOf(notesTblId));
	    		System.out.println("new NoteFragment");
		    	new NoteFragment();
	    	} 
		}
	}
	
	/**
	 * set tab Edit listener
	 * @param activity 
	 * 
	 */
	protected static void setTabEditListener(final Activity activity)
	{
	    // set listener for editing tab info
	    int i = 0;
	    while(i < mTabCount)
		{
			final int tabCursor = i;
			View tabView= mTabHost.getTabWidget().getChildAt(i);
			
			// on long click listener
			tabView.setOnLongClickListener(new OnLongClickListener() 
	    	{	
				@Override
				public boolean onLongClick(View v) 
				{
					editTab(tabCursor, activity);
					return true;
				}
			});
			i++;
		}
	}
	
	/**
	 * delete page
	 * 
	 */
	public static  void deletePage(int TabId, Activity activity) {
		
		// check if only one page left
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		if(mTabCursor.getCount() != 1)
		{
			final int tabId =  mDb.getTabId(mCurrentTabIndex);
			//if current page is the first page and will be delete,
			//try to get next existence of note page
	        if(tabId == mFirstExist_TabId)
	        {
	        	int cGetNextExistIndex = mCurrentTabIndex+1;
	        	boolean bGotNext = false;
				while(!bGotNext){
		        	try{
		        	   	mFirstExist_TabId =  mDb.getTabId(cGetNextExistIndex);
		        		bGotNext = true;
		        	}catch(Exception e){
    		        	 bGotNext = false;
    		        	 cGetNextExistIndex++;}}		            		        	
	        }
            
	        //change to first existing page
	        int notesTblId = 0;
	        for(int i=0;i<DB.getTabsCount();i++)
	        {
	        	if(	mDb.getTabId(i)== mFirstExist_TabId)
	        		notesTblId =  DB.getTab_NotesTableId(i);
	        }
	        Util.setPref_lastTimeView_NotesTableId(activity, notesTblId);
		}
		else{
             Toast.makeText(activity, R.string.toast_keep_one_page , Toast.LENGTH_SHORT).show();
             return;
		}
		
		// set scroll X
		int scrollX = 0; //over the last scroll X
        mPref_FinalPageViewed = activity.getSharedPreferences("last_time_view", 0);
		mPref_FinalPageViewed.edit().putInt("KEY_LAST_TIME_VIEW_SCROLL_X",scrollX).commit();
	 	  
		mTabCursor = DB.mTabCursor;
		
		// drop table
		int iTabTableId = DB.getTab_NotesTableId(mCurrentTabIndex);

 	    // delete tab name
		mDb.dropNoteTable(iTabTableId);
		mDb.deleteTab(DB.getCurrentTabsTableName(),TabId);

		mTabCount--;
		mDb.doClose();
		updateChange(activity);
    	
    	// Note: _onTabChanged will reset scroll X to another value,
    	// so we need to add the following to set scroll X again
        mHorScrollView.post(new Runnable() 
        {
	        @Override
	        public void run() {
	        	mHorScrollView.scrollTo(0, 0);
        		mPref_FinalPageViewed.edit().putInt("KEY_LAST_TIME_VIEW_SCROLL_X",0).commit();
	        }
	    });
	}

	/**
	 * edit tab 
	 * 
	 */
	static int mStyle = 0;
	static void editTab(int tabCursor, final Activity activity)
	{
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		mTabCursor = DB.mTabCursor;
		final int tabId = mDb.getTabId(tabCursor);
		if(mTabCursor.isFirst())
			mFirstExist_TabId = tabId;
		
		if(tabCursor == mCurrentTabIndex )
		{
			// get tab name
			String tabName = DB.getTabTitle(tabCursor);
			mDb.doClose();
	        
	        final EditText editText1 = new EditText(activity.getBaseContext());
	        editText1.setText(tabName);
	        editText1.setSelection(tabName.length()); // set edit text start position
	        //update tab info
	        Builder builder = new Builder(mTabHost.getContext());
	        builder.setTitle(R.string.edit_page_tab_title)
	                .setMessage(R.string.edit_page_tab_message)
	                .setView(editText1)   
	                .setNegativeButton(R.string.btn_Cancel, new OnClickListener()
	                {   @Override
	                    public void onClick(DialogInterface dialog, int which)
	                    {/*cancel*/}
	                })
	                .setNeutralButton(R.string.edit_page_button_delete, new OnClickListener()
	                {   @Override
	                    public void onClick(DialogInterface dialog, int which)
	                	{
	                		// delete
							//warning:start
		                	mPref_delete_warn = activity.getSharedPreferences("delete_warn", 0);
		                	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
		 	                   mPref_delete_warn.getString("KEY_DELETE_PAGE_WARN","yes").equalsIgnoreCase("yes")) 
		                	{
		            			Util util = new Util(activity);
		        				util.vibrate();
		        				
		                		Builder builder1 = new Builder(mTabHost.getContext()); 
		                		builder1.setTitle(R.string.confirm_dialog_title)
	                            .setMessage(R.string.confirm_dialog_message_page)
	                            .setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener(){
	                            	@Override
	                                public void onClick(DialogInterface dialog1, int which1){
	                            		/*nothing to do*/}})
	                            .setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener(){
	                            	@Override
	                                public void onClick(DialogInterface dialog1, int which1){
	                                	deletePage(tabId, activity);
	                            	}})
	                            .show();
		                	} //warning:end
		                	else
		                	{
		                		deletePage(tabId, activity);
		                	}
	                    }
	                })	
	                .setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
	                {   @Override
	                    public void onClick(DialogInterface dialog, int which)
	                    {
	                		// save
	                	    mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        					final int tabId =  mDb.getTabId(mCurrentTabIndex);
        					final int tblNotesTblId =  DB.getTab_NotesTableId(mCurrentTabIndex);
        					final int tabPlaylistId =  DB.getTabPlaylistId(mCurrentTabIndex);
        					
	                        int tabStyle = mDb.getTabStyle(mCurrentTabIndex);
							mDb.updateTab(tabId,
											  editText1.getText().toString(),
											  tblNotesTblId, 
											  editText1.getText().toString(), 
											  tabPlaylistId, 
											  tabStyle );
	                        
							// Before _recreate, store latest page number currently viewed
							Util.setPref_lastTimeView_NotesTableId(activity, tblNotesTblId);
	                        mDb.doClose();
	                        
	                        updateChange(activity);
	                    }
	                })	
	                .setIcon(android.R.drawable.ic_menu_edit);
	        
			        AlertDialog d1 = builder.create();
			        d1.show();
			        // android.R.id.button1 for positive: save
			        ((Button)d1.findViewById(android.R.id.button1))
			        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
			        
			        // android.R.id.button2 for negative: color 
			        ((Button)d1.findViewById(android.R.id.button2))
  			        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
			        
			        // android.R.id.button3 for neutral: delete
			        ((Button)d1.findViewById(android.R.id.button3))
			        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
			}
	}
	
    
	/**
	 * on destroy
	 */
//	@Override
//	protected void onDestroy() {
//			mTabCursor.close();
//			mDb.close();
//		super.onDestroy();
//	}
	/**
	 * update change 
	 */
	static void updateChange(Activity activity)
	{
		mTabHost.clearAllTabs(); //must add this in order to clear onTanChange event
    	setTab(activity);
	}    
	
	static public int getLastTabId()
	{
		return mLastExist_TabId;
	}
	
	static public void setLastTabId(int lastTabId)
	{
		mLastExist_TabId = lastTabId;
	}
}
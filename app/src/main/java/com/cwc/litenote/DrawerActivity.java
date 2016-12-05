package com.cwc.litenote;

import java.util.ArrayList;
import java.util.List;
import android.support.v4.app.FragmentActivity;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

/**
 * This example illustrates a common usage of the DrawerLayout widget
 * in the Android support library.
 * <p/>
 * <p>When a navigation (left) drawer is present, the host activity should detect presses of
 * the action bar's Up affordance as a signal to open and close the navigation drawer. The
 * ActionBarDrawerToggle facilitates this behavior.
 * Items within the drawer should fall into one of two categories:</p>
 * <p/>
 * <ul>
 * <li><strong>View switches</strong>. A view switch follows the same basic policies as
 * list or tab navigation in that a view switch does not create navigation history.
 * This pattern should only be used at the root activity of a task, leaving some form
 * of Up navigation active for activities further down the navigation hierarchy.</li>
 * <li><strong>Selective Up</strong>. The drawer allows the user to choose an alternate
 * parent for Up navigation. This allows a user to jump across an app's navigation
 * hierarchy at will. The application should treat this as it treats Up navigation from
 * a different task, replacing the current task stack using TaskStackBuilder or similar.
 * This is the only form of navigation drawer that should be used outside of the root
 * activity of a task.</li>
 * </ul>
 * <p/>
 * <p>Right side drawers should be used for actions, not navigation. This follows the pattern
 * established by the Action Bar that navigation should be to the left and actions to the right.
 * An action should be an operation performed on the current contents of the window,
 * for example enabling or disabling a data overlay on top of the current content.</p>
 */
public class DrawerActivity extends FragmentActivity 
{
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mAppTitle;
    private static Context mContext;
	static Config mConfigFragment;
	static boolean bEnableConfig;
    static Menu mMenu;
    static DB mDb;
    DrawerInfoAdapter drawerInfoAdapter;
    List<String> mDrawerTitles;
    public static int mCurrentDrawerIndex;
    
    private static SharedPreferences mPref_lastTimeView;
    SharedPreferences mPref_add_new_note_option;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_activity_main);
        System.out.println("================start application ==================");

        mAppTitle = getTitle();
        
        mDrawerTitles = new ArrayList<String>();
        
		Context context = getApplicationContext();
        mDb = new DB(context);        
        DB.setDrawerTabsTableId(1); // init only
        DB.setNotesTableId("1"); // init only
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		
		if(mDb.getDrawersCount() == 0)
		{
			String drawerPrefix = "D";
	        for(int i=0;i< DB.DEFAULT_DRAWER_COUNT;i++)
	        {
	        	String drawerTitle = drawerPrefix.concat(String.valueOf(i+1));
	        	mDrawerTitles.add(drawerTitle);
	        	mDb.insertDrawer(i+1, drawerTitle ); 
	        	System.out.println("insert drawer " + (i+1));
	        }
		}
		else
		{
	        for(int i=0;i< DB.DEFAULT_DRAWER_COUNT;i++)
	        {
	        	mDrawerTitles.add(""); // init only
	        	mDrawerTitles.set(i, mDb.getDrawerTitle(i)); 
	        }
		}
		mDb.doClose();

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerListView = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        
        // set adapter
        drawerInfoAdapter = new DrawerInfoAdapter(this, mDrawerTitles);       
        mDrawerListView.setAdapter(drawerInfoAdapter);

        // set up click listener
        mDrawerListView.setOnItemClickListener(new DrawerItemClickListener());
        
        // set up long click listener
        mDrawerListView.setOnItemLongClickListener(new DrawerItemLongClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
	                this,                  /* host Activity */
	                mDrawerLayout,         /* DrawerLayout object */
	                R.drawable.ic_drawer,  /* navigation drawer image to replace 'Up' caret */
	                R.string.drawer_open,  /* "open drawer" description for accessibility */
	                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) 
        {
            public void onDrawerClosed(View view) 
            {
//        		System.out.println("mDrawerToggle onDrawerClosed ");
        		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        		mDrawerTitle = mDb.getDrawerTitle(mDrawerListView.getCheckedItemPosition());
        		mDb.doClose();  
                setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) 
            {
//        		System.out.println("mDrawerToggle onDrawerOpened ");
                setTitle(mAppTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mContext = getBaseContext();
        bEnableConfig = false;
        
        // get last time drawer
        mPref_lastTimeView = getSharedPreferences("last_time_view", 0);
        if (savedInstanceState == null)
        	mCurrentDrawerIndex = mPref_lastTimeView.getInt("KEY_LAST_TIME_VIEW_DRAWER", 0);
        

    }

    /*
     * Life cycle
     * 
     */
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	System.out.println("DrawerActivity / onRestoreInstanceState");    	
    	if(savedInstanceState != null)
    		mCurrentDrawerIndex = savedInstanceState.getInt("CurrentDrawerIndex");
    }

    @Override
    protected void onResume() {
    	System.out.println("DrawerActivity / onResume");   
        selectDrawerItem(mCurrentDrawerIndex);
        super.onResume();
    }
	
	// for finish(), for Rotate screen
    @Override
    protected void onPause() {
        super.onPause();
 	   System.out.println("DrawerActivity / onPause");
        
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
  	   System.out.println("DrawerActivity / onSaveInstanceState");
       outState.putInt("CurrentDrawerIndex",mCurrentDrawerIndex);
    }

    /*
     * on Activity Result
     * 
     * 
     */
	int ADD_NEW_TO_TOP = 2;
	int ADD_NEW_TO_BOTTOM = 3;
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		String noteAdded;
		if ( resultCode ==  Activity.RESULT_OK)
		{
			Bundle extras = data.getExtras();
			noteAdded = extras.getString("NOTE_ADDED"); // "empty" or "edited"

			if ( (noteAdded.equalsIgnoreCase("edited")) && 
				 (requestCode==ADD_NEW_TO_TOP) )
				NoteFragment.swap();
//			NoteFragment.fillData();
			
			//add new note again
			if ( noteAdded.equalsIgnoreCase("edited"))
			{
				final Intent intent = new Intent(this, Note_addNew.class);
				SharedPreferences pref_add_new_note_option = this.getSharedPreferences("add_new_note_option", 0);
	    		if(pref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_AT_TOP","false").equalsIgnoreCase("true"))
	    			startActivityForResult(intent, ADD_NEW_TO_TOP);
	    		else if(pref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_AT_BOTTOM","false").equalsIgnoreCase("true"))
	    			startActivityForResult(intent, ADD_NEW_TO_BOTTOM);
			}
		}
		else if(resultCode ==  Activity.RESULT_CANCELED)
		{ //cancel
			System.out.println("NoteFragment / Activity.RESULT_CANCELED");
		}
	}

	
    /*
     * Listeners
     * 
     */
    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener 
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
        {
        	mCurrentDrawerIndex = position;
            selectDrawerItem(position);
            mPref_lastTimeView.edit().putInt("KEY_LAST_TIME_VIEW_DRAWER",mCurrentDrawerIndex).commit();
        }
    }

    // select drawer item
    private void selectDrawerItem(int position) {

        
    	// update selected item and title, then close the drawer
        mDrawerListView.setItemChecked(position, true);
        
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		mDrawerTitle = mDb.getDrawerTitle(position);
		mDb.doClose();        
        mDrawerLayout.closeDrawer(mDrawerListView);    	
        setTitle(mDrawerTitle);
        
        DB.setDrawerTabsTableId(position+1); // position: start from 0, table Id starts from 1
        
    	Fragment fragment = new TabsHostFragment();
    	
    	Bundle args = new Bundle();
    	args.putInt(TabsHostFragment.ARG_PLANET_NUMBER, position);
    	fragment.setArguments(args);
    	
    	FragmentManager fragmentManager = getSupportFragmentManager();
    	FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        fragmentTransaction.replace(R.id.content_frame, fragment)
        				   .commit();
        
    }
    
    
    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemLongClickListener implements ListView.OnItemLongClickListener 
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) 
        {
        	editDrawerItem(position);
			return true;
        }
    }
    
	void editDrawerItem(final int position)
	{
		// insert when table is empty, activated only for the first time 
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		final String drawerName = mDb.getDrawerTitle(position);
		mDb.doClose();

		final EditText editText = new EditText(this);
	    editText.setText(drawerName);
	    editText.setSelection(drawerName.length()); // set edit text start position
	    //update tab info
	    Builder builder = new Builder(this);
	    builder.setTitle(R.string.edit_page_tab_title)
	    	.setMessage(R.string.edit_page_tab_message)
	    	.setView(editText)   
	    	.setNegativeButton(R.string.btn_Cancel, new OnClickListener()
	    	{   @Override
	    		public void onClick(DialogInterface dialog, int which)
	    		{/*cancel*/}
	    	})
	    	.setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
	    	{   @Override
	    		public void onClick(DialogInterface dialog, int which)
	    		{
	    			// save
	    			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
	    			int drawerId =  (int) mDb.getDrawerId(position);
	    			int drawerTabInfoTableId =  mDb.getDrawerTabsTableId(position);
					mDb.updateDrawer(drawerId,
							drawerTabInfoTableId,
							editText.getText().toString())
							;
                    mDb.doClose();
                    drawerInfoAdapter.notifyDataSetChanged();
                    setTitle(editText.getText().toString());
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
        
	}
    

    @Override
    public void setTitle(CharSequence title) {
        getActionBar().setTitle(title);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    
    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the navigation drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerListView);
        System.out.println("drawerOpen = " + drawerOpen);
        if(drawerOpen)
        	mMenu.setGroupVisible(0, false); 
        else
            setTitle(mDrawerTitle);

        return super.onPrepareOptionsMenu(menu);
    }

	private static class ViewHolder
	{
		TextView drawerTitle; // refers to ListView item's ImageView
	}
    
	class DrawerInfoAdapter extends ArrayAdapter<String>
	{
//		private List<String> items; 
		private LayoutInflater inflater;
      
		public DrawerInfoAdapter(Context context, List<String> items)
		{
			super(context, -1, items); // -1 indicates we're customizing view
//			this.items = items;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder viewHolder; // holds references to current item's GUI
         
			// if convertView is null, inflate GUI and create ViewHolder;
			// otherwise, get existing ViewHolder
			if (convertView == null)
			{
				convertView = inflater.inflate(R.layout.drawer_list_item, parent, false);
				// set up ViewHolder for this ListView item
				viewHolder = new ViewHolder();
				viewHolder.drawerTitle = (TextView) convertView.findViewById(android.R.id.text1);
				convertView.setTag(viewHolder); // store as View's tag
			}
			else // get the ViewHolder from the convertView's tag
				viewHolder = (ViewHolder) convertView.getTag();

			// get media info 
//			String item = items.get(position);
			
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			viewHolder.drawerTitle.setText(mDb.getDrawerTitle(position));
  		    mDb.doClose();

//			// configure the "Delete" Button
//			viewHolder.deleteButton.setTag(item);
//			viewHolder.deleteButton.setOnClickListener(deleteButtonListener);

			return convertView;
		}
	}
    
    
	/******************************************************
	 * Menu
	 * 
	 */
    // Menu identifiers
	private static SharedPreferences mPref_show_note_attribute;

    static final int ADD_NEW_NOTE = R.id.ADD_NEW_NOTE;
    static final int ADD_TEXT = R.id.ADD_TEXT;
    static final int ADD_NEW_PICTURE = R.id.ADD_NEW_PICTURE;
    static final int ADD_OLD_PICTURE = R.id.ADD_OLD_PICTURE;
    static final int ADD_MUSIC = R.id.ADD_MUSIC;
    
    static final int CHECK_ALL = R.id.CHECK_ALL;
    static final int UNCHECK_ALL = R.id.UNCHECK_ALL;
    static final int MOVE_CHECKED_NOTE = R.id.MOVE_CHECKED_NOTE;
    static final int COPY_CHECKED_NOTE = R.id.COPY_CHECKED_NOTE;
    static final int MAIL_CHECKED_NOTE = R.id.MAIL_CHECKED_NOTE;
    static final int DELETE_CHECKED_NOTE = R.id.DELETE_CHECKED_NOTE;
    static final int SLIDE_SHOW_CHECKED_NOTE = R.id.SLIDE_SHOW_CHECKED_NOTE;
    static final int ADD_NEW_PAGE = R.id.ADD_NEW_PAGE;
    static final int CHANGE_PAGE_COLOR = R.id.CHANGE_PAGE_COLOR;
    static final int SHIFT_PAGE = R.id.SHIFT_PAGE;
    static final int SHOW_BODY = R.id.SHOW_BODY;
    static final int ENABLE_DRAGGABLE = R.id.ENABLE_DND;
    static final int SEND_PAGES = R.id.SEND_PAGES;
    static final int GALLERY = R.id.GALLERY;
	static final int CONFIG_PREF = R.id.CONFIG_PREF;    
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
//	    menu.add(0, ADD_NEW_NOTE, 0, R.string.add_new_note )
//	    .setIcon(R.drawable.ic_input_add)
//	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		mMenu = menu;
		
		//
		// set sub menu 0: add new note
		//
	    SubMenu subMenu0 = menu.addSubMenu(0, 0, 0, R.string.add_new_note);//order starts from 0
	    
	    // add item
	    subMenu0.add(0, ADD_TEXT, 1, "Text")
        		.setIcon(android.R.drawable.ic_menu_edit);
	    subMenu0.add(0, ADD_NEW_PICTURE, 2, "New picture")
				.setIcon(android.R.drawable.ic_menu_camera);
	    subMenu0.add(0, ADD_OLD_PICTURE, 3, "Old picture")
        		.setIcon(android.R.drawable.ic_menu_gallery);	    
	    subMenu0.add(0, ADD_MUSIC, 4, "Music")
        		.setIcon(R.drawable.ic_lock_ringer_on);
	    
	    // icon
	    MenuItem subMenuItem0 = subMenu0.getItem();
	    subMenuItem0.setIcon(R.drawable.ic_input_add);
		
	    // set sub menu display
		subMenuItem0.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | 
				                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);	 
		
		//
		// set sub menu 1: Play slide show
		//
	    menu.add(0, SLIDE_SHOW_CHECKED_NOTE, 1, R.string.checked_notes_slide_show )
	    .setIcon(android.R.drawable.ic_menu_slideshow)
	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
	    //
		// set sub menu 2: handle checked note
	    //
	    SubMenu subMenu2 = menu.addSubMenu(0, 0, 2, R.string.checked_notes);//order starts from 0
	    
	    // add item
	    subMenu2.add(0, CHECK_ALL, 1, R.string.checked_notes_check_all)
        		.setIcon(R.drawable.btn_check_on_holo_dark);
	    subMenu2.add(0, UNCHECK_ALL, 2, R.string.checked_notes_uncheck_all)
				.setIcon(R.drawable.btn_check_off_holo_dark);
	    subMenu2.add(0, MOVE_CHECKED_NOTE, 3, R.string.checked_notes_move_to)
        		.setIcon(R.drawable.ic_menu_goto);	    
	    subMenu2.add(0, COPY_CHECKED_NOTE, 4, R.string.checked_notes_copy_to)
        		.setIcon(R.drawable.ic_menu_copy_holo_dark);
	    subMenu2.add(0, MAIL_CHECKED_NOTE, 5, R.string.mail_notes_btn)
        		.setIcon(android.R.drawable.ic_menu_send);
	    subMenu2.add(0, DELETE_CHECKED_NOTE, 6, R.string.checked_notes_delete)
        		.setIcon(R.drawable.ic_menu_clear_playlist);
	    // icon
	    MenuItem subMenuItem2 = subMenu2.getItem();
	    subMenuItem2.setIcon(R.drawable.ic_menu_mark);
	    
	    // set sub menu display
		subMenuItem2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | 
				                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		
		//
		// set sub menu 3: overflow
		//
	    SubMenu subMenu3 = menu.addSubMenu(0, 0, 3, R.string.options);//order starts from 0
	    // add item
	    subMenu3.add(0, ADD_NEW_PAGE, 1, R.string.add_new_page)
	            .setIcon(R.drawable.ic_menu_add_new_page);
	    
	    subMenu3.add(0, CHANGE_PAGE_COLOR, 2, R.string.change_page_color)
        	    .setIcon(R.drawable.ic_color_a);
	    
	    subMenu3.add(0, SHIFT_PAGE, 3, R.string.rearrange_page)
	            .setIcon(R.drawable.ic_dragger_h);
    	
	    // show body
	    mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
    	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
    		subMenu3.add(0, SHOW_BODY, 4, R.string.preview_note_body_no)
     	   		    .setIcon(R.drawable.ic_media_group_collapse);
    	else
    		subMenu3.add(0, SHOW_BODY, 4, R.string.preview_note_body_yes)
        	        .setIcon(R.drawable.ic_media_group_expand);
    	
    	// show draggable
	    mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
    	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
    		subMenu3.add(0, ENABLE_DRAGGABLE, 5, getResources().getText(R.string.draggable_no))
		    				.setIcon(R.drawable.ic_dragger_off);
    	else
    		subMenu3.add(0, ENABLE_DRAGGABLE, 5, getResources().getText(R.string.draggable_yes))
    						.setIcon(R.drawable.ic_dragger_on);
    	
	    subMenu3.add(0, SEND_PAGES, 6, R.string.mail_notes_title)
 	   			.setIcon(android.R.drawable.ic_menu_send);

	    subMenu3.add(0, GALLERY, 7, R.string.gallery)
			.setIcon(android.R.drawable.ic_menu_gallery);	    
	    
	    subMenu3.add(0, CONFIG_PREF, 8, R.string.settings)
	    	   .setIcon(R.drawable.ic_menu_preferences);
	    
	    // set icon
	    MenuItem subMenuItem1 = subMenu3.getItem();
	    subMenuItem1.setIcon(R.drawable.ic_menu_moreoverflow);
	    
	    // set sub menu display
		subMenuItem1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | 
				                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * on options item selected(non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	FragmentManager fragmentManager = null;
    	
   		fragmentManager = getSupportFragmentManager();

		// Go back: check if Configure fragment now
    	if( (item.getItemId() == android.R.id.home) && bEnableConfig)
    	{
    		getSupportFragmentManager().popBackStack();
    		mConfigFragment = null;  
    		bEnableConfig = false;
    		mMenu.setGroupVisible(0, true);        		
            mDrawerLayout.closeDrawer(mDrawerListView);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
            return true;
    	}
    	
    	// The action bar home/up action should open or close the drawer.
    	// ActionBarDrawerToggle will take care of this.
    	if (mDrawerToggle.onOptionsItemSelected(item))
    	{
    		System.out.println("mDrawerToggle.onOptionsItemSelected(item)");
    		return true;
    	}
    	
    	final Intent intent;
        switch (item.getItemId()) 
        {
//        case ADD_NEW_NOTE:
        	case ADD_TEXT:
	        	mPref_add_new_note_option = getSharedPreferences("add_new_note_option", 0);
				intent = new Intent(this, Note_addNew.class); //??? add 
				new Intent(this, Note_addNew.class);
				intent.putExtra("ADD_NEW", "TEXT");
				// show Optional dialog or not
	    		if(mPref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_OPTIONAL","true").equalsIgnoreCase("true"))
	        	{
		        	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		    		int noteCount = mDb.getNotesCount();
		    		mDb.doClose();
		        	final Note_addNew_optional dlg = new Note_addNew_optional(this,
		        													noteCount,
		        													Note_addNew_optional.ADD_NEW);
		        	if(noteCount == 0)
		        	{
		        		startActivityForResult(intent, ADD_NEW_TO_BOTTOM);
		        	}
		        	else if(noteCount > 0)
					{
			        	dlg.mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
			    		{
			    			@Override
			    			public void onCheckedChanged(RadioGroup RG, int id) 
			    			{
		    					final int sel = dlg.mRadioGroup.indexOfChild(dlg.mRadioGroup.findViewById(id));
			    					// check if Optional is checked
				 					if(dlg.mSetOptional)
				 					{
				 						// optional
				 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_OPTIONAL", "true").commit();
				 						dlg.mDialog.dismiss();
				 						
				 						if(sel ==0)
				 							startActivityForResult(intent, ADD_NEW_TO_TOP);
				 						else if(sel == 1)
				 							startActivityForResult(intent, ADD_NEW_TO_BOTTOM);
				 					}
				 					else
				 					{
				 						// not optional
				 						AlertDialog.Builder builder = new AlertDialog.Builder(DrawerActivity.this);
				 						builder.setTitle(R.string.add_new_note_notice_title)
				 							   .setMessage(R.string.add_new_note_notice_message) 	
				 							   .setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener()
				 							   {	@Override
				 									public void onClick(DialogInterface dialog, int which) {
				 										dlg.mDialog.dismiss();
							 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_OPTIONAL", "false").commit();
				 										
				 										if(sel ==0)
				 										{
								 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_TOP", "true").commit();
								 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_BOTTOM", "false").commit();
				 											startActivityForResult(intent, ADD_NEW_TO_TOP);
				 										}
				 										else if (sel == 1)
				 										{
								 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_TOP", "false").commit();
								 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_BOTTOM", "true").commit();
				 											startActivityForResult(intent, ADD_NEW_TO_BOTTOM);
				 										}
				 									}
				 							   })
				 							   .show();
				 					}
		    				}
			    		});
			        }
	        	}
	        	else
	        	{
	        		// for not optional 
	        		if(mPref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_AT_TOP","false").equalsIgnoreCase("true"))
	        			startActivityForResult(intent, ADD_NEW_TO_TOP);
	        		else if(mPref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_AT_BOTTOM","false").equalsIgnoreCase("true"))
	        			startActivityForResult(intent, ADD_NEW_TO_BOTTOM);
	        	}
	            return true;

        	case ADD_NEW_PICTURE:
	        	mPref_add_new_note_option = getSharedPreferences("add_new_note_option", 0);
				intent = new Intent(this, Note_addNew.class); //??? add 
				new Intent(this, Note_addNew.class);
				intent.putExtra("ADD_NEW", "NEW_PICTURE");
				// show Optional dialog or not
	    		if(mPref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_OPTIONAL","true").equalsIgnoreCase("true"))
	        	{
		        	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		    		int noteCount = mDb.getNotesCount();
		    		mDb.doClose();
		        	final Note_addNew_optional dlg = new Note_addNew_optional(this,
		        													noteCount,
		        													Note_addNew_optional.ADD_NEW);
		        	if(noteCount == 0)
		        	{
		        		startActivityForResult(intent, ADD_NEW_TO_BOTTOM);
		        	}
		        	else if(noteCount > 0)
					{
			        	dlg.mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
			    		{
			    			@Override
			    			public void onCheckedChanged(RadioGroup RG, int id) 
			    			{
		    					final int sel = dlg.mRadioGroup.indexOfChild(dlg.mRadioGroup.findViewById(id));
			    					// check if Optional is checked
				 					if(dlg.mSetOptional)
				 					{
				 						// optional
				 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_OPTIONAL", "true").commit();
				 						dlg.mDialog.dismiss();
				 						
				 						if(sel ==0)
				 						{
				 							intent.putExtra("ADD_NEW_TO_TOP", "true");
				 							startActivityForResult(intent, ADD_NEW_TO_TOP);
				 						}
				 						else if(sel == 1)
				 						{
				 							intent.putExtra("ADD_NEW_TO_TOP", "false");
				 							startActivityForResult(intent, ADD_NEW_TO_BOTTOM);
				 						}
				 					}
				 					else
				 					{
				 						// not optional
				 						AlertDialog.Builder builder = new AlertDialog.Builder(DrawerActivity.this);
				 						builder.setTitle(R.string.add_new_note_notice_title)
				 							   .setMessage(R.string.add_new_note_notice_message) 	
				 							   .setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener()
				 							   {	@Override
				 									public void onClick(DialogInterface dialog, int which) {
				 										dlg.mDialog.dismiss();
							 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_OPTIONAL", "false").commit();
				 										
				 										if(sel ==0)
				 										{
								 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_TOP", "true").commit();
								 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_BOTTOM", "false").commit();
				 											startActivityForResult(intent, ADD_NEW_TO_TOP);
				 										}
				 										else if (sel == 1)
				 										{
								 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_TOP", "false").commit();
								 							mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_BOTTOM", "true").commit();
				 											startActivityForResult(intent, ADD_NEW_TO_BOTTOM);
				 										}
				 									}
				 							   })
				 							   .show();
				 					}
		    				}
			    		});
			        }
	        	}
	        	else
	        	{
	        		// for not optional 
	        		if(mPref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_AT_TOP","false").equalsIgnoreCase("true"))
	        			startActivityForResult(intent, ADD_NEW_TO_TOP);
	        		else if(mPref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_AT_BOTTOM","false").equalsIgnoreCase("true"))
	        			startActivityForResult(intent, ADD_NEW_TO_BOTTOM);
	        	}
	            return true;
	            
	            
            case ADD_NEW_PAGE:
                addNewPage(TabsHostFragment.mLastExist_TabId + 1);
                return true;
                
            case CHANGE_PAGE_COLOR:
            	changePageColor();
                return true;    
                
            case SHIFT_PAGE:
            	shiftPage();
                return true;  
                
            case SHOW_BODY:
            	mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
            	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
            		mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY","no").commit();
            	else
            		mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY","yes").commit();
            	TabsHostFragment.updateChange(this);
                return true; 

            case ENABLE_DRAGGABLE:
            	mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
            	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE","no").commit();
            	else
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE","yes").commit();
            	TabsHostFragment.updateChange(this);
                return true;                 
                
            case SEND_PAGES:
				Intent intentSend = new Intent(this, SendMailAct.class);
				startActivity(intentSend);
				TabsHostFragment.updateChange(this);
            	return true;

            case GALLERY:
				Intent i_browsePic = new Intent(this, PictureGridAct.class);
				i_browsePic.putExtra("gallery", true);
				startActivity(i_browsePic);
            	return true; 	

            case CONFIG_PREF:
            	mMenu.setGroupVisible(0, false); //hide the menu
        		setTitle(R.string.settings);
        		bEnableConfig = true;
            	Fragment mConfigFragment = new Config();
            	fragmentManager = getSupportFragmentManager();
            	FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.content_frame, mConfigFragment).addToBackStack("config").commit();
                return true;
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    
    /*
     *  on Back button pressed(non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onBackPressed()
     */
    @Override
    public void onBackPressed()
    {
        System.out.println("_onBackPressed");
        if(!bEnableConfig)
        	super.onBackPressed();
        else
        {
    		getSupportFragmentManager().popBackStack();
    		mConfigFragment = null;  
    		bEnableConfig = false;
    		mMenu.setGroupVisible(0, true);
            mDrawerLayout.closeDrawer(mDrawerListView);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
    }

    
    /**
     * Add new page
     * @param newTabId
     */
	public  void addNewPage(final int newTabId) {
		// get tab name
		String tabName = "N".concat(String.valueOf(newTabId));
        
        final EditText editText1 = new EditText(getBaseContext());
        editText1.setText(tabName);
        editText1.setSelection(tabName.length()); // set edit text start position
        
        //update tab info
        Builder builder = new Builder(DrawerActivity.this);
        builder.setTitle(R.string.edit_page_tab_title)
                .setMessage(R.string.edit_page_tab_message)
                .setView(editText1)   
                .setNegativeButton(R.string.edit_page_button_ignore, new OnClickListener(){   
                	@Override
                    public void onClick(DialogInterface dialog, int which)
                    {/*nothing*/}
                })
                .setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
                {   @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                		
	    	            final String[] items = new String[]{
	    	            		getResources().getText(R.string.add_new_page_leftmost).toString(),
	    	            		getResources().getText(R.string.add_new_page_rightmost).toString() };
	    	            
						AlertDialog.Builder builder = new AlertDialog.Builder(DrawerActivity.this);
						  
						builder.setTitle(R.string.add_new_page_select_position)
						.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which) {
						
							if(which ==0)
								insertTabLeftmost(newTabId, editText1.getText().toString());
							else
								insertTabRightmost(newTabId, editText1.getText().toString());
							//end
							dialog.dismiss();
						}})
						.setNegativeButton(R.string.btn_Cancel, null)
						.show();
                    }
                })	 
                .setIcon(android.R.drawable.ic_menu_edit);
        
	        final AlertDialog d = builder.create();
	        d.show();
	        // android.R.id.button1 for negative: cancel 
	        ((Button)d.findViewById(android.R.id.button1))
	        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
	        // android.R.id.button2 for positive: save
	        ((Button)d.findViewById(android.R.id.button2))
	        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
	}
	
	/* 
	 * Insert Tab to Leftmost
	 * 
	 */
	void insertTabLeftmost(int newTabId,String tabName)
	{
 	    // insert tab name
		TabsHostFragment.mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		int style = Util.getNewPageStyle(mContext);
		TabsHostFragment.mDb.insertTab(DB.getCurrentTabsTableName(),tabName, newTabId,tabName, newTabId,style );
		
		// insert table for new tab
		TabsHostFragment.mDb.insertNoteTable(newTabId);
		TabsHostFragment.mTabCount++;
		TabsHostFragment.mDb.doClose();
		
		//change to leftmost tab Id
		TabsHostFragment.mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		int tabTotalCount = DB.getTabsCount();
		TabsHostFragment.mDb.doClose();
		for(int i=0;i <(tabTotalCount-1);i++)
		{
			int tabIndex = tabTotalCount -1 -i ;
			swapTabInfo(tabIndex,tabIndex-1);
			updateFinalPageViewed();
		}
		
        // set scroll X
		final int scrollX = 0; // leftmost
		
		// commit: scroll X
		TabsHostFragment.mPref_FinalPageViewed.edit().putInt("KEY_LAST_TIME_VIEW_SCROLL_X",scrollX).commit();
		
		TabsHostFragment.updateChange(this);
    	
		TabsHostFragment.mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
	        	TabsHostFragment.mHorScrollView.scrollTo(scrollX, 0);
	        	TabsHostFragment.mPref_FinalPageViewed.edit().putInt("KEY_LAST_TIME_VIEW_SCROLL_X",scrollX).commit();
	        } 
	    });
	}
	
	/*
	 * Update Final page which was viewed last time
	 * 
	 */
	protected void updateFinalPageViewed()
	{
        // get final viewed table Id
        String tblId = Util.getPref_lastTimeView_NotesTableId(this);
		Context context = getApplicationContext();

		DB.setNotesTableId(tblId);
		TabsHostFragment.mDb = new DB(context);
		
		TabsHostFragment.mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		// get final view tab index of last time
		for(int i =0;i<DB.getTabsCount();i++)
		{
			if(Integer.valueOf(tblId) == DB.getTab_NotesTableId(i))
				TabsHostFragment.mFinalPageViewed_TabIndex = i;	// starts from 0
			
        	if(	TabsHostFragment.mDb.getTabId(i)== TabsHostFragment.mFirstExist_TabId)
        		Util.setPref_lastTimeView_NotesTableId(this, DB.getTab_NotesTableId(i) );
		}
		TabsHostFragment.mDb.doClose();
	}
	
	/*
	 * Insert Tab to Rightmost
	 * 
	 */
	void insertTabRightmost(int newTblId,String tabName)
	{
 	    // insert tab name
		TabsHostFragment.mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		int style = Util.getNewPageStyle(mContext);
		TabsHostFragment.mDb.insertTab(DB.getCurrentTabsTableName(),tabName,newTblId,tabName,newTblId,style );
		
		// insert table for new tab
		TabsHostFragment.mDb.insertNoteTable(newTblId);
		TabsHostFragment.mTabCount++;
		TabsHostFragment.mDb.doClose();
		
		// commit: final page viewed
		Util.setPref_lastTimeView_NotesTableId(this, newTblId);
		
        // set scroll X
		final int scrollX = (TabsHostFragment.mTabCount) * 60 * 5; //over the last scroll X
		
		// commit: scroll X
		TabsHostFragment.mPref_FinalPageViewed.edit().putInt("KEY_LAST_TIME_VIEW_SCROLL_X",scrollX).commit();
		
		TabsHostFragment.updateChange(this);
    	
		TabsHostFragment.mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
	        	TabsHostFragment.mHorScrollView.scrollTo(scrollX, 0);
	        	TabsHostFragment.mPref_FinalPageViewed.edit().putInt("KEY_LAST_TIME_VIEW_SCROLL_X",scrollX).commit();
	        } 
	    });
	}
	
	/*
	 * Change Page Color
	 * 
	 */
	void changePageColor()
	{
		// set color
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit_page_color_title)
	    	   .setPositiveButton(R.string.edit_page_button_ignore, new OnClickListener(){   
	            	@Override
	                public void onClick(DialogInterface dialog, int which)
	                {/*cancel*/}
	            	});
		// inflate select style layout
		LayoutInflater mInflator= (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = mInflator.inflate(R.layout.select_style, null);
		RadioGroup RG_view = (RadioGroup)view.findViewById(R.id.radioGroup1);
		
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio0),0);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio1),1);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio2),2);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio3),3);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio4),4);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio5),5);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio6),6);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio7),7);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio8),8);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio9),9);
		
		// set current selection
		for(int i=0;i< Util.getStyleCount();i++)
		{
			if(Util.getCurrentPageStyle(this) == i)
			{
				RadioButton buttton = (RadioButton) RG_view.getChildAt(i);
		    	if(i%2 == 0)
		    		buttton.setButtonDrawable(R.drawable.btn_radio_on_holo_dark);
		    	else
		    		buttton.setButtonDrawable(R.drawable.btn_radio_on_holo_light);		    		
			}
		}
		
		builder.setView(view);
		
		RadioGroup radioGroup = (RadioGroup) RG_view.findViewById(R.id.radioGroup1);
		    
		final AlertDialog dlg = builder.create();
	    dlg.show();
	    
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup RG, int id) {
				TabsHostFragment.mStyle = RG.indexOfChild(RG.findViewById(id));
				TabsHostFragment.mDb = new DB(DrawerActivity.this);
				TabsHostFragment.mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
				TabsHostFragment.mDb.updateTab(TabsHostFragment.mDb.getTabId(TabsHostFragment.mCurrentTabIndex),
	 							  DB.getTabTitle(TabsHostFragment.mCurrentTabIndex),
	 							  DB.getTab_NotesTableId(TabsHostFragment.mCurrentTabIndex),
	 							  DB.getTabPlaylistTitle(TabsHostFragment.mCurrentTabIndex),
	 							  DB.getTabPlaylistId(TabsHostFragment.mCurrentTabIndex),
	 							 TabsHostFragment.mStyle );
				TabsHostFragment.mDb.doClose();
	 			dlg.dismiss();
	 			TabsHostFragment.updateChange(DrawerActivity.this);
		}});
	}

	
	
    /**
     * shift page right or left
     * 
     */
    void shiftPage()
    {
        Builder builder = new Builder(this);
        builder.setTitle(R.string.rearrange_page_title)
          	   .setMessage(null)
               .setNegativeButton(R.string.rearrange_page_left, null)
               .setNeutralButton(R.string.edit_note_button_back, null)
               .setPositiveButton(R.string.rearrange_page_right,null)
               .setIcon(R.drawable.ic_dragger_h);
        final AlertDialog d = builder.create();
        
        // disable dim background 
    	d.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    	d.show();
    	
    	
    	final int dividerWidth = getResources().getDrawable(R.drawable.ic_tab_divider).getMinimumWidth();
    	System.out.println("divWidth = " + dividerWidth);
    	// To left
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
        {  @Override
           public void onClick(View v)
           {
        		//change to OK
        		Button mButton=(Button)d.findViewById(android.R.id.button3);
    	        mButton.setText(R.string.btn_Finish);
    	        mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_finish , 0, 0, 0);

    	        int[] leftMargin = {0,0};
    	        if(TabsHostFragment.mCurrentTabIndex == 0)
    	        	TabsHostFragment.mTabHost.getTabWidget().getChildAt(0).getLocationInWindow(leftMargin);
    	        else
    	        	TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex-1).getLocationInWindow(leftMargin);

    			int curTabWidth,nextTabWidth;
    			curTabWidth = TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex).getWidth();
    			if(TabsHostFragment.mCurrentTabIndex == 0)
    				nextTabWidth = curTabWidth;
    			else
    				nextTabWidth = TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex-1).getWidth(); 

    			// when leftmost tab margin over window border
           		if(leftMargin[0] < 0) 
           			TabsHostFragment.mHorScrollView.scrollBy(- (nextTabWidth + dividerWidth) , 0);
				
        		d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        	    if(TabsHostFragment.mCurrentTabIndex == 0)
        	    {
        	    	Toast.makeText(TabsHostFragment.mTabHost.getContext(), R.string.toast_leftmost ,Toast.LENGTH_SHORT).show();
        	    	d.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);//avoid long time toast
        	    }
        	    else
        	    {
        	    	TabsHostFragment.mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        	    	Util.setPref_lastTimeView_NotesTableId(DrawerActivity.this, DB.getTab_NotesTableId(TabsHostFragment.mCurrentTabIndex));
        	    	TabsHostFragment.mDb.doClose();
					swapTabInfo(TabsHostFragment.mCurrentTabIndex,TabsHostFragment.mCurrentTabIndex-1);
					TabsHostFragment.updateChange(DrawerActivity.this);
        	    }
           }
        });
        
        // done
        d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
        {   @Override
           public void onClick(View v)
           {
               d.dismiss();
           }
        });
        
        // To right
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {  @Override
           public void onClick(View v)
           {
        		d.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
        		
        		// middle button text: change to OK
	    		Button mButton=(Button)d.findViewById(android.R.id.button3);
		        mButton.setText(R.string.btn_Finish);
		        mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_finish , 0, 0, 0);
   	    		
		        TabsHostFragment.mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
   	    		int count = DB.getTabsCount();
   	    		TabsHostFragment.mDb.doClose();
                
    			int[] rightMargin = {0,0};
    			if(TabsHostFragment.mCurrentTabIndex == (count-1))
    				TabsHostFragment.mTabHost.getTabWidget().getChildAt(count-1).getLocationInWindow(rightMargin);
    			else
    				TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex+1).getLocationInWindow(rightMargin);

    			int curTabWidth, nextTabWidth;
    			curTabWidth = TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex).getWidth();
    			if(TabsHostFragment.mCurrentTabIndex == (count-1))
    				nextTabWidth = curTabWidth;
    			else
    				nextTabWidth = TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex+1).getWidth();
    			
	    		// when rightmost tab margin plus its tab width over screen border 
    			int screenWidth = UtilImage.getScreenWidth(DrawerActivity.this);
	    		if( screenWidth <= rightMargin[0] + nextTabWidth )
	    			TabsHostFragment.mHorScrollView.scrollBy(nextTabWidth + dividerWidth, 0);	
				
	    		d.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
   	    		
       	    	if(TabsHostFragment.mCurrentTabIndex == (count-1))
       	    	{
       	    		// end of the right side
       	    		Toast.makeText(TabsHostFragment.mTabHost.getContext(),R.string.toast_rightmost,Toast.LENGTH_SHORT).show();
       	    		d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);//avoid long time toast
       	    	}
       	    	else
       	    	{
        	    	Util.setPref_lastTimeView_NotesTableId(DrawerActivity.this, DB.getTab_NotesTableId(TabsHostFragment.mCurrentTabIndex));
					swapTabInfo(TabsHostFragment.mCurrentTabIndex,TabsHostFragment.mCurrentTabIndex+1);
					TabsHostFragment.updateChange(DrawerActivity.this);
       	    	}
           }
        });
        
        // android.R.id.button1 for positive: next 
        ((Button)d.findViewById(android.R.id.button1))
        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_forward, 0, 0, 0);
        // android.R.id.button2 for negative: previous
        ((Button)d.findViewById(android.R.id.button2))
        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
        // android.R.id.button3 for neutral: cancel
        ((Button)d.findViewById(android.R.id.button3))
        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
    }
    
    /**
     * swap tab info
     * 
     */
    void swapTabInfo(int start, int end)
    {
    	TabsHostFragment.mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
    	TabsHostFragment.mDb.updateTab(TabsHostFragment.mDb.getTabId(end),
        		DB.getTabTitle(start),
        		DB.getTab_NotesTableId(start),
        		DB.getTabPlaylistTitle(start),
        		DB.getTabPlaylistId(start),
        		TabsHostFragment.mDb.getTabStyle(start));		        
		
        TabsHostFragment.mDb.updateTab(TabsHostFragment.mDb.getTabId(start),
				DB.getTabTitle(end),
				DB.getTab_NotesTableId(end),
				DB.getTabPlaylistTitle(end),
				DB.getTabPlaylistId(end),
				TabsHostFragment.mDb.getTabStyle(end));
		TabsHostFragment.mDb.doClose();
    }
    
}
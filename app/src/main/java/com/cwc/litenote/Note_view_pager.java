package com.cwc.litenote;

import java.util.ArrayList;
import java.util.List;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class Note_view_pager extends UilBaseFragment 
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    static ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    // DB
    DB mDb;
    Long mRowId;
    int mEntryPosition;
    int EDIT_VIEW = 5;
    int MAIL_VIEW = 6;
    int mStyle;
    
    ImageLoader imageLoader;
    DisplayImageOptions options;
    SharedPreferences mPref_show_note_attribute;
    
    Toast toast;
    Button editButton;
    Button sendButton;
    Button backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.note_view_slide);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		imageLoader = ImageLoader.getInstance();
		
		options = new DisplayImageOptions.Builder()
		.showImageForEmptyUri(R.drawable.btn_radio_off_holo_light)//R.drawable.ic_empty
		.showImageOnFail(R.drawable.ic_cab_done_holo)// R.drawable.ic_error		
		.resetViewBeforeLoading(true)
		.cacheOnDisk(true)
		.imageScaleType(ImageScaleType.EXACTLY)
		.bitmapConfig(Bitmap.Config.RGB_565)
//		.bitmapConfig(Bitmap.Config.ARGB_8888)
		.considerExifParams(true)
		.displayer(new FadeInBitmapDisplayer(300))
		.build();
		
        // DB
		String strFinalPageViewed_tableId = Util.getPref_lastTimeView_NotesTableId(Note_view_pager.this);
        DB.setNotesTableId(strFinalPageViewed_tableId);
        mDb = new DB(Note_view_pager.this);
        
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        
        mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        
        // set current selection
        mEntryPosition = getIntent().getExtras().getInt("POSITION");
        System.out.println("onCreate / mEntryPosition = " + mEntryPosition);
        mPager.setCurrentItem(mEntryPosition);
        mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        mRowId = mDb.getNoteId(mEntryPosition);
        mStyle = mDb.getTabStyle(TabsHostFragment.mCurrentTabIndex);
        mDb.doClose();
        
        // Note: if mPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
        //       be called again  after rotation
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() 
        {
            @Override
            public void onPageSelected(int position) 
            {
            	System.out.println("onCreate / onPageSelected /  position = " + position);

            	if((position == mEntryPosition+1) || (position == mEntryPosition-1))
            	{
	            	String tagStr = "current"+position+"webView";
	            	CustomWebView customWebView = (CustomWebView) mPager.findViewWithTag(tagStr);
	            	if (customWebView != null )
	            	{
	                    int defaultScale = pref_web_view.getInt("KEY_WEB_VIEW_SCALE",0);

	                    customWebView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
	                	customWebView.getSettings().setLoadWithOverviewMode(true);
	                	customWebView.getSettings().setBuiltInZoomControls(true);
	                	customWebView.getSettings().setUseWideViewPort(false);
	                	customWebView.getSettings().setSupportZoom(true);
	                	customWebView.setInitialScale(defaultScale);
//	                	  customWebView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);//240dpi
//	                	  customWebView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
	                	
	                    // load empty data to fix double width issue
	                    customWebView.loadData("","text/html; charset=utf-8", "UTF-8");
	                	customWebView.loadData(getHTMLstring(position), "text/html; charset=utf-8", "UTF-8");
	            	}
	            	mEntryPosition = mPager.getCurrentItem();
            	}
            	
            	// When changing pages, reset the action bar actions since they are dependent
                // on which page is currently active. An alternative approach is to have each
                // fragment expose actions itself (rather than the activity exposing actions),
                // but for simplicity, the activity provides the actions in this sample.
                invalidateOptionsMenu();//The onCreateOptionsMenu(Menu) method will be called the next time it needs to be displayed.
            }
        });
        
		// edit note button
        editButton = (Button) findViewById(R.id.view_edit);
        editButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);

		//edit 
        editButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
                
		        Intent i = new Intent(Note_view_pager.this, Note_edit.class);
		        
				mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		        i.putExtra(DB.KEY_NOTE_ID, mRowId);
		        i.putExtra(DB.KEY_NOTE_TITLE, mDb.getNoteTitleById(mRowId));
		        i.putExtra(DB.KEY_NOTE_PICTURE_URI , mDb.getNotePictureUriById(mRowId));
		        i.putExtra(DB.KEY_NOTE_BODY, mDb.getNoteBodyById(mRowId));
		        i.putExtra(DB.KEY_NOTE_CREATED, mDb.getNoteCreatedTimeById(mRowId));
				mDb.doClose();
		        
		        startActivityForResult(i, EDIT_VIEW);
            }

        });        
        
        // send note button
        sendButton = (Button) findViewById(R.id.view_send);
        sendButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_send, 0, 0, 0);
        
		//send 
        sendButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
                
                // set Sent string Id
				List<Long> rowArr = new ArrayList<Long>();
				rowArr.add(0,mRowId);
				
                // mail
				Intent intent = new Intent(Note_view_pager.this, SendMailAct.class);
		        String extraStr = Util.getSendStringWithXmlTag(rowArr);
		        extraStr = Util.addRssVersionAndChannel(extraStr);
		        intent.putExtra("SentString", extraStr);
		        mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		        String picFile = mDb.getNotePictureUriById(mRowId);
				mDb.doClose();
				if( (picFile != null) && 
				 	(picFile.length() > 0) )
				{
					String picFileArray[] = new String[]{picFile};
			        intent.putExtra("SentPictureFileNameArray", picFileArray);
				}
				startActivityForResult(intent, MAIL_VIEW);
            }

        });
        
        // back button
        backButton = (Button) findViewById(R.id.view_back);
        backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
        
        //cancel
        backButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        
        //show Toast for display mode
    	mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
	  	if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
	  			.equalsIgnoreCase("PICTURE_AND_TEXT"))
	  	{
//	  		showToast(R.string.view_note_mode_all);
	  		editButton.setVisibility(View.VISIBLE);
	  	    sendButton.setVisibility(View.VISIBLE);
	  	    backButton.setVisibility(View.VISIBLE);
	  	}
	  	else if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
	  			.equalsIgnoreCase("PICTURE_ONLY"))
	  	{	
//	  		showToast(R.string.view_note_mode_picture);
	  		editButton.setVisibility(View.GONE);
	  	    sendButton.setVisibility(View.GONE);
	  	    backButton.setVisibility(View.GONE);
	  	}
	  	else if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
	  			.equalsIgnoreCase("TEXT_ONLY"))
	  	{
//	  		showToast(R.string.view_note_mode_text);
	  		editButton.setVisibility(View.VISIBLE);
	  	    sendButton.setVisibility(View.VISIBLE);
	  	    backButton.setVisibility(View.VISIBLE);
	  	}
        
    }
    
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
        if((requestCode==EDIT_VIEW) || (requestCode==MAIL_VIEW))
        {
        	finish();
        }
	}
	
    static final int VIEW_ALL = R.id.VIEW_ALL;
    static final int VIEW_PICTURE = R.id.VIEW_PICTURE;
    static final int VIEW_TEXT = R.id.VIEW_TEXT;

    Menu mMenu;
    int mSubMenu0Id;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
    	mMenu = menu;
        // update row Id
        mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        mRowId = mDb.getNoteId(mPager.getCurrentItem());
        mDb.doClose();

        // item: sub menu
        SubMenu subMenu0 = menu.addSubMenu(0, R.id.action_mode, 0, R.string.view_note_mode);
	    // add item
	    MenuItem subItem = subMenu0.add(0, VIEW_ALL, 1, R.string.view_note_mode_all);
        subItem.setIcon(R.drawable.btn_check_on_holo_dark);
   		markCurrentSelected(subItem,"PICTURE_AND_TEXT");
        		
        subItem = subMenu0.add(0, VIEW_PICTURE, 2, R.string.view_note_mode_picture);
        markCurrentSelected(subItem,"PICTURE_ONLY");		
        		
	    subItem = subMenu0.add(0, VIEW_TEXT, 3, R.string.view_note_mode_text);
	    markCurrentSelected(subItem,"TEXT_ONLY");
	    
	    // icon
	    MenuItem subMenuItem0 = subMenu0.getItem();
	    subMenuItem0.setIcon(android.R.drawable.ic_menu_view);
	    mSubMenu0Id = subMenuItem0.getItemId();
	    
	    // set sub menu display
		subMenuItem0.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
	    // item: previous
		MenuItem itemPrev = menu.add(0, R.id.action_previous, 1, R.string.slide_action_previous )
				 				.setIcon(R.drawable.ic_media_previous);
	    itemPrev.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		itemPrev.setEnabled(mPager.getCurrentItem() > 0);
		itemPrev.getIcon().setAlpha(mPager.getCurrentItem() > 0?255:30);
		
		// item: next
        // Add either a "next" or "finish" button to the action bar, depending on which page is currently selected.
		MenuItem itemNext = menu.add(0, R.id.action_next, 2, 
                					(mPager.getCurrentItem() == mPagerAdapter.getCount() - 1)
                							? R.string.slide_action_finish
                							: R.string.slide_action_next)
                				.setIcon(R.drawable.ic_media_next);
        itemNext.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        
        // set Gray for Last item
        if(mPager.getCurrentItem() == (mPagerAdapter.getCount() - 1))
        	itemNext.setEnabled( false );

        itemNext.getIcon().setAlpha(mPager.getCurrentItem() == (mPagerAdapter.getCount() - 1)?30:255);
        //??? why affect image button, workaround: one uses local, one uses system
        
        return true;
    }
    
    void markCurrentSelected(MenuItem subItem, String str)
    {
        if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT").equalsIgnoreCase(str))
        	subItem.setIcon(R.drawable.btn_radio_on_holo_dark);
	  	else
        	subItem.setIcon(R.drawable.btn_radio_off_holo_dark);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }    

    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpTo(this, new Intent(this, DrawerActivity.class));
                return true;
            	
            case VIEW_ALL:
        		mPref_show_note_attribute.edit().putString("KEY_VIEW_NOTE_MODE","PICTURE_AND_TEXT").commit();
        		showSelectedView();
            	return true;
            	
            case VIEW_PICTURE:
        		mPref_show_note_attribute.edit().putString("KEY_VIEW_NOTE_MODE","PICTURE_ONLY").commit();
        		showSelectedView();
            	return true;

            case VIEW_TEXT:
        		mPref_show_note_attribute.edit().putString("KEY_VIEW_NOTE_MODE","TEXT_ONLY").commit();
        		showSelectedView();
            	return true;
            	
            case R.id.action_previous:
                // Go to the previous step in the wizard. If there is no previous step,
                // setCurrentItem will do nothing.
            	mPager.setCurrentItem(mPager.getCurrentItem() - 1);
                return true;

            case R.id.action_next:
                // Advance to the next step in the wizard. If there is no next step, setCurrentItem
                // will do nothing.
            	mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    void showSelectedView()
    {
	  	setResult(RESULT_CANCELED);
	  	finish();
	  	
		Intent intent;
		intent = new Intent( Note_view_pager.this, Note_view_pager.class);
        intent.putExtra("POSITION", mPager.getCurrentItem());
        startActivity(intent);
    }
    
//    public void showToast (int strId){
//    	String st = getResources().getText(strId).toString();
//        try{ toast.getView().isShown();     // true if visible
//            toast.setText(st);
//        } catch (Exception e) {         // invisible if exception
//            toast = Toast.makeText(Note_view_pager.this, st, Toast.LENGTH_SHORT);
//            }
//        toast.show();  //finally display it
//    }
    
    /**
     * A simple pager adapter 
     */
    CustomWebView customWebView;
    SharedPreferences pref_web_view;
    TouchImageView mPicView;
    private class ScreenSlidePagerAdapter extends PagerAdapter 

    {
		private LayoutInflater inflater;

		 
        public ScreenSlidePagerAdapter(FragmentManager fm) 
        {
            inflater = getLayoutInflater();
        }
        
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
			object = null;
		}

        @Override
		public Object instantiateItem(ViewGroup container, final int position) 
        {
        	
//        	System.out.println("Note_view_pager / instantiateItem / position = " + position);
            // Inflate the layout containing 
        	// 1. text group: title, body, time 
        	// 2. image group: picture, control buttons
        	View pagerView = (ViewGroup) inflater.inflate(R.layout.note_view_slide_pager, container, false);
            pagerView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
            
        	// text group
            ViewGroup textGroup = (ViewGroup) pagerView.findViewById(R.id.textGroup);
            customWebView = new CustomWebView(Note_view_pager.this);
            customWebView = ((CustomWebView) textGroup.findViewById(R.id.textBody));

            // set tag for custom web view
            String tagStr = "current"+position+"webView";
            customWebView.setTag(tagStr);
            
            pref_web_view = getSharedPreferences("web_view", 0);
            customWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onScaleChanged(WebView web_view, float oldScale, float newScale) 
                {
                    super.onScaleChanged(web_view, oldScale, newScale);
                    int newDefaultScale = (int) (newScale*100);
                    pref_web_view.edit().putInt("KEY_WEB_VIEW_SCALE",newDefaultScale).commit();
//                    System.out.println("onScaleChanged / mPager.getCurrentItem() " + mPager.getCurrentItem());
                    //update current position
                    mEntryPosition = mPager.getCurrentItem();
                }
            });
            
            int defaultScale = pref_web_view.getInt("KEY_WEB_VIEW_SCALE",0);
//          System.out.println("instantiateItem / defaultScale = " + defaultScale);
            customWebView.mPref_web_view = pref_web_view;
            
        	customWebView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
            customWebView.getSettings().setLoadWithOverviewMode(true);
        	customWebView.getSettings().setBuiltInZoomControls(true);
        	customWebView.getSettings().setUseWideViewPort(false);
        	customWebView.getSettings().setSupportZoom(true);
            customWebView.setInitialScale(defaultScale); 
//        	  webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);//240dpi        	
//            webView.getSettings().setRenderPriority(RenderPriority.HIGH);
//            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

            // load empty data to fix double width issue
            customWebView.loadData("","text/html; charset=utf-8", "UTF-8");
        	customWebView.loadData(getHTMLstring(position), "text/html; charset=utf-8", "UTF-8");
        	// image group
            ViewGroup imageGroup = (ViewGroup) pagerView.findViewById(R.id.imageContent);
            String tagImageStr = "current"+ position +"imageView";
            imageGroup.setTag(tagImageStr);
            
            Button imageViewBackButton = (Button) (imageGroup.findViewById(R.id.image_view_back));
            Button imageViewModeButton = (Button) (imageGroup.findViewById(R.id.image_view_mode));
            Button imagePreviousButton = (Button) (imageGroup.findViewById(R.id.image_previous));
            Button imageNextButton = (Button) (imageGroup.findViewById(R.id.image_next));
        	
        	TouchImageView pictureView = new TouchImageView(container.getContext());
			pictureView = ((TouchImageView) pagerView.findViewById(R.id.img_picture));

			// spinner
			final ProgressBar spinner = (ProgressBar) pagerView.findViewById(R.id.loading);

			// get picture name
        	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        	String strPicture = mDb.getNotePictureUri(position);
        	mDb.doClose();

            // view mode 
    	  	if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
    	  			.equalsIgnoreCase("PICTURE_ONLY"))
    	  	{
    	  	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
	    					 		 WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	  		getActionBar().hide();
    	  		
    			// picture only
    	  		textGroup.setVisibility(View.GONE);
    	  		imageGroup.setVisibility(View.VISIBLE);
    	  		
    			showPictureByTouchImageView(spinner, pictureView, strPicture);
    			
    			// image: view back
    			imageViewBackButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
    			// click to select view mode 
    			imageViewBackButton.setOnClickListener(new View.OnClickListener() {

    	            public void onClick(View view) {
    	            	finish();
    	            }
    	        });   
    			
    			// image: view mode
    			imageViewModeButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_view, 0, 0, 0);
    			// click to select view mode 
    			imageViewModeButton.setOnClickListener(new View.OnClickListener() {

    	            public void onClick(View view) {
	            		mMenu.performIdentifierAction(R.id.action_mode, 0);
    	            }
    	        });       			
    			
    			// image: previous button
    	        imagePreviousButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_previous, 0, 0, 0);
    			// click to previous 
    	        imagePreviousButton.setOnClickListener(new View.OnClickListener() 
    	        {
    	            public void onClick(View view) {
    	            	mPager.setCurrentItem(mPager.getCurrentItem() - 1);
    	            }
    	        });   
    	        
    			// image: next button
    	        imageNextButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_next, 0, 0, 0);
    			// click to next 
    	        imageNextButton.setOnClickListener(new View.OnClickListener()
    	        {
    	            public void onClick(View view) {
    	            	mPager.setCurrentItem(mPager.getCurrentItem() + 1);
    	            }
    	        }); 
    	        
    	  	}
    	  	else if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
    	  			.equalsIgnoreCase("TEXT_ONLY"))
    	  	{
    	  		// text only
    	  		textGroup.setVisibility(View.VISIBLE);
    	  		imageGroup.setVisibility(View.GONE);
    	  	}
    	  	else if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
    	  			.equalsIgnoreCase("PICTURE_AND_TEXT"))
    	  	{
    	  		// picture and text
    	  		textGroup.setVisibility(View.VISIBLE);
    	  		imageGroup.setVisibility(View.VISIBLE);
    	  		imagePreviousButton.setVisibility(View.GONE);
    	  		imageNextButton.setVisibility(View.GONE);
    	  		imageViewModeButton.setVisibility(View.GONE);
    	  		
    			showPictureByTouchImageView(spinner, pictureView, strPicture);
    	  	}
            
			mPicView =  pictureView;
        	container.addView(pagerView, 0);
        	
        	// set here since pagerView is just added 
        	showImageControlButtons(position); 
        	
			return pagerView;			
        }

        // show picture or not
        private void showPictureByTouchImageView(final View spinner, TouchImageView pictureView, String strPicture) 
        {
        	
        	if(strPicture.isEmpty())
        	{
//        		pictureView.setVisibility(View.GONE);
        		pictureView.setImageResource(mStyle%2 == 1 ?
		    			R.drawable.btn_radio_off_holo_light:
		    			R.drawable.btn_radio_off_holo_dark);//R.drawable.ic_empty);
        	}
        	else
        	{
        		Uri imageUri = Uri.parse(strPicture);
        		
        		if(Util.isUriExist(strPicture, getBaseContext()))
        		{
	        		if(imageUri.isAbsolute())
	        			imageLoader.displayImage(imageUri.toString(), 
											 	 pictureView,
											 	 options,
											 	 new SimpleImageLoadingListener()
	        		{
						@Override
						public void onLoadingStarted(String imageUri, View view) 
						{
	
							// make spinner appears at center
							LinearLayout.LayoutParams paramsSpinner = (LinearLayout.LayoutParams) spinner.getLayoutParams();
	//						paramsSpinner.weight = (float) 1.0;
							paramsSpinner.weight = (float) 1000.0; //??? still see garbage at left top corner
							spinner.setLayoutParams(paramsSpinner);
							 
							spinner.setVisibility(View.VISIBLE);
							view.setVisibility(View.GONE);
							
						}
		
						@Override
						public void onLoadingFailed(String imageUri, View view, FailReason failReason) 
						{
							String message = null;
							switch (failReason.getType()) 
							{
								case IO_ERROR:
									message = "Input/Output error";
									break;
								case DECODING_ERROR:
									message = "Image can't be decoded";
									break;
								case NETWORK_DENIED:
									message = "Downloads are denied";
									break;
								case OUT_OF_MEMORY:
									message = "Out Of Memory error";
									break;
								case UNKNOWN:
									message = "Unknown error";
									break;
							}
							Toast.makeText(Note_view_pager.this, message, Toast.LENGTH_SHORT).show();
							spinner.setVisibility(View.GONE);
							view.setVisibility(View.GONE);
						}
		
						@Override
						public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage)
						{
							spinner.setVisibility(View.GONE);
							view.setVisibility(View.VISIBLE);
						}
					});
        		}
        		else
        			pictureView.setImageResource(R.drawable.ic_cab_done_holo);
        	}
		}
        
		@Override
        public int getCount() 
        {
        	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        	int count = mDb.getNotesCount();
        	mDb.doClose();
        	return count;
        }

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view.equals(object);
		}
    }
    
    boolean isPictureMode()
    {
    	mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
	  	if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
	  	   	  			.equalsIgnoreCase("PICTURE_ONLY"))
	  		return true;
	  	else
	  		return false;
    }
    
    
    static ViewGroup imageGroup;
    void showImageControlButtons(int position)
    {
		if(isPictureMode())
	  	{
	        String tagImageStr = "current"+ position +"imageView";
	
	        System.out.println("tagImageStr = " + tagImageStr);
	        imageGroup = (ViewGroup) mPager.findViewWithTag(tagImageStr);
	        
	        if(imageGroup != null)
	        {
		        Button imageViewBackButton = (Button) (imageGroup.findViewById(R.id.image_view_back));
		        Button imageViewModeButton = (Button) (imageGroup.findViewById(R.id.image_view_mode));
		        Button imagePreviousButton = (Button) (imageGroup.findViewById(R.id.image_previous));
		        Button imageNextButton = (Button) (imageGroup.findViewById(R.id.image_next));
		
		        imageViewBackButton.setVisibility(View.VISIBLE);
		    	imageViewModeButton.setVisibility(View.VISIBLE);
				imagePreviousButton.setVisibility(View.VISIBLE);
		        imageNextButton.setVisibility(View.VISIBLE);
	        
		        imagePreviousButton.setEnabled(position==0? false:true);
		        imagePreviousButton.setAlpha(position==0? 0.1f:1f);

		        imageNextButton.setEnabled(position == (mPagerAdapter.getCount()-1 )? false:true);
		        imageNextButton.setAlpha(position == (mPagerAdapter.getCount()-1 )? 0.1f:1f);
		        
		    	new Note_view_pager_controller(Note_view_pager.this, 
    		            System.currentTimeMillis() + 1000 * 10); // for 10 seconds
//	            System.currentTimeMillis() + 1000 * 3); // for 3 seconds (for test)
	        }
	  	}
	  	else 
	  		return;
    }
    
    String getHTMLstring(int position)
    {
    	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
    	String strTitle = mDb.getNoteTitle(position);
    	String strBody = mDb.getNoteBody(position);
    	Long createTime = mDb.getNoteCreatedTime(position);
    	mDb.doClose();
    	String head = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+
    		       	  "<html><head>"+
    		       	  "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />"+
    		       	  //"<meta name=\"viewport\" content=\"target-densitydpi=device-dpi,width=device-width\">"
     	  			  "<head>";

       	String seperatedLineTitle = (strTitle.length()!=0)?"<hr size=2 color=blue width=99% >":"";
       	String seperatedLineBody = (strBody.length()!=0)?"<hr size=1 color=black width=99% >":"";

       	// title
    	Spannable spanTitle = new SpannableString(strTitle);
    	Linkify.addLinks(spanTitle, Linkify.ALL);
    	spanTitle.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_CENTER), 
    					  0,
    					  spanTitle.length(), 
    					  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    	strTitle = Html.toHtml(spanTitle);
    	
    	// body
    	Spannable spanBody = new SpannableString(strBody);
    	Linkify.addLinks(spanBody, Linkify.ALL);
    	strBody = Html.toHtml(spanBody);
    	
    	// set web view text color
    	String colorStr = Integer.toHexString(Util.mText_ColorArray[mStyle]);
    	colorStr = colorStr.substring(2);
    	
    	String bgColorStr = Integer.toHexString(Util.mBG_ColorArray[mStyle]);
    	bgColorStr = bgColorStr.substring(2);
    	
    	String content = head + "<body color=\"" + bgColorStr + "\">" +
		         "<p align=\"center\"><b>" + 
				 "<font color=\"" + colorStr + "\">" + strTitle + "</font>" + 
         		 "</b></p>" + seperatedLineTitle + 
		         "<p>" + 
				 "<font color=\"" + colorStr + "\">" + strBody + "</font>" +
				 "</p>" + seperatedLineBody + 
		         "<p align=\"right\">" + 
				 "<font color=\"" + colorStr + "\">"  + Util.getTimeString(createTime) + "</font>" +
		         "</p>" + 
		         "</body></html>";
		return content;
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int maskedAction = event.getActionMasked();
        switch (maskedAction) {

	        case MotionEvent.ACTION_DOWN:
	        case MotionEvent.ACTION_POINTER_DOWN: 
	    	  		showImageControlButtons(mPager.getCurrentItem());
	          break;
	        case MotionEvent.ACTION_MOVE: 
	        case MotionEvent.ACTION_UP:
	        case MotionEvent.ACTION_POINTER_UP:
	        case MotionEvent.ACTION_CANCEL: 
	          break;
        }

        boolean ret = super.dispatchTouchEvent(event);
        return ret;
    }    
}


abstract class UilBaseFragment extends FragmentActivity 
{
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) {
			default:
				return false;
		}
	}
}
package com.cwc.litenote;

import java.util.List;

import com.cwc.litenote.lib.DragSortController;
import com.cwc.litenote.lib.DragSortListView;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SlideshowEditor extends FragmentActivity
{
	static SlideshowInfo slideshow; 
	private static DB mDb;
	static MediaInfoAdapter mediaInfoAdapter;
	String mLastTimeView_NotesTableId_string;
	static DragSortListView plListView;
	private DragSortController mController;
	static int mCurrentPlayingMediaIndex;
	static Button playMusicButton;
	static boolean mIsPauseState;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.slideshow_editor);
       
   	   ActionBar actionBar = getActionBar();
   	   actionBar.setDisplayHomeAsUpEnabled(true);
	
       // initialize slide show information
       plListView = (DragSortListView) findViewById(R.id.plListView);
       slideshow = new SlideshowInfo();
       if (savedInstanceState == null) 
       {
    	   System.out.println("SlideshowEditor / onCreate / savedInstanceState == null");
    	   if(SlideshowMediaPlayer.mediaPlayer != null)
    	   {
    		   if(!SlideshowMediaPlayer.mediaPlayer.isPlaying())
    			   mCurrentPlayingMediaIndex = SlideshowMediaPlayer.mediaIndex;
    	   }
    	   else
    	   {
    		   mCurrentPlayingMediaIndex = 0;
    	   }
       }          
       
       // set DB table
       mLastTimeView_NotesTableId_string = Util.getPref_lastTimeView_NotesTableId(SlideshowEditor.this);
       DB.setNotesTableId(mLastTimeView_NotesTableId_string);
       
       // add images for slide show
       mDb = new DB(SlideshowEditor.this);
	   mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
	   for(int i=0;i< mDb.getNotesCount() ;i++)
	   {
	       if(mDb.getNoteMarking(i) == 1)
	       {
	    	   String pictureUri = mDb.getNotePictureUri(i);
	    	   slideshow.addImage(pictureUri);
	       }
	   }
	   mDb.doClose();
	  
	   // buttons
       playMusicButton = (Button) findViewById(R.id.playMusicButton);
       playMusicButton.setOnClickListener(playMediaButtonListener);
       Button playButton = (Button) findViewById(R.id.playSlideshowButton);
       playButton.setOnClickListener(playButtonListener);
       
       // set adapter
       mediaInfoAdapter = new MediaInfoAdapter(this, slideshow.getMediaList());       
       plListView.setAdapter(mediaInfoAdapter);
       mController = buildController(plListView);
       plListView.setFloatViewManager(mController);
       plListView.setOnTouchListener(mController);
       plListView.setDragEnabled(true);
       plListView.setDropListener(onDrop);
       plListView.setMarkListener(onMark);
       
       //fill play list data
       DB.setPlaylistId(DB.getDrawer_TabsTableId()+
				 		"_" +
				 		mLastTimeView_NotesTableId_string);
       mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
       if(DB.isTableExisted(DB.getPlaylistTitle()))
       {
    	   mDb.doGetMediaCursor();
	       for(int i=0; i< mDb.getMediumCount(); i++)
	       {
				// insert Uri string to playlist
   	    		slideshow.addMedia(mDb.getMediaUri(i));
   	    		
   	    		// set marking
   	    		slideshow.addMediaMarking(0); // just for initialization
   	    		if(mDb.getMediaMarking(i) == 1)
   	    			slideshow.setMediaMarking(i,1);
   	    		else
   	    			slideshow.setMediaMarking(i,0);
	       }
       }
       mDb.doClose();
       mediaInfoAdapter.notifyDataSetChanged();
       
       plListView.setOnItemClickListener(onItemClickListener);
       mIsPauseState = true;
    }
	
	// controller for list view
	public DragSortController buildController(DragSortListView dslv)
    {
        // defaults are
        DragSortController controller = new DragSortController(dslv);
        controller.setSortEnabled(true);
        
        //drag
	  	controller.setDragInitMode(DragSortController.ON_DOWN); // click

	  	controller.setDragHandleId(R.id.img_dragger);// handler
	  	controller.setBackgroundColor(Color.argb(128,128,64,0));// background color when dragging
        
	  	// mark
        controller.setMarkEnabled(true);
        controller.setClickMarkId(R.id.img_check);
        controller.setMarkMode(DragSortController.ON_DOWN);

        return controller;
    }        

	/**************************************
	 * Life cycle
	 * 
	 */

    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	System.out.println("SlideshowEditor / onRestoreInstanceState");    	
    	if(savedInstanceState != null)
    	{
    		System.out.println("SlideshowEditor / onRestoreInstanceState / savedInstanceState != null");
    		mCurrentPlayingMediaIndex = savedInstanceState.getInt("CurrentIndex");
    		mIsPauseState = savedInstanceState.getBoolean("IsPauseState");
    		mediaInfoAdapter.notifyDataSetChanged();
    	}
    }

    @Override
    protected void onResume() {
    	System.out.println("SlideshowEditor / onResume");        	
        super.onResume();
    }
	
	// for finish(), for Rotate screen
    @Override
    protected void onPause() {
        super.onPause();
 	   System.out.println("SlideshowEditor / onPause");
        
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
  	   System.out.println("SlideshowEditor / onSaveInstanceState");
       outState.putInt("CurrentIndex",mCurrentPlayingMediaIndex);
       outState.putBoolean("IsPauseState",mIsPauseState);
    }
    
	/**************************************************************
	 * Listeners
	 * 
	 */
    // list view item: Click to play media 
    //cf: http://stackoverflow.com/questions/5551042/onitemclicklistener-not-working-in-listview-android
	private OnItemClickListener onItemClickListener = new OnItemClickListener()
	{
		@Override
    	   public void onItemClick(AdapterView<?> parent, View view, int clickedPosition, long id) 
		   {
		       mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		       mDb.doGetMediaCursor();
		       boolean bClickMarked = (mDb.getMediaMarking((int)id) == 1?true:false);
		       mDb.doClose();
		   
		       if(bClickMarked)
		       {
//		    	   mClickedRowId = (int)id;
		    	   mCurrentPlayingMediaIndex  = (int)id;
		    	   // update play list and point to selected
			   	   updatePlaylist();
			   	   setCurrentMediaIndexTo((int)id);

			   	   // cancel playing
		    	   if(SlideshowMediaPlayer.mediaPlayer != null)
		    	   {
		   			   if(SlideshowMediaPlayer.mediaPlayer.isPlaying())
		   			   {
		   					SlideshowMediaPlayer.mediaPlayer.pause();
		   			   }
		    		   SlideshowMediaPlayer.mediaHandler.removeCallbacks(SlideshowMediaPlayer.runMediaPlay);     
			    	   SlideshowMediaPlayer.mediaPlayer = null;
		    	   }
			       // create new Intent to launch the slideShow player Activity
		    	   SlideshowMediaPlayer.mediaIndex = mCurrentPlayingMediaIndex; 
		    	   playMedia();
		       }
		       mediaInfoAdapter.notifyDataSetChanged();
			}
	};
	
    // list view listener: On Drop
	String currentMediaUriStr;
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() 
    {   @Override
        public void drop(int startPosition, int endPosition)
        {   
			int loop = Math.abs(startPosition-endPosition);
			
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			mDb.doGetMediaCursor();
    		currentMediaUriStr = mDb.getMediaUri(mCurrentPlayingMediaIndex);	
			mDb.doClose();
			
			for(int i=0;i< loop;i++)
			{
				swapRows(startPosition,endPosition);
				
				if((startPosition-endPosition) >0)
					endPosition++;
				else
					endPosition--;
			}
			mediaInfoAdapter.notifyDataSetChanged();
			updatePlaylist();
        }
    };	

    // list view listener: On Mark
    private DragSortListView.MarkListener onMark = new DragSortListView.MarkListener() 
	{   @Override
        public void mark(int position) 
		{
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			mDb.doGetMediaCursor();
			currentMediaUriStr = mDb.getMediaUri(mCurrentPlayingMediaIndex);	
			mDb.doClose();
		
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
            mDb.doGetMediaCursor();
            String title = mDb.getMediaTitle(position);
            String uriString = mDb.getMediaUri(position);
            Long id =  (long) mDb.getMediaId(position);
            boolean bToNext = false;
		
            if(mDb.getMediaMarking(position) == 0)
            {
          	  	mDb.updateMedia(id, title, uriString,1);
          	  	slideshow.setMediaMarking(position,1);   // mark
            }
            else if(mDb.getMediaMarking(position) == 1)
            {
          	  	mDb.updateMedia(id, title, uriString,0);
          	  	slideshow.setMediaMarking(position,0); // unmark
          	  	
          	  	// if unmarked current playing item
          	  	if(mCurrentPlayingMediaIndex == position)
          	  		bToNext = true;
            }
            mDb.doClose();
          
            updatePlaylist();
            mediaInfoAdapter.notifyDataSetChanged();
            
            System.out.println("bToNext = " + bToNext);
            if(bToNext)
            {
            	setCurrentMediaIndexToNext();
		   	   	// cancel playing
            	if(SlideshowMediaPlayer.mediaPlayer != null)
            	{
	   			   	if(SlideshowMediaPlayer.mediaPlayer.isPlaying())
	   			   		SlideshowMediaPlayer.mediaPlayer.pause();
	   			   	
	   			   	SlideshowMediaPlayer.mediaHandler.removeCallbacks(SlideshowMediaPlayer.runMediaPlay);     
	   			   	SlideshowMediaPlayer.mediaPlayer = null;
	   			   	System.out.println("SlideshowMediaPlayer.mediaPlayer = null");
            	}
            	// create new Intent to launch the slideShow player Activity
            	SlideshowMediaPlayer.mediaIndex = mCurrentPlayingMediaIndex; 
	    	   	playMedia();
            }
             
            return;
        }
    };   	
	
	// listener for Slide show button
	private OnClickListener playButtonListener = new OnClickListener()
	{
		@Override
      	public void onClick(View v)
		{
		        if	(slideshow.getImageList().size() == 0)
		        {
	    			Toast.makeText(SlideshowEditor.this,
							   R.string.delete_checked_no_checked_items,
							   Toast.LENGTH_SHORT)
							 .show();	
		        }
		        else
		        {
					// create new Intent to launch the slideShow player Activity
					Intent playSlideshow = new Intent(SlideshowEditor.this, SlideshowPlayer.class);
					startActivityForResult(playSlideshow, SLIDESHOW_PLAY);
		        }
		}
	};

	// listener of Play media button
	private OnClickListener playMediaButtonListener = new OnClickListener()
	{
		// launch music choosing activity
		@Override
		public void onClick(View v)
		{
			playMedia();
		}
	};
	
	// listener for list view: Delete button
	/* 
	 * 
	 * Bug:
	  	delete playing item
		=> still playing

		delete unplaying item
		=> last one: 1. no highlight
	 *
	 *
	 */
	private OnClickListener deleteButtonListener = new OnClickListener()
	{
		// removes the image
		@Override
		public void onClick(View v)
		{
			// backup current media index
			int backupCurrentMediaIndex = mCurrentPlayingMediaIndex;
			
			// remove it from adapter
			mediaInfoAdapter.remove((String) v.getTag());
			
			// current media index before Delete
			int deletedMediaIndex = plListView.getPositionForView(v);
			
			// get next media index for playing before Delete
			int nextPlayingMediaIndex = getNextMediaIndexAfterDelete(backupCurrentMediaIndex);
			
			// keep current media string before Delete
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			mDb.doGetMediaCursor();
    		currentMediaUriStr = mDb.getMediaUri(mCurrentPlayingMediaIndex);	
			mDb.doClose();			
			
			// delete the selected item
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
//			System.out.println("deleteButtonListener / deletedMediaIndex = " + deletedMediaIndex);
			mDb.deleteMedia(mDb.getMediaId(deletedMediaIndex));
			mDb.doClose();
            
            if(backupCurrentMediaIndex == deletedMediaIndex)          
            {
		   	   	// cancel playing
            	if(SlideshowMediaPlayer.mediaPlayer != null)
            	{
	   			   	if(SlideshowMediaPlayer.mediaPlayer.isPlaying())
	   			   		SlideshowMediaPlayer.mediaPlayer.pause();
	   			   	
	   			   	SlideshowMediaPlayer.mediaHandler.removeCallbacks(SlideshowMediaPlayer.runMediaPlay);     
	   			   	SlideshowMediaPlayer.mediaPlayer = null;
//	   			   	System.out.println("SlideshowMediaPlayer.mediaPlayer = null");
            	}
            	
            	System.out.println("nextMediaIndex = " + nextPlayingMediaIndex);
            	SlideshowMediaPlayer.mediaIndex = nextPlayingMediaIndex; 
            	mCurrentPlayingMediaIndex = nextPlayingMediaIndex; // update new media index
	    	   	playMedia();
            }
            else
            {
    			// update list view
    			updatePlaylist();
                mediaInfoAdapter.notifyDataSetChanged();
            }
		}
	};	
	
	
	/**********************************************************
	 * Menu
	 * 
	 */
    static final int ADD_NEW_NOTE = R.id.ADD_NEW_NOTE; //??? need to localize

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    menu.add(0, ADD_NEW_NOTE, 0, R.string.slide_show_add_music )
	    .setIcon(R.drawable.ic_menu_add)
	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * on options item selected
	 * 
	 */
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) 
	    {
		    case android.R.id.home:
		    	NavUtils.navigateUpTo(this, new Intent(this, DrawerActivity.class));
		        return true;
	        case ADD_NEW_NOTE:
	        	addMedia();
	            return true;
	    }
	    return super.onOptionsItemSelected(item);
	}	
		
    
	/*****************************************************
	 * Subroutines
	 * 
	 */
	// update play list
	void updatePlaylist()
	{
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		if(DB.isTableExisted(DB.getPlaylistTitle()))
		{
			mDb.doGetMediaCursor();
			for(int i=0; i< mDb.getMediumCount(); i++)
			{
				// update Uri string to playlist
   	    		slideshow.setMedia(i,mDb.getMediaUri(i));
   	    		

   	    		if( (currentMediaUriStr != null) && 
   	    		 	currentMediaUriStr.equalsIgnoreCase(mDb.getMediaUri(i)) )
   	    		{
   	   	    		System.out.println("getMediaUriString(i) = " + mDb.getMediaUri(i));
   	    			mCurrentPlayingMediaIndex = i;
   	    			SlideshowMediaPlayer.mediaIndex = i;
   	    		}
   	    			
   	    		// set marking
   	    		if(mDb.getMediaMarking(i) == 1)
   	    			slideshow.setMediaMarking(i,1);
   	    		else
   	    			slideshow.setMediaMarking(i,0);
			}
		}
		mDb.doClose();
	}
	
	// set current media index to new position
	void setCurrentMediaIndexTo(int newPosition)
	{
		// update play list and point to selected
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		mDb.doGetMediaCursor();
		for(int i=0; i< mDb.getMediumCount(); i++)
		{
			if( (mDb.getMediaMarking(i) == 1) && (newPosition == i) )
					mCurrentPlayingMediaIndex = i;
		}
		mDb.doClose();		
	}
	
	// set current media index to next item
	void setCurrentMediaIndexToNext()
	{
		// update play list and point to selected
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		mDb.doGetMediaCursor();
		boolean bGot = false;
		for(int i=0; i< mDb.getMediumCount(); i++)
		{
			if( (mDb.getMediaMarking(i) == 1) && (i > mCurrentPlayingMediaIndex) && (bGot == false) )
			{
					mCurrentPlayingMediaIndex = i;
					bGot = true;
			}
		}
		
		// if not got, starts from head
		if(!bGot)
		{
			mCurrentPlayingMediaIndex = 0;
			for(int i=0; i< mDb.getMediumCount(); i++)
			{
				if((mDb.getMediaMarking(i) == 1) && (bGot == false)) 
				{
						mCurrentPlayingMediaIndex = i;
						bGot = true;
				}
			}
		}
		
		mDb.doClose();		
	}

	// Get next media index after Delete 
	int getNextMediaIndexAfterDelete(int mCurrentMediaIndex)
	{
		int nextMediaIndex = 0;
		// update play list and point to selected
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		mDb.doGetMediaCursor();
		boolean bGot = false;
		for(int i=0; i< mDb.getMediumCount(); i++)
		{
			if( (mDb.getMediaMarking(i) == 1) &&
					(i > mCurrentMediaIndex)  &&
					(bGot == false)                      )
			{
				    nextMediaIndex = i-1; //note: mCurrentMediaIndex item will be deleted
					bGot = true; // for Stop looking for next
			}
		}
		
		// if not got, starts from head
		if(!bGot)
		{
			nextMediaIndex = 0;
			for(int i=0; i< mDb.getMediumCount(); i++)
			{
				if((mDb.getMediaMarking(i) == 1) && (bGot == false)) 
				{
					    nextMediaIndex = i;
						bGot = true; // for Stop looking for next
				}
			}
		}
		
		mDb.doClose();	
		
		return nextMediaIndex;
	}

    // Swap rows after Drop
    private static Long mId1 ;
	private static String mTitle1;
	private static String mUriString1;
	private static int mMarking1;
	private static Long mId2 ;
	private static String mUriString2;
	private static String mTitle2;
	private static int mMarking2; 
	
	protected static void swapRows(int startPosition, int endPosition) 
	{
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		mDb.doGetMediaCursor();

		mId1 = mDb.getMediaId(startPosition);
        mTitle1 = mDb.getMediaTitle(startPosition);
        mUriString1 = mDb.getMediaUri(startPosition);
        mMarking1 = mDb.getMediaMarking(startPosition);

		mId2 = mDb.getMediaId(endPosition);
        mTitle2 = mDb.getMediaTitle(endPosition);
        mUriString2 = mDb.getMediaUri(endPosition);
        mMarking2 = mDb.getMediaMarking(endPosition);
		
        mDb.updateMedia(mId2,
				 mTitle1,
				 mUriString1,
				 mMarking1);		        
		
		mDb.updateMedia(mId1,
		 		 mTitle2,
		 		 mUriString2,
		 		 mMarking2);	
    	mDb.doClose();
	}    

	/****************************************************
	 * Fork
	 * 
	 */
	// set IDs for each type of media result
	private static final int MEDIA_ADD = 1;
	private static final int SLIDESHOW_PLAY = 2;
	
	
	// Add media to list view
	void addMedia()
	{
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("audio/*");
		startActivityForResult(Intent.createChooser(intent, 
									   getResources().getText(R.string.slide_show_chooser_music)),
													MEDIA_ADD);
	}
	
	// Play media
	void playMedia()
	{
		// if media player is null, set new fragment
		if(SlideshowMediaPlayer.mediaPlayer == null)
		{
			System.out.println("new SlideshowMediaPlayer");
			playMusicButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_pause , 0, 0, 0);
			playMusicButton.setText("Pause Music");
			SlideshowMediaPlayer.mediaTime = 0;
			
			FragmentManager fragmentManager;
			Fragment fragment = new SlideshowMediaPlayer();
        	fragmentManager = getSupportFragmentManager();
        	FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(fragment, "mediaPlay").commit();
            mIsPauseState = false;
		}
		else
		{
			// play to pause
			if(SlideshowMediaPlayer.mediaPlayer.isPlaying())
			{
				System.out.println("play -> pause");
				playMusicButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_play_clip , 0, 0, 0);
				playMusicButton.setText("Play Music");
				
				SlideshowMediaPlayer.mediaPlayer.pause();
				SlideshowMediaPlayer.mediaHandler.removeCallbacks(SlideshowMediaPlayer.runMediaPlay); 
				mIsPauseState = true;
			}
			else // pause to play
			{
				System.out.println("pause -> play");
				playMusicButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_pause , 0, 0, 0);
				playMusicButton.setText("Pause Music");
				SlideshowMediaPlayer.mediaPlayer.start();
				SlideshowMediaPlayer.mediaHandler.post(SlideshowMediaPlayer.runMediaPlay);  
				mIsPauseState = false;
			}
		}
	}

	// called after media is added
	@Override
	protected final void onActivityResult(int requestCode, int resultCode, 
			Intent data)
	{
		if (resultCode != RESULT_OK) 
			return;  
	   
		if (resultCode == RESULT_OK) // if there was no error
		{
			if (requestCode == MEDIA_ADD) // Activity returns music
			{
				Uri selectedUri = data.getData(); 
				System.out.println("selected Uri = " + selectedUri.toString());

				slideshow.addMedia(selectedUri.toString());
				slideshow.addMediaMarking(1); 
				mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
				mDb.insertNewPlaylistId(DB.getDrawer_TabsTableId()+
						 			  "_"+
						 			  mLastTimeView_NotesTableId_string);
				mDb.doClose();
				
				String uriStr = selectedUri.toString();
				// insert Uri string to DB
				mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
				mDb.insertMedia(Util.getDisplayNameByUri(selectedUri, SlideshowEditor.this), uriStr);
				mDb.doClose();
				
				mediaInfoAdapter.notifyDataSetChanged();				
			}
		}
	}	
	
	/*******************************************
	 * Media information adapter
	 * 
	 */
	private static class ViewHolder
	{
		TextView mediaInfo; // refers to ListView item's ImageView
		ImageView imageCheck;
		Button deleteButton; // refers to ListView item's Button
	}

	// ArrayAdapter displaying media info
	class MediaInfoAdapter extends ArrayAdapter<String>
	{
		private List<String> items; // list of image Uris
		private LayoutInflater inflater;
      
		public MediaInfoAdapter(Context context, List<String> items)
		{
			super(context, -1, items); // -1 indicates we're customizing view
			this.items = items;
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
				//ref: http://stackoverflow.com/questions/7312481/android-textview-not-filling-width-why
				convertView = inflater.inflate(R.layout.slideshow_edit_item, parent, false);
				// set up ViewHolder for this ListView item
				viewHolder = new ViewHolder();
				viewHolder.imageCheck = (ImageView) convertView.findViewById(R.id.img_check);
				viewHolder.mediaInfo = (TextView) convertView.findViewById(R.id.mediaInfo);
				viewHolder.deleteButton = (Button) convertView.findViewById(R.id.deleteButton);
				
				convertView.setTag(viewHolder); // store as View's tag
			}
			else // get the ViewHolder from the convertView's tag
				viewHolder = (ViewHolder) convertView.getTag();

			if(mCurrentPlayingMediaIndex == position)
				viewHolder.mediaInfo.setAlpha(0.5f);
			else
				viewHolder.mediaInfo.setAlpha(1.0f);
			
		  	// marking
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			mDb.doGetMediaCursor();
			if( mDb.getMediaMarking(position) == 1) 
				viewHolder.imageCheck.setBackgroundResource(
		    			R.drawable.btn_check_on_holo_light);
			else
				viewHolder.imageCheck.setBackgroundResource(
						R.drawable.btn_check_off_holo_light);
			 mDb.doClose();
			// get media info 
			String item = items.get(position);
			
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			mDb.doGetMediaCursor();
			viewHolder.mediaInfo.setText(mDb.getMediaTitle(position));
  		    mDb.doClose();

			// configure the "Delete" Button
			viewHolder.deleteButton.setTag(item);
			viewHolder.deleteButton.setOnClickListener(deleteButtonListener);

			return convertView;
		}
	}

}
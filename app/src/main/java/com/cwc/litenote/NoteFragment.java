package com.cwc.litenote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.cwc.litenote.lib.DragSortController;
import com.cwc.litenote.lib.DragSortListView;
import com.cwc.litenote.lib.SimpleDragSortCursorAdapter;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class NoteFragment extends UilListViewBaseFragment 
						  implements LoaderManager.LoaderCallbacks<List<String>> 
{
	private static Cursor mNotesCursor;
	private static DB mDb;
    SharedPreferences mPref_delete_warn;
    SharedPreferences mPref_style;
	SharedPreferences mPref_show_note_attribute;

    private static Long mNoteNumber1 = (long) 1;
	private static String mNoteTitle1;
	private static String mNotePictureString1;
	private static String mNoteBodyString1;
	private static int mMarkingIndex1;
	private static Long mCreateTime1;
	private static Long mNoteNumber2 ;
	private static String mNotePictureString2;
	private static String mNoteTitle2;
	private static String mNoteBodyString2;
	private static int mMarkingIndex2;
	private static Long mCreateTime2;
	private List<Boolean> mSelectedList = new ArrayList<Boolean>();
	
	// This is the Adapter being used to display the list's data.
	NoteListAdapter mAdapter;
	DragSortListView mDndListView;
	private DragSortController mController;
	int MOVE_TO = 0;
	int COPY_TO = 1;
    int mStyle = 0;
    int mCount;
    
    DisplayImageOptions options;
	public NoteFragment(){}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		System.out.println("NoteFragment");  
		listView = (DragSortListView)getActivity().findViewById(R.id.list1);
		mDndListView = listView;
    	mDb = new DB(getActivity()); 
    	
    	mStyle = Util.getCurrentPageStyle(getActivity());

		options = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.ic_stub)
		.showImageForEmptyUri(R.drawable.btn_radio_off_holo_light)//R.drawable.ic_empty
		.showImageOnFail(R.drawable.ic_cab_done_holo)// R.drawable.ic_error
		.cacheInMemory(true) // true: can speed up bitmap showing
		.cacheOnDisk(true)
		.considerExifParams(true)
		.displayer(new RoundedBitmapDisplayer(10))
		.build();
		
    	//listener: view note 
    	mDndListView.setOnItemClickListener(new OnItemClickListener()
    	{   @Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) 
			{
	    		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
	    		mCount = mDb.getNotesCount();
	    		mDb.doClose();
    		
				if(position < mCount) // avoid footer error
				{
					Intent intent;
					intent = new Intent(getActivity(), Note_view_pager.class);
			        intent.putExtra("POSITION", position);
			        startActivity(intent);
				}
			}
    	}
    	);
    	
    	// listener: edit note 
    	mDndListView.setOnItemLongClickListener(new OnItemLongClickListener()
    	{
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
             {	
		        Intent i = new Intent(getActivity(), Note_edit.class);
				mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
				Long rowId = mDb.getNoteId(position);
		        i.putExtra(DB.KEY_NOTE_ID, mDb.getNoteId(position));
		        i.putExtra(DB.KEY_NOTE_TITLE, mDb.getNoteTitleById(rowId));
		        i.putExtra(DB.KEY_NOTE_PICTURE_URI , mDb.getNotePictureUriById(rowId));
		        i.putExtra(DB.KEY_NOTE_BODY, mDb.getNoteBodyById(rowId));
		        i.putExtra(DB.KEY_NOTE_CREATED, mDb.getNoteCreatedTimeById(rowId));
				mDb.doClose();
		        startActivity(i);
            	return true;
             }
	    });
    	
        mController = buildController(mDndListView);
        mDndListView.setFloatViewManager(mController);
        mDndListView.setOnTouchListener(mController);
  		mDndListView.setDragEnabled(true);
	  	
		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = new NoteListAdapter(getActivity());

		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(true); //set progress indicator

		// Prepare the loader. Either re-connect with an existing one or start a new one.
		getLoaderManager().initLoader(0, null, this);
	}
	
    // list view listener: on drag
    private DragSortListView.DragListener onDrag = new DragSortListView.DragListener() 
    {
                @Override
                public void drag(int startPosition, int endPosition) {
                	//add highlight boarder
//                    View v = mDndListView.mFloatView;
//                    v.setBackgroundColor(Color.rgb(255,128,0));
//                	v.setBackgroundResource(R.drawable.listview_item_shape_dragging);
//                    v.setPadding(0, 4, 0,4);
                }
    };
	
    // list view listener: on drop
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() 
    {
        @Override
        public void drop(int startPosition, int endPosition) {
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			if(startPosition >= mDb.getNotesCount()) // avoid footer error
				return;
	    	mDb.doClose();
			
			mSelectedList.set(startPosition, true);
			mSelectedList.set(endPosition, true);
			
			//reorder data base storage
			int loop = Math.abs(startPosition-endPosition);
			for(int i=0;i< loop;i++)
			{
				swapRows(startPosition,endPosition);
				if((startPosition-endPosition) >0)
					endPosition++;
				else
					endPosition--;
			}
			fillData();
        }
    };
	
    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(DragSortListView dslv)
    {
        // defaults are
        DragSortController controller = new DragSortController(dslv);
        controller.setSortEnabled(true);
        
        //drag
	  	mPref_show_note_attribute = getActivity().getSharedPreferences("show_note_attribute", 0);
	  	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
	  		controller.setDragInitMode(DragSortController.ON_DOWN); // click
	  	else
	        controller.setDragInitMode(DragSortController.MISS); 

	  	controller.setDragHandleId(R.id.img_dragger);// handler
//        controller.setDragInitMode(DragSortController.ON_LONG_PRESS); //long click to drag
	  	controller.setBackgroundColor(Color.argb(128,128,64,0));// background color when dragging
//        controller.setBackgroundColor(Util.mBG_ColorArray[mStyle]);// background color when dragging
        
    // mark
        controller.setMarkEnabled(true);
        controller.setClickMarkId(R.id.img_check);
        controller.setMarkMode(DragSortController.ON_DOWN);

        return controller;
    }        

	@Override
	public Loader<List<String>> onCreateLoader(int id, Bundle args) 
	{
		// This is called when a new Loader needs to be created. 
		return new NoteListLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<String>> loader,
							   List<String> data) 
	{
		// Set the new data in the adapter.
		mAdapter.setData(data);

		// The list should now be shown.
		if (isResumed()) 
			setListShown(true);
		else 
			setListShownNoAnimation(true);
		
		fillData();
	}

	@Override
	public void onLoaderReset(Loader<List<String>> loader) {
		// Clear the data in the adapter.
		mAdapter.setData(null);
	}
	
    
	/**
	 * fill data
	 */
	
    public void fillData()
    {
    	System.out.println("fillData");
    	// save index and top position
        int index = mDndListView.getFirstVisiblePosition();
        View v = mDndListView.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();
        
        // set background color of list view
        mDndListView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);

    	//show divider color
        if(mStyle%2 == 0)
	    	mDndListView.setDivider(new ColorDrawable(0xFFffffff));//for dark
        else
           	mDndListView.setDivider(new ColorDrawable(0xff000000));//for light

        mDndListView.setDividerHeight(1);
        
    	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
    	mNotesCursor = mDb.mNoteCursor;
    	int count = mDb.getNotesCount();
        mDb.doClose();
        // set adapter
        String[] from = new String[] { DB.KEY_NOTE_TITLE};
        int[] to = new int[] { R.id.text_whole };
        ((DragSortListView) mDndListView).setAdapter(new ItemAdapter(
	        												getActivity(),
	        												R.layout.activity_main_list_row,
	        												mNotesCursor,
	        												from,
	        												to,
	        												0
	        												));
//        mDb.doClose();//??? mNotesCursor is not used?
        
		// selected list
		for(int i=0; i< count ; i++ )
		{
			mSelectedList.add(true);
			mSelectedList.set(i,true);
		}
		
        // restore index and top position
        mDndListView.setSelectionFromTop(index, top);
        
        mDndListView.setDropListener(onDrop);
        mDndListView.setDragListener(onDrag);
        mDndListView.setMarkListener(onMark);
        
        setFooter();
    }
    
    // swap rows
	protected static void swapRows(int startPosition, int endPosition) {

		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
//		mDb.getAllNotesCount();

		mNoteNumber1 = mDb.getNoteId(startPosition);
        mNoteTitle1 = mDb.getNoteTitle(startPosition);
        mNotePictureString1 = mDb.getNotePictureUri(startPosition);
        mNoteBodyString1 = mDb.getNoteBody(startPosition);
        mMarkingIndex1 = mDb.getNoteMarking(startPosition);
    	mCreateTime1 = mDb.getNoteCreatedTime(startPosition); 

		mNoteNumber2 = mDb.getNoteId(endPosition);
        mNoteTitle2 = mDb.getNoteTitle(endPosition);
        mNotePictureString2 = mDb.getNotePictureUri(endPosition);
        mNoteBodyString2 = mDb.getNoteBody(endPosition);
        mMarkingIndex2 = mDb.getNoteMarking(endPosition);
    	mCreateTime2 = mDb.getNoteCreatedTime(endPosition); 
		
        mDb.updateNote(mNoteNumber2,
				 mNoteTitle1,
				 mNotePictureString1,
				 "", //???
				 "", //???
				 mNoteBodyString1,
				 mMarkingIndex1,
				 mCreateTime1);		        
		
		mDb.updateNote(mNoteNumber1,
		 		 mNoteTitle2,
		 		 mNotePictureString2,
				 "", //???
				 "", //???
		 		 mNoteBodyString2,
		 		 mMarkingIndex2,
		 		 mCreateTime2);	
    	mDb.doClose();
	}

    // list view listener: on mark
    private DragSortListView.MarkListener onMark =
        new DragSortListView.MarkListener() 
		{   @Override
            public void mark(int position) 
			{
                mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
                if(position >= mNotesCursor.getCount()) //end of list
                	return ;
                
                String strNote = mDb.getNoteTitle(position);
                String strNotePicture = mDb.getNotePictureUri(position);
                String strNoteBody = mDb.getNoteBody(position);
                Long idNote =  mDb.getNoteId(position);
			
                DragSortListView parent = mDndListView;
                if(mDb.getNoteMarking(position) == 0)                
              	  mDb.updateNote(idNote, strNote, strNotePicture, "" , "", strNoteBody , 1, 0); //???
                else
              	  mDb.updateNote(idNote, strNote, strNotePicture, "" , "", strNoteBody ,0, 0); //???
                
                mDb.doClose();
              
                // save index and top position
                int index = parent.getFirstVisiblePosition();
                View v = parent.getChildAt(0);
                int top = (v == null) ? 0 : v.getTop();
              
                fillData();
              
                // restore index and top position
                ((DragSortListView) parent).setSelectionFromTop(index, top);
                
                return;
            }
        };    
    
    // set footer
    void setFooter()
    {
	    mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
	    TextView footerTextView = (TextView) getActivity().findViewById(R.id.footerText);
	    if(footerTextView != null) //add this for avoiding null exception when after e-Mail action
	    {
		    footerTextView.setText( mDb.getCheckedNotesCount() + 
		    		                " / " +
		    		                mDb.getNotesCount() + 
		    		                " ( " +
		    		                getResources().getText(R.string.footer_checked).toString() + 
		    		                " / " +
		    		                getResources().getText(R.string.footer_total).toString() +
		       		                " ) " );
	    }
	    mDb.doClose();
    }
	
	class ItemAdapter extends SimpleDragSortCursorAdapter 
	{
		public ItemAdapter(Context context, int layout, Cursor c,
							String[] from, int[] to, int flags) 
		{
			super(context, layout, c, from, to, flags);
		}

		private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

		private class ViewHolder {
			public TextView textTitle;
			public TextView textBody;
			public TextView textTime;
			public ImageView imagePicture;
			public ImageView imageCheck;
			public ImageView imageDragger;
		}

		@Override
		public int getCount() {
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			int count = mDb.getNotesCount();
			mDb.doClose();
			return count;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			if (convertView == null) 
			{
				view = getActivity().getLayoutInflater().inflate(R.layout.activity_main_list_row, parent, false);
				holder = new ViewHolder();
				holder.textTitle = (TextView) view.findViewById(R.id.text_title);
				holder.textBody = (TextView) view.findViewById(R.id.text_body);
				holder.textTime = (TextView) view.findViewById(R.id.text_time);
				holder.imageCheck= (ImageView) view.findViewById(R.id.img_check);
				holder.imagePicture = (ImageView) view.findViewById(R.id.img_picture);
				holder.imageDragger = (ImageView) view.findViewById(R.id.img_dragger);
				view.setTag(holder);
			} 
			else 
			{
				holder = (ViewHolder) view.getTag();
			}

			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			// show check box, title , picture
			holder.textTitle.setText(mDb.getNoteTitle(position));
			holder.textTitle.setTextColor(Util.mText_ColorArray[mStyle]);   

			if( mDb.getNotePictureUri(position).isEmpty() )
			{
//				holder.imagePicture.setVisibility(View.GONE);
				holder.imagePicture.setImageResource(mStyle%2 == 1 ?
		    			R.drawable.btn_radio_off_holo_light:
		    			R.drawable.btn_radio_off_holo_dark);//R.drawable.ic_empty
			}
			else
			{
				if(Util.isUriExist(mDb.getNotePictureUri(position), getActivity()))
				{
					holder.imagePicture.setVisibility(View.VISIBLE);
					imageLoader.displayImage(mDb.getNotePictureUri(position) ,
											 holder.imagePicture,
											 options,
											 animateFirstListener); //??? how can this find image file which had been deleted
				}
				else
    				holder.imagePicture.setImageResource(R.drawable.ic_cab_done_holo);
			}
	        
			// show body or not
		  	mPref_show_note_attribute = getActivity().getSharedPreferences("show_note_attribute", 0);
		  	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
		  	{
				holder.textBody.setText(mDb.getNoteBody(position));
				holder.textBody.setTextColor(Util.mText_ColorArray[mStyle]);
				holder.textTime.setText(Util.getTimeString(mDb.getNoteCreatedTime(position)));
				holder.textTime.setTextColor(Util.mText_ColorArray[mStyle]);
		  	}
		  	else
		  	{
		  		holder.textBody.setVisibility(View.GONE); // 1 invisible
		  		holder.textTime.setVisibility(View.GONE); // 1 invisible
		  	}
		  	
		  	// dragger
		  	mPref_show_note_attribute = getActivity().getSharedPreferences("show_note_attribute", 0);
		  	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
		  		holder.imageDragger.setVisibility(View.VISIBLE); 
		  	else
		  		holder.imageDragger.setVisibility(View.GONE); 
			
		  	// marking
			if( mDb.getNoteMarking(position) == 1)
				holder.imageCheck.setBackgroundResource(mStyle%2 == 1 ?
		    			R.drawable.btn_check_on_holo_light:
		    			R.drawable.btn_check_on_holo_dark);	
			else
				holder.imageCheck.setBackgroundResource(mStyle%2 == 1 ?
						R.drawable.btn_check_off_holo_light:
						R.drawable.btn_check_off_holo_dark);
			
			mDb.doClose();
			return view;
		}
	}

	private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener
	{

		static final List<String> displayedImages = Collections.synchronizedList(new LinkedList<String>());

		@Override
		public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) 
		{
			if (loadedImage != null) 
			{
				ImageView imageView = (ImageView) view;
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) 
				{
					FadeInBitmapDisplayer.animate(imageView, 500);
					displayedImages.add(imageUri);
				}
			}
		}
	}

	
	/*******************************************
	 * 					menu
	 *******************************************/
    // Menu identifiers
    static final int CHECK_ALL = R.id.CHECK_ALL;
    static final int UNCHECK_ALL = R.id.UNCHECK_ALL;
    static final int MOVE_CHECKED_NOTE = R.id.MOVE_CHECKED_NOTE;
    static final int COPY_CHECKED_NOTE = R.id.COPY_CHECKED_NOTE;
    static final int MAIL_CHECKED_NOTE = R.id.MAIL_CHECKED_NOTE;
    static final int DELETE_CHECKED_NOTE = R.id.DELETE_CHECKED_NOTE;
    static final int SLIDE_SHOW_CHECKED_NOTE = R.id.SLIDE_SHOW_CHECKED_NOTE;
    
	int noteCount;
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) 
        {
	        case CHECK_ALL:
	        	checkAll(1); 
	            return true;
	        case UNCHECK_ALL:
	        	checkAll(0); 
	            return true;
	        case MOVE_CHECKED_NOTE:
	        case COPY_CHECKED_NOTE:
	    		if(!noItemChecked())
	        	{
		    		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		    		String copyItems[] = new String[mDb.getCheckedNotesCount()];
		    		String copyItemsPicture[] = new String[mDb.getCheckedNotesCount()];
		    		String copyItemsBody[] = new String[mDb.getCheckedNotesCount()];
		    		Long copyItemsTime[] = new Long[mDb.getCheckedNotesCount()];
		    		int cCopy = 0;
		    		for(int i=0;i<mDb.getNotesCount();i++)
		    		{
		    			if(mDb.getNoteMarking(i) == 1)
		    			{
		    				copyItems[cCopy] = mDb.getNoteTitle(i);
		    				copyItemsPicture[cCopy] = mDb.getNotePictureUri(i);
		    				copyItemsBody[cCopy] = mDb.getNoteBody(i);
		    				copyItemsTime[cCopy] = mDb.getNoteCreatedTime(i);
		    				cCopy++;
		    			}
		    		}
		    		mDb.doClose();
		           
		    		if(item.getItemId() == MOVE_CHECKED_NOTE)
		    			operateCheckedTo(copyItems, copyItemsPicture, copyItemsBody, copyItemsTime, MOVE_TO); // move to
		    		else if(item.getItemId() == COPY_CHECKED_NOTE)
			    		operateCheckedTo(copyItems, copyItemsPicture, copyItemsBody, copyItemsTime, COPY_TO);// copy to
		    			
	        	}
	        	else
	    			Toast.makeText(getActivity(),
							   R.string.delete_checked_no_checked_items,
							   Toast.LENGTH_SHORT)
					     .show();
	            return true;
	            
	        case MAIL_CHECKED_NOTE:
	    		if(!noItemChecked())
	        	{
		        	// set Sent string Id
					List<Long> rowArr = new ArrayList<Long>();
					List<String> pictureFileNameList = new ArrayList<String>();
	            	int j=0;
		    		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		    		for(int i=0;i<mDb.getNotesCount();i++)
		    		{
		    			if(mDb.getNoteMarking(i) == 1)
		    			{
		    				rowArr.add(j,(long) mDb.getNoteId(i));
		    				j++;
		    				
		    				String picFile = mDb.getNotePictureUriById((long) mDb.getNoteId(i));
//		    				System.out.println("picFile = " + picFile);
		    				if((picFile != null) && (picFile.length() > 0))
		    					pictureFileNameList.add(picFile);
		    			}
		    		}
		    		mDb.doClose();
		    		
		    		// message
		    		Intent intentMail = new Intent(getActivity(), SendMailAct.class);
		    		String extraStr = Util.getSendStringWithXmlTag(rowArr);
		    		extraStr = Util.addRssVersionAndChannel(extraStr);
		    		intentMail.putExtra("SentString", extraStr);
		    		
		    		// picture array
		    		int cnt = pictureFileNameList.size();
		    		String pictureFileNameArr[] = new String[cnt];
		    		for(int i=0; i < cnt ; i++ )
		    		{
		    			pictureFileNameArr[i] = pictureFileNameList.get(i);
		    		}
		    		intentMail.putExtra("SentPictureFileNameArray", pictureFileNameArr );
		    		
					startActivity(intentMail);
	        	}
	        	else
	    			Toast.makeText(getActivity(),
							   R.string.delete_checked_no_checked_items,
							   Toast.LENGTH_SHORT)
						 .show();
	        	return true;
	        	
	        case DELETE_CHECKED_NOTE:
	        	if(!noItemChecked())
	        		deleteCheckedNotes();
	        	else
	    			Toast.makeText(getActivity(),
	    						   R.string.delete_checked_no_checked_items,
	    						   Toast.LENGTH_SHORT)
	    				 .show();
	            return true;     

	        case SLIDE_SHOW_CHECKED_NOTE:
	        	if(noItemChecked())
	        	{
	    			Toast.makeText(getActivity(),
 						   R.string.delete_checked_no_checked_items,
 						   Toast.LENGTH_SHORT)
 						 .show();	
	        	}
	    		Intent intentSlideshowEdit = new Intent(getActivity(), SlideshowEditor.class);
				startActivity(intentSlideshowEdit);
	        	
	            return true;	            
	            
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
	
	static public void swap()
	{
        mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        int startCursor = mDb.getNotesCount()-1;
        mDb.doClose();
        int endCursor = 0;
		
		//reorder data base storage for ADD_NEW_TO_TOP option
		int loop = Math.abs(startCursor-endCursor);
		for(int i=0;i< loop;i++)
		{
			swapRows(startCursor,endCursor);
			if((startCursor-endCursor) >0)
				endCursor++;
			else
				endCursor--;
		}
	}
    
	/**
	 *  check all or uncheck all
	 */
	public void checkAll(int action) 
	{
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		for(int i=0;i<mDb.getNotesCount();i++)
		{
			Long rowId = mDb.getNoteId(i);
			String noteTitle = mDb.getNoteTitle(i);
			String notePicture = mDb.getNotePictureUri(i);
			String noteBody = mDb.getNoteBody(i);
			mDb.updateNote(rowId, noteTitle, notePicture, "", "", noteBody , action, 0);//??? // action 1:check all, 0:uncheck all
		}
		mDb.doClose();
		fillData();
	}
	
    /**
     *   operate checked to: move to, copy to
     * 
     */
	void operateCheckedTo(final String[] copyItems,final String[] copyItemsPicture, final String[] copyItemsBody, final Long[] copyItemsTime, final int action)
	{
		//list all tabs
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		int tabCount = DB.getTabsCount();
		final String[] tabNames = new String[tabCount];
		final int[] tableIds = new int[tabCount];
		for(int i=0;i<tabCount;i++)
		{
			tabNames[i] = DB.getTabTitle(i);
			tableIds[i] = DB.getTab_NotesTableId(i);
		}
		tabNames[TabsHostFragment.mCurrentTabIndex] = tabNames[TabsHostFragment.mCurrentTabIndex] + " *"; // add mark to current page 
		mDb.doClose();
		   
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				//keep original table id
				String curTableNum = DB.getNotesTableId();

				//copy checked item to destination tab
				String destTableNum = String.valueOf(tableIds[which]);
				DB.setNotesTableId(destTableNum);
				mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
				for(int i=0;i< copyItems.length;i++)
				{
					mDb.insertNote(copyItems[i],copyItemsPicture[i], "", "", copyItemsBody[i],copyItemsTime[i]); //???
				}
				mDb.doClose();
				
				//recover to original table id
				if(action == MOVE_TO)
				{
					DB.setNotesTableId(curTableNum);
					mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
					//delete checked
					for(int i=0;i< mDb.getNotesCount() ;i++)
					{
						if(mDb.getNoteMarking(i) == 1)
							mDb.deleteNote(mDb.getNoteId(i));
					}
					mDb.doClose();
					fillData();
				}
				else if(action == COPY_TO)
				{
					DB.setNotesTableId(curTableNum);
					if(destTableNum.equalsIgnoreCase(curTableNum))
						fillData();
				}
				
				dialog.dismiss();
			}
		};
		
		if(action == MOVE_TO)
			builder.setTitle(R.string.checked_notes_move_to_dlg);
		else if(action == COPY_TO)
			builder.setTitle(R.string.checked_notes_copy_to_dlg);
		
		builder.setSingleChoiceItems(tabNames, -1, listener)
		  	.setNegativeButton(R.string.btn_Cancel, null);
		
		// override onShow to mark current page status
		AlertDialog alertDlg = builder.create();
		alertDlg.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dlgInterface) {
				// add mark for current page
				Util util = new Util(getActivity());
				util.markCurrent(dlgInterface);
			}
		});
		alertDlg.show();
	}
	
	/**
	 * delete checked notes
	 */
	public void deleteCheckedNotes()
	{
		final Context context = getActivity();

		mPref_delete_warn = context.getSharedPreferences("delete_warn", 0);
    	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
           mPref_delete_warn.getString("KEY_DELETE_CHECKED_WARN","yes").equalsIgnoreCase("yes"))
    	{
			Util util = new Util(getActivity());
			util.vibrate();
    		
    		// show warning dialog
			Builder builder = new Builder(context);
			builder.setTitle(R.string.delete_checked_note_title)
					.setMessage(R.string.delete_checked_message)
					.setNegativeButton(R.string.btn_Cancel, 
							new OnClickListener() 
					{	@Override
						public void onClick(DialogInterface dialog, int which) 
						{/*cancel*/} })
					.setPositiveButton(R.string.btn_OK, 
							new OnClickListener() 
					{	@Override
						public void onClick(DialogInterface dialog, int which) 
						{
							mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
							for(int i=0;i< mDb.getNotesCount() ;i++)
							{
								if(mDb.getNoteMarking(i) == 1)
									mDb.deleteNote(mDb.getNoteId(i));
							}
							mDb.doClose();
							fillData();
						}
					});
			
	        AlertDialog d = builder.create();
	        d.show();
    	}
    	else
    	{
    		// not show warning dialog
			mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
			for(int i=0;i< mDb.getNotesCount() ;i++)
			{
				if(mDb.getNoteMarking(i) == 1)
					mDb.deleteNote(mDb.getNoteId(i));
			}
			mDb.doClose();
			fillData();
    	}
	}
    
	@Override
	public void onDestroy() {
		mDb.doClose();
		super.onDestroy();
	}
	
	boolean noItemChecked()
	{
		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		int checkedItemCount = mDb.getCheckedNotesCount(); 
		mDb.doClose();
		return (checkedItemCount == 0);
	}
	
	/*
	 * inner class for note list loader
	 */
	public static class NoteListLoader extends AsyncTaskLoader<List<String>> 
	{
		List<String> mApps;

		public NoteListLoader(Context context) {
			super(context);
		}

		@Override
		public List<String> loadInBackground() {
			List<String> entries = new ArrayList<String>();
			return entries;
		}

		@Override
		protected void onStartLoading() {
//			System.out.println("NoteFragment / _onStartLoading");
			forceLoad();
		}
	}

	/*
	 * 	inner class for note list adapter
	 */
	public static class NoteListAdapter extends ArrayAdapter<String> 
	{
		public NoteListAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1);
		}
		public void setData(List<String> data) {
			clear();
			if (data != null) {		
					addAll(data);
			}
		}
	}
}

package com.cwc.litenote;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Note_addNew extends Activity {

    static Long mRowId;
    static String mCameraPictureUri;
    SharedPreferences mPref_style;
    SharedPreferences mPref_delete_warn;
    Note_common note_common;
    static boolean mEnSaveDb = true;
	static String mPictureUriInDB;
	private static DB mDb;
    boolean bUseCameraPicture;
    Button addButton;
    String mAddWhat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.note_add_new);
        setTitle(R.string.add_new_note_title);// set title
        
        System.out.println("Note_addNew / onCreate");
        
        note_common = new Note_common(this);
        note_common.UI_init();
        mPictureUriInDB = "";
        mCameraPictureUri = "";
        bUseCameraPicture = false;
			
        // get row Id from saved instance
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DB.KEY_NOTE_ID);
        
        // get picture Uri in DB if instance is not null
        mDb = new DB(Note_addNew.this);
        if(savedInstanceState != null)
        {
	        mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
	        System.out.println("Note_addNew / mRowId =  " + mRowId);
	        if(mRowId != null)
	        {
	        	mPictureUriInDB = mDb.getNotePictureUriById(mRowId);
	       		Note_common.mCurrentPictureUri = mPictureUriInDB;
	        }
	        mDb.doClose();
        }
        
        
        mAddWhat = getIntent().getExtras().getString("ADD_NEW");
        
        if(mAddWhat.equalsIgnoreCase("TEXT"))
        {        
	        note_common.populateFields(mRowId);
	        
	    	// button: add
	        addButton = (Button) findViewById(R.id.note_add_new_add);
	        addButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_input_add, 0, 0, 0);
	        
			// set add note button visibility
	        if(!note_common.isEdited())
	        	addButton.setVisibility(View.GONE);
	        
	        Note_common.mTitleEditText.addTextChangedListener(setTextWatcher());
	        Note_common.mBodyEditText.addTextChangedListener(setTextWatcher());
	
	        // listener: add new note
	        addButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) {
	            	
				    Intent intent = getIntent();
	            	if(!note_common.isEdited())
					    intent.putExtra("NOTE_ADDED","empty");
	            	else
	            		intent.putExtra("NOTE_ADDED","edited");
				    
	            	setResult(Activity.RESULT_OK, intent);
	                mEnSaveDb = true;
	                finish();
	            }
	
	        });
	        
	        // button: cancel new note
	        Button cancelButton = (Button) findViewById(R.id.note_add_new_cancel);
	        cancelButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
	        // listener: cancel
	        cancelButton.setOnClickListener(new View.OnClickListener() {
	
	            public void onClick(View view) 
	            {
	            	if(note_common.isEdited())
	            		confirmUpdateChangeDlg();
	            	else
	            	{
	                    note_common.deleteNote(mRowId);
	                    mEnSaveDb = false;
	                    setResult(RESULT_CANCELED, getIntent());
	                    finish();
	            	}
	            }
	        });
        }
        else if(mAddWhat.equalsIgnoreCase("NEW_PICTURE"))
        {
        	takePicture();
        }
    }
    
    TextWatcher setTextWatcher()
    {
    	return new TextWatcher(){
	        public void afterTextChanged(Editable s) 
	        {
			    if(!note_common.isEdited())
		        	addButton.setVisibility(View.GONE);
		        else
		        	addButton.setVisibility(View.VISIBLE);
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
    	};
    }
    
    // confirmation to update change or not
    void confirmUpdateChangeDlg()
    {
        getIntent().putExtra("NOTE_ADDED","edited");

        AlertDialog.Builder builder = new AlertDialog.Builder(Note_addNew.this);
		builder.setTitle(R.string.confirm_dialog_title)
	           .setMessage(R.string.add_new_note_confirm_save)
			   .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
					    mEnSaveDb = true;
		            	setResult(RESULT_OK, getIntent());
					    finish();
					}})
			   .setNeutralButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{   // do nothing
					}})					
			   .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						note_common.deleteNote(mRowId);
	                    mEnSaveDb = false;
	                    setResult(RESULT_CANCELED, getIntent());
	                    finish();
					}})
			   .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	if(savedInstanceState.getBoolean("UseCameraPicture"))
    		bUseCameraPicture = true;
    	else
    		bUseCameraPicture = false;
    	
    	mCameraPictureUri = savedInstanceState.getString("showCameraPictureUri");
    	
    	if(bUseCameraPicture)
    		Note_common.mCurrentPictureUri = mCameraPictureUri;
    	
    	if(savedInstanceState.getBoolean("ShowConfirmContinueDialog"))
    	{
    		showContinueConfirmationDialog();
    		System.out.println("showContinueDialog again");
    	}
    	
//    	System.out.println("Note_addNew / onRestoreInstanceState / bUseCameraPicture = " + bUseCameraPicture);
//    	System.out.println("Note_addNew / onRestoreInstanceState / mCameraPicFileName = " + mCameraPictureUri);
    }

    // for Add new note
    // for Add new picture (stage 1)
    // for Rotate screen (stage 2)
    @Override
    protected void onPause() {
    	System.out.println("Note_addNew / onPause");
        super.onPause();
        mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,mPictureUriInDB, "", ""); //???
    }

    // for Add new picture (stage 2)
    // for Rotate screen (stage 2)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
   	 	System.out.println("Note_addNew / onSaveInstanceState");
//   	 	System.out.println("Note_addNew / onSaveInstanceState / mRowId = " + mRowId );
//   	 	System.out.println("Note_addNew / onSaveInstanceState / mEnSaveDb = " + mEnSaveDb );
//   	 	System.out.println("Note_addNew / onSaveInstanceState / mCameraPictureUri = " + mCameraPictureUri );
   	 	
        if(bUseCameraPicture)
        {
        	outState.putBoolean("UseCameraPicture",true);
        	outState.putString("showCameraPictureUri", mCameraPictureUri);
        }
        else
        {
        	outState.putBoolean("UseCameraPicture",false);
        	outState.putString("showCameraPictureUri", "");
        }
        
        // if confirmation dialog still shows?
        if(UtilImage.bShowExpandedImage == true)
        {
        	UtilImage.mDialog.dismiss();
        	outState.putBoolean("ShowConfirmContinueDialog",true);
        }
        else
        	outState.putBoolean("ShowConfirmContinueDialog",false);
        
        mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,mPictureUriInDB, "", "" ); //???
        note_common.mRowId = mRowId;
        outState.putSerializable(DB.KEY_NOTE_ID, mRowId);
    }
    
    @Override
    public void onBackPressed() {
    	if(UtilImage.bShowExpandedImage == true)
	    	UtilImage.closeExpandImage();
	    else
	    {
	    	if(note_common.isEdited())
	    	{
	    		confirmUpdateChangeDlg();
	    	}
	    	else
	    	{
	            note_common.deleteNote(mRowId);
	            mEnSaveDb = false;
	            finish();
	    	}
	    }
    }
    
    static final int TAKE_PICTURE = R.id.TAKE_PICTURE;
	static int TAKE_PICTURE_ACT = 1;    
	private Uri imageUri;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    menu.add(0, TAKE_PICTURE, 0, R.string.note_take_picture )
	    .setIcon(android.R.drawable.ic_menu_camera)
	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}
    
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch (item.getItemId()) 
        {
            case TAKE_PICTURE:
            	takePicture();
			    return true;
            
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    void takePicture()
    {
    	Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
    	imageUri = UtilImage.getPictureUri(Util.getCurrentTimeString() + ".jpg",
    									   Note_addNew.this);
    	mPictureUriInDB = imageUri.toString();
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
	    startActivityForResult(intent, TAKE_PICTURE_ACT); 
	    mEnSaveDb = true;
	    
	    if(UtilImage.mExpandedImageView != null)
	    	UtilImage.closeExpandImage();
    }
    
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
	{
		System.out.println("Note_addNew / onActivityResult");
		if (requestCode == TAKE_PICTURE_ACT)
		{
			System.out.println("Note_addNew / onActivityResult / requestCode == TAKE_PICTURE_ACT");
			if (resultCode == Activity.RESULT_OK)
			{
				// disable Rotate to avoid leak window
//				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);				
				System.out.println("Note_addNew / onActivityResult /resultCode == Activity.RESULT_OK");
				note_common.populateFields(mRowId);
	            
	            // set for Rotate any times
	            bUseCameraPicture = true; 
	            mCameraPictureUri = Note_common.mCurrentPictureUri;
	            
	            
	        	SharedPreferences pref_takePicture;
        		pref_takePicture = getSharedPreferences("takePicutre", 0);
	        	if(pref_takePicture.getString("KEY_SHOW_CONFIRMATION_DIALOG","yes").equalsIgnoreCase("yes"))
	        	{
		            // set Continue Taking Picture dialog
	        		showContinueConfirmationDialog();
					System.out.println("Note_addNew / onActivityResult /showContinueConfirmationDialog");
	        	}
	        	else
	        	{
					System.out.println("Note_addNew / onActivityResult /not showContinueConfirmationDialog");
	        		// save picture Uri
	    			SharedPreferences pref_add_new_note_option = getSharedPreferences("add_new_note_option", 0);
	        		if(pref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_AT_TOP","false").equalsIgnoreCase("true") ||
	        		   getIntent().getExtras().getString("ADD_NEW_TO_TOP", "true").equalsIgnoreCase("true") )
	        		{
	                    Note_common.saveStateInDB( mRowId,true,mPictureUriInDB, "", ""); //???
	                    NoteFragment.swap();
	                    System.out.println("=== swap");
	        		}
	        		
	        		// take picture without confirmation dialog 
		  		    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
		  	    	Uri imageUri = UtilImage.getPictureUri(Util.getCurrentTimeString() + ".jpg", this);
		  			Note_addNew.mPictureUriInDB = imageUri.toString();

		  		    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
		  		    Note_addNew.mRowId = null; // set null for Insert
		  		    startActivityForResult(intent, Note_addNew.TAKE_PICTURE_ACT);		        		
	        	}

	            // enable Rotate 
//	            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				bUseCameraPicture = false;
				Toast.makeText(Note_addNew.this, R.string.note_take_picture_Cancel, Toast.LENGTH_LONG).show();
				
		    	System.out.println("Note_addNew / onActivityResult / RESULT_CANCELED  / mCameraPictureUri = " + mCameraPictureUri);

				mEnSaveDb = true;

	        	SharedPreferences pref_takePicture;
        		pref_takePicture = getSharedPreferences("takePicutre", 0);
	        	
        		if(pref_takePicture.getString("KEY_SHOW_CONFIRMATION_DIALOG","yes").equalsIgnoreCase("yes"))
        		{
        			// show confirmation dialog
    				if( UtilImage.bEnableContinue == true)
    				{
                        note_common.deleteNote(mRowId);
                        mEnSaveDb = false;
                        setResult(RESULT_CANCELED, getIntent());
                        finish();
                        return; // must add this
    				}

    				if(!mCameraPictureUri.isEmpty())
    				{
    					// update
    					Note_common.saveStateInDB(mRowId,mEnSaveDb,mCameraPictureUri, "", ""); //??? // replace with existing picture
    					System.out.println("Note_addNew / onActivityResult / RESULT_CANCELED / update mCameraPictureUri = " + mCameraPictureUri);
    					note_common.populateFields(mRowId);
    		            
    					// set for Rotate any times
    		            bUseCameraPicture = true;
    		            mPictureUriInDB = Note_common.mCurrentPictureUri; // for pause
    		            mCameraPictureUri = Note_common.mCurrentPictureUri; // for save instance
    				}
    				else
    				{
    					// delete unused picture name
    					System.out.println("Note_addNew / onActivityResult / RESULT_CANCELED / set picture Uri in DB = " + "");
    					Note_common.saveStateInDB(mRowId,mEnSaveDb,"", "", ""); //??? 
    					
    					// reset DB status
    					mRowId = null;
    					mPictureUriInDB = "";
    				}
        		}
        		else	
				{   // not show confirmation dialog
                    note_common.deleteNote(mRowId);
                    mEnSaveDb = false;
                    setResult(RESULT_CANCELED, getIntent());
                    finish();
                    return; // must add this
				}
			}
			
			// set add note button visibility
			if(mAddWhat.equalsIgnoreCase("TEXT"))
			{
			    if(!note_common.isEdited())
		        	addButton.setVisibility(View.GONE);
		        else
		        	addButton.setVisibility(View.VISIBLE);
			}
		}
	}
	
	
	// show Continue dialog
	void showContinueConfirmationDialog()
	{
        final String zoomPicture = note_common.mPictureUriInDB;
        
        System.out.println("zoomPicture = " + zoomPicture);
		if(note_common.mPicImageView.getVisibility() == View.VISIBLE)
	    {
	    	note_common.mPicImageView.post(new Runnable() {
		        @Override
		        public void run() {
		        	try {
		        		boolean bAfterTake = true;
						UtilImage.zoomImageFromThumb(note_common.mPicImageView,
													 zoomPicture ,
													 Note_addNew.this,
													 bAfterTake);
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("zoom error");
					}
		        } 
		    });
	    }
	}
	
}

package com.cwc.litenote;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Note_edit extends Activity {

    private Long mRowId, mCreatedTime;
    private String mTitle, mPictureUri, mCameraPictureUri, mBody;
    SharedPreferences mPref_style;
    SharedPreferences mPref_delete_warn;
    Note_common note_common;
    private boolean mEnSaveDb = true;
    boolean bUseCameraPicture;
    DB mDb;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.note_edit);
        setTitle(R.string.edit_note_title);// set title
    	
        System.out.println("Note_edit / onCreate");
        
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
        
    	Bundle extras = getIntent().getExtras();
    	mRowId = extras.getLong(DB.KEY_NOTE_ID);
    	mPictureUri = extras.getString(DB.KEY_NOTE_PICTURE_URI);
    	mTitle = extras.getString(DB.KEY_NOTE_TITLE);
    	mBody = extras.getString(DB.KEY_NOTE_BODY);
    	mCreatedTime = extras.getLong(DB.KEY_NOTE_CREATED);
        
    	//initialization
    	//???
        note_common = new Note_common(this, mRowId, mTitle, mPictureUri, "", "", mBody, mCreatedTime);
        note_common.UI_init();
        mCameraPictureUri = "";
        bUseCameraPicture = false;
        
        // get picture Uri in DB if instance is not null
        mDb = new DB(Note_edit.this);
        if(savedInstanceState != null)
        {
	        mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
	        System.out.println("Note_edit / onCreate / mRowId =  " + mRowId);
	        if(mRowId != null)
	        {
	        	mPictureUri = mDb.getNotePictureUriById(mRowId);
	       		Note_common.mCurrentPictureUri = mPictureUri;
	        }
	        mDb.doClose();
        }
        
    	// show view
        note_common.populateFields(mRowId);
		
		// OK button: edit OK, save
        Button okButton = (Button) findViewById(R.id.note_edit_ok);
        okButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
		// OK
        okButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
				if(note_common.bRemovePictureUri)
				{
					mPictureUri = "";
				}
                mEnSaveDb = true;
                finish();
            }

        });
        
        // delete button: delete note
        Button delButton = (Button) findViewById(R.id.note_edit_delete);
        delButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
        // delete
        delButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
				//warning :start
        		mPref_delete_warn = getSharedPreferences("delete_warn", 0);
            	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
            	   mPref_delete_warn.getString("KEY_DELETE_NOTE_WARN","yes").equalsIgnoreCase("yes")) 
            	{
        			Util util = new Util(Note_edit.this);
    				util.vibrate();
            		
            		Builder builder1 = new Builder(Note_edit.this ); 
            		builder1.setTitle(R.string.confirm_dialog_title)
                        .setMessage(R.string.confirm_dialog_message)
                        .setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener()
                        {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                        	{/*nothing to do*/}
                        })
                        .setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener()
                        {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                        	{
                        		note_common.deleteNote(mRowId);
                            	finish();
                        	}
                        })
                        .show();//warning:end
            	}
            	else{
            	    //no warning:start
	                setResult(RESULT_CANCELED);
	                note_common.deleteNote(mRowId);
	                finish();
            	}
            }
        });
        
        // cancel button: leave, do not save current modification
        Button cancelButton = (Button) findViewById(R.id.note_edit_cancel);
        cancelButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
        // cancel
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                
                // check if note content is modified
               	if(note_common.isModified())
            	{
               		// show confirmation dialog
            		confirmToUpdateDlg();
            	}
            	else
            	{
            		mEnSaveDb = false;
                    finish();
            	}
            }
        });
    }
    
    // confirm to update change or not
    void confirmToUpdateDlg()
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(Note_edit.this);
		builder.setTitle(R.string.confirm_dialog_title)
	           .setMessage(R.string.edit_note_confirm_update)
	           // Yes, to update
			   .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						if(note_common.bRemovePictureUri)
						{
							mPictureUri = "";
						}
					    mEnSaveDb = true;
					    finish();
					}})
			   // cancel
			   .setNeutralButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{   // do nothing
					}})
			   // no, roll back to original status		
			   .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						Bundle extras = getIntent().getExtras();
						String originalPictureFileName = extras.getString(DB.KEY_NOTE_PICTURE_URI);

						if(originalPictureFileName.isEmpty())
						{   // no picture at first
							note_common.removePictureStringFromOriginalNote(mRowId);
		                    mEnSaveDb = false;
						}
						else
						{	// roll back existing picture
							Note_common.bRollBackData = true;
							mPictureUri = originalPictureFileName;
							mEnSaveDb = true;
						}	
	                    finish();
					}})
			   .show();
    }
    

    // for finish(), for Rotate screen
    @Override
    protected void onPause() {
        super.onPause();
        
        System.out.println("Note_edit / onPause / mEnSaveDb = " + mEnSaveDb);
        System.out.println("Note_edit / onPause / mPictureUri = " + mPictureUri);
        
        mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,mPictureUri, "", ""); //??? 
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        System.out.println("Note_edit / onSaveInstanceState / mEnSaveDb = " + mEnSaveDb);
        System.out.println("Note_edit / onSaveInstanceState / bUseCameraPicture = " + bUseCameraPicture);
        System.out.println("Note_edit / onSaveInstanceState / mCameraPictureUri = " + mCameraPictureUri);
        
        
        if(note_common.bRemovePictureUri)
    	    outState.putBoolean("removeOriginalPictureUri",true);
        
        if(bUseCameraPicture)
        {
        	outState.putBoolean("UseCameraPicture",true);
        	outState.putString("showCameraPictureUri", mPictureUri);
        }
        else
        {
        	outState.putBoolean("UseCameraPicture",false);
        	outState.putString("showCameraPictureUri", "");
        }
        
        mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,mPictureUri, "", ""); //??? 
        outState.putSerializable(DB.KEY_NOTE_ID, mRowId);
        
    }
    
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	if(savedInstanceState.getBoolean("UseCameraPicture"))
    		bUseCameraPicture = true;
    	else
    		bUseCameraPicture = false;
    	
    	mCameraPictureUri = savedInstanceState.getString("showCameraPictureUri");
    	
    	System.out.println("Note_edit / onRestoreInstanceState / savedInstanceState.getBoolean removeOriginalPictureUri =" +
    							savedInstanceState.getBoolean("removeOriginalPictureUri"));
        if(savedInstanceState.getBoolean("removeOriginalPictureUri"))
        {
        	mCameraPictureUri = "";
        	note_common.mOriginalPictureUri="";
        	Note_common.mCurrentPictureUri="";
        	note_common.removePictureStringFromOriginalNote(mRowId);
        	note_common.populateFields(mRowId);
        	note_common.bRemovePictureUri = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public void onBackPressed() {
	    if(UtilImage.bShowExpandedImage == true)
	    {
	    	UtilImage.closeExpandImage();
	    }
	    else
	    {
	    	if(note_common.isModified())
	    	{
	    		confirmToUpdateDlg();
	    	}
	    	else
	    	{
	            mEnSaveDb = false;
	            finish();
	    	}
	    }
    }
    
    static final int TAKE_PICTURE = R.id.TAKE_PICTURE;
	private static int TAKE_PICTURE_ACT = 1;    
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
		    case android.R.id.home:
		    	NavUtils.navigateUpTo(this, new Intent(this, DrawerActivity.class));
		        return true;
	        
            case TAKE_PICTURE:
            	Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            	// new picture Uri with current time stamp
            	imageUri = UtilImage.getPictureUri(Util.getCurrentTimeString() + ".jpg",
						   						   Note_edit.this); 
            	mPictureUri = imageUri.toString();
			    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
			    startActivityForResult(intent, TAKE_PICTURE_ACT); 
			    mEnSaveDb = true;
			    note_common.bRemovePictureUri = false; // reset
			    
			    if(UtilImage.mExpandedImageView != null)
			    	UtilImage.closeExpandImage();
		        
			    return true;
            
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
	{
		if (requestCode == TAKE_PICTURE_ACT)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				System.out.println("Note_edit / onActivity / .RESULT_OK");
				imageUri = Uri.parse(Note_common.mCurrentPictureUri);
//				String str = getResources().getText(R.string.note_take_picture_OK ).toString();
//	            Toast.makeText(Note_edit.this, str + " " + imageUri.toString(), Toast.LENGTH_SHORT).show();
	            note_common.populateFields(mRowId);
	            bUseCameraPicture = true;
	            mCameraPictureUri = Note_common.mCurrentPictureUri;
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				System.out.println("Note_edit / onActivity / .RESULT_CANCELED");
				bUseCameraPicture = false;
				// to use captured picture or original picture
				if(!mCameraPictureUri.isEmpty())
				{
					// update
					Note_common.saveStateInDB(mRowId,mEnSaveDb,mCameraPictureUri, "", "");//???  // replace with existing picture
					note_common.populateFields(mRowId);
		            
					// set for Rotate any times
		            bUseCameraPicture = true;
		            mPictureUri = Note_common.mCurrentPictureUri; // for pause
		            mCameraPictureUri = Note_common.mCurrentPictureUri; // for save instance

				}
				else
				{
					// skip new Uri, roll back to original one
			    	Note_common.mCurrentPictureUri = note_common.mOriginalPictureUri;
			    	mPictureUri = note_common.mOriginalPictureUri;
					Toast.makeText(Note_edit.this, R.string.note_take_picture_Cancel, Toast.LENGTH_LONG).show();
				}
				
				mEnSaveDb = true;
				Note_common.saveStateInDB(mRowId,mEnSaveDb,mPictureUri, "", ""); //???
				note_common.populateFields(mRowId);
			}
		}
		
        if(requestCode == Util.ACTIVITY_SELECT_PICTURE && resultCode == Activity.RESULT_OK)
        {
//			Bundle extras = imageReturnedIntent.getExtras();
//			String fileName = extras.getString("FILENAME");
			Uri selectedUri = imageReturnedIntent.getData(); 
			System.out.println("selected Uri = " + selectedUri.toString());
			
			String uriStr = selectedUri.toString();
        	System.out.println("check onActivityResult / uriStr = " + uriStr);
        	
        	mRowId = Note_common.saveStateInDB(mRowId,true,uriStr, "", ""); //??? 
        	
            note_common.populateFields(mRowId);
			
            // set for Rotate any times
            bUseCameraPicture = true;
            mPictureUri = Note_common.mCurrentPictureUri; // for pause
            mCameraPictureUri = Note_common.mCurrentPictureUri; // for save instance

        }   		
	}
}

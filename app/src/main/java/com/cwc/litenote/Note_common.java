/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cwc.litenote;

import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class Note_common {

    static EditText mTitleEditText;
    static String mOriginalTitle;
    
    ImageView mPicImageView;
    String mPictureUriInDB;
    String mOriginalPictureUri;
    static String mCurrentPictureUri;

    String mOriginalAudioUri;
    String mOriginalDrawingUri;

    static EditText mBodyEditText;
    
    static String mOriginalBody;
    
    Long mRowId;
	static Long mOriginalCreatedTime;
	static Long mOriginalMarking;
    
    static boolean bRollBackData;
    boolean bRemovePictureUri = false;
    boolean bEditPicture = false;

    private static DB mDb;
    SharedPreferences mPref_style;
    SharedPreferences mPref_delete_warn;
    static Activity mAct;
    int style;
    
    public Note_common(Activity act,Long rowId,String strTitle, String pictureUri, String audioUri, String drawingUri, String strBody, Long createdTime)
    {
    	mAct = act;
    	mRowId = rowId;
    			
    	mOriginalTitle = strTitle;
	    mOriginalBody = strBody;
	    mOriginalPictureUri = pictureUri;
	    mOriginalAudioUri = audioUri;
	    mOriginalDrawingUri = drawingUri;
	    
	    mOriginalCreatedTime = createdTime;
	    mCurrentPictureUri = pictureUri;
	    
    	DB.setNotesTableId(DB.getNotesTableId()); 
        mDb = new DB(mAct);
    	mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
	    mOriginalMarking = mDb.getNoteMarkingById(rowId);
		mDb.doClose();

		bRollBackData = false;
		bEditPicture = true;
    }
    
    public Note_common(Activity act)
    {
    	mAct = act;
    	DB.setNotesTableId(DB.getNotesTableId()); 
        mDb = new DB(mAct);
    }
    
    void UI_init()
    {
        mTitleEditText = (EditText) mAct.findViewById(R.id.edit_title);
        mPicImageView = (ImageView) mAct.findViewById(R.id.edit_picture);
        mBodyEditText = (EditText) mAct.findViewById(R.id.edit_body);
        
        mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
		style = mDb.getTabStyle(TabsHostFragment.mCurrentTabIndex);
		mDb.doClose();
        
		//set title color
		mTitleEditText.setTextColor(Util.mText_ColorArray[style]);
		mTitleEditText.setBackgroundColor(Util.mBG_ColorArray[style]);
		
		mPicImageView.setBackgroundColor(Util.mBG_ColorArray[style]);
		
		//set body color 
		mBodyEditText.setTextColor(Util.mText_ColorArray[style]);
		mBodyEditText.setBackgroundColor(Util.mBG_ColorArray[style]);	
		
		// set thumb nail listener
        mPicImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try 
                {
                	if(!mPictureUriInDB.isEmpty())
                	{
                		bRemovePictureUri = false;
                		boolean bAfterTake = false;
                		System.out.println("mPicImageView.setOnClickListener / mPictureUriInDB = " + mPictureUriInDB);
                		
                		// check if pictureUri has scheme
                		if(Util.isUriExist(mPictureUriInDB, mAct))
                		{
	                		if(Uri.parse(mPictureUriInDB).isAbsolute())
	                			UtilImage.zoomImageFromThumb(mPicImageView,
	                										 mPictureUriInDB ,
	                										 mAct,
	                										 bAfterTake);
	                		else
	                		{
	                			System.out.println("mPictureUriInDB is not Uri formate");
	                		}
                		}
                		else
                			Toast.makeText(mAct,R.string.file_not_found,Toast.LENGTH_SHORT).show();
                	}
                	else
            			Toast.makeText(mAct,R.string.file_is_not_created,Toast.LENGTH_SHORT).show();

				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
            }
        });
        
		// set thumb nail long click listener
        mPicImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
            	if(bEditPicture)
            		setPictureImage();
                return false;
            }
        });
    }
    
    
    void setPictureImage() 
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(mAct);
		builder.setTitle(R.string.edit_note_set_picture_dlg_title)
			   .setNeutralButton(R.string.btn_Select, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						bRemovePictureUri = false; // reset
//						Intent intent = new Intent(mAct, PictureGridAct.class);
//						intent.putExtra("gallery", false);
//						mAct.startActivityForResult(intent, Util.ACTIVITY_SELECT_PICTURE);
						
						Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
						intent.setType("image/*");
						mAct.startActivityForResult(Intent.createChooser(intent, 
													   "Select image"),
													   Util.ACTIVITY_SELECT_PICTURE);
						
					}})					
			   .setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{// cancel
					}});

		if(!mPictureUriInDB.isEmpty())
		{
				builder.setPositiveButton(R.string.btn_None, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					//just delete picture file name
					mCurrentPictureUri = "";
					mOriginalPictureUri = "";
			    	removePictureStringFromCurrentEditNote(mRowId);
			    	populateFields(mRowId);
			    	bRemovePictureUri = true;
				}});
		}
		
		Dialog dialog = builder.create();
		dialog.show();
    }
    
    void deleteNote(Long rowId)
    {
        mDb.open();
        // for Add new note (mRowId is null first), but decide to cancel 
        if(rowId != null)
        	mDb.deleteNote(rowId);
        mDb.close();
    }
    
    void populateFields(Long rowId) 
    {
    	mDb.open();
    	
    	if (rowId != null) 
    	{
    		mPictureUriInDB = mDb.getNotePictureUriById(rowId);
			System.out.println("populateFields / mPictureFileNameInDB = " + mPictureUriInDB);
    		
    		if(!mPictureUriInDB.isEmpty())
    		{
    			Uri imageUri = Uri.parse(mPictureUriInDB);
				System.out.println("populateFields / set image bitmap / imageUri = " + imageUri);
	    		// set picture
				try 
				{
					mPicImageView.setVisibility(View.VISIBLE );
					mPicImageView.setImageBitmap(UtilImage.decodeSampledBitmapFromUri(imageUri, 50, 50, mAct));
				} 
				catch (Exception e) 
				{
			        Log.w("Picture file name is not found", e.toString());
					mPicImageView.setImageResource(R.drawable.ic_cab_done_holo);//ic_dialog_focused_holo);
			    }
    		}
    		else
    		{
				mPicImageView.setImageResource(style%2 == 1 ?
		    			R.drawable.btn_radio_off_holo_light:
		    			R.drawable.btn_radio_off_holo_dark);//R.drawable.ic_empty);
    		}
			
			String strTitleEdit = mDb.getNoteTitleById(rowId);
            mTitleEditText.setText(strTitleEdit);
            mTitleEditText.setSelection(strTitleEdit.length());

            String strBodyEdit = mDb.getNoteBodyById(rowId);
            mBodyEditText.setText(strBodyEdit);
            mBodyEditText.setSelection(strBodyEdit.length());
        }
        mDb.close();
    }
    
    boolean isTitleModified()
    {
    	return !mOriginalTitle.equals(mTitleEditText.getText().toString());
    }
    
    boolean isPictureModified()
    {
    	return !mOriginalPictureUri.equals(mPictureUriInDB);
    }
    
    boolean isBodyModified()
    {
    	return !mOriginalBody.equals(mBodyEditText.getText().toString());
    }
    
    boolean isTimeCreatedModified()
    {
    	return false; 
    }
    
    boolean isModified()
    {
    	boolean bModified = false;
    	if( isTitleModified() || isPictureModified() || isBodyModified() || bRemovePictureUri)
    	{
    		bModified = true;
    	}
    	
    	return bModified;
    }
    
    boolean isEdited()
    {
    	boolean bEdit = false;
    	String curTitle = mTitleEditText.getText().toString();
    	String curBody = mBodyEditText.getText().toString();
    	if(!curTitle.isEmpty() || !curBody.isEmpty() || !(null ==  mPictureUriInDB))    		
    		bEdit = true;
    	
    	return bEdit;
    }

	public static Long saveStateInDB(Long rowId,boolean enSaveDb, String pictureUri, String audioUri, String drawingUri) 
	{
		boolean mEnSaveDb = enSaveDb;
    	mDb.open();
        String title = mTitleEditText.getText().toString();
        String body = mBodyEditText.getText().toString();

        if(mEnSaveDb)
        {
	        if (rowId == null) // for Add new
	        {
	        	if( (!title.isEmpty()) || (!body.isEmpty()) ||(!pictureUri.isEmpty()))
	        	{
	        		// insert
	        		System.out.println("Note_common / saveState / insert");
	        		rowId = mDb.insertNote(title, pictureUri, audioUri, drawingUri, body, (long) 0);// add new note, get return row Id
	        	}
        		mCurrentPictureUri = pictureUri; // update file name
	        } 
	        else // for Edit
	        {
    	        Date now = new Date(); 
	        	if( (!title.isEmpty()) || (!body.isEmpty()) || (!pictureUri.isEmpty()))
	        	{
	        		// update
	        		if(bRollBackData) //roll back
	        		{
			        	System.out.println("Note_common / saveState / update: roll back");
	        			title = mOriginalTitle;
	        			body = mOriginalBody;
	        			Long time = mOriginalCreatedTime;
	        			mDb.updateNote(rowId, title, pictureUri, audioUri, drawingUri, body, mOriginalMarking, time);
	        		}
	        		else // update new
	        		{
	        			System.out.println("Note_common / saveState / update new");
	        			mDb.updateNote(rowId, title, pictureUri, audioUri, drawingUri, body, 0, now.getTime()); // update note
	        		}
	        		mCurrentPictureUri = pictureUri; // update file name
	        	}
	        	else if(title.isEmpty() && body.isEmpty() && pictureUri.isEmpty())
	        	{
	        		// delete
	        		System.out.println("Note_common / saveState / delete");
	        		mDb.deleteNote(rowId);
	        	}
	        }
        }
        mDb.close();
        
		return rowId;
	}
	
	public void removePictureStringFromOriginalNote(Long rowId) {
    	mDb.open();
    	mDb.updateNote(rowId, 
    				   mOriginalTitle,
    				   "", 
    				   mOriginalAudioUri,
    				   mOriginalDrawingUri,
    				   mOriginalBody,
    				   mOriginalMarking,
    				   mOriginalCreatedTime );
        mDb.close();
	}
	
	public void removePictureStringFromCurrentEditNote(Long rowId) {
        String title = mTitleEditText.getText().toString();
        String body = mBodyEditText.getText().toString();
        
    	mDb.open();
    	mDb.updateNote(rowId, 
    				   title,
    				   "", 
    				   mOriginalAudioUri,
    				   mOriginalDrawingUri,
    				   body,
    				   mOriginalMarking,
    				   mOriginalCreatedTime );
        mDb.close();
	}
}
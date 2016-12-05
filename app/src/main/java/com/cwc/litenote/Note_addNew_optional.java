package com.cwc.litenote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class Note_addNew_optional 
{
	RadioGroup mRadioGroup;
    AlertDialog mDialog = null;
	SharedPreferences mPref_add_new_note_option;
	static int ADD_NEW = 1;
	static int CONFIG = 2;
	int mAddAt = 99;
	boolean mSetOptional = true;

	Activity mActivity;
	Note_addNew_optional(){}
	
	Note_addNew_optional(final Activity activity, int count, int stage)
	{
		mActivity = activity;
		mPref_add_new_note_option = mActivity.getSharedPreferences("add_new_note_option", 0);

		if(count > 0)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
  		
			builder.setTitle(R.string.add_new_note_option_title)
				.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
 		        {	@Override
 		    		public void onClick(DialogInterface dialog, int which) {
 		    			//cancel
 		    		}
 		        });
			
			if(stage == CONFIG)
			{
  			   	builder.setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener()
 		        {	@Override
 		    		public void onClick(DialogInterface dialog, int which) 
 		        	{	//OK
	 					if(mAddAt ==0)
	 					{
	 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_TOP", "true").commit();
	 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_BOTTOM", "false").commit();
	 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_OPTIONAL", "false").commit();
	 					}
	 					else if(mAddAt ==1)
	 					{
	 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_TOP", "false").commit();
	 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_BOTTOM", "true").commit();
	 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_OPTIONAL", "false").commit();
	 					}
	 					else if(mAddAt ==2)
	 					{
	 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_TOP", "false").commit();
	 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_AT_BOTTOM", "false").commit();
	 						mPref_add_new_note_option.edit().putString("KEY_ADD_NEW_NOTE_OPTIONAL", "true").commit();
	 					}
	 					
 		        	}
 		        });
			}
			
	  		// inflate select style layout
	  		LayoutInflater inflator;
	  		inflator= (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	  		View view = inflator.inflate(R.layout.note_add_new_optional, null);
	  		
	  		mRadioGroup = (RadioGroup)view.findViewById(R.id.radioGroup_new_at);
	  		
	  		final CheckedTextView chkAddNewOption = (CheckedTextView) view.findViewById(R.id.chkSetPreference);
	  		if(stage == ADD_NEW)
	  		{
	  			mRadioGroup.removeViewAt(2);
		  		
		  		// show current checked status
		  		showCurrentChecked("KEY_ADD_NEW_NOTE_OPTIONAL",chkAddNewOption,"true");
		  		
		  		// listener
		  		chkAddNewOption.setOnClickListener(new View.OnClickListener() { 
		  			   @Override
		  			   public void onClick(View v){
		  				   setNewChecked("KEY_ADD_NEW_NOTE_OPTIONAL",chkAddNewOption);
		  			   }
		  			});
	  		}
	  		else if(stage == CONFIG)
	  		{
	  			if(mPref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_AT_TOP","false").equalsIgnoreCase("true"))
	  				mRadioGroup.check(mRadioGroup.getChildAt(0).getId());
	  			else if (mPref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_AT_BOTTOM","false").equalsIgnoreCase("true"))
	  				mRadioGroup.check(mRadioGroup.getChildAt(1).getId());
	  			else if (mPref_add_new_note_option.getString("KEY_ADD_NEW_NOTE_OPTIONAL","true").equalsIgnoreCase("true"))
	  				mRadioGroup.check(mRadioGroup.getChildAt(2).getId());

	  			chkAddNewOption.setVisibility(View.GONE);
	  		}
	  		
	  		builder.setView(view);
	  		mDialog = builder.create();
	  		mDialog.show();
		}
	}
	
	void radioGroupListener()
	{
    	mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup RG, int id) {
				if(mRadioGroup.indexOfChild(mRadioGroup.findViewById(id)) ==0)
					mAddAt = 0;
				else if(mRadioGroup.indexOfChild(mRadioGroup.findViewById(id)) ==1)
					mAddAt = 1;
				else if(mRadioGroup.indexOfChild(mRadioGroup.findViewById(id)) ==2)
					mAddAt = 2;
		}});
	}
	
	// show current
	void showCurrentChecked(String key,CheckedTextView chkTV,String defaultSet)
	{
		if(mPref_add_new_note_option.getString(key,defaultSet).equalsIgnoreCase("true")){
			chkTV.setChecked(true);
		}else{
			chkTV.setChecked(false);
		}
	}
	
	// set new
	void setNewChecked(String key,CheckedTextView chkTV)
	{
	   chkTV.setChecked(!chkTV.isChecked());
	   if(chkTV.isChecked())
		   mSetOptional = true;
	   else
		   mSetOptional = false;
	}
	
}
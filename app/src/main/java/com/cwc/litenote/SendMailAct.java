package com.cwc.litenote;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class SendMailAct extends Activity{
	Context mContext;
	Intent mEMmailIntent;
	CheckedTextView mCheckTvSelAll;
	Button btnSelPageOK;
    ListView mListView;
	String mSentString;
	SelectPageList selPgLst;

	public SendMailAct(){}
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		mContext = SendMailAct.this;
		
	    Bundle extras = getIntent().getExtras();
	    if(extras == null)
	    {
			setContentView(R.layout.select_page_list);

			// checked Text View: select all 
			mCheckTvSelAll = (CheckedTextView) findViewById(R.id.chkSelectAllPages);
			mCheckTvSelAll.setOnClickListener(new OnClickListener()
			{	@Override
				public void onClick(View checkSelAll) 
				{
					boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
					((CheckedTextView)checkSelAll).setChecked(!currentCheck);
					
					if(((CheckedTextView)checkSelAll).isChecked())
						selPgLst.selectAllPages(true);
					else
						selPgLst.selectAllPages(false);
				}
			});
			
			// list view: selecting which pages to send 
			mListView = (ListView)findViewById(R.id.listView1);
			
			// OK button: click to do next
			btnSelPageOK = (Button) findViewById(R.id.btnSelPageOK);
			btnSelPageOK.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// input mail address: dialog
					System.out.println("OK");
					if(selPgLst.mChkNum > 0)
					{
						inputEMailAddrDialog(); // call next dialog
					}
					else
		    			Toast.makeText(SendMailAct.this,
								   R.string.delete_checked_no_checked_items,
								   Toast.LENGTH_SHORT).show();
				}
			});

			// cancel button
			Button btnSelPageCancel = (Button) findViewById(R.id.btnSelPageCancel);
			btnSelPageCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("Cancel");
					finish();
				}
			});

			//Send e-Mail 1: show list for selection
			selPgLst = new SelectPageList(SendMailAct.this, mListView);
	    }
	    else
	    {
	    	// send checked pages
	    	inputEMailAddrDialog();
	    }
	    
	}
	
	// Send e-Mail 2 
	// case A: input mail address from current activity
	// case B: input mail address from ViewNote activity
    String mDefaultEmailAddr;
    SharedPreferences mPref_email;
	EditText editEMailAddrText;
	Activity mActVE; // activity from ViewEdit
	String mEMailBodyString;
	AlertDialog mDialog;
	
	void inputEMailAddrDialog()
	{

		AlertDialog.Builder builder1;

		mPref_email = getSharedPreferences("email_addr", 0);
	    editEMailAddrText = (EditText)getLayoutInflater()
	    							.inflate(R.layout.edit_text_dlg, null);
		builder1 = new AlertDialog.Builder(SendMailAct.this);
		
		// get default email address
		mDefaultEmailAddr = mPref_email.getString("KEY_DEFAULT_EMAIL_ADDR","@");
		editEMailAddrText.setText(mDefaultEmailAddr);
		
		builder1.setTitle(R.string.mail_notes_dlg_title)
				.setMessage(R.string.mail_notes_dlg_message)
				.setView(editEMailAddrText)
				.setNegativeButton(R.string.edit_note_button_back, 
						new DialogInterface.OnClickListener() 
				{   @Override
					public void onClick(DialogInterface dialog, int which) 
					{/*cancel*/finish();}
				})
				.setPositiveButton(R.string.mail_notes_btn, null); //call override
		
		mDialog = builder1.create();
		mDialog.show();
		
		// override positive button
		Button enterButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		enterButton.setOnClickListener(new CustomListener(mDialog));
		
		
		// back
		mDialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                    KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish();
                    return true;
                }
                return false;
            }
        });
	}
	
	//for keeping dialog when eMail address is empty
	class CustomListener implements View.OnClickListener 
	{
		private final Dialog dialog;
	    public CustomListener(Dialog dialog){
	    	this.dialog = dialog;
	    }
	    
	    @Override
	    public void onClick(View v){
	    	String attachmentFileName;
	        String strEMailAddr = editEMailAddrText.getText().toString();
	        if(strEMailAddr.length() > 0)
	        {
	    	    Bundle extras = getIntent().getExtras();
	    	    
				// save to SD card
				attachmentFileName = Util.getAppName(SendMailAct.this) + "_SEND_" + // file name 
		        							Util.getCurrentTimeString() + // time
		        							".xml"; // extension name
				Util util = new Util(SendMailAct.this);
				
				// null: for page selection
				String[] picFileNameArr = null;
		        if(extras == null)
		        {
					mEMailBodyString = util.saveToSdCard(attachmentFileName, // attachment name
														 selPgLst.mCheckedArr, // checked page array
														 false); 
					mEMailBodyString = util.trimXMLtag(mEMailBodyString);
	        	}
	        	else //other: for Note_view_pager or selected notes
	        	{
		    	    mSentString = extras.getString("SentString");
					mEMailBodyString = util.saveStringToSdCard(attachmentFileName, // attachment name
															   mSentString); // sent string
					mEMailBodyString = util.trimXMLtag(mEMailBodyString);
		        	picFileNameArr = extras.getStringArray("SentPictureFileNameArray");
	        	}
		        
	        	mPref_email.edit().putString("KEY_DEFAULT_EMAIL_ADDR", strEMailAddr).commit();
	        	
	        	// call next dialog
				sendEMail(strEMailAddr,  // eMail address
					      attachmentFileName, // attachment file name
						  picFileNameArr ); // picture file name array. For page selection, this is null 
				dialog.dismiss();
	        }
	        else
	        {
    			Toast.makeText(SendMailAct.this,
						R.string.toast_no_email_address,
						Toast.LENGTH_SHORT).show();
	        }
	    }
	}
	
	// Send e-Mail 3: send file by e-Mail
	String mAttachmentFileName;
	void sendEMail(String strEMailAddr,  // eMail address
			       String attachmentFileName, // attachment name
			       String[] picFileNameArray) // attachment picture file name
	{
		mAttachmentFileName = attachmentFileName;
		// new ACTION_SEND intent
		mEMmailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE); // for multiple attachments
//		mEMmailIntent = new Intent(android.content.Intent.ACTION_SEND); // for single attachment
	    
		// set type
//		mEMmailIntent.setType("text/plain");//can select which APP will be used to send mail
		mEMmailIntent.setType("text/xml");//only G-mail
		
		// Put extra
    	if(mActVE != null)
    		mContext = mActVE;

    	List<String> filePaths = new ArrayList<String>();
    	String messagePath = Environment.getExternalStorageDirectory().getPath() + 
                			 "/" + Util.getAppName(mContext) + "/" + 
                			 attachmentFileName;// message file name
    	filePaths.add(messagePath);
    	
    	if(picFileNameArray != null)
    	{
	    	for(int i=0;i<picFileNameArray.length;i++)
	    	{
	    		String picturePath = Environment.getExternalStorageDirectory().getPath() + 
	                		"/" + Util.getAppName(mContext) + "/picture/" + 
							picFileNameArray[i];// picture file name
	        	filePaths.add(picturePath);
	    	}
    	}
    	
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (String file : filePaths)
        {
            File fileIn = new File(file);
            Uri u = Uri.fromFile(fileIn);
            uris.add(u);
        }
    	
    	mEMmailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, 
	    					 new String[] {strEMailAddr}) // eMail address
	               .putExtra(android.content.Intent.EXTRA_SUBJECT, 
	    					 Util.getAppName(mContext) + // eMail subject
	    					 " " + mContext.getResources().getString(R.string.eMail_subject ))// eMail subject
	               .putExtra(android.content.Intent.EXTRA_TEXT, 
	    					 mContext.getResources().getString(R.string.eMail_body)// eMail text (body)
	    					 + " " + Util.getAppName(mContext) + " (UTF-8)" + Util.NEW_LINE
	    					 + mEMailBodyString)
	    		   .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
    	
    			    //for single attachment  	
//	               .putExtra(Intent.EXTRA_STREAM, 
//	    		             Uri.parse("file://"+ 
//	    	                 Environment.getExternalStorageDirectory().getPath() + 
//	    	                 "/" + Util.getAppName(mContext) + "/" + 
//	                         attachmentFileName));// eMail stream (attachment)	    					 
//	               .putExtra(Intent.EXTRA_STREAM, 
//	    		             Uri.parse("file://"+ 
//	    	                 Environment.getExternalStorageDirectory().getPath() + 
//	    	                 "/" + Util.getAppName(mContext) + "/picture/" + 
//	    	                 picFileName));// picture file name
		
	    Log.v(getClass().getSimpleName(), 
			  "attachment " + Uri.parse("file name is:"+ attachmentFileName));
	    
	    System.out.println("external storage directory path:" + 
	                         Environment.getExternalStorageDirectory().getPath() ); // got  /storage/emulated/0
	    startActivityForResult(Intent.createChooser(mEMmailIntent, 
	    											getResources().getText(R.string.mail_chooser_title)) ,
				   				EMAIL);
	} 
	
	//Send e-Mail 4: set alarm to delete attachment
	int EMAIL = 101;
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
        if(requestCode==EMAIL)
        {
        	System.out.println("SendMailDlg / _onActivityResult");
        	// note: result code is always 0 (cancel), so it is not used 
        	new DeleteFileAlarmReceiver(SendMailAct.this, 
			    		                System.currentTimeMillis() + 1000 * 60 * 5, // 300 seconds
//						    		    System.currentTimeMillis() + 1000 * 10, // 10 seconds
			    		                mAttachmentFileName);
        }   
    	finish();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		if( null != mDialog)
			mDialog.dismiss();//fix leaked window
	}
	
}
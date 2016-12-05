package com.cwc.litenote;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class SelectPageList {
	View mView;
	CheckedTextView mCheckTvSelAll;
    ListView mListView;
    List<String> mListStrArr; // list view string array
    List<Boolean> mCheckedArr; // checked list view items array
    DB mDb;
    int COUNT;
    Activity mActivity;
    
	public SelectPageList(Activity act, View view) 
	{
		mActivity = act;
		
		// checked Text View: select all 
		mCheckTvSelAll = (CheckedTextView) mActivity.findViewById(R.id.chkSelectAllPages);
		mCheckTvSelAll.setOnClickListener(new OnClickListener()
		{	@Override
			public void onClick(View checkSelAll) 
			{
				boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
				((CheckedTextView)checkSelAll).setChecked(!currentCheck);
				
				if(((CheckedTextView)checkSelAll).isChecked())
					selectAllPages(true);
				else
					selectAllPages(false);
			}
		});
		
		// list view: selecting which pages to send 
		mListView = (ListView)view;
		listForSelect();
    }
    
	// select all pages
	void selectAllPages(boolean enAll) {
		mChkNum = 0;

		mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        COUNT = DB.getTabsCount();
        for(int i=0;i<COUNT;i++)
        {
	         CheckedTextView chkTV = (CheckedTextView) mListView.findViewById(R.id.checkTV);
	         mCheckedArr.set(i, enAll);
             mListStrArr.set(i,DB.getTabTitle(i));
             
             int style = mDb.getTabStyle(i);
             
 			 if( enAll)
 				 chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
 		    			R.drawable.btn_check_on_holo_light:
 		    			R.drawable.btn_check_on_holo_dark,0,0,0);
 			 else
 				 chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
 						R.drawable.btn_check_off_holo_light:
 						R.drawable.btn_check_off_holo_dark,0,0,0);
        }
        mDb.doClose();
        
    	mChkNum = (enAll == true)? COUNT : 0;
        
        // set list adapter
        ListAdapter listAdapter = new ListAdapter(mActivity, mListStrArr);
        
        // list view: set adapter 
        mListView.setAdapter(listAdapter);
	}

	// show list for Select
    int mChkNum;
    void listForSelect()
    {
		mDb = new DB(mActivity);
		mChkNum = 0;
        // set list view
        mListView = (ListView) mActivity.findViewById(R.id.listView1);
        mListView.setOnItemClickListener(new OnItemClickListener()
               {
                    public void onItemClick(AdapterView<?> parent, View vw, int position, long id)
                    {
                         CheckedTextView chkTV = (CheckedTextView) vw.findViewById(R.id.checkTV);
                         chkTV.setChecked(!chkTV.isChecked());
                         mCheckedArr.set(position, chkTV.isChecked());
                         if(mCheckedArr.get(position) == true)
                        	 mChkNum++;
                         else
                        	 mChkNum--;
                         
                         if(!chkTV.isChecked())
                         {
                        	 mCheckTvSelAll.setChecked(false);
                         }
                         
                         // set for contrast
                         mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
                         int mStyle = mDb.getTabStyle(position);
             			 if( chkTV.isChecked())
             				 chkTV.setCompoundDrawablesWithIntrinsicBounds(mStyle%2 == 1 ?
             		    			R.drawable.btn_check_on_holo_light:
             		    			R.drawable.btn_check_on_holo_dark,0,0,0);
             			 else
             				 chkTV.setCompoundDrawablesWithIntrinsicBounds(mStyle%2 == 1 ?
             						R.drawable.btn_check_off_holo_light:
             						R.drawable.btn_check_off_holo_dark,0,0,0);
             			 mDb.doClose();
                    }
               });
 
        // set list string array
        mCheckedArr = new ArrayList<Boolean>();
        mListStrArr = new ArrayList<String>();
        
        // DB
		String strFinalPageViewed_tableId = Util.getPref_lastTimeView_NotesTableId(mActivity);
        DB.setNotesTableId(strFinalPageViewed_tableId);
        mDb = new DB(mActivity);
        
        mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
        COUNT = DB.getTabsCount();

        for(int i=0;i<COUNT;i++)
        {
        	 // list string array: init
             mListStrArr.add(DB.getTabTitle(i));
             // checked mark array: init
             mCheckedArr.add(false);
        }
        mDb.doClose();
        
        // set list adapter
        ListAdapter listAdapter = new ListAdapter(mActivity, mListStrArr);
        
        // list view: set adapter 
        mListView.setAdapter(listAdapter);
    }
    
	// list adapter
    public class ListAdapter extends BaseAdapter
    {
        private Activity activity;
        private List<String> mList;
        private LayoutInflater inflater = null;
         
        public ListAdapter(Activity a, List<String> list)
        {
            activity = a;
            mList = list;
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
     
        public int getCount()
        {
            return mList.size();
        }
     
        public Object getItem(int position)
        {
            return mCheckedArr.get(position);
        }
     
        public long getItemId(int position)
        {
            return position;
        }
         
        public View getView(int position, View convertView, ViewGroup parent)
        {
            mView = inflater.inflate(R.layout.select_page_list_row, null);
            
            // set checked text view
            CheckedTextView chkTV = (CheckedTextView) mView.findViewById(R.id.checkTV);
            mDb.doOpenByDrawer(DB.getDrawer_TabsTableId());
            // show style
            int style = mDb.getTabStyle(position);
            chkTV.setBackgroundColor(Util.mBG_ColorArray[style]);
            chkTV.setTextColor(Util.mText_ColorArray[style]);
  		    mDb.doClose();            	
            
            // show selected
            if(DB.getTab_NotesTableId(position) == Integer.valueOf(DB.getNotesTableId()))
            {
            	chkTV.setText(mList.get(position).toString() + " *");
        		chkTV.setTypeface(null, Typeface.BOLD_ITALIC);
            }
            else	
            	chkTV.setText(mList.get(position).toString());
            
            chkTV.setChecked(mCheckedArr.get(position));

             // set for contrast
			 if( chkTV.isChecked())
	        	 // note: have to remove the following in XML file
	             // android:drawableLeft="?android:attr/listChoiceIndicatorMultiple"
	             // otherwise, setCompoundDrawablesWithIntrinsicBounds will not work on ICS
				 chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
		    			R.drawable.btn_check_on_holo_light:
		    			R.drawable.btn_check_on_holo_dark,0,0,0);
			 else
				 chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
						R.drawable.btn_check_off_holo_light:
						R.drawable.btn_check_off_holo_dark,0,0,0);

			 return mView;
        }
    }

}

package com.cwc.litenote;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class Note_view_pager_controller extends BroadcastReceiver 
{
   public Note_view_pager_controller(){}

   public Note_view_pager_controller(Context context, long timeMilliSec) 
   {
		Intent intent = new Intent(context, Note_view_pager_controller.class);
		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, timeMilliSec, pendIntent);
   }

   @Override
   public void onReceive(final Context context, Intent intent) 
   {
	   
//	   System.out.println("ShowControlReceiver / _onReceive");
//	   System.out.println("Note_view_pager.mPager.getCurrentItem() = " + Note_view_pager.mPager.getCurrentItem());
	   String tagImageStr = "current"+ Note_view_pager.mPager.getCurrentItem() +"imageView";
	   Note_view_pager.imageGroup = (ViewGroup) Note_view_pager.mPager.findViewWithTag(tagImageStr);
	   ViewGroup imageGroup = Note_view_pager.imageGroup;
       
	   if(imageGroup != null)
	   {
	       Button imageViewBackButton = (Button) (imageGroup.findViewById(R.id.image_view_back));
	       Button imageViewModeButton = (Button) (imageGroup.findViewById(R.id.image_view_mode));
	       Button imagePreviousButton = (Button) (imageGroup.findViewById(R.id.image_previous));
	       Button imageNextButton = (Button) (imageGroup.findViewById(R.id.image_next));
	       
	       imageViewBackButton.setVisibility(View.GONE);
		   imageViewModeButton.setVisibility(View.GONE);
		   imagePreviousButton.setVisibility(View.GONE);
		   imageNextButton.setVisibility(View.GONE);
	   }
   }
}

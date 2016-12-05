package com.cwc.litenote;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;

public class SlideshowMediaPlayer extends Fragment
{
   private static final String TAG = "MEDIA_PLAYER"; // error logging tag
   private static final int DURATION_1S = 1000; // 1 seconds per slide
   private static SlideshowInfo slideshow; // slide show being played
   static Handler mediaHandler; // used to update the slide show
   static int mediaIndex; // index of current media to play
   static int mediaTime; // time in miniSeconds from which media should play 
   private static int mdeiaDuration; // media length
   public static MediaPlayer mediaPlayer; // plays the background music, if any
   static Activity mAct;
   
   // initializes the SlideshowPlayer Activity
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      mAct = getActivity();
      System.out.println("SlideshowMediaPlayer / onCreate");
      
      // get SlideshowInfo for Slide show to play
      slideshow = SlideshowEditor.slideshow;  
      mediaHandler = new Handler();
   }
   
   // called after onStart or onPause
   @Override
   public void onResume()
   {
      super.onResume();
      System.out.println("SlideshowMediaPlayer / onResume");
      if(SlideshowEditor.mIsPauseState == false)
    	  mediaHandler.post(runMediaPlay);
   }

   // Runnable: media play 
   static Runnable runMediaPlay = new Runnable()
   {   @Override
       public void run()
	   {	
         // media player
          if (slideshow.getMediaAt(mediaIndex) != null)
          {  
        	 if(slideshow.getMediaMarking(mediaIndex) == 1)
        	 { 
	        	 // if media is null, try to create a MediaPlayer to play the music
	        	 if(mediaPlayer == null)
	        	 {
	    	         try
	    	         {
	    	        	 mediaPlayer = new MediaPlayer(); 
	    	        	 System.out.println("Runnable updateMediaPlay / new media player");
	       				 mediaPlayer.reset();
	       				 mediaPlayer.setDataSource(mAct, Uri.parse(slideshow.getMediaAt(mediaIndex)));
	       				 mediaPlayer.prepare(); // prepare the MediaPlayer to play
	       				 mediaPlayer.start();	    	            
	       				 mdeiaDuration = mediaPlayer.getDuration();
	       				 mediaPlayer.seekTo(mediaTime); // seek to mediaTime, after start() sounds better
		        		 //??? below, why set 1S will cause media player abnormal on Power key short click
	       				 mediaHandler.postDelayed(runMediaPlay,DURATION_1S * 2); 
	    	         }
	    	         catch (Exception e)
	    	         {
	    	        	 Log.v(TAG, e.toString());
	    	         }
	        	 }
	        	 else
	             {	 // set looping: media is not playing 
			         if (!mediaPlayer.isPlaying() )
		        	 {
		        		 // increase media index
		        		 if(isMediaEndWasMet())	 
		        		 {
			        		 mediaPlayer.release();
				        	 mediaPlayer = null;
				        	 mediaTime = 0;
			        		 
			       			 mediaIndex++;
			       			 
			       			 if(mediaIndex == slideshow.getMediaList().size())
			        			 mediaIndex = 0;	// back to first index

			        		 // update SlideshowEditor current media index
			       			 SlideshowEditor.mCurrentPlayingMediaIndex = mediaIndex;
			       			 SlideshowEditor.mediaInfoAdapter.notifyDataSetChanged();
		        		 }
		        		 else
		        			 mediaPlayer.start();
		        	 }

			         // endless loop, do not set post() here, it will affect slide show timing
	        		 mediaHandler.postDelayed(runMediaPlay,DURATION_1S);
	             }
        	 }
        	 else
        	 {
        		 mediaIndex++;
        		 // check if goes back to first index
        		 if(mediaIndex >= slideshow.getMediaList().size())
        			 mediaIndex = 0;

        		 // update SlideshowEditor current media index
        		 SlideshowEditor.mCurrentPlayingMediaIndex = mediaIndex;
       			 SlideshowEditor.mediaInfoAdapter.notifyDataSetChanged();
     			 mediaHandler.postDelayed(runMediaPlay,DURATION_1S);
        	 }	
          }
          
          // update playing state for button
  		  if(SlideshowMediaPlayer.mediaPlayer != null)
  		  {
  			  if(SlideshowMediaPlayer.mediaPlayer.isPlaying())
  			  {
  				  SlideshowEditor.playMusicButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_pause , 0, 0, 0);
  				  SlideshowEditor.playMusicButton.setText("Pause Music");
  			  }
  		  }
  		  else
  		  {
  			  SlideshowEditor.playMusicButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_play_clip, 0, 0, 0);
  			  SlideshowEditor.playMusicButton.setText("Select next");
  		  }
          
       } 
   };  

   static boolean isMediaEndWasMet()
   {
		 mediaTime = mediaPlayer.getCurrentPosition();
		 mdeiaDuration = mediaPlayer.getDuration();
//		 System.out.println("mediaTime / mdeiaDuration = " + (int)((mediaTime * 100.0f) /mdeiaDuration) + "%" );
//		 System.out.println("mediaTime - mdeiaDuration = " + Math.abs(mediaTime - mdeiaDuration) );
		 return Math.abs(mediaTime - mdeiaDuration) < 1500; // toleration
   }
   
}
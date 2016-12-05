package com.cwc.litenote;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

public class SlideshowPlayer extends FragmentActivity
{
   private static final String TAG = "SLIDESHOW"; // error logging tag
   private static final String IMAGE_INDEX = "IMAGE_INDEX";
   private static final int DURATION = 5000; // 5 seconds per slide
   private ImageView imageView; // displays the current image
   private SlideshowInfo slideshow; // slide show being played
   private Handler imageHandler; // used to update the slide show
   private Handler bgHandler; 
   private int imageIndex; // index of the next image to display
   private BroadcastReceiver mReceiver;
   
   private boolean bShowImage; 
   private float fAlpha;
   private boolean mPositiveDirection; 
   
   // initializes the SlideshowPlayer Activity
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.slideshow_player);
      
      imageView = (ImageView) findViewById(R.id.imageView);

      // set full screen
	  getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		 		 		   WindowManager.LayoutParams.FLAG_FULLSCREEN);
      
      // disable screen saving
	  getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

	  // disable key guard
	  getWindow().addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
      
      System.out.println(" ");
      if (savedInstanceState == null) // Activity starting
      {
    	 System.out.println("_onCreate / savedInstanceState == null"); 
         imageIndex = 0; // start from first image
      }
      else // Activity resuming
      {
         imageIndex = savedInstanceState.getInt(IMAGE_INDEX);     
      }       
      
      // get SlideshowInfo for slideshow to play
      slideshow = SlideshowEditor.slideshow;  
      
      if(slideshow.size() > 0)
    	  bShowImage = true;
      else
    	  bShowImage = false;
      
      if(bShowImage)
    	  imageHandler = new Handler(); // create handler to control slideshow
      else
      {
          // set background for playing media only
    	  imageView.setImageDrawable(getWallpaper());
    	  bgHandler = new Handler();
    	  fAlpha = 0.5f; // initialization
    	  mPositiveDirection = true; 
      }

      IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
      filter.addAction(Intent.ACTION_SCREEN_OFF);
      mReceiver = new SlideshowScreenReceiver();
      registerReceiver(mReceiver, filter);   
      
      // restore saved bitmap
      if(bShowImage)
    	  mRestoredPictureBmp = (Bitmap) getLastCustomNonConfigurationInstance(); 
   }
   
   @Override
   protected void onRestart()
   {
	   super.onRestart();
	   System.out.println("_onRestart");
   }   

   
   // called after onCreate and sometimes onStop
   @Override
   protected void onStart()
   {
      super.onStart();
      System.out.println("_onStart");
   }

   @Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
   {
		super.onRestoreInstanceState(savedInstanceState);
		imageIndex = savedInstanceState.getInt(IMAGE_INDEX); 		
	}
   
   // called after onStart or onPause
   @Override
   protected void onResume()
   {
      super.onResume();
	  
      if(bShowImage)
    	  imageHandler.post(runSlideshow); // post updateSlideshow to execute
      else
    	  bgHandler.post(runBackground);

      System.out.println("_onResume");
   }

   // called when the Activity is paused
   @Override
   protected void onPause()
   {
      super.onPause();
      System.out.println(" ");
      System.out.println("_onPause");
      
      if(bShowImage)
    	  imageHandler.removeCallbacks(runSlideshow);
      else
    	  bgHandler.removeCallbacks(runBackground);
   }

   // save slide show state so it can be restored in onCreate
   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      super.onSaveInstanceState(outState);
      // save nextItemIndex and slideshowName
      imageIndex--;
      if(imageIndex<0)
    	  imageIndex =0;
      outState.putInt(IMAGE_INDEX, imageIndex); 
   }    
   
   @Override
   public Object onRetainCustomNonConfigurationInstance()
   {
	  if(bShowImage)
	  {
		  final Bitmap pictureSaved = mPictureBmp;
		  System.out.println("_onRetainNonConfigurationInstance");
		  return pictureSaved;
	  }
	  else
		  return null;
   }
   
   // called when the Activity stops
   @Override
   protected void onStop()
   {
      super.onStop();
      System.out.println("_onStop");
   }

   // called when the Activity is destroyed
   @Override
   protected void onDestroy()
   {
      super.onDestroy();
      unregisterReceiver(mReceiver);
      System.out.println("_onDestroy");
   }


   // Runnable: updateSlideshow
   Bitmap mRestoredPictureBmp;
   Bitmap mPictureBmp;
   private Runnable runSlideshow = new Runnable()
   {
      @Override
      public void run()
      {
    	  if(imageIndex >= slideshow.size())
    		  imageIndex = 0;

    	  String item = slideshow.getImageAt(imageIndex);
    	  System.out.println(" Runnable updateSlideshow / imageIndex = " + imageIndex);
    	  if(SlideshowScreenReceiver.wasScreenOn)
    	  {
    		  new LoadImageTaskWeakReference(imageView).execute(Uri.parse(item));
        	  imageIndex++; 
    	  }
    	  else
    		  imageHandler.postDelayed(runSlideshow, DURATION);
    	  
      }
      
      // load bitmap: set weak reference for OOM issue
      class LoadImageTaskWeakReference extends AsyncTask<Uri, Object, Bitmap> 
      {
    	  private WeakReference<ImageView> imgInputView;
    	  private WeakReference<Bitmap> rotateBitmap;

    	  public LoadImageTaskWeakReference(ImageView imgInputView)
    	  {
    		  this.imgInputView = new WeakReference<ImageView>(imgInputView);
    	  }

    	  @Override
    	  protected Bitmap doInBackground(Uri... params) {
    		  BitmapFactory.Options options = new BitmapFactory.Options();
    		  
    		  // test experience of setting inSampleSize below
    		  // set 1: - keep image quality, but sound could be stopped shortly when image loading
    		  //        - OOM issue occurred sometimes
    		  // set 2: avoid OOM issue and can keep sound quality 
    		  options.inSampleSize = 2; 
    		  
    		  if(mRestoredPictureBmp != null)
    		  {
    			  mPictureBmp = mRestoredPictureBmp;
    			  mRestoredPictureBmp = null;
    		  }
    		  else
    			  mPictureBmp = getBitmap(params[0], getContentResolver(), options);  
    	    		
    		  Matrix matrix = null;
    		  try {
    			  matrix = UtilImage.getMatrix(params[0]);
    		  } catch (IOException e) {
    			  e.printStackTrace();
    		  }
    		  
    		  if(mPictureBmp != null)
    		  {
    			  rotateBitmap = new WeakReference<Bitmap>(Bitmap.createBitmap(mPictureBmp,
			    				  											0, 
			    				  											0,
			    				  											mPictureBmp.getWidth(),
			    				  											mPictureBmp.getHeight(),
			    				  											matrix,
			    				  											true));
    		  }
    		  else
    			  return null;
    		  
    		  return rotateBitmap.get();
    	  }

    	  @Override
    	  protected void onPostExecute(Bitmap result) 
    	  {
    		  BitmapDrawable next = new BitmapDrawable(null, result);
    		  next.setGravity(android.view.Gravity.CENTER);
    		  Drawable previous = imageView.getDrawable();
                
    		  // if previous is a TransitionDrawable, get its second drawable item
    		  if (previous instanceof TransitionDrawable)
    			  previous = ((TransitionDrawable) previous).getDrawable(1);
                
    		  if (previous == null)
    		  {
    			  imgInputView.get().setImageBitmap(result);
    		  }
    		  else
    		  {
    			  Drawable[] drawables = { previous, next };
    			  TransitionDrawable transition = new TransitionDrawable(drawables);
    			  imgInputView.get().setImageBitmap(result);
    			  transition.startTransition(1000);
    		  }
    		  imageHandler.postDelayed(runSlideshow, DURATION);    	    	
    	  }
      }
      
      // utility method to get a Bitmap from a Uri
      public Bitmap getBitmap(Uri uri, ContentResolver cr, BitmapFactory.Options options)
      {
         Bitmap bitmap = null;
         // get the image
         try
         {
            InputStream input = cr.openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(input, null, options);
         }
         catch (FileNotFoundException e) 
         {
            Log.v(TAG, e.toString());
         }
         return bitmap;
      }
   }; 
   
   // Runnable: background
   private float step = 0.03f;
   private float highBoundary = 1.0f;
   private float lowBoundary = 0.0f;
   private int stepPeriod = 200;
   
   private Runnable runBackground = new Runnable()
   {   @Override
       public void run()
	   {	
  			imageView.setAlpha(fAlpha);
	   		if(mPositiveDirection) 
	   		{
	   			fAlpha += step;
		   		if(fAlpha >= highBoundary)
		   		{
		   			mPositiveDirection = false;
		   			fAlpha = highBoundary;
		   		}
	   		}
	   		else
	   		{
	   			fAlpha -= step;
		   		if(fAlpha <= lowBoundary)
		   		{
		   			mPositiveDirection = true;
		   			fAlpha = lowBoundary;
		   		}
	   		}
	   		bgHandler.postDelayed(runBackground,stepPeriod); 
       } 
   };  
}
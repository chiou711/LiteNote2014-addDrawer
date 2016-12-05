/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.cwc.litenote;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class PictureGridAct extends Activity 
{
	String IMAGES = "IMAGES";
	String IMAGE_POSITION = "IMAGE_POSITION";		
    DB mDb;
    Long mRowId;
    int mEntryPosition;
	String[] imageUrls;
	private List<String> items = null;
    List<String> fileNames = null;
	DisplayImageOptions options;
	
	protected ImageLoader imageLoader = ImageLoader.getInstance();
	protected AbsListView gridView;
	String dirString;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.picture_grid);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		options = new DisplayImageOptions.Builder()
			.showImageOnLoading(R.drawable.ic_stub)
			.showImageForEmptyUri(R.drawable.btn_radio_off_holo_light)//R.drawable.ic_empty
			.showImageOnFail(R.drawable.ic_cab_done_holo)// R.drawable.ic_error
			.cacheInMemory(true)
			.cacheOnDisk(true)
			.imageScaleType(ImageScaleType.EXACTLY)
			.bitmapConfig(Bitmap.Config.RGB_565)
			.considerExifParams(true)
			.build();
		
		dirString = Environment.getExternalStorageDirectory().toString() + 
		          "/" + 
		          Util.getAppName(this) + "/picture";
		getFiles(new File(dirString).listFiles());
        
		gridView = (GridView) findViewById(R.id.gridview);
		
		// check if directory is created AND not empty
		if( (imageUrls != null  ) && (imageUrls.length > 0)) 
		{
			((GridView) gridView).setAdapter(new ImageAdapter());
		}
		else
		{
			Toast.makeText(PictureGridAct.this,R.string.gallery_toast_no_file,Toast.LENGTH_SHORT).show();
			finish();
		}
		
		gridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startImagePagerActivity(position);
			}
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case android.R.id.home:
	    	NavUtils.navigateUpTo(this, new Intent(this, DrawerActivity.class));
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
    private void getFiles(File[] files)
    {
        if(files == null)
        {
        	Toast.makeText(PictureGridAct.this,R.string.gallery_toast_no_file,Toast.LENGTH_SHORT).show();
        	finish();
        }
        else
        {
//        	System.out.println("files lenght = " + files.length);
        	String imagePath[] = new String[files.length];
        	
            items = new ArrayList<String>();
            fileNames = new ArrayList<String>();
            items.add("");
            fileNames.add("ROOT");
            int i=0;
            
	        for(File file : files)
	        {
	            items.add(file.getPath());
	            fileNames.add(file.getName());
		        if( (file.getName().endsWith("jpg")) )
	            {
//		            System.out.println("file.getPath() = " + file.getPath());
//		            System.out.println("i = " + i);
		            
		            if(i< files.length)
		            {
		            	imagePath[i] = "file:///" + file.getPath();
		            	System.out.println("imagePath[i] = " + imagePath[i]);
		            	i++;
		            }
	            }
	        }
	        imageUrls = imagePath;
        }
    }

	private void startImagePagerActivity(int position) 
	{
		Bundle bundle = getIntent().getExtras();
		boolean bGallery = bundle.getBoolean("gallery");
		if(bGallery)
		{
			Intent intent = new Intent(this, PicturePagerAct.class);
			intent.putExtra(IMAGES, imageUrls);
			intent.putExtra(IMAGE_POSITION, position);
			startActivity(intent);
		}
		else
		{
		    Intent intent = getIntent();
		    String fileName = imageUrls[position].replace( "file:///"+ dirString ,"");
		    intent.putExtra("FILENAME",fileName /*file.getName()*/);
		    setResult(RESULT_OK, intent);
		    finish();
		}
	}

	public class ImageAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return imageUrls.length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			View view = convertView;
			if (view == null) {
				view = getLayoutInflater().inflate(R.layout.picture_grid_item, parent, false);
				holder = new ViewHolder();
				assert view != null;
				holder.imageView = (ImageView) view.findViewById(R.id.image);
				holder.progressBar = (ProgressBar) view.findViewById(R.id.progress);
				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}

			imageLoader.displayImage(imageUrls[position],
									 holder.imageView,
									 options, 
						 new SimpleImageLoadingListener() 
						 {
							 @Override
							 public void onLoadingStarted(String imageUri, View view) {
								 holder.progressBar.setProgress(0);
								 holder.progressBar.setVisibility(View.VISIBLE);
							 }

							 @Override
							 public void onLoadingFailed(String imageUri, View view,
									 FailReason failReason) {
								 holder.progressBar.setVisibility(View.GONE);
							 }

							 @Override
							 public void onLoadingComplete(String imageUri, View view, 
									 Bitmap loadedImage) {
								 holder.progressBar.setVisibility(View.GONE);
							 }
						 }, 
						 new ImageLoadingProgressListener() 
						 {
							 @Override
							 public void onProgressUpdate(String imageUri, View view, int current,
									 int total) {
								 holder.progressBar.setProgress(Math.round(100.0f * current / total));
							 }
						 }
			);
			return view;
		}

		class ViewHolder {
			ImageView imageView;
			ProgressBar progressBar;
		}
	}
}
package com.cwc.litenote;

import java.util.ArrayList;
import java.util.List;

public class SlideshowInfo
{
	private List<String> imageList; // this slideshow's images
	private List<String> mediaList; // this slideshow's medium
	private List<Integer> mediaMarkingList; // this slideshow's medium marking
   
   // constructor 
   public SlideshowInfo()
   {
      imageList = new ArrayList<String>(); 
      mediaList = new ArrayList<String>(); 
      mediaMarkingList = new ArrayList<Integer>(); 
   }

   // return List of Strings pointing to the slideshow's images
   public List<String> getImageList()
   {
      return imageList;
   }

   public List<String> getMediaList()
   {
      return mediaList;
   }
   
   // add a new image path
   public void addImage(String path)
   {
	  System.out.println("path = " + path); 
      imageList.add(path);
   }
   
   public void addMedia(String path)
   {
	  System.out.println("media path = " + path); 
      mediaList.add(path);
   }
   
   public void setMedia(int i, String path)
   {
      mediaList.set(i,path);
   }   
   
   public void addMediaMarking(int i)
   {
	   mediaMarkingList.add(i);
   }   
   
   public void setMediaMarking(int idx, int marking)
   {
	   mediaMarkingList.set(idx,marking);
   }

   public int getMediaMarking(int idx)
   {
	   return  mediaMarkingList.get(idx);
   }
   
   public int getFirstMediaMarking()
   {
	   int first = 0;
	   for(int i = 0;i < mediaMarkingList.size() ; i++ )
	   {
		   if( mediaMarkingList.get(i) == 1)
			   first = Math.min(first,i);
	   }
	   return first;
   }   
   
   // return String at position index
   public String getImageAt(int index)
   {
      if (index >= 0 && index < imageList.size())
         return imageList.get(index);
      else
         return null;
   }
   
   // return String at position index
   public String getMediaAt(int index)
   {
      if (index >= 0 && index < mediaList.size())
         return mediaList.get(index);
      else
         return null;
   }
   
   // return number of images/videos in the slideshow
   public int size()
   {
      return imageList.size();
   }
}
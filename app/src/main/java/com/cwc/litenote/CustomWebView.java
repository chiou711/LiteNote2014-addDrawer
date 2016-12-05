package com.cwc.litenote;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class CustomWebView extends WebView {
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    float scale = 0f;
    float oldscale = 0f;
    int displayHeight;
    
    SharedPreferences mPref_web_view;
    static int defaultSize;

    public CustomWebView(Context context) {
        super(context);
    }    
    
    public CustomWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) 
    {
        boolean consumed = super.onTouchEvent(ev);

        if (isClickable())
            switch (ev.getAction() & MotionEvent.ACTION_MASK)
            {

               case MotionEvent.ACTION_DOWN: 
                  start.set(ev.getX(), ev.getY());
                  mode = DRAG;
                  break;
               case MotionEvent.ACTION_UP: 
               case MotionEvent.ACTION_POINTER_UP: 
                  mode = NONE;
                  break;
               case MotionEvent.ACTION_POINTER_DOWN: 
                  oldDist = spacing(ev);
                  if (oldDist > 5f) {
                     midPoint(mid, ev);
                     mode = ZOOM;
                  }
                  break;

               case MotionEvent.ACTION_MOVE: 
                  if (mode == DRAG) 
                  { 
                  }
                  else if (mode == ZOOM) 
                  { 

                     float newDist = spacing(ev);
                     if (newDist > 5f) 
                     {
                        scale = newDist / oldDist; 
                        if(scale>1)
                        {
                            if(Math.abs(oldscale-scale)>0.3)
                            {
                                zoomIn();
                                oldscale = scale;
//                                System.out.println("zoomIn / scale = " + scale);
//                                System.out.println("zoomIn / getScale() = " + getScale());
                            }
                        }
//                        System.out.println("scale = " + scale);
                        if(scale<1)
                        {
                            if((getContentHeight()*getScale()>displayHeight))
                            {
                                zoomOut();
//                                System.out.println("zoomOut / scale = " + scale);
//                                System.out.println("zoomOut / getScale() = " + getScale());
                            }
                        }

                        int newDefaultScale = (int) (getScale()*100);
//                        System.out.println("newDefaultScale = " + newDefaultScale);
                        mPref_web_view.edit().putInt("KEY_WEB_VIEW_SCALE",newDefaultScale).commit();                     }
                  }
                  break;
               }
        
        return consumed;
    }

    private float spacing(MotionEvent event) {
           float x = event.getX(0) - event.getX(1);
           float y = event.getY(0) - event.getY(1);
//           return FloatMath.sqrt(x * x + y * y);
           return (float) Math.sqrt(x * x + y * y);
        }

        private void midPoint(PointF point, MotionEvent event) {
           float x = event.getX(0) + event.getX(1);
           float y = event.getY(0) + event.getY(1);
           point.set(x / 2, y / 2);
        }

}


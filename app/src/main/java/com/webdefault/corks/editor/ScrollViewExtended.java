package com.webdefault.corks.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by orlandoleite on 2/26/18.
 */

public class ScrollViewExtended extends ScrollView
{
    
    private Runnable scrollerTask;
    private int initialPosition;
    
    private int newCheck = 100;
    private static final String LOG_TAG = "ScrollViewExtended";
    
    public interface OnScrollStoppedListener
    {
        void onScrollStopped();
    }
    
    private OnScrollStoppedListener onScrollStoppedListener;
    
    public ScrollViewExtended( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        scrollerTask = new Runnable()
        {
            
            public void run()
            {
                
                int newPosition = getScrollY();
                if( initialPosition - newPosition == 0 )
                {//has stopped
                    
                    if( onScrollStoppedListener != null )
                    {
                        
                        onScrollStoppedListener.onScrollStopped();
                    }
                }
                else
                {
                    initialPosition = getScrollY();
                    ScrollViewExtended.this.postDelayed( scrollerTask, newCheck );
                }
            }
        };
    }
    
    public void setOnScrollStoppedListener( ScrollViewExtended.OnScrollStoppedListener listener )
    {
        onScrollStoppedListener = listener;
    }
    
    public void startScrollerTask()
    {
        
        initialPosition = getScrollY();
        ScrollViewExtended.this.postDelayed( scrollerTask, newCheck );
    }
    
}
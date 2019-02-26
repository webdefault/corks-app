package com.webdefault.corks.editor.tool;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.webdefault.corks.LocalDatabase;
import com.webdefault.corks.R;
import com.webdefault.corks.editor.EditingFile;
import com.webdefault.corks.editor.Editor;
import com.webdefault.corks.editor.ToolsSelector;

/**
 * Created by orlandoleite on 3/27/18.
 */

public class UndoTool extends LinearLayout implements Tool, View.OnTouchListener
{
    private static final String LOG_TAG = "FindTool";
    
    private Editor editor;
    private ToolsSelector tools;
    private EditingFile editingFile;
    
    private LocalDatabase db;
    
    private View undo, redo;
    
    private Handler handler;
    
    private static final int BUTTON_IDLE = 0;
    private static final int BUTTON_UNDO = 1;
    private static final int BUTTON_REDO = 2;
    
    private int buttonState;
    
    public UndoTool( Context context, @Nullable AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    public void init( Editor editor, ToolsSelector tools )
    {
        this.editor = editor;
        this.tools = tools;
        
        undo = findViewById( R.id.undo_btn );
        undo.setOnTouchListener( this );
        
        redo = findViewById( R.id.redo_btn );
        redo.setOnTouchListener( this );
        
        handler = new Handler( getContext().getMainLooper() );
        
        editingFile = editor.getCurrentFile();
        
        updateButtons();
    }
    
    private void updateButtons()
    {
        if( editingFile.canUndo() )
        {
            undo.setEnabled( true );
            undo.setAlpha( 1.0f );
        }
        else
        {
            undo.setEnabled( false );
            undo.setAlpha( 0.5f );
        }
        
        if( editingFile.canRedo() )
        {
            redo.setEnabled( true );
            redo.setAlpha( 1.0f );
        }
        else
        {
            redo.setEnabled( false );
            redo.setAlpha( 0.5f );
        }
    }
    
    @Override
    public boolean canClose()
    {
        return true;
    }
    
    private void callUndoRedo()
    {
        if( buttonState == BUTTON_UNDO )
        {
            if( editingFile.canUndo() )
            {
                editingFile.undo();
            }
            else
            {
                buttonState = BUTTON_IDLE;
            }
        }
        else if( buttonState == BUTTON_REDO )
        {
            if( editingFile.canRedo() )
            {
                editingFile.redo();
            }
            else
            {
                buttonState = BUTTON_REDO;
            }
        }
    }
    
    private boolean buttonDown( View view )
    {
        boolean result = false;
        
        switch( view.getId() )
        {
            case R.id.undo_btn:
                buttonState = BUTTON_UNDO;
                result = true;
                break;
            
            case R.id.redo_btn:
                buttonState = BUTTON_REDO;
                result = true;
                break;
        }
        
        Runnable runnable = new Runnable()
        {
            private int runs = 0;
            private int delay = 1000;
            
            @Override
            public void run()
            {
                try
                {
                    callUndoRedo();
                }
                catch( Exception e )
                {
                    // TODO: handle exception
                }
                finally
                {
                    //also call the same runnable to call it at regular interval
                    if( buttonState != BUTTON_IDLE )
                    {
                        if( runs == 2 )
                            delay = 500;
                        else if( runs == 6 )
                            delay = 300;
                        else if( runs == 15 )
                            delay = 100;
                        
                        runs++;
                        
                        handler.postDelayed( this, delay );
                    }
                }
            }
        };
        
        handler.post( runnable );
        // callUndoRedo();
        
        return result;
    }
    
    @Override
    public boolean onTouch( View view, MotionEvent motionEvent )
    {
        Log.v( LOG_TAG, "event: " + motionEvent );
        
        switch( motionEvent.getAction() )
        {
            case MotionEvent.ACTION_DOWN:
                return buttonDown( view );
            
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                buttonState = BUTTON_IDLE;
                updateButtons();
                return true;
        }
        
        return false;
    }
}

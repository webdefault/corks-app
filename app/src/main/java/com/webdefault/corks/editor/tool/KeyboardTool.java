package com.webdefault.corks.editor.tool;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.github.johnkil.print.PrintImageButton;
import com.webdefault.corks.MainActivity;
import com.webdefault.corks.R;
import com.webdefault.corks.editor.Editor;
import com.webdefault.corks.editor.ToolsSelector;

import static com.webdefault.corks.R.id.parenthesis_btn;

public class KeyboardTool extends LinearLayout implements Tool, View.OnClickListener, View.OnLongClickListener
{
    private static final String LOG_TAG = "DefaultTool";
    
    Button ctrlBtn;
    Button tabBtn;
    Button bracketsBtn;
    Button parenthesisBtn;
    Button lessGreaterBtn;
    Button dashBtn;
    Button slashBtn;
    
    PrintImageButton cursorBtn;
    ImageButton consoleBtn;
    
    boolean controlBlocked = false;
    
    private Editor editor;
    
    public KeyboardTool( Context context, @Nullable AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    @Override
    public void init( Editor editor, ToolsSelector tools )
    {
        ctrlBtn = (Button) findViewById( R.id.control_btn );
        tabBtn = (Button) findViewById( R.id.tab_btn );
        bracketsBtn = (Button) findViewById( R.id.brackets_btn );
        parenthesisBtn = (Button) findViewById( parenthesis_btn );
        lessGreaterBtn = (Button) findViewById( R.id.less_greater_btn );
        dashBtn = (Button) findViewById( R.id.dash_btn );
        slashBtn = (Button) findViewById( R.id.slash_btn );
        
        cursorBtn = (PrintImageButton) findViewById( R.id.cursor_btn );
        consoleBtn = (ImageButton) findViewById( R.id.console_btn );
        
        ctrlBtn.setOnClickListener( this );
        tabBtn.setOnClickListener( this );
        bracketsBtn.setOnClickListener( this );
        parenthesisBtn.setOnClickListener( this );
        lessGreaterBtn.setOnClickListener( this );
        dashBtn.setOnClickListener( this );
        slashBtn.setOnClickListener( this );
        
        cursorBtn.setOnClickListener( this );
        consoleBtn.setOnClickListener( this );
        
        ctrlBtn.setOnLongClickListener( this );
        
        this.editor = editor;
        editor.setKeyboardTool( this );
    }
    
    public boolean isControlPressed()
    {
        return ctrlBtn.isSelected();
    }
    
    public boolean isCursorPressed()
    {
        return cursorBtn.isSelected();
    }
    
    public void resetControls()
    {
        if( !controlBlocked )
        {
            ctrlBtn.setSelected( false );
        }
    }
    
    @Override
    public boolean canClose()
    {
        return false;
    }
    
    @Override
    public void onClick( View view )
    {
        int p;
        switch( view.getId() )
        {
            case R.id.control_btn:
                ctrlBtn.setSelected( !ctrlBtn.isSelected() );
                cursorBtn.setSelected( false );
                controlBlocked = false;
                break;
            
            case R.id.tab_btn:
                editor.append( "\t" );
                break;
            
            case R.id.brackets_btn:
                p = editor.getSelectionEnd();
                editor.getText().insert( editor.getSelectionStart(), "{}" );
                editor.setSelection( p + 1 );
                break;
            
            case R.id.parenthesis_btn:
                p = editor.getSelectionEnd();
                editor.getText().insert( editor.getSelectionStart(), "()" );
                editor.setSelection( p + 1 );
                break;
            
            case R.id.less_greater_btn:
                p = editor.getSelectionEnd();
                editor.getText().insert( editor.getSelectionStart(), "<>" );
                editor.setSelection( p + 1 );
                break;
                
            case R.id.dash_btn:
                editor.getText().insert( editor.getSelectionStart(), "-" );
                break;
            
            case R.id.slash_btn:
                editor.getText().insert( editor.getSelectionStart(), "/" );
                break;
            
            case R.id.cursor_btn:
                ctrlBtn.setSelected( false );
                cursorBtn.setSelected( !cursorBtn.isSelected() );
                controlBlocked = false;
                break;
            
            case R.id.console_btn:
                (( MainActivity) getContext()).showConsoleFragment();
                break;
        }
    }
    
    @Override
    public boolean onLongClick( View view )
    {
        switch( view.getId() )
        {
            case R.id.control_btn:
                ctrlBtn.setSelected( !ctrlBtn.isSelected() );
                cursorBtn.setSelected( false );
                controlBlocked = true;
                return true;
        }
        
        return false;
    }
}

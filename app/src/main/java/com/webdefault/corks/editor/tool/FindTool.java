package com.webdefault.corks.editor.tool;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.webdefault.corks.LocalDatabase;
import com.webdefault.corks.MainActivity;
import com.webdefault.corks.R;
import com.webdefault.corks.editor.EditingFile;
import com.webdefault.corks.editor.Editor;
import com.webdefault.corks.editor.ToolsSelector;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by orlandoleite on 3/1/18.
 */

public class FindTool extends LinearLayout implements Tool, View.OnClickListener
{
    private static final String LOG_TAG = "FindTool";
    
    private Editor editor;
    private ToolsSelector tools;
    
    private LocalDatabase db;
    private boolean matchCase, regex, showReplace;
    
    public static final String MATCH_CASE = "find_tool_match_case";
    public static final String USING_REGEX = "find_tool_using_regex";
    //public static final String SHOW_REPLACE = "find_tool_show_replace";
    
    private EditText findField;
    private EditText replaceField;
    
    private Paint highlighterFill;
    private Paint highlighterStroke;
    
    private ArrayList<Integer[]> matches = new ArrayList<>();
    private int currentMatch = -1;
    
    private Pattern pattern;
    
    public FindTool( Context context, @Nullable AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    public void init( Editor editor, ToolsSelector tools )
    {
        this.editor = editor;
        this.tools = tools;
        
        mCanClose = false;
        
        highlighterFill = new Paint();
        highlighterFill.setStyle( Paint.Style.FILL );
        highlighterFill.setColor( ContextCompat.getColor( getContext(), R.color.editorHighlightFill ) );
        
        highlighterStroke = new Paint();
        highlighterStroke.setStyle( Paint.Style.STROKE );
        highlighterStroke.setColor( ContextCompat.getColor( getContext(), R.color.editorHighlightStroke ) );
        highlighterStroke.setStrokeWidth( 2 );
        
        db = LocalDatabase.Companion.getInstance( getContext() );
        matchCase = db.intOption( MATCH_CASE ) == 1;
        regex = db.intOption( USING_REGEX ) == 1;
        showReplace = false;//db.intOption( SHOW_REPLACE ) == 1;
        
        
        findViewById( R.id.close_btn ).setOnClickListener( this );
        findViewById( R.id.find_prev_btn ).setOnClickListener( this );
        findViewById( R.id.find_next_btn ).setOnClickListener( this );
        findViewById( R.id.options_btn ).setOnClickListener( this );
        
        findViewById( R.id.replace_one_btn ).setOnClickListener( this );
        findViewById( R.id.replace_all_btn ).setOnClickListener( this );
        
        findViewById( R.id.replace_container ).setVisibility( showReplace ? View.VISIBLE : View.GONE );
        
        findField = (EditText) findViewById( R.id.find_field );
        replaceField = (EditText) findViewById( R.id.replace_field );
        
        findField.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService( Context.INPUT_METHOD_SERVICE );
        imm.showSoftInput( findField, InputMethodManager.SHOW_IMPLICIT );
        //findViewById( R.id.save_encoding_btn ).setOnClickListener( this );
        
        editor.setFindTool( this );
        
        // Setup first content of find selector;
        int startSel = editor.getSelectionStart();
        int endSel = editor.getSelectionEnd();
        if( startSel != endSel )
        {
            String value = String.valueOf( editor.getText().toString().subSequence( startSel, endSel ) );
            findField.setText( value );
        }
        else if( findField.getText().toString().isEmpty() )
        {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService( Context.CLIPBOARD_SERVICE );
            ClipData clip = clipboard.getPrimaryClip();
            
            if( clip != null && clip.getItemCount() != 0 )
            {
                findField.setText( clip.getItemAt( 0 ).getText().toString() );
            }
        }
        
        findField.selectAll();
        updateSelection();
        
        findField.addTextChangedListener( textWatcher );
    }
    
    private boolean mCanClose = false;
    
    public boolean canClose()
    {
        return mCanClose;
    }
    
    @Override
    public void onClick( View view )
    {
        switch( view.getId() )
        {
            case R.id.options_btn:
                findOptions();
                break;
            
            case R.id.close_btn:
                mCanClose = true;
                matches.clear();
                tools.hideToolView();
                break;
            
            case R.id.find_prev_btn:
                findPrev();
                break;
            
            case R.id.find_next_btn:
                findNext();
                break;
                
            case R.id.replace_one_btn:
                replaceOne();
                break;
                
            case R.id.replace_all_btn:
                replaceAll();
                break;
        }
    }
    
    private void findPrev()
    {
        findItem( false );
    }
    
    private void findNext()
    {
        findItem( true );
    }
    
    private void findItem( boolean next )
    {
        if( matches.size() > 0 )
        {
            if( currentMatch == -1 )
            {
                int selStart = editor.getSelectionStart();
                
                currentMatch = 0;
                while( currentMatch < matches.size() )
                {
                    if( selStart <= matches.get( currentMatch )[0] )
                    {
                        break;
                    }
                    else
                        currentMatch++;
                }
            }
            else if( next )
                currentMatch++;
            else
                currentMatch--;
            
            if( currentMatch >= matches.size() )
                currentMatch = 0;
            else if( currentMatch < 0 )
                currentMatch = matches.size() - 1;
            
            final Integer[] match = matches.get( currentMatch );
            
            Log.v( LOG_TAG, "d" + match[0] + " " + match[1] );
            editor.setSelection( match[0], match[0] + match[1] );
            editor.requestFocus();
            
            
            EditingFile file = editor.getCurrentFile();
            int[] charPos = file.getLastCharPos();
            
            int start = match[0];
            int line = 0;
            while( line < charPos.length && charPos[line] <= start ) line++;
            
            int y = editor.getLayout().getLineTop( line ); // e.g. I want to scroll to line 40
            // editor.scrollTo( 0, y );
            
            ((MainActivity) getContext()).scrollTo( y );
        }
    }
    
    private void replaceOne()
    {
        if( matches.size() > 0 )
        {
            if( currentMatch == -1 )
            {
                int selStart = editor.getSelectionStart();
                
                currentMatch = 0;
                while( currentMatch < matches.size() )
                {
                    if( selStart <= matches.get( currentMatch )[0] )
                    {
                        break;
                    }
                    else
                        currentMatch++;
                }
            }
            else
                currentMatch++;
            
            if( currentMatch >= matches.size() )
                currentMatch = 0;
            else if( currentMatch < 0 )
                currentMatch = matches.size() - 1;
            
            final Integer[] match = matches.get( currentMatch );
            
            String value = editor.getText().toString();
            
            if( regex )
            {
                String replace = replaceField.getText().toString();
                String part = value.substring( match[0], match[0] + match[1] );
                String result = pattern.matcher( part ).replaceFirst( replace ); 
                editor.getText().replace( match[0], match[0] + match[1], result );
            }
            else
            {
                String replace = replaceField.getText().toString();
                editor.getText().replace( match[0], match[0] + match[1], replace );
            }
            
            EditingFile file = editor.getCurrentFile();
            int[] charPos = file.getLastCharPos();
            
            int start = match[0];
            int line = 0;
            while( line < charPos.length && charPos[line] <= start ) line++;
            
            int y = editor.getLayout().getLineTop( line ); // e.g. I want to scroll to line 40
            // editor.scrollTo( 0, y );
            
            ((MainActivity) getContext()).scrollTo( y );
            
            updateSelection();
        }
    }
    
    private void replaceAll()
    {
        if( matches.size() > 0 )
        {
            updateSelection();
            
            int total = matches.size();
            int offset = 0;
            for( int i = 0; i < total; i++ )
            {
                final Integer[] match = matches.get( i );
                match[0] += offset;
                
                String value = editor.getText().toString();
                
                if( regex )
                {
                    String replace = replaceField.getText().toString();
                    String part = value.substring( match[0], match[0] + match[1] );
                    String result = pattern.matcher( part ).replaceFirst( replace );
                    editor.getText().replace( match[0], match[0] + match[1], result );
                    
                    offset += result.length() - part.length();
                }
                else
                {
                    String replace = replaceField.getText().toString();
                    editor.getText().replace( match[0], match[0] + match[1], replace );
                    
                    offset += replace.length() - match[1];
                }
            }
            
            EditingFile file = editor.getCurrentFile();
            int[] charPos = file.getLastCharPos();
            
            int y = editor.getLayout().getLineTop( charPos.length-1 ); // e.g. I want to scroll to line 40
            // editor.scrollTo( 0, y );
            
            ((MainActivity) getContext()).scrollTo( y );
            
            updateSelection();
        }
    }
    
    public void setShowReplace( boolean value )
    {
        showReplace = value;
        
        findViewById( R.id.replace_container ).setVisibility( showReplace ? View.VISIBLE : View.GONE );
    }
    
    public void findOptions()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( getContext(), R.style.DialogAlert );
        builder.setTitle( "Options" );
        
        LinearLayout layout = (LinearLayout) View.inflate( getContext(), R.layout.dialog_find_tool_options, null );
        //input.addTextChangedListener( new MoneyTextWatcher( input ) );
        builder.setView( layout );
        
        final CheckBox matchCaseBox = (CheckBox) layout.findViewById( R.id.match_case );
        matchCaseBox.setChecked( matchCase );
        
        final CheckBox usingRegexBox = (CheckBox) layout.findViewById( R.id.using_regex );
        usingRegexBox.setChecked( regex );
        
        final CheckBox showReplaceBox = (CheckBox) layout.findViewById( R.id.show_replace );
        showReplaceBox.setChecked( showReplace );
        
        //InputMethodManager imm = (InputMethodManager) getContext().getSystemService( Context.INPUT_METHOD_SERVICE );
        //imm.toggleSoftInput( InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY );
        
        builder.setPositiveButton( "Set", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialogInterface, int i )
            {
                Dialog dialog = (Dialog) dialogInterface;
                matchCase = matchCaseBox.isChecked();
                regex = usingRegexBox.isChecked();
                showReplace = showReplaceBox.isChecked();
                
                db.setIntOption( MATCH_CASE, matchCase ? 1 : 0 );
                db.setIntOption( USING_REGEX, regex ? 1 : 0 );
                //db.setIntOption( SHOW_REPLACE, showReplace ? 1 : 0 );
                
                FindTool.this.findViewById( R.id.replace_container ).setVisibility( showReplace ? View.VISIBLE : View.GONE );
            }
        } );
        builder.setNegativeButton( "Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                dialog.cancel();
            }
        } );
        
        final Dialog dialog = builder.show();
    }
    
    private void showEncodeSelector()
    {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder( getContext() );
        builderSingle.setTitle( "Save with encoding" );
        
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>( getContext(), R.layout.dialog_listitem );
        arrayAdapter.add( "UTF-8" );
        arrayAdapter.add( "UTF-16" );
        arrayAdapter.add( "UTF-16BE" );
        arrayAdapter.add( "UTF-16LE" );
        arrayAdapter.add( "US-ASCII" );
        arrayAdapter.add( "ISO-8859-1" );
        
        
        builderSingle.setNegativeButton( "cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                dialog.dismiss();
            }
        } );
        
        builderSingle.setAdapter( arrayAdapter, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                String encode = arrayAdapter.getItem( which );
                editor.saveCurrentFileWithEncode( encode );
            }
        } );
        builderSingle.show();
    }
    
    public void onEditorDraw( Canvas canvas )
    {
        EditingFile file = editor.getCurrentFile();
        int[] charPos = file.getLastCharPos();
        
        int paddingLineStart = editor.getPaddingLineStart();
        int lineHeight = editor.getLineHeight();
        int paddingTop = (int) ( editor.getBaseline() - ( lineHeight * 0.75 ) );
        float charWidth = editor.getCharWidth();
        //int linesPadding = (int) Math.ceil( paintLines.measureText( " " + totalLines + " " ) );
        
        RectF rect = new RectF( 0, 0, 0, 0 );
        
        int startLine = 0;
        int total = matches.size();
        
        for( int i = 0; i < total; i++ ) //Integer[] match : matches )
        {
            if( currentMatch != i )
            {
                Integer[] match = matches.get( i );
                
                int start = match[0];
                while( startLine < charPos.length && charPos[startLine] <= start ) startLine++;
                
                int startOffset = 0;
                if( startLine > 0 )
                {
                    startOffset = charPos[startLine - 1];
                }
                else
                {
                    startOffset = 0;
                }
                
                int end = match[0] + match[1];
                int endLine = startLine;
                while( endLine < charPos.length && charPos[endLine] <= end ) endLine++;
                
                int endOffset = 0;
                if( endLine > 0 )
                {
                    endOffset = charPos[endLine - 1];
                }
                else
                {
                    endOffset = 0;
                }
                
                int currentLine = startLine;
                
                while( currentLine <= endLine )
                {
                    int left = currentLine == startLine ? ( start - startOffset ) : 0;
                    rect.left = paddingLineStart + left * charWidth;
                    rect.top = paddingTop + currentLine * lineHeight;
                    
                    int right = currentLine == endLine ? ( end - endOffset ) : ( charPos[currentLine] - startOffset );
                    // if( currentLine == endLine && currentLine == startLine ) right -= start;
                    rect.right = paddingLineStart + right * charWidth;
                    rect.bottom = rect.top + lineHeight;
                    
                    canvas.drawRoundRect( rect, 6, 6, highlighterFill );
                    canvas.drawRoundRect( rect, 6, 6, highlighterStroke );
                    
                    currentLine++;
                }
            }
        }
    }
    
    public void updateSelection()
    {
        String value = editor.getText().toString();
        String search = editor.applyTabs( findField.getText().toString() );
        
        if( !matchCase )
        {
            value = value.toLowerCase();
            search = search.toLowerCase();
        }
        
        matches.clear();
        currentMatch = -1;
        
        if( !search.isEmpty() )
        {
            if( regex )
            {
                int i = 0;
                int tabChars = editor.tabChars().length();
                StringBuilder replacement = new StringBuilder();
                while( i < tabChars )
                {
                    replacement.append( "\\t" );
                    i++;
                }
                
                String temp = findField.getText().toString().replace( "\\t", replacement );
                
                try
                {
                    pattern = Pattern.compile( search );
                    Matcher m = pattern.matcher( value );
                    
                    while( m.find() )
                    {
                        matches.add( new Integer[]{ m.start(), m.end() - m.start() } );
                        
                        Log.v( LOG_TAG, "Rep: " + m.toString() );
                        for( i = 0; i < m.groupCount(); i++ )
                        {
                            Log.v( LOG_TAG, "Found: " + m.group( i ) );
                        }
                        
                        // Log.v( LOG_TAG, "Found: " + m.group( 0 ) );
                    }
                }
                catch( PatternSyntaxException e )
                {
                    // e.printStackTrace();
                }
            }
            else
            {
                int index = -1;
                
                do
                {
                    index += search.length();
                    
                    index = value.indexOf( search, index );
                    
                    if( index >= 0 )
                    {
                        matches.add( new Integer[]{ index, search.length() } );
                    }
                }
                while( index >= 0 );
            }
        }
        
        editor.invalidate();
    }
    
    private TextWatcher textWatcher = new TextWatcher()
    {
        @Override
        public void afterTextChanged( Editable s )
        {
            updateSelection();
        }
        
        @Override
        public void beforeTextChanged( CharSequence s, int start, int count, int after )
        {
            // TODO Auto-generated method stub
        }
        
        @Override
        public void onTextChanged( CharSequence s, int start, int before, int count )
        {
            
        }
    };
}

package com.webdefault.corks.editor;

import android.content.Context;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.webdefault.corks.R;
import com.webdefault.corks.editor.highlight.Highlighter;
import com.webdefault.corks.editor.highlight.Syntax;
import com.webdefault.lib.Utils;
import com.webdefault.lib.db.ResultColumns;
import com.webdefault.corks.LocalDatabase;
import com.webdefault.corks.MainActivity;
import com.webdefault.corks.editor.history.EditHistory;
import com.webdefault.corks.editor.history.EditItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by orlandoleite on 2/22/18.
 */

class EditingTextFile extends EditingFile
{
    private static final String LOG_TAG = "EditingTextFile";
    
    private String path;
    private String value;
    
    private boolean[] lineCounter = null;
    private int[] lastCharPos = null;
    private int totalLines;
    private File file;
    private File editingFile;
    
    private String charset;
    private Context context;
    
    private ResultColumns fileInfo;
    
    private EditHistory mEditHistory;
    private Editor editor;
    
    private boolean mIsUndoOrRedo = false;
    
    private LocalDatabase db;
    
    private Highlighter highlighter;
    
    public EditingTextFile( Context context, Editor editor, ResultColumns fileInfo )
    {
        db = LocalDatabase.Companion.getInstance( context );
        this.fileInfo = fileInfo;
        
        this.editor = editor;
        this.context = context;
        this.path = fileInfo.get( "path" );
        
        mEditHistory = new EditHistory();
        mEditHistory.setMaxHistorySize( -1 );
        
        editingFile = db.getFileDataFromId( fileInfo.get( "uuid" ) );
        
        if( editingFile.exists() )
            mEditHistory.load(
                    Integer.valueOf( fileInfo.get( "edit_position" ) ),
                    db.getOpenFileEdition( fileInfo ) );
        
        int status = Integer.valueOf( fileInfo.get( "status" ) );
        
        if( path.isEmpty() || status == -2 )
        {
            charset = "UTF-8";
            this.value = "";
            
            highlighter = new Highlighter( context, editor, Syntax.getFor( context, "", "" ) );
        }
        else
        {
            try
            {
                file = new File( path );
                
                File tFile = editingFile.exists() ? editingFile : file;
                FileInputStream fileInputStream = new FileInputStream( tFile );
                
                charset = detectCharset( fileInputStream );
                charset = charset == null ? "UTF-8" : charset;
                
                fileInputStream.close();
                
                fileInputStream = new FileInputStream( tFile );
                
                if( fileInputStream != null )
                {
                    InputStreamReader inputStreamReader = new InputStreamReader( fileInputStream, charset );
                    
                    StringBuilder stringBuilder = new StringBuilder();
                    char[] buff = new char[500];
                    for( int charsRead; ( charsRead = inputStreamReader.read( buff ) ) != -1; )
                    {
                        stringBuilder.append( buff, 0, charsRead );
                    }
                    
                    inputStreamReader.close();
                    this.value = stringBuilder.toString();
                }
                
                String[] split = value.split( "\r\n|\r|\n", 2 );
                String firstLine = split.length > 1 ? split[0] : "";
                
                highlighter = new Highlighter( 
                        context,
                        editor, 
                        Syntax.getFor( context, MimeTypeMap.getFileExtensionFromUrl( path ), firstLine ) );
            }
            catch( FileNotFoundException e )
            {
                Log.e( LOG_TAG, "File not found: " + e.toString() );
                this.value = "File not found: " + e.toString();
            }
            catch( IOException e )
            {
                Log.e( LOG_TAG, "Can not read file: " + e.toString() );
                this.value = "Can not read file: " + e.toString();
            }
        }
    }
    
    public String getSyntax()
    {
        if( highlighter != null )
            return highlighter.getSyntax();
        else
            return "";
    }
    
    public void setSyntax( String name )
    {
        if( highlighter != null ) highlighter.setSyntax( context, name );
    }
    
    public void applySyntaxHighlighting()
    {
        if( highlighter != null ) highlighter.highlight();
    }
    
    /**
     * Can undo be performed?
     */
    public boolean canUndo()
    {
        return mEditHistory.canUndo();
    }
    
    /**
     * Perform undo.
     */
    public void undo()
    {
        EditItem edit = mEditHistory.getPrevious();
        if( edit == null )
        {
            return;
        }
        
        Editable text = editor.getText();
        int start = edit.start;
        int end = start + ( edit.after != null ? edit.after.length() : 0 );
        
        mIsUndoOrRedo = true;
        Log.v( LOG_TAG, "undo: " + edit.before + ", " + start + ", " + end );
        
        editor.replaceWithTextWatcher( start, end, edit.before );
        
        mIsUndoOrRedo = false;
        
        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        /*for( Object o : text.getSpans( 0, text.length(), UnderlineSpan.class ) )
        {
            text.removeSpan( o );
        }*/
        
        int index = edit.before == null ? start : ( start + edit.before.length() );
        if( index >= text.length() ) index = text.length();
        Selection.setSelection( text, index );
    }
    
    public boolean isUndoOrRedo()
    {
        return mIsUndoOrRedo;
    }
    
    /**
     * Can redo be performed?
     */
    public boolean canRedo()
    {
        return mEditHistory.canRedo();
    }
    
    /**
     * Perform redo.
     */
    public void redo()
    {
        EditItem edit = mEditHistory.getNext();
        if( edit == null )
        {
            return;
        }
        
        Editable text = editor.getText();
        int start = edit.start;
        int end = start + ( edit.before != null ? edit.before.length() : 0 );
        
        mIsUndoOrRedo = true;
        editor.replaceWithTextWatcher( start, end, edit.after );
        mIsUndoOrRedo = false;
        
        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        /*for( Object o : text.getSpans( 0, text.length(), UnderlineSpan.class ) )
        {
            text.removeSpan( o );
        }*/
        
        Selection.setSelection( text, edit.after == null ? start
                : ( start + edit.after.length() ) );
    }
    
    public String getPath()
    {
        return path;
    }
    
    public String getText()
    {
        return value;
    }
    
    public void setText( String value )
    {
        this.value = value;
    }
    
    public void setupLayout( Layout layout )
    {
        setupLayout( layout, value );
    }
    
    public void setupLayout( Layout layout, String value )
    {
        if( layout != null )
        {
            int i = 0, total = layout.getLineCount();
            lineCounter = new boolean[total];
            lastCharPos = new int[total];
            
            boolean hasToMark = true;
            totalLines = 0;
            
            if( value.isEmpty() )
            {
                lineCounter[0] = true;
                lastCharPos[0] = 0;
                totalLines = 1;
            }
            else
            {
                while( i < total )
                {
                    lineCounter[i] = hasToMark;
                    hasToMark = false;
                    
                    int lineEnd = layout.getLineEnd( i );
                    lastCharPos[i] = lineEnd;
                    if( value.charAt( lineEnd - 1 ) == '\n' )
                    {
                        totalLines++;
                        hasToMark = true;
                    }
                    
                    i++;
                }
            }
        }
    }
    
    /*
    public TextWatcher getTextWatcher()
    {
        return textWatcher;
    }
    */
    public boolean[] getLineCounter( Layout layout )
    {
        if( lineCounter == null ) setupLayout( layout );
        
        return lineCounter;
    }
    
    public int getTotalLines()
    {
        return totalLines;
    }
    
    public int[] getLastCharPos()
    {
        return lastCharPos;
    }
    
    public String getAbout()
    {
        String lastModDate;
        long size;
        String name;
        
        if( file == null )
        {
            lastModDate = "---";
            size = 0;
            name = context.getResources().getString( R.string.action_file_untitled );
        }
        else
        {
            lastModDate = SimpleDateFormat.getInstance().format( new Date( file.lastModified() ) );
            size = file.length();
            name = file.getName();
        }
        
        return	name + "\n" + context.getResources().getString( R.string.action_info_filesize ) + ": " +
                Utils.shortFileSize( size ) + "\n" +
                context.getResources().getString( R.string.action_info_encoding ) + ": " +
                charset + "\n" + context.getResources().getString( R.string.action_info_edited_at ) + ": " +
                lastModDate;
    }
    
    public void save()
    {
        if( Integer.valueOf( fileInfo.get( "status" ) ) < 0 )
            ( (MainActivity) context ).saveAs( path );
        else
        {
            try
            {
                FileOutputStream out = new FileOutputStream( file, false );
                byte[] contents = value.getBytes( charset );
                out.write( contents );
                out.flush();
                out.close();
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            
            fileInfo.put( "status", "" + LocalDatabase.Companion.getFILE_SAVED());
            db.updateOpenFile( fileInfo );
            
            if( editingFile == null )
            {
                editingFile = db.getFileDataFromId( fileInfo.get( "uuid" ) );
            }
            
            if( editingFile.exists() ) editingFile.delete();
            
            ( (MainActivity) context ).updateFileList();
        }
        
        ((MainActivity) context).showAd();
    }
    
    public void saveAs( String path )
    {
        file = new File( path );
        fileInfo.put( "name", file.getName() );
        fileInfo.put( "path", path );
        fileInfo.put( "status", "" + LocalDatabase.Companion.getFILE_SAVED());
        
        LocalDatabase.Companion.getInstance( context ).saveAs( path, fileInfo );
        
        this.path = path;
        
        ( (MainActivity) context ).updateFileList();
        
        save();
    }
    
    public void saveWithEncode( String encode )
    {
        this.charset = encode;
        
        save();
    }
    
    public void textChanged()
    {
        int status = Integer.valueOf( fileInfo.get( "status" ) );
        if( status == LocalDatabase.Companion.getFILE_SAVED())
        {
            fileInfo.put( "status", "" + LocalDatabase.Companion.getFILE_EDITED());
            ( (MainActivity) context ).updateFileList();
        }
        else if( status == LocalDatabase.Companion.getNEW_FILE_UNTOUCHED())
        {
            fileInfo.put( "status", "" + LocalDatabase.Companion.getNEW_FILE_EDITED());
            ( (MainActivity) context ).updateFileList();
        }
    }
    
    int lastEdtition = 0;
    
    public void addEdition( int start, String beforeChange, String afterChange )
    {
        if( !mIsUndoOrRedo )
        {
            if( beforeChange.isEmpty() )
            {
                if( afterChange.length() > 1 || start - 1 != lastEdtition ||
                        afterChange.equals( " " ) || afterChange.equals( "\t" ) || afterChange.equals( "\n" ) )
                {
                    Log.v( LOG_TAG, "history add " + start + ", " + beforeChange + ", " + afterChange );
                    mEditHistory.add( new EditItem( start, beforeChange, afterChange ) );
                }
                else
                {
                    Log.v( LOG_TAG, "history last " + start + ", " + beforeChange + ", " + afterChange );
                    
                    EditItem item = mEditHistory.getLast();
                    item.after = item.after + afterChange;
                    mEditHistory.updateLast( item );
                }
            }
            else if( afterChange.isEmpty() )
            {
                if( beforeChange.length() > 1 || start + 1 != lastEdtition ||
                        beforeChange.equals( " " ) || beforeChange.equals( "\t" ) || beforeChange.equals( "\n" ) )
                {
                    Log.v( LOG_TAG, "history 2 add " + start + ", " + beforeChange + ", " + afterChange );
                    
                    mEditHistory.add( new EditItem( start, beforeChange, afterChange ) );
                }
                else
                {
                    Log.v( LOG_TAG, "history 2 last " + start + ", " + beforeChange + ", " + afterChange );
                    
                    EditItem item = mEditHistory.getLast();
                    item.start = start;
                    item.before = beforeChange + item.before;
                    Log.v( LOG_TAG, "item: " + start + ", " + item.before );
                    mEditHistory.updateLast( item );
                }
            }
            
            lastEdtition = start;
        }
    }
    
    @Override
    public void pause()
    {
        int status = Integer.valueOf( fileInfo.get( "status" ) );
        
        if( status == 1 || status == -1 )
        {
            if( editingFile == null )
            {
                editingFile = db.getFileDataFromId( fileInfo.get( "uuid" ) );
            }
            
            try
            {
                FileOutputStream out = new FileOutputStream( editingFile, false );
                byte[] contents = value.getBytes( charset );
                out.write( contents );
                out.flush();
                out.close();
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            
            db.pauseOpenFile( fileInfo, mEditHistory.getHistory(), mEditHistory.getPosition() );
        }
    }
}

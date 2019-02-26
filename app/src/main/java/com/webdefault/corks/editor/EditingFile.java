package com.webdefault.corks.editor;

import android.content.Context;
import android.text.Layout;

import com.webdefault.lib.db.ResultColumns;
import com.webdefault.corks.MainActivity;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by orlandoleite on 2/22/18.
 */

public abstract class EditingFile
{
    private static final String LOG_TAG = "EditingFile";
    
    public abstract String getPath();
    
    public abstract String getText();
    
    public abstract void setText( String value );
    
    private static final UniversalDetector detector = new UniversalDetector( null );
    
    protected static String detectCharset( InputStream fileInputStream )
    {
        String encoding = null;
        
        try
        {
            byte[] buf = new byte[4096];
            int nread;
            while( ( nread = fileInputStream.read( buf ) ) > 0 && !detector.isDone() )
            {
                // Log.v( LOG_TAG, "buf: " + new String( buf, "UTF-8" ) );
                detector.handleData( buf, 0, nread );
            }
            // (3)
            detector.dataEnd();
            
            // (4)
            encoding = detector.getDetectedCharset();
            
            // (5)
            detector.reset();
            // fileInputStream.reset();
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
        
        
        return encoding;
    }
    
    public abstract void setupLayout( Layout layout );
    
    public abstract void setupLayout( Layout layout, String value );
    
    public abstract boolean[] getLineCounter( Layout layout );
    
    public abstract int getTotalLines();
    
    public abstract int[] getLastCharPos();
    
    public abstract String getAbout();
    
    public abstract void save();
    
    public abstract void saveAs( String path );
    
    public abstract void saveWithEncode( String encode );
    
    public abstract void textChanged();
    
    public abstract void addEdition( int start, String beforeChange, String afterChange );
    
    public abstract boolean canUndo();
    public abstract boolean canRedo();
    
    public abstract void undo();
    public abstract void redo();
    
    public abstract void pause();
    
    
    public abstract String getSyntax();
    public abstract void setSyntax( String name );
    
    public abstract void applySyntaxHighlighting();
    
    public abstract boolean isUndoOrRedo();
    
    
    public static EditingFile getFile( Context context, Editor editor, ResultColumns file )
    {
        int status = Integer.valueOf( file.get( "status" ) );
        String path = file.get( "path" );
        
        if( status < 0 )
        {
            return new EditingTextFile( context, editor, file );
        }
        else
        {
            String mime = MainActivity.getMimeType( path );
            // Log.v( LOG_TAG, "MIME: " + mime );
            
            if( mime.indexOf( "text/" ) == 0 )
                return new EditingTextFile( context, editor, file );
            else
                return null;
        }
    }
}

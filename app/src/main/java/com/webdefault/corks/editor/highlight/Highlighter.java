package com.webdefault.corks.editor.highlight;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Highlighter
{
    private static final String LOG_TAG = "Highlighter";
    
    private TextView textView;
    private SyntaxDoc doc;
    private Context context;
    
    public Highlighter( Context context, TextView textView, SyntaxDoc doc )
    {
        this.textView = textView;
        this.doc = doc;
        this.context = context;
        
        loadPatterns();
    }
    
    private void loadPatterns()
    {
        if( doc.patterns != null )
        {
            for( PatternDoc p : doc.patterns )
            {
                Log.v( LOG_TAG, "loadPatterns: " + p.match );
                if( p.pattern == null ) p.pattern = Pattern.compile( p.match );
                
                String packageName = context.getPackageName();
                int resId = context.getResources().getIdentifier( "syntax_" + p.scope, "color", packageName );
                p.color = ContextCompat.getColor( context, resId ); 
            }
        }
    }
    
    public void highlight()
    {
        if( doc != null && doc.patterns != null )
        {
            Editable editable = textView.getEditableText();
            String text = editable.toString();
            
            for( PatternDoc p : doc.patterns )
            {
                Matcher matcher = p.pattern.matcher( text );
                
                while( matcher.find() )
                {
                    // Log.v( LOG_TAG, "match: " + matcher.group() + " scope " + p.scope );
                    editable.setSpan( new ForegroundColorSpan( p.color ), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
                }
            }
        }
    }
    
    public String getSyntax()
    {
        return doc.name;
    }
    
    public void setSyntax( Context context, String name )
    {
        doc = Syntax.getByName( context, name );
    }
}

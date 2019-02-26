package com.webdefault.corks.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.johnkil.print.PrintButton;
import com.webdefault.corks.editor.highlight.Syntax;
import com.webdefault.corks.editor.tool.KeyboardTool;
import com.webdefault.lib.db.ResultColumns;
import com.webdefault.corks.MainActivity;
import com.webdefault.corks.R;
import com.webdefault.corks.editor.tool.FindTool;
import com.webdefault.corks.editor.tool.SaveTool;
import com.webdefault.corks.editor.tool.Tool;
import com.webdefault.corks.editor.tool.UndoTool;

import java.util.Set;

/**
 * Created by orlandoleite on 2/28/18.
 */

public class ToolsSelector extends LinearLayout implements View.OnClickListener, View.OnLongClickListener
{
	private static String LOG_TAG = "ToolsSelector";
	
	private View currentToolView;
	private Tool currentTool;
	private ViewGroup toolContainer;
	
	private Editor editor;
	private TextView aboutFile;
	
	private String path;
	
	private KeyboardTool keyboardTool;
	private SaveTool saveTool;
	private FindTool findTool;
	private UndoTool undoTool;
	
	private PrintButton syntaxBtn;
	
	public ToolsSelector( Context context, @Nullable AttributeSet attrs )
	{
		super( context, attrs );
	}
	
	public void init( Editor editor )
	{
		this.editor = editor;
		
		aboutFile = (TextView) findViewById( R.id.about_file );
		View view = findViewById( R.id.save_btn );
		view.setOnClickListener( this );
		view.setOnLongClickListener( this );
		
		view = findViewById( R.id.find_btn );
		view.setOnClickListener( this );
		view.setOnLongClickListener( this );
		
		view = findViewById( R.id.undo_btn );
		view.setOnClickListener( this );
		view.setOnLongClickListener( this );
		
		syntaxBtn = (PrintButton) findViewById( R.id.syntax_btn );
		syntaxBtn.setOnClickListener( this );
		
		findViewById( R.id.buy_btn ).setOnClickListener( this );
		
		toolContainer = (ViewGroup) ( (MainActivity) getContext() ).findViewById( R.id.main_container );
		
		keyboardTool = (KeyboardTool) ((MainActivity) getContext() ).findViewById( R.id.default_tool );
		keyboardTool.init( editor, this );
		// showToolView( defaultTool );
	}
	
	public void loadFile( ResultColumns file )
	{
		String path = file.get("path");
		
		this.path = path;
		// Log.v( "ToolsSelector", "about: " + editor.getAbout() );
		aboutFile.setText( editor.getAbout() );
		syntaxBtn.setText( editor.getSyntax() );
	}
	
	public String getPath()
	{
		return path;
	}
	
	@Override
	public void onClick( View view )
	{
		switch( view.getId() )
		{
			case R.id.save_btn:
				editor.saveCurrentFile();
				// TODO: CHECK HERE
				// loadFile( "" );
				break;
			
			case R.id.find_btn:
				showFindTool( false );
				break;
			
			case R.id.undo_btn:
				showUndoTool();
				break;
				
			case R.id.syntax_btn:
				showSyntaxSelector();
				break;
				
			case R.id.buy_btn:
				((MainActivity) getContext()).subscribe();
				break;
		}
		
		( (MainActivity) getContext() ).closeDrawers();
	}
	
	public void showFindTool( boolean replace )
	{
		if( findTool == null )
			findTool = (FindTool) LayoutInflater.from( getContext() ).inflate( R.layout.find_tool, null );
		showToolView( findTool );
		
		findTool.setShowReplace( true );
	}
	
	public void showUndoTool()
	{
		if( undoTool == null )
			undoTool = (UndoTool) LayoutInflater.from( getContext() ).inflate( R.layout.undo_tool, null );
		showToolView( undoTool );
	}
	
	private void showSyntaxSelector()
	{
		AlertDialog.Builder builderSingle = new AlertDialog.Builder( getContext() );
		builderSingle.setTitle( "Available syntaxes" );
		
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>( getContext(), R.layout.dialog_listitem );
		
		Set<String> available = Syntax.getAvailable( getContext() );
		
		for( String item : available )
		{
			arrayAdapter.add( item );
		}
		
		arrayAdapter.add( "Text/plain" );
		
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
				String syntax = arrayAdapter.getItem( which );
				editor.setSyntax( syntax );
				syntaxBtn.setText( syntax );
			}
		} );
		builderSingle.show();
	}
	
	public void showToolView( View tool )
	{
		if( currentToolView == tool )
		{
			currentTool.init( editor, this );
		}
		else
		{
			if( currentToolView != null ) hideToolView( true );
			
			currentToolView = tool;
			currentTool = (Tool) tool;
			
			toolContainer.addView( (View) currentToolView );
			currentTool.init( editor, this );
			
			CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
			params.gravity = Gravity.BOTTOM;
			currentToolView.setLayoutParams( params );
			
			currentToolView.animate().alpha( 1 ).setDuration( 300 ).start();
		}
	}
	
	public void hideToolView()
	{
		hideToolView( false );
	}
	
	public void hideToolView( boolean force )
	{
		// Log.v( LOG_TAG, "hideToolView" );
		if( currentToolView != null )
		{
			final View view = currentToolView;
			if( currentTool.canClose() || force )
			{
				currentToolView = null;
				currentTool = null;
				
				view.animate().alpha( 0 ).setDuration( 150 ).withEndAction( new Runnable()
				{
					@Override
					public void run()
					{
						toolContainer.removeView( view );
					}
				} ).start();
			}
		}
	}
	
	@Override
	public boolean onLongClick( View view )
	{
		boolean result = false;
		View tool;
		switch( view.getId() )
		{
			case R.id.save_btn:
				if( saveTool == null )
					saveTool = (SaveTool) LayoutInflater.from( getContext() ).inflate( R.layout.save_tool, null );
				
				showToolView( saveTool );
				result = true;
				break;
			
			case R.id.find_btn:
				showFindTool( true );
				result = true;
				break;
			
			case R.id.undo_btn:
				result = true;
				break;
		}
		
		if( result ) ( (MainActivity) getContext() ).closeDrawers();
		
		return result;
	}
}

package com.webdefault.corks.editor.tool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.webdefault.corks.MainActivity;
import com.webdefault.corks.R;
import com.webdefault.corks.editor.Editor;
import com.webdefault.corks.editor.ToolsSelector;

/**
 * Created by orlandoleite on 3/1/18.
 */

public class SaveTool extends LinearLayout implements Tool, View.OnClickListener
{
	private Editor editor;
	private ToolsSelector tools;
	
	public SaveTool( Context context, @Nullable AttributeSet attrs )
	{
		super( context, attrs );
	}
	
	public void init( Editor editor, ToolsSelector tools )
	{
		this.editor = editor;
		this.tools = tools;
		
		findViewById( R.id.save_as_btn ).setOnClickListener( this );
		findViewById( R.id.save_encoding_btn ).setOnClickListener( this );
	}
	
	public boolean canClose()
	{
		return true;
	}
	
	@Override
	public void onClick( View view )
	{
		switch( view.getId() )
		{
			case R.id.save_as_btn:
				( (MainActivity) getContext() ).saveAs( tools.getPath() );
				break;
			
			case R.id.save_encoding_btn:
				showEncodeSelector();
				break;
		}
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
}

package com.webdefault.corks.editor.adapter;

import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.webdefault.lib.db.ResultColumns;
import com.webdefault.lib.db.ResultLines;
import com.webdefault.corks.Dialogs;
import com.webdefault.corks.LocalDatabase;
import com.webdefault.corks.MainActivity;
import com.webdefault.corks.R;

/**
 * Created by orlandoleite on 2/21/18.
 */
public class FileLineAdapter extends RecyclerView.Adapter<FileHolder> implements View.OnClickListener
{
	private static final String LOG_TAG = "FileLineAdapter";
	
	private int focusedItem = 0;
	
	private ResultLines mFiles;
	private LocalDatabase db;
	private MainActivity context;
	
	private int newCount = 0;
	
	public FileLineAdapter( MainActivity context, ResultLines files, LocalDatabase db )
	{
		mFiles = files;
		this.db = db;
		this.context = context;
		
		Log.v( LOG_TAG, "mFiles: " + mFiles );
		if( mFiles.size() == 0 )
		{
			addNew();
		}
	}
	
	@Override
	public FileHolder onCreateViewHolder( ViewGroup parent, int viewType )
	{
		return new FileHolder( LayoutInflater.from( parent.getContext() )
				.inflate(
						viewType == 1 ? R.layout.open_file_listitem : R.layout.new_file_listitem,
						parent,
						false ), viewType, this );
	}
	
	public void addNew()
	{
		add( context.addNewFile() );
		newCount++;
	}
	
	public void add( ResultColumns file )
	{
		mFiles.add( file );
		focusedItem = mFiles.size() - 1;
		
		if( context != null )
			context.selectFile( file );
		
		notifyItemInserted( getItemCount() );
		notifyDataSetChanged();
	}
	
	public int getFocusedItem()
	{
		return focusedItem;
	}
	
	public void set( String oldPath, ResultColumns file )
	{
		int arraySize = mFiles.size();
		for( int i = 0; i < arraySize; i++ )
		{
			if( mFiles.get( i ).get( "path" ).equals( oldPath ) )
			{
				mFiles.set( i, file );
			}
		}
		
		notifyDataSetChanged();
	}
	
	private void remove( int position )
	{
		Log.v( LOG_TAG, "files: " + mFiles + " position: " + position );
		context.closeFile( mFiles.get( position ) );
		
		mFiles.remove( position );
		
		if( mFiles.size() == 0 )
		{
			addNew();
		}
		
		focusedItem = mFiles.size() - 1;
		context.selectFile( mFiles.get( focusedItem ) );
		
		notifyItemRemoved( position );
		notifyItemRangeChanged( position, mFiles.size() );
	}
	
	public void closeCurrentFile()
	{
		ResultColumns file = mFiles.get( focusedItem );
		
		if( file != null )
		{
			int status = Integer.valueOf( file.get( "status" ) );
			if( status == LocalDatabase.Companion.getNEW_FILE_EDITED() || status == LocalDatabase.Companion.getFILE_EDITED())
			{
				Dialogs.question( context, "Attention", "Are you sure you want to close it without save?", "Don't save", "Cancel", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick( DialogInterface dialogInterface, int i )
					{
						remove( focusedItem );
					}
				} );
			}
			else
			{
				Log.v( LOG_TAG, "remove 2: " + file.get( "id" ) );
				remove( focusedItem );
			}
		}
	}
	
	@Override
	public int getItemViewType( int position )
	{
		// Just as an example, return 0 or 2 depending on position
		// Note that unlike in ListView adapters, types don't have to be contiguous
		return position == mFiles.size() ? 2 : 1;
	}
	
	
	
	@Override
	public void onBindViewHolder( FileHolder holder, final int position )
	{
		if( holder.type == 1 )
		{
			final ResultColumns file = mFiles.get( position );
			
			holder.itemView.setSelected( focusedItem == position );
			if( focusedItem == position )
				context.selectFile( mFiles.get( position ) );
			
			int status = Integer.valueOf( file.get( "status" ) );
			Log.v( LOG_TAG, "onBindViewHolder: " + file );
			holder.title.setText( file.get( "name" ) +
					( status == LocalDatabase.Companion.getNEW_FILE_EDITED() || status == LocalDatabase.Companion.getFILE_EDITED() ? " *" : "" ) );
			holder.position = position;
			holder.closeBtn.setTag( holder );
			
			holder.itemView.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick( View v )
				{
					// Redraw the old selection and the new
					notifyItemChanged( focusedItem );
					focusedItem = position;
					notifyItemChanged( focusedItem );
				}
			} );
		}
		else
		{
			holder.closeBtn.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick( View view )
				{
					addNew();
				}
			} );
		}
		//holder.deleteButton.setOnClickListener( view -> removerItem( position ) );
	}
	
	@Override
	public int getItemCount()
	{
		return mFiles != null ? mFiles.size() + 1 : 1;
	}
	
	
	@Override
	public void onClick( View view )
	{
		if( view.getId() == R.id.close_btn )
		{
			final FileHolder holder = (FileHolder) view.getTag();
			ResultColumns file = mFiles.get( holder.position );
			
			if( file != null )
			{
				int status = Integer.valueOf( file.get( "status" ) );
				if( status == LocalDatabase.Companion.getNEW_FILE_EDITED() || status == LocalDatabase.Companion.getFILE_EDITED())
				{
					Dialogs.question( context, "Attention", "Are you sure you want to close it without save?", "Don't save", "Cancel", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick( DialogInterface dialogInterface, int i )
						{
							remove( holder.position );
						}
					} );
				}
				else
				{
					Log.v( LOG_TAG, "remove 2: " + file.get( "id" ) );
					remove( holder.position );
				}
			}
		}
	}
}

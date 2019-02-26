package com.webdefault.corks.editor.history;

/**
 * Created by orlandoleite on 3/27/18.
 */

import com.webdefault.lib.db.ResultColumns;
import com.webdefault.lib.db.ResultLines;

import java.util.LinkedList;

/**
 * Keeps track of all the edit history of a text.
 */
public class EditHistory
{
	/**
	 * The position from which an EditItem will be retrieved when getNext()
	 * is called. If getPrevious() has not been called, this has the same
	 * value as mmHistory.size().
	 */
	private int mmPosition = 0;
	
	/**
	 * Maximum undo history size.
	 */
	private int mmMaxHistorySize = -1;
	
	/**
	 * The list of edits in chronological order.
	 */
	private final LinkedList<EditItem> mmHistory = new LinkedList<EditItem>();
	
	/**
	 * Clear history.
	 */
	private void clear()
	{
		mmPosition = 0;
		mmHistory.clear();
	}
	
	public void load( int position, ResultLines editions )
	{
		for( ResultColumns edit : editions )
		{
			mmHistory.add( new EditItem( Integer.valueOf( edit.get( "start" ) ), edit.get( "before" ), edit.get( "after" ) ) );
		}
		
		mmPosition = position;
	}
	
	/**
	 * Adds a new edit operation to the history at the current position. If
	 * executed after a call to getPrevious() removes all the future history
	 * (elements with positions >= current history position).
	 */
	public void add( EditItem item )
	{
		while( mmHistory.size() > mmPosition )
		{
			mmHistory.removeLast();
		}
		mmHistory.add( item );
		mmPosition++;
		
		if( mmMaxHistorySize >= 0 )
		{
			trimHistory();
		}
	}
	
	
	public EditItem getLast()
	{
		return mmHistory.getLast();
	}
	
	public void updateLast( EditItem item )
	{
		mmHistory.set( mmHistory.size() - 1, item );
	}
	
	/**
	 * Set the maximum history size. If size is negative, then history size
	 * is only limited by the device memory.
	 */
	public void setMaxHistorySize( int maxHistorySize )
	{
		mmMaxHistorySize = maxHistorySize;
		if( mmMaxHistorySize >= 0 )
		{
			trimHistory();
		}
	}
	
	/**
	 * Trim history when it exceeds max history size.
	 */
	private void trimHistory()
	{
		while( mmHistory.size() > mmMaxHistorySize )
		{
			mmHistory.removeFirst();
			mmPosition--;
		}
		
		if( mmPosition < 0 )
		{
			mmPosition = 0;
		}
	}
	
	/**
	 * Traverses the history backward by one position, returns and item at
	 * that position.
	 */
	public EditItem getPrevious()
	{
		if( mmPosition == 0 )
		{
			return null;
		}
		mmPosition--;
		return mmHistory.get( mmPosition );
	}
	
	/**
	 * Traverses the history forward by one position, returns and item at
	 * that position.
	 */
	public EditItem getNext()
	{
		if( mmPosition >= mmHistory.size() )
		{
			return null;
		}
		
		EditItem item = mmHistory.get( mmPosition );
		mmPosition++;
		return item;
	}
	
	public boolean canUndo()
	{
		return mmPosition > 0;
	}
	
	public boolean canRedo()
	{
		return (mmPosition < mmHistory.size());
	}
	
	
	public LinkedList<EditItem> getHistory()
	{
		return mmHistory;
	}
	
	public int getPosition()
	{
		return mmPosition;
	}
}

package com.webdefault.corks.editor.history;

/**
 * Created by orlandoleite on 3/27/18.
 */

/**
 * Represents the changes performed by a single edit operation.
 */
public class EditItem
{
	public int start;
	public String before;
	public String after;
	
	/**
	 * Constructs EditItem of a modification that was applied at position
	 * start and replaced CharSequence before with CharSequence after.
	 */
	public EditItem( int start, String before, String after )
	{
		this.start = start;
		this.before = before;
		this.after = after;
	}
}

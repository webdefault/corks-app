package com.webdefault.corks.editor.adapter;

/**
 * Created by orlandoleite on 2/18/18.
 */

public class FileObject
{
	public int icon;
	public String name, path;
	public boolean isDirectory;
	public boolean isRendered;
	
	public FileObject( int icon, String name, String path, boolean isDirectory )
	{
		this.icon = icon;
		this.name = name;
		this.path = path;
		this.isDirectory = isDirectory;
	}
}

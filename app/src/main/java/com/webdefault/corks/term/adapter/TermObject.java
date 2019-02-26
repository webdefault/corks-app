package com.webdefault.corks.term.adapter;

public class TermObject
{
	public int icon;
	public String name, path;
	public boolean isDirectory;
	public boolean isRendered;
	
	public TermObject( int icon, String name, String path, boolean isDirectory )
	{
		this.icon = icon;
		this.name = name;
		this.path = path;
		this.isDirectory = isDirectory;
	}
}

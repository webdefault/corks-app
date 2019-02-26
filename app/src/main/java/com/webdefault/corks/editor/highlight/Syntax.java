package com.webdefault.corks.editor.highlight;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.Patterns;
import android.webkit.MimeTypeMap;

import com.webdefault.lib.Utils;
import com.webdefault.lib.db.ResultLines;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by orlandoleite on 4/3/18.
 */

public class Syntax
{
	private static final String LOG_TAG = "Syntax";
	
	private static final String SYNTAX = "/home/.corks/syntax";
	private static final String ASSETS_SYNTAX = "syntax";
	private static List<SyntaxDoc> available = null;
	private static HashMap<String, SyntaxDoc> names = null;
	private static HashMap<String, SyntaxDoc> extensions = null;
	private static HashMap<Pattern, SyntaxDoc> firstLines = null;
	
	private static Yaml yaml = new Yaml( new Constructor( SyntaxDoc.class ) );
	private static File directory = null;
	
	public static void refreshSyntax( Context context )
	{
		directory = new File( context.getFilesDir(), SYNTAX );
		if( Utils.deleteRecursive( directory ) )
		{
			available = null;
			getAvailable( context );
		}
		else
			Log.e( LOG_TAG, "refreshSyntax cannot delete" );
	}
	
	private static void reset( Context context )
	{
		directory = new File( context.getFilesDir(), SYNTAX );
		if( !directory.isDirectory() ) directory.mkdirs();
		
		if( directory.list().length == 0 )
		{
			Log.v( LOG_TAG, "reset start copying" );
			AssetManager assetManager = context.getAssets();
			String[] files = null;
			try
			{
				files = assetManager.list( ASSETS_SYNTAX );
				Log.v( LOG_TAG, "try files size:" + files.length );
			}
			catch( IOException e )
			{
				Log.e( LOG_TAG, "Failed to get asset file list.", e );
			}
			
			if( files != null )
			{
				for( String filename : files )
				{
					InputStream in = null;
					OutputStream out = null;
					
					try
					{
						in = assetManager.open( ASSETS_SYNTAX + "/" + filename );
						File outFile = new File( directory, filename );
						out = new FileOutputStream( outFile );
						Utils.copyFile( in, out );
						
						Log.v( LOG_TAG, "copyFile: " + outFile.getPath() );
					}
					catch( IOException e )
					{
						Log.e( "tag", "Failed to copy asset file: " + filename, e );
					}
					finally
					{
						if( in != null )
						{
							try
							{
								in.close();
							}
							catch( IOException e )
							{
								// NOOP
							}
						}
						if( out != null )
						{
							try
							{
								out.close();
							}
							catch( IOException e )
							{
								// NOOP
							}
						}
					}
				}
			}
		}
	}
	
	private static void loadAvailable( Context context )
	{
		reset( context );
		
		final File directory = new File( context.getFilesDir(), SYNTAX );
		String[] files = directory.list();
		
		File cache = new File( directory, "cache" );
		
		if( !cache.exists() )
		{
			available = new ArrayList<>();
			names = new HashMap<>();
			extensions = new HashMap<>();
			firstLines = new HashMap<>();
			
			for( String f : files )
			{
				SyntaxDoc doc = getSyntaxDoc( f );
				Log.v( LOG_TAG, "doc: " + doc );
				if( doc != null )
				{
					SyntaxDoc cached = new SyntaxDoc();
					cached.extensions = doc.extensions;
					cached.firstLine = doc.firstLine;
					cached.name = doc.name;
					cached.fileName = f;
					
					available.add( cached );
					
					if( cached.extensions != null )
					{
						for( String name : cached.extensions )
						{
							if( !extensions.containsKey( name ) )
							{
								extensions.put( name, cached );
							}
						}
					}
					
					if( cached.firstLine != null )
						firstLines.put( Pattern.compile( cached.firstLine ), cached );
					
					names.put( cached.name, cached );
				}
			}
			
			String output = yaml.dump( available );
			
			try
			{
				FileOutputStream out = new FileOutputStream( cache, false );
				byte[] contents = output.getBytes( Charset.forName( "UTF-8" ) );
				out.write( contents );
				out.flush();
				out.close();
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
			
			Log.v( LOG_TAG, "output: " + output );
		}
		else
		{
			try
			{
				Yaml y = new Yaml();
				available = (ArrayList<SyntaxDoc>) y.load( new FileInputStream( cache ) );
				
				if( files.length != available.size() )
				{
					available = null;
					cache.delete();
					
					loadAvailable( context );
				}
				else
				{
					for( SyntaxDoc cached : available )
					{
						for( String name : cached.extensions )
						{
							if( !extensions.containsKey( name ) )
							{
								extensions.put( name, cached );
							}
						}
						
						firstLines.put( Pattern.compile( cached.firstLine ), cached );
					}
				}
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}
	}
	
	private static SyntaxDoc getSyntaxDoc( String f )
	{
		File file = new File( directory, f );
		SyntaxDoc doc = null;
		
		try
		{
			doc = (SyntaxDoc) yaml.load( new FileInputStream( file ) );
		}
		catch( FileNotFoundException e )
		{
			Log.e( LOG_TAG, "File not found", e );
		}
		catch( Exception e )
		{
			e.printStackTrace();
			doc = null;
		}
		
		return doc;
	}
	
	public static Set<String> getAvailable( Context context )
	{
		if( available == null )
		{
			loadAvailable( context );
		}
		
		return names.keySet();
	}
	
	public static SyntaxDoc getByName( Context context, String name )
	{
		if( available == null )
		{
			loadAvailable( context );
		}
		
		SyntaxDoc cached = names.get( name );
		
		return getSyntaxDoc( cached.fileName );
	}
	
	public static SyntaxDoc getFor( Context context, String extension, String firstLine )
	{
		if( available == null )
		{
			loadAvailable( context );
		}
		
		for( Map.Entry<String, SyntaxDoc> entry : extensions.entrySet() )
		{
			if( entry.getKey().equals( extension ) )
			{
				return Syntax.getSyntaxDoc( entry.getValue().fileName );
			}
		}
		
		for( Map.Entry<Pattern, SyntaxDoc> entry : firstLines.entrySet() )
		{
			Matcher matcher = entry.getKey().matcher( firstLine );
			if( matcher.find() )
			{
				return Syntax.getSyntaxDoc( entry.getValue().fileName );
			}
		}
		
		SyntaxDoc doc = new SyntaxDoc();
		doc.name = "text/plain";
		
		return doc;
	}
}

package com.webdefault.corks;

/**
 * Created by orlandoleite on 2/12/18.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.johnkil.print.PrintConfig;
import com.github.johnkil.print.PrintView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FileBrowser extends Activity implements View.OnClickListener
{
	private static final String LOG_TAG = "FileBrowser";

	public static final String DIALOG_TYPE = "dialog_type";
	public static final String PATH = "path";
	public static final String FILETYPE = "filetype";
	public static final String DEFAULT_NAME = "default_name";
	public static final String FOLDER_RESOURCE = "folder_resource";
	public static final String FILE_RESOURCE = "file_resource";
	public static final String BACK_FOLDER_RESOURCE = "back_folder_resource";
	public static final String FILENAME = "filename";
	public static final String OLD_FILENAME = "old_filename";

	public static final int OPEN_FILE = 1;
	public static final int SAVE_FILE = 2;
	public static final int OPEN_FOLDER = 3;

	private RelativeLayout mContainer;
	private int mContainerMinHeight;
	private ListView mCurrent;
	private int mBasePathLength;
	private String currentPath;

	//private int mBackFolderResource;
	//private int mFolderResource;
	//private int mFileResource;
	private String fileType;

	private int mDialogType;
	private AppCompatEditText editText;

	private String oldPath = null;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		PrintConfig.initDefault( getAssets(), "fonts/material-icon-font.ttf" );
		// Log.v( LOG_TAG, "temp: " );
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize( size );

		mContainerMinHeight = 0;

		LinearLayout linear = new LinearLayout( this );
		linear.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );
		linear.setOrientation( LinearLayout.VERTICAL );
		setContentView( linear );

		mContainer = new RelativeLayout( this );
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, 0 );
		params.weight = 1.0f;
		mContainer.setLayoutParams( params );
		linear.addView( mContainer );

		// Bottom
		TypedValue typedValue = new TypedValue();
		final DisplayMetrics metrics = new android.util.DisplayMetrics();
		WindowManager wm = (WindowManager) getSystemService( Context.WINDOW_SERVICE );
		wm.getDefaultDisplay().getMetrics( metrics );

		// Divider
		View view = new View( this );
		view.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, 1 ) );
		linear.addView( view );

		getTheme().resolveAttribute( android.R.attr.dividerHorizontal, typedValue, true );
		view.setBackgroundResource( typedValue.resourceId );

		// Container
		getTheme().resolveAttribute( android.R.attr.buttonBarStyle, typedValue, true );
		LinearLayout btns = new LinearLayout( this, null, android.R.attr.buttonBarStyle );
		btns.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
		btns.setMeasureWithLargestChildEnabled( true );
		btns.setOrientation( LinearLayout.HORIZONTAL );
		btns.setPadding( 20, 0, 2, 0 );
		linear.addView( btns );

		// TextView
		editText = new AppCompatEditText( this );
		params = new LinearLayout.LayoutParams( 0, ViewGroup.LayoutParams.WRAP_CONTENT );
		params.weight = 1.0f;
		editText.setLayoutParams( params );
		editText.setMaxLines( 1 );
		getTheme().resolveAttribute( android.R.attr.textAppearanceListItemSmall, typedValue, true );
		//editText.setTextAppearance( typedValue.resourceId );
		btns.addView( editText );

		// Button
		getTheme().resolveAttribute( android.R.attr.buttonBarButtonStyle, typedValue, true );
		AppCompatButton btn = new AppCompatButton( this, null, android.R.attr.buttonBarButtonStyle );
		//btn.setTextAppearance( typedValue.resourceId );
		btn.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
		btns.addView( btn );

		Intent intent = getIntent();
		mDialogType = intent.getIntExtra( DIALOG_TYPE, OPEN_FILE );
		if( mDialogType == OPEN_FILE )
		{
			btn.setText( "Open" );
			editText.setEnabled( false );
		}
		else if( mDialogType == SAVE_FILE )
		{
			btn.setText( "Save" );
			editText.setText( intent.getStringExtra( DEFAULT_NAME ) );
		}
		else if( mDialogType == OPEN_FOLDER )
		{
			btn.setText( "Open folder" );
			editText.setVisibility( View.INVISIBLE );//.setText( intent.getStringExtra( DEFAULT_NAME ) );
		}

		btn.setOnClickListener( this );

		fileType = intent.getStringExtra( FILETYPE );
		//mBackFolderResource = intent.getIntExtra( BACK_FOLDER_RESOURCE, 0 );
		//mFolderResource = intent.getIntExtra( FOLDER_RESOURCE, 0 );
		//mFileResource = intent.getIntExtra( FILE_RESOURCE, 0 );

		mBasePathLength = ( getFilesDir().getAbsolutePath() +  "/home" ).length();
		// Log.v( LOG_TAG, getFilesDir().getAbsolutePath() );

		oldPath = intent.getStringExtra( PATH );
		if( oldPath != null )
		{
			File file = new File( oldPath );

			if( file.exists() )
				appendDirList( file.isDirectory() ? file : file.getParentFile() );
			else
				appendDirList( new File( getFilesDir(), "home" ) );
		}
		else
		{
			appendDirList( new File( getFilesDir(), "home" ) );
		}
	}

	@Override
	public void onClick( View v )
	{
		final String path = currentPath + "/" + editText.getText() + ( fileType != null ? fileType : "" );
		File file = new File( path );

		if( mDialogType == SAVE_FILE && file.exists() )
		{
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialog, int which )
				{
					switch( which )
					{
						case DialogInterface.BUTTON_POSITIVE:
							finishAct( path );
							break;

						case DialogInterface.BUTTON_NEGATIVE:
							//No button clicked
							break;
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder( this );
			builder.setMessage( "There is another file with this same name. Do you want to overwrite it?" )
					.setPositiveButton( "Yes", dialogClickListener )
					.setNegativeButton( "No", dialogClickListener ).show();
		}
		else
		{
			finishAct( path );
		}

	}

	private void finishAct( String path )
	{
		Intent intent = new Intent();
		intent.putExtra( OLD_FILENAME, oldPath );
		intent.putExtra( FILENAME, path );
		setResult( RESULT_OK, intent );
		finish();
	}

	private void selectFile( String name, File file )
	{
		if( fileType != null && name.endsWith( fileType ) )
			editText.setText( name.substring( 0, name.length() - fileType.length() ) );
		else
			editText.setText( name );
	}

	private void backDirList()
	{
		final ListView old = (ListView) mContainer.getChildAt( mContainer.getChildCount() - 1 );
		mCurrent = (ListView) mContainer.getChildAt( mContainer.getChildCount() - 2 );

		if( mCurrent == null )
		{
			File file = new File( currentPath );
			mCurrent = createDirList( file.getParentFile() );

			mContainer.addView( mCurrent );
			if( mContainer.getHeight() > mContainerMinHeight )
			{
				mContainerMinHeight = mContainer.getHeight();
				mContainer.setMinimumHeight( mContainerMinHeight );
			}
		}
		currentPath = mCurrent.getTag().toString();

		setTitle( "/" + mCurrent.getTag().toString().substring( mBasePathLength ) );
		float w = old.getWidth();

		mCurrent.setX( 0 );
		old.setX( w );

		TranslateAnimation animOld = new TranslateAnimation(
				TranslateAnimation.ABSOLUTE, -w,
				TranslateAnimation.ABSOLUTE, 0,
				TranslateAnimation.ABSOLUTE, 0,
				TranslateAnimation.ABSOLUTE, 0 );
		animOld.setDuration( 300 );
		animOld.setAnimationListener( new Animation.AnimationListener()
		{
			@Override
			public void onAnimationStart( Animation animation )
			{
			}

			@Override
			public void onAnimationEnd( Animation animation )
			{
				mContainer.removeView( old );
			}

			@Override
			public void onAnimationRepeat( Animation animation )
			{

			}
		} );
		old.startAnimation( animOld );

		TranslateAnimation animCur = new TranslateAnimation(
				TranslateAnimation.ABSOLUTE, -w,
				TranslateAnimation.ABSOLUTE, 0,
				TranslateAnimation.ABSOLUTE, 0,
				TranslateAnimation.ABSOLUTE, 0 );
		animCur.setDuration( 300 );
		mCurrent.startAnimation( animCur );
	}

	private ListView createDirList( File file )
	{
		String path = file.getAbsolutePath();

		ListView list = new ListView( this );
		list.setTag( path );

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		list.setLayoutParams( params );
		list.setX( 0.0f );
		list.setY( 0.0f );
		DirectoryAdapter adapter = new DirectoryAdapter( this, list, file, file.getAbsolutePath().length() > mBasePathLength );
		//
		list.bringToFront();

		// setTitle( (String) list.getTag() );
		String t = list.getTag().toString().substring( mBasePathLength );
		setTitle( t.length() > 0 ? t : "/" );

		return list;
	}


	private void appendDirList( File file )
	{
		Log.v( LOG_TAG, "file: " + file );
		currentPath = file.toString();

		ListView list = createDirList( file );
		list.bringToFront();

		ListView old = mCurrent;
		mCurrent = list;

		if( old != null )
		{
			float w = old.getWidth();

			old.setX( -w );
			//mContainer.removeView( old );

			TranslateAnimation animOld = new TranslateAnimation(
					TranslateAnimation.ABSOLUTE, w,
					TranslateAnimation.ABSOLUTE, 0,
					TranslateAnimation.ABSOLUTE, 0,
					TranslateAnimation.ABSOLUTE, 0 );
			animOld.setDuration( 300 );
			old.startAnimation( animOld );


			TranslateAnimation animCur = new TranslateAnimation(
					TranslateAnimation.ABSOLUTE, w,
					TranslateAnimation.ABSOLUTE, 0,
					TranslateAnimation.ABSOLUTE, 0,
					TranslateAnimation.ABSOLUTE, 0 );
			animCur.setDuration( 300 );
			mCurrent.startAnimation( animCur );
		}

		mContainer.addView( list );
		if( mContainer.getHeight() > mContainerMinHeight )
		{
			mContainer.setMinimumHeight( mContainerMinHeight );
		}

		//setTitle( (String) list.getTag() );
		String t = list.getTag().toString().substring( mBasePathLength );
		setTitle( t.length() > 0 ? t : "/" );
	}

	private void prependDirList( File file )
	{
		currentPath = file.toString();

		ListView list = createDirList( file );
		list.bringToFront();

		ListView old = mCurrent;
		mCurrent = list;

		if( old != null )
		{
			float w = old.getWidth();

			old.setX( -w );
			//mContainer.removeView( old );

			TranslateAnimation animOld = new TranslateAnimation(
					TranslateAnimation.ABSOLUTE, -w,
					TranslateAnimation.ABSOLUTE, 0,
					TranslateAnimation.ABSOLUTE, 0,
					TranslateAnimation.ABSOLUTE, 0 );
			animOld.setDuration( 300 );
			old.startAnimation( animOld );


			TranslateAnimation animCur = new TranslateAnimation(
					TranslateAnimation.ABSOLUTE, -w,
					TranslateAnimation.ABSOLUTE, 0,
					TranslateAnimation.ABSOLUTE, 0,
					TranslateAnimation.ABSOLUTE, 0 );
			animCur.setDuration( 300 );
			mCurrent.startAnimation( animCur );
		}

		mContainer.addView( list );
		if( mContainer.getHeight() > mContainerMinHeight )
		{
			mContainerMinHeight = mContainer.getHeight();
			mContainer.setMinimumHeight( mContainerMinHeight );
		}

		//setTitle( (String) list.getTag() );
		String t = list.getTag().toString().substring( mBasePathLength );
		setTitle( t.length() > 0 ? t : "/" );
	}

	//In an Activity
	private String[] loadFileList( File file, boolean withBackButton )
	{
		try
		{
			file.mkdirs();
		}
		catch( SecurityException e )
		{
			Log.e( "Load File", "unable to write on the sd card " + e.toString() );
		}

		if( file.exists() )
		{
			FilenameFilter filter = new FilenameFilter()
			{
				public boolean accept( File dir, String filename )
				{
					File sel = new File( dir, filename );
					return
							filename.charAt( 0 ) != '.' &&
									( fileType == null ||
							fileType.isEmpty() ? true : filename.toLowerCase().endsWith( fileType ) ||
							sel.isDirectory() );
				}
			};


			String[] result = file.list( filter );
			Arrays.sort( result );

			if( withBackButton )
			{
				List<String> list = new LinkedList<String>( Arrays.asList( result ) );
				list.add( 0, "Parent folder" );

				result = new String[list.size()];
				list.toArray( result );
			}

			return result;
		}
		else
		{
			return new String[0];
		}
	}

	class DirectoryAdapter extends BaseAdapter implements AdapterView.OnItemClickListener
	{
		private String[] mList;
		private FileBrowser mContext;
		private File mDir;

		//private int mBackFolderResource;
		//private int mFolderResource;
		//private int mFileResource;
		private ListView mListView;
		private boolean mWithBackButton;

		public DirectoryAdapter( FileBrowser context, ListView listView, File dir, int backFolderResource, int folderResource, int fileResource, boolean withBackButton )
		{
			mWithBackButton = withBackButton;

			mContext = context;

			mList = loadFileList( dir, mWithBackButton );
			Log.v( LOG_TAG, "mList: " + mList + " " + dir );
			mDir = dir;

			mListView = listView;

			mListView.setAdapter( this );
			mListView.setOnItemClickListener( this );
		}

		public DirectoryAdapter( FileBrowser context, ListView listView, File dir, boolean withBackButton )
		{
			mWithBackButton = withBackButton;

			mContext = context;

			mList = loadFileList( dir, mWithBackButton );
			Log.v( LOG_TAG, "mList: " + mList + " " + dir );
			mDir = dir;

			mListView = listView;

			mListView.setAdapter( this );
			mListView.setOnItemClickListener( this );
		}

		@Override
		public int getCount()
		{
			return mList.length;
		}

		@Override
		public String getItem( int position )
		{
			return mList[position];
		}

		@Override
		public long getItemId( int position )
		{
			return 0;
		}

		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			LinearLayout layout = (LinearLayout) convertView;
			if( layout == null )
			{
				TypedValue typedValue = new TypedValue();
				final DisplayMetrics metrics = new android.util.DisplayMetrics();
				WindowManager wm = (WindowManager) mContext.getSystemService( Context.WINDOW_SERVICE );
				wm.getDefaultDisplay().getMetrics( metrics );

				layout = new LinearLayout( mContext );
				layout.setLayoutParams( new AbsListView.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
				layout.setGravity( Gravity.CENTER_VERTICAL );
				layout.setOrientation( LinearLayout.HORIZONTAL );

				mContext.getTheme().resolveAttribute( android.R.attr.listPreferredItemPaddingStart, typedValue, true );
				int paddingStart = (int) typedValue.getDimension( metrics );

				mContext.getTheme().resolveAttribute( android.R.attr.listPreferredItemPaddingEnd, typedValue, true );
				int paddingEnd = (int) typedValue.getDimension( metrics );

				layout.setPadding( paddingStart, 0, paddingEnd, 0 );

				PrintView image = new PrintView( mContext );
				image.setId( android.R.id.icon );
				image.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
				image.setIconSizeDp( 24 );
				layout.addView( image );

				TextView text = new TextView( mContext );
				text.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
				text.setGravity( Gravity.CENTER_VERTICAL );

				mContext.getTheme().resolveAttribute( android.R.attr.textAppearanceListItemSmall, typedValue, true );
				//text.setTextAppearance( typedValue.resourceId );

				mContext.getTheme().resolveAttribute( android.R.attr.listPreferredItemPaddingStart, typedValue, true );
				text.setPadding( (int) ( typedValue.getDimension( metrics ) * 0.5f ), 0, 0, 0 );

				mContext.getTheme().resolveAttribute( android.R.attr.listPreferredItemHeightSmall, typedValue, true );
				text.setMinimumHeight( (int) typedValue.getDimension( metrics ) );
				text.setId( android.R.id.text1 );
				layout.addView( text );
			}

			String item = getItem( position );
			File file = new File( mDir + "/" + item );

			//ImageView icon = (ImageView) layout.findViewById( android.R.id.icon );
			PrintView icon = (PrintView) layout.findViewById( android.R.id.icon );
			;
			icon.setIconColor( 0xffffffff );
			if( mWithBackButton && position == 0 )
				icon.setIconText( getResources().getString( R.string.ic_arrow_back ) );
			else
				icon.setIconText( getResources().getString( file.isDirectory() ? R.string.ic_folder : R.string.ic_drive_file ) );
			//icon.setImageResource( file.isDirectory() ? mFolderResource : mFileResource );

			TextView text = (TextView) layout.findViewById( android.R.id.text1 );
			text.setText( item );

			return layout;
		}

		@Override
		public void onItemClick( AdapterView<?> parent, View view, int position, long id )
		{
			String item = getItem( position );
			File file = new File( mDir + "/" + item );

			if( position == 0 && mWithBackButton )
			{
				mContext.backDirList();
			}
			else if( file.isDirectory() )
			{
				mContext.appendDirList( file );
			}
			else if( mDialogType != OPEN_FOLDER )
			{
				mContext.selectFile( item, file );
				Log.v( "FILE IS CHOSEN", "ESCOLHIDO" );
			}
		}
	}
}


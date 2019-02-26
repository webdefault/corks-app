package com.webdefault.corks.editor;

/**
 * Created by orlandoleite on 2/21/18.
 */


import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;

import com.webdefault.corks.editor.tool.KeyboardTool;
import com.webdefault.lib.db.ResultColumns;
import com.webdefault.corks.LocalDatabase;
import com.webdefault.corks.MainActivity;
import com.webdefault.corks.R;
import com.webdefault.corks.editor.tool.FindTool;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by orlandoleite on 2/21/18.
 */
public class Editor extends AppCompatEditText
{
	private static final String LOG_TAG = "Editor";
	
	private boolean hasToUpdateLines = true;
	
	// Line numbers
	private int currentLineCount;
	private Paint paintLines;
	private Paint paintLinesBg;
	private float mFontSize;
	
	private HashMap<String, EditingFile> map = new HashMap<>();
	private EditingFile currentFile = null;
	
	private LocalDatabase db;
	
	private DisplayMetrics metrics;
	
	private AppBarLayout toolbarLayout;
	private ScrollViewExtended scrollView;
	private boolean scrollActionUp = false;
	
	private int tabWidth = 4;
	
	private FindTool findTool = null;
	private Handler findToolHandler = null;
	private long lastTextChange = 0;
	
	private KeyboardTool keyboardTool = null;
	
	public Editor( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		
		Typeface myFont = Typeface.createFromAsset( context.getAssets(), "fonts/inconsolata-regular.ttf" );
		setTypeface( myFont );
		
		metrics = getResources().getDisplayMetrics();
		
		// Setup drawings
		paintLines = new Paint();
		paintLines.setAntiAlias( true );
		paintLines.setTextAlign( Paint.Align.RIGHT );
		paintLines.setTypeface( myFont );
		paintLines.setColor( ContextCompat.getColor( context, R.color.editorDefaultText ) );
		
		paintLinesBg = new Paint();
		paintLinesBg.setStyle( Paint.Style.FILL );
		paintLinesBg.setColor( ContextCompat.getColor( context, R.color.editorDefaultLineMark ) );
		
		db = LocalDatabase.Companion.getInstance( context );
		
		tabWidth = db.intOption( "tab_char_size", 4 );
		
		setupGestures();
		
		InputFilter filterName = new InputFilter()
		{
			@Override
			public CharSequence filter( CharSequence source, int start, int end, Spanned dest, int dstart, int dend )
			{
				if( source == null ) return null;
				
				Log.v( LOG_TAG, "filter called" );
				if( keyboardTool != null && keyboardTool.isControlPressed() && source.length() == 1 && !currentFile.isUndoOrRedo() )
				{
					if( oldSelStart != oldSelEnd )
					{
						newSelStart = oldSelStart;
						newSelEnd = oldSelEnd;
						
						setSelection( oldSelStart, oldSelEnd );
					}
					
					keyboardTool.resetControls();
					char v = source.charAt( 0 );
					controlShortcut( v );
					
					Log.v( LOG_TAG, "dest: " + dstart + ", " + dend );
					if( dstart == dend )
						return "";
					else
						return dest.subSequence( dstart, dend );
				}
				else if( keyboardTool != null && keyboardTool.isCursorPressed() )
				{
					char v = source.charAt( 0 );
					switch( v )
					{
						case 'i':
						case 'I':
						case 'w':
						case 'W':
							dispatchKeyEvent( new KeyEvent( 0, 0, KeyEvent.ACTION_DOWN,
									KeyEvent.KEYCODE_DPAD_UP, 0 ) );
							dispatchKeyEvent( new KeyEvent( 0, 0, KeyEvent.ACTION_UP,
									KeyEvent.KEYCODE_DPAD_UP, 0 ) );
							break;
						
						case 'k':
						case 'K':
						case 's':
						case 'S':
							dispatchKeyEvent( new KeyEvent( 0, 0, KeyEvent.ACTION_DOWN,
									KeyEvent.KEYCODE_DPAD_DOWN, 0 ) );
							dispatchKeyEvent( new KeyEvent( 0, 0, KeyEvent.ACTION_UP,
									KeyEvent.KEYCODE_DPAD_DOWN, 0 ) );
							break;
						
						case 'j':
						case 'J':
						case 'a':
						case 'A':
							dispatchKeyEvent( new KeyEvent( 0, 0, KeyEvent.ACTION_DOWN,
									KeyEvent.KEYCODE_DPAD_LEFT, 0 ) );
							dispatchKeyEvent( new KeyEvent( 0, 0, KeyEvent.ACTION_UP,
									KeyEvent.KEYCODE_DPAD_LEFT, 0 ) );
							break;
						
						case 'l':
						case 'L':
						case 'd':
						case 'D':
							dispatchKeyEvent( new KeyEvent( 0, 0, KeyEvent.ACTION_DOWN,
									KeyEvent.KEYCODE_DPAD_RIGHT, 0 ) );
							dispatchKeyEvent( new KeyEvent( 0, 0, KeyEvent.ACTION_UP,
									KeyEvent.KEYCODE_DPAD_RIGHT, 0 ) );
							break;
					}
					
					return dest.subSequence( dstart, dend );
				}
				else
				{
					oldSelStart = newSelStart;
					oldSelEnd = newSelEnd;
					
					return source;
				}
			}
		};
		
		InputFilter[] oldInputFilter = getFilters();
		InputFilter[] newInputFilter = new InputFilter[oldInputFilter.length + 1];
		System.arraycopy( oldInputFilter, 0, newInputFilter, 0, oldInputFilter.length );
		newInputFilter[newInputFilter.length - 1] = filterName;
		setFilters( newInputFilter );
	}
	
	private void controlShortcut( char v )
	{
		switch( v )
		{
			case 'a':
			case 'A':
				selectAll();
				break;
			
			case 'c':
			case 'C':
				copyToClipboard();
				break;
			
			case 'x':
			case 'X':
				copyToClipboard();
				getText().delete( getSelectionStart(), getSelectionEnd() );
				//onTextContextMenuItem( android.R.id.cut );
				break;
			
			case 'v':
			case 'V':
				pasteClipboard();
				//onTextContextMenuItem( android.R.id.paste );
				break;
			
			case 's':
			case 'S':
				saveCurrentFile();
				break;
			
			case 'o':
			case 'O':
				( (MainActivity) getContext() ).openFile();
				break;
			
			case 'n':
			case 'N':
				( (MainActivity) getContext() ).addNewFile();
				break;
			
			case 'f':
			case 'F':
				( (MainActivity) getContext() ).getToolsSelector().showFindTool( false );
				break;
			
			case 'r':
			case 'R':
				( (MainActivity) getContext() ).getToolsSelector().showFindTool( true );
				break;
			
			case '/':
				// comment or uncomment;
				break;
			
			case 'w':
			case 'W':
			case 'q':
			case 'Q':
				( (MainActivity) getContext() ).closeCurrentFile();
				break;
			
			//case KeyEvent.KEYCODE_DPAD_UP:
			case 'i':
			case 'I':
				setSelection( 0 );
				break;
			
			//case KeyEvent.KEYCODE_DPAD_DOWN:
			case 'k':
			case 'K':
				setSelection( getText().length() );
				break;
			
			//case KeyEvent.KEYCODE_DPAD_LEFT:
			case 'j':
			case 'J':
				setSelection( getLayout().getLineStart( getCurrentCursorLine() ) );
				break;
			
			//case KeyEvent.KEYCODE_DPAD_RIGHT:
			case 'l':
			case 'L':
				int pos = getLayout().getLineEnd( getCurrentCursorLine() );
				// Log.v( LOG_TAG, "setSelection: " + pos + " " + getSelectionStart() );
				setSelection( pos > 1 ? pos - 1 : pos );
				break;
			
			case 'z':
				postDelayed( new Runnable()
				{
					@Override
					public void run()
					{
						currentFile.undo();
					}
				}, 10 );
				break;
			
			case 'Y':
			case 'Z':
				postDelayed( new Runnable()
				{
					@Override
					public void run()
					{
						currentFile.redo();
					}
				}, 10 );
				break;
		}
	}
	
	public int getCurrentCursorLine()
	{
		int selectionStart = Selection.getSelectionStart( getText() );
		;
		
		if( !( selectionStart == -1 ) )
		{
			return getLayout().getLineForOffset( selectionStart );
		}
		
		return -1;
	}
	
	public void setKeyboardTool( KeyboardTool tool )
	{
		keyboardTool = tool;
	}
	
	public void pause()
	{
		for( Map.Entry<String, EditingFile> entry : map.entrySet() )
		{
			EditingFile value = entry.getValue();
			value.pause();
		}
	}
	
	public void loadFile( ResultColumns file )
	{
		String path = file.get( "path" );
		
		Log.v( LOG_TAG, "preload: " + path + ( currentFile != null ? currentFile.getPath() : null ) );
		if( currentFile == null || path != currentFile.getPath() )
		{
			Log.v( LOG_TAG, "loadFile: " + path );
			
			removeTextChangedListener( textWatcher );
			
			if( currentFile != null )
			{
				currentFile.setText( removeTabs( getText().toString() ) );
				map.put( currentFile.getPath(), currentFile );
			}
			
			EditingFile editingFile = map.get( path );
			if( editingFile == null )
			{
				editingFile = EditingFile.getFile( getContext(), this, file );
				
				map.put( path, editingFile );
			}
			
			currentFile = editingFile;
			if( editingFile != null )
				applyText( editingFile.getText() );
			
			if( findTool != null ) findTool.updateSelection();
			
			lastTextChange = System.currentTimeMillis();
			
			addTextChangedListener( textWatcher );
		}
	}
	
	public void closeFile( ResultColumns fileInfo )
	{
		/*String path = fileInfo.get( "path" );
		//Log.v( LOG_TAG, "closeFile: " + fileInfo );
		EditingFile file = map.get( path );
		if( file != null)
		{
			Log.v( LOG_TAG, "close 1" );
			file.close();
		}
		else
		{
			Log.v( LOG_TAG, "close 2" );
			db.closeFile( fileInfo );
		}*/
		
		map.remove( fileInfo.get( "path" ) );
		currentFile = null;
		setText( "" );
	}
	
	public String tabChars()
	{
		StringBuilder v = new StringBuilder();
		int total = tabWidth;
		
		for( int i = 0; i < total; i++ )
		{
			v.append( "\t" );
		}
		
		return v.toString();
	}
	
	public String applyTabs( String value )
	{
		// Log.d( LOG_TAG, Log.getStackTraceString( new Exception() ) );
		return value.replaceAll( "\t", tabChars() );
	}
	
	private String removeTabs( String value )
	{
		return value.replaceAll( tabChars(), "\t" );
	}
	
	private void applyText( String value )
	{
		SpannableString text = new SpannableString( value );
		
		int strLength = value.length();
		//float tabWidth = getCharWidth() * tabWidth;
		
		int index = 0;
		while( index < strLength )
		{
			index = value.indexOf( "\t", index );
			if( index < 0 ) break;
			
			// text.setSpan( new TabWidthSpan( Float.valueOf( tabWidth ).intValue() ), index, index + 1, 0 );
			index++;
		}
		
		setText( text, BufferType.SPANNABLE );
		
		currentFile.applySyntaxHighlighting();
		
		//Log.v( LOG_TAG, "32: " + ( (char) 32 ) + ", 127: " + ( (char) 127 ) );
	}
	
	int oldSelStart, oldSelEnd, newSelStart, newSelEnd;
	
	@Override
	protected void onSelectionChanged( int selStart, int selEnd )
	{
		oldSelStart = newSelStart;
		oldSelEnd = newSelEnd;
		
		newSelStart = selStart;
		newSelEnd = selEnd;
		
		Log.v( LOG_TAG, "selStart " + selStart + " selEnd " + selEnd );
		
		if( !getText().toString().isEmpty() )
		{
			String value = getText().toString();
			char s = selStart < value.length() ? value.charAt( selStart ) : 0;
			
			int start = selStart;
			int end = selEnd;
			
			if( s == '\t' )
			{
				do
				{
					start--;
				}
				while( start >= 0 && value.charAt( start ) == '\t' );
				start++;
				
				start = start + Math.round( ( selStart - start ) / tabWidth ) * tabWidth;
			}
			
			if( selEnd == selStart )
			{
				end = start;
			}
			else
			{
				s = value.charAt( selEnd >= value.length() ? value.length() - 1 : selEnd );
				if( s == '\t' )
				{
					do
					{
						end++;
					}
					while( end < value.length() && value.charAt( end ) == '\t' );
					
					end = end + Math.round( ( selEnd - end ) / tabWidth ) * tabWidth;
					
					
				}
			}
			
			if( selStart != start || selEnd != end )
			{
				oldSelStart = newSelStart;
				oldSelEnd = newSelEnd;
				
				newSelStart = selStart;
				newSelEnd = selEnd;
				
				setSelection( start, end );
			}
		}
	}
	
	public void saveAs( String path )
	{
		if( currentFile != null ) currentFile.saveAs( path );
	}
	
	public void saveCurrentFile()
	{
		currentFile.setText( getText().toString() );
		currentFile.save();
	}
	
	public void saveCurrentFileWithEncode( String encode )
	{
		currentFile.saveWithEncode( encode );
	}
	
	public String getAbout()
	{
		if( currentFile != null )
			return currentFile.getAbout();
		else
			return "";
	}
	
	public String getSyntax()
	{
		if( currentFile != null )
			return currentFile.getSyntax();
		else
			return "TEXT/PLAIN";
	}
	
	public void setSyntax( String name )
	{
		if( currentFile != null )
			currentFile.setSyntax( name );
	}
	
	private ScaleGestureDetector mScaleDetector;
	private float mScaleFactor = 1.f;
	/*
	public int getRealTextLength( String value )
	{
		return applyTabs( value ).length();
	}
	*/
	private void setupGestures()
	{
		if( mScaleDetector == null )
		{
			mScaleDetector = new ScaleGestureDetector( getContext(), new ScaleGestureDetector.OnScaleGestureListener()
			{
				float fontSize;
				
				@Override
				public boolean onScaleBegin( ScaleGestureDetector scaleGestureDetector )
				{
					fontSize = mFontSize;
					return true;
				}
				
				@Override
				public boolean onScale( ScaleGestureDetector scaleGestureDetector )
				{
					mScaleFactor *= scaleGestureDetector.getScaleFactor();
					
					// Don't let the object get too small or too large.
					mScaleFactor = Math.max( 0.1f, Math.min( mScaleFactor, 10.0f ) );
					mFontSize = Math.max( 5f, Math.min( mScaleFactor * fontSize, 30.0f ) );
					
					Editor.this.setTextSize( mFontSize );
					// invalidate();
					return true;
				}
				
				@Override
				public void onScaleEnd( ScaleGestureDetector scaleGestureDetector )
				{
					mScaleFactor *= scaleGestureDetector.getScaleFactor();
					
					// Don't let the object get too small or too large.
					mScaleFactor = Math.max( 0.1f, Math.min( mScaleFactor, 10.0f ) );
					Log.v( LOG_TAG, "mScaleFactor " + scaleGestureDetector.getScaleFactor() );
					
					mFontSize = Math.max( 5f, Math.min( mScaleFactor * fontSize, 30.0f ) );
					db.setRealOption( "editor_font_size", (double) mFontSize );
					Editor.this.setTextSize( mFontSize );
					
					hasToUpdateLines = true;
				}
			} );
			
			final ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener()
			{
				@Override
				public void onGlobalLayout()
				{
					scrollView = (ScrollViewExtended) getParent();
					scrollView.setOnTouchListener( new OnTouchListener()
					{
						@Override
						public boolean onTouch( View view, MotionEvent motionEvent )
						{
							( (MainActivity) getContext() ).hideTool();
							
							switch( motionEvent.getAction() )
							{
								case MotionEvent.ACTION_UP:
									scrollView.startScrollerTask();
									break;
							}
							
							scrollActionUp = false;
							if( motionEvent.getPointerCount() == 2 )
							{
								mScaleDetector.onTouchEvent( motionEvent );
								return true;
							}
							else
							{
								return false;
							}
						}
					} );
					
					mFontSize = (float) db.realOption( "editor_font_size", 20.0 );
					setTextSize( mFontSize );
					setMinHeight( scrollView.getMeasuredHeight() );
					
					getViewTreeObserver().removeOnGlobalLayoutListener( this );
					
					toolbarLayout = ( (Activity) getContext() ).getWindow().getDecorView().findViewById( R.id.toolbar_actionbar );
					
					final ViewTreeObserver.OnScrollChangedListener scrollChangedListener = new ViewTreeObserver.OnScrollChangedListener()
					{
						int lastScrollY = 0;
						
						@Override
						public void onScrollChanged()
						{
							int scrollY = scrollView.getScrollY(); // For ScrollView
							// int scrollX = scrollView.getScrollX(); // For HorizontalScrollView
							// DO SOMETHING WITH THE SCROLL COORDINATES
							// Log.v( LOG_TAG, "scroll x: " + scrollX + " y: " + scrollY );
							int diff = scrollY - lastScrollY;
							float nextPos = Math.min( Math.max( toolbarLayout.getY() - diff, -toolbarLayout.getMeasuredHeight() ), 0 );
							toolbarLayout.setY( (float) nextPos );
				
					/*
					int diff = scrollY - lastScrollY;
					float nextPos = Math.min(Math.max( toolbar.getY() - diff, 0 ), -toolbar.getMeasuredHeight() );
					toolbar.setY( (float) nextPos );
					*/
							
							lastScrollY = scrollY;
						}
					};
					
					scrollView.getViewTreeObserver().addOnScrollChangedListener( scrollChangedListener );
					
					ScrollViewExtended.OnScrollStoppedListener scrollStoppedListener = new ScrollViewExtended.OnScrollStoppedListener()
					{
						
						public void onScrollStopped()
						{
							
							Log.i( LOG_TAG, "stopped" );
							
							scrollActionUp = true;
							
							float val = -toolbarLayout.getY();
							
							int measuredHeight = toolbarLayout.getMeasuredHeight();
							if( val != 0 && val != measuredHeight )
							{
								int diff = 0;
								diff = (int) ( val > measuredHeight * 0.5f ? val - measuredHeight : val );
								
								ObjectAnimator objectAnimator = ObjectAnimator.ofInt(
										scrollView,
										"scrollY",
										scrollView.getScrollY(),
										scrollView.getScrollY() - diff )
										.setDuration( 200 );
								objectAnimator.start();
								
								objectAnimator = ObjectAnimator.ofFloat(
										toolbarLayout,
										"Y",
										toolbarLayout.getY(),
										toolbarLayout.getY() + diff ).setDuration( 200 );
								objectAnimator.start();
							}
						}
					};
					
					scrollView.setOnScrollStoppedListener( scrollStoppedListener );
				}
			};
			
			getViewTreeObserver().addOnGlobalLayoutListener( layoutListener );
		}
	}
	
	@Override
	public boolean onTouchEvent( MotionEvent event )
	{
		( (MainActivity) getContext() ).hideTool();
		
		if( event.getPointerCount() == 2 )
		{
			mScaleDetector.onTouchEvent( event );
			
			event.setAction( MotionEvent.ACTION_CANCEL );
			return super.onTouchEvent( event );
		}
		else if( !mScaleDetector.isInProgress() )
		{
			return super.onTouchEvent( event );
		}
		else
			return false;
	}
	
	@Override
	protected void onDraw( Canvas canvas )
	{
		// Draw lines
		paintLines.setTextSize( mFontSize * metrics.density );
		
		boolean[] lineCounter = null;
		int totalLines = 1;
		int linesPadding = 2;
		int offset = 4;
		
		int lineCount = getLineCount();
		
		if( currentFile != null )
		{
			if( currentLineCount != lineCount || currentFile != null )
			{
				currentFile.setText( getText().toString() );
				hasToUpdateLines = true;
			}
			
			if( hasToUpdateLines )
			{
				currentFile.setupLayout( getLayout() );
				hasToUpdateLines = false;
				
				currentLineCount = getLineCount();
			}
			
			lineCounter = currentFile.getLineCounter( getLayout() );
			totalLines = currentFile.getTotalLines();
			linesPadding = getPaddingLineStart();
			offset = (int) Math.ceil( paintLines.measureText( "." ) );
			int paddingStart = getPaddingStart();
			
			if( linesPadding != paddingStart )
			{
				setPadding( linesPadding, getPaddingTop(), getPaddingRight(), getPaddingBottom() );
				hasToUpdateLines = true;
			}
		}
		
		linesPadding -= 2;
		int lineHeight = getLineHeight();
		int baseline = getBaseline();
		int lcount = 0;
		
		if( lineCounter != null && lineCount > lineCounter.length ) lineCount = lineCounter.length;
		
		canvas.drawRect( new Rect( 0, 0, (int) ( linesPadding - ( offset * 0.5f ) ), getMeasuredHeight() ), paintLinesBg );
		
		for( int i = 0; i < lineCount; i++ )
		{
			if( lineCounter != null && lineCounter[i] )
			{
				canvas.drawText( "" + ( ++lcount ), linesPadding - offset, baseline, paintLines );
			}
			
			baseline += lineHeight;
		}
		
		if( findTool != null ) findTool.onEditorDraw( canvas );
		
		super.onDraw( canvas );
	}
	
	public int getPaddingLineStart()
	{
		return (int) Math.ceil( paintLines.measureText( " " + currentFile.getTotalLines() + " " ) );
	}
	
	public float getCharWidth()
	{
		return paintLines.measureText( "m" );
	}
	
	public EditingFile getCurrentFile()
	{
		return currentFile;
	}
	
	public void setFindTool( final FindTool findTool )
	{
		this.findTool = findTool;
		
		if( findToolHandler == null )
		{
			findToolHandler = new Handler( getContext().getMainLooper() );
			Runnable runnable = new Runnable()
			{
				
				@Override
				public void run()
				{
					try
					{
						findToolUpdate();
					}
					catch( Exception e )
					{
						// TODO: handle exception
					}
					finally
					{
						//also call the same runnable to call it at regular interval
						findToolHandler.postDelayed( this, 1000 );
					}
				}
			};
			
			findToolHandler.postDelayed( runnable, 1000 );
		}
	}
	
	private void findToolUpdate()
	{
		if( findTool != null && lastTextChange != 0 && System.currentTimeMillis() > lastTextChange + 1000 )
		{
			lastTextChange = 0;
			findTool.updateSelection();
		}
	}
	
	@Override
	public boolean onTextContextMenuItem( int id )
	{
		// Do your thing:
		boolean consumed = super.onTextContextMenuItem( id );
		// React:
		switch( id )
		{
			case android.R.id.copy:
			case android.R.id.cut:
				onTextCopyOrCut();
				break;
		}
		
		return consumed;
	}
	
	public void copyToClipboard()
	{
		ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService( Context.CLIPBOARD_SERVICE );
		Log.v( LOG_TAG, "sel: " + oldSelStart + oldSelEnd );
		String selectedText = getText().toString().substring( oldSelStart, oldSelEnd );
		Log.v( LOG_TAG, "copy: " + selectedText );
		ClipData clip = ClipData.newPlainText( "Copy", removeTabs( selectedText ) );
		clipboard.setPrimaryClip( clip );
	}
	
	public void pasteClipboard()
	{
		getText().delete( oldSelStart, oldSelEnd );
		
		ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService( Context.CLIPBOARD_SERVICE );
		ClipData clip = clipboard.getPrimaryClip();
		
		String data = "";
		if( clip != null && clip.getItemCount() != 0 )
		{
			data = clip.getItemAt( 0 ).getText().toString();
			getText().append( data );
		}
	}
	
	/**
	 * Text was copied from this EditText.
	 */
	public void onTextCopyOrCut()
	{
		ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService( Context.CLIPBOARD_SERVICE );
		ClipData clip = clipboard.getPrimaryClip();
		
		String data = "";
		if( clip != null && clip.getItemCount() != 0 )
		{
			data = clip.getItemAt( 0 ).getText().toString();
			
			Log.v( LOG_TAG, "data:" + data + ", removeTabs:" + removeTabs( data ) );
			clip = ClipData.newPlainText( clip.getDescription().getLabel(), removeTabs( data ) );
			clipboard.setPrimaryClip( clip );
		}
		
		// Toast.makeText( getContext(), "Copy! " + data, Toast.LENGTH_SHORT ).show();
	}
	
	private TextWatcher textWatcher = new TextWatcher()
	{
		/**
		 * The text that will be removed by the change event.
		 */
		private CharSequence mBeforeChange;
		
		/**
		 * The text that was inserted by the change event.
		 */
		private CharSequence mAfterChange;
		
		private String removed;
		
		@Override
		public void afterTextChanged( Editable s )
		{
			lastTextChange = System.currentTimeMillis();
		}
		
		@Override
		public void beforeTextChanged( CharSequence s, int start, int count, int after )
		{
			removed = count > 0 ? String.valueOf( s.subSequence( start, start + count ) ) : null;
			mBeforeChange = s.subSequence( start, start + count );
			mAfterChange = null;
		}
		
		@Override
		public void onTextChanged( CharSequence s, int start, int before, int count )
		{
			if( removed != null )
			{
				int length = 0;
				if( removed.charAt( length ) == '\t' )
				{
					do
					{
						length++;
					}
					while( length < removed.length() && removed.charAt( length ) == '	' );
					
					if( tabWidth >= length )
					{
						removed = null;
						
						int p = tabWidth - length;
						
						mBeforeChange = String.valueOf( s.subSequence( start - p, start ) ) + mBeforeChange;
						
						removeTextChangedListener( this );
						getText().delete( start - p, start );
						addTextChangedListener( this );
						
						start -= p;
					}
					
					mAfterChange = "";
				}
			}
			else if( count > 0 )
			{
				String value = applyTabs( String.valueOf( s.subSequence( start, start + count ) ) );
				removeTextChangedListener( this );
				getText().replace( start, start + count, value );
				addTextChangedListener( this );
				
				mAfterChange = value;
			}
			
			if( currentFile != null )
			{
				currentFile.textChanged();
				
				currentFile.addEdition(
						start,
						mBeforeChange != null ? mBeforeChange.toString() : "",
						mAfterChange != null ? mAfterChange.toString() : "" );
			}
		}
	};
	
	public void replaceWithTextWatcher( int start, int end, CharSequence text )
	{
		removeTextChangedListener( textWatcher );
		getText().replace( start, end, text );
		addTextChangedListener( textWatcher );
	}
}
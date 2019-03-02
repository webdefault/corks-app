package com.webdefault.corks.term;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import com.offsec.nhterm.CheckRoot;
import com.offsec.nhterm.GenericTermSession;
import com.offsec.nhterm.ShellTermSession;
import com.offsec.nhterm.ShellType;
import com.offsec.nhterm.TermDebug;
import com.offsec.nhterm.TermPreferences;
import com.offsec.nhterm.TermView;
import com.offsec.nhterm.TermViewFlipper;
import com.offsec.nhterm.compat.AndroidCompat;
import com.offsec.nhterm.emulatorview.EmulatorView;
import com.offsec.nhterm.emulatorview.TermSession;
import com.offsec.nhterm.emulatorview.UpdateCallback;
import com.offsec.nhterm.emulatorview.compat.ClipboardManagerCompat;
import com.offsec.nhterm.emulatorview.compat.ClipboardManagerCompatFactory;
import com.offsec.nhterm.emulatorview.compat.KeycodeConstants;
import com.offsec.nhterm.util.SessionList;
import com.offsec.nhterm.util.TermSettings;
import com.webdefault.corks.Dialogs;
import com.webdefault.corks.R;
import com.webdefault.corks.editor.adapter.FileLineAdapter;
import com.webdefault.corks.term.adapter.TermLineAdapter;
import com.webdefault.lib.db.ResultLines;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TermFragment extends Fragment implements UpdateCallback
{
    private static final String LOG_TAG = "TermFragment";

    private final static int SELECT_TEXT_ID = 200;
    private final static int COPY_ALL_ID = 201;
    private final static int PASTE_ID = 202;
    private final static int SEND_CONTROL_KEY_ID = 203;
    private final static int SEND_FN_KEY_ID = 204;

    private Intent TSIntent;

    private TermTool tool;
    private boolean isAnimating;

    private int oldLength;
    private int selectedTab;

    private TermLineAdapter termLineAdapter;

    private static final int DO_CREATE_NEW_WINDOW = 0;
    private static final int POPULATE_VIEW_FLIPPER = 1;

    private static final String ACTION_PATH_BROADCAST = "com.webdefault.corks.broadcast.APPEND_TO_PATH";
    private static final String ACTION_PATH_PREPEND_BROADCAST = "com.webdefault.corks.broadcast.PREPEND_TO_PATH";
    private static final String PERMISSION_PATH_BROADCAST = "com.webdefault.corks.permission.APPEND_TO_PATH";
    private static final String PERMISSION_PATH_PREPEND_BROADCAST = "com.webdefault.corks.permission.PREPEND_TO_PATH";
    private int mPendingPathBroadcasts = 0;

    private int onResumeSelectWindow = -1;

    private boolean mAlreadyStarted = false;
    private boolean mStopServiceOnFinish = false;

    private boolean mUseKeyboardShortcuts;

    private RecyclerView navOpenTerms;

    private BroadcastReceiver mPathReceiver = new BroadcastReceiver()
    {
        public void onReceive( Context context, Intent intent )
        {
            String path = makePathFromBundle( getResultExtras( false ) );
            if( intent.getAction().equals( ACTION_PATH_PREPEND_BROADCAST ) )
            {
                mSettings.setPrependPath( path );
            }
            else
            {
                mSettings.setAppendPath( path );
            }
            mPendingPathBroadcasts--;

            if( mPendingPathBroadcasts <= 0 && mTermService != null )
            {
                populateViewFlipper();
                populateWindowList();
            }
        }
    };

    private String makePathFromBundle( Bundle extras )
    {
        if( extras == null || extras.size() == 0 )
        {
            return "";
        }

        String[] keys = new String[extras.size()];
        keys = extras.keySet().toArray( keys );
        Collator collator = Collator.getInstance( Locale.US );
        Arrays.sort( keys, collator );

        StringBuilder path = new StringBuilder();
        for( String key : keys )
        {
            String dir = extras.getString( key );
            if( dir != null && !dir.equals( "" ) )
            {
                path.append( dir );
                path.append( ":" );
            }
        }

        return path.substring( 0, path.length() - 1 );
    }

    private SessionList mTermSessions;

    private TermSettings mSettings;

    private TermService mTermService;
    private TermViewFlipper mViewFlipper;

    private ServiceConnection mTSConnection = new ServiceConnection()
    {
        public void onServiceConnected( ComponentName className, IBinder service )
        {
            Log.i( TermDebug.LOG_TAG, "Bound to TermService" );
            TermService.TSBinder binder = (TermService.TSBinder) service;
            mTermService = binder.getService();
            if( mPendingPathBroadcasts <= 0 )
            {
                populateViewFlipper();
                populateWindowList();
            }
        }

        public void onServiceDisconnected( ComponentName arg0 )
        {
            mTermService = null;
            Log.d( "onServiceDisconnected", "onServiceDisconnected" );
        }
    };

    private boolean mHaveFullHwKeyboard = false;

    private class EmulatorViewGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        private EmulatorView view;

        public EmulatorViewGestureListener( EmulatorView view )
        {
            this.view = view;
        }

        @Override
        public boolean onSingleTapUp( MotionEvent e )
        {
            // Let the EmulatorView handle taps if mouse tracking is active
            if( view.isMouseTrackingActive() ) return false;

            //Check for link at tap location
            String link = view.getURLat( e.getX(), e.getY() );
            if( link != null )
                execURL( link );
            else
                doUIToggle( (int) e.getX(), (int) e.getY(), view.getVisibleWidth(), view.getVisibleHeight() );
            return true;
        }
    }

    private void execURL( String link )
    {
        Uri webLink = Uri.parse( link );
        Intent openLink = new Intent( Intent.ACTION_VIEW, webLink );
        PackageManager pm = getActivity().getPackageManager();
        List<ResolveInfo> handlers = pm.queryIntentActivities( openLink, 0 );
        if( handlers.size() > 0 )
            startActivity( openLink );
    }

    private int mActionBarMode = TermSettings.ACTION_BAR_MODE_NONE;

    private void doUIToggle( int x, int y, int width, int height )
    {
        switch( mActionBarMode )
        {
            case TermSettings.ACTION_BAR_MODE_NONE:
                if( AndroidCompat.SDK >= 11 && ( mHaveFullHwKeyboard || y < height / 2 ) )
                {
                    //getActivity().openOptionsMenu();
                    return;
                }
                else
                {
                    doToggleSoftKeyboard();
                }
                break;
            case TermSettings.ACTION_BAR_MODE_ALWAYS_VISIBLE:
                if( !mHaveFullHwKeyboard )
                {
                    doToggleSoftKeyboard();
                }
                break;
            case TermSettings.ACTION_BAR_MODE_HIDES:
                if( mHaveFullHwKeyboard || y < height / 2 )
                {
                    //doToggleActionBar();
                    return;
                }
                else
                {
                    doToggleSoftKeyboard();
                }
                break;
        }
        getCurrentEmulatorView().requestFocus();
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v,
                                     ContextMenu.ContextMenuInfo menuInfo
    )
    {
        // super.onCreateContextMenu( menu, v, menuInfo );
        menu.setHeaderTitle( com.offsec.nhterm.R.string.edit_text );
        menu.add( 0, SELECT_TEXT_ID, 0, com.offsec.nhterm.R.string.select_text );
        menu.add( 0, COPY_ALL_ID, 0, com.offsec.nhterm.R.string.copy_all );
        menu.add( 0, PASTE_ID, 0, com.offsec.nhterm.R.string.paste );
        menu.add( 0, SEND_CONTROL_KEY_ID, 0, com.offsec.nhterm.R.string.send_control_key );
        menu.add( 0, SEND_FN_KEY_ID, 0, com.offsec.nhterm.R.string.send_fn_key );
        if( !canPaste() )
        {
            menu.getItem( PASTE_ID ).setEnabled( false );
        }
    }

    @Override
    public boolean onContextItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case SELECT_TEXT_ID:
                getCurrentEmulatorView().toggleSelectingText();
                return true;
            case COPY_ALL_ID:
                doCopyAll();
                return true;
            case PASTE_ID:
                doPaste();
                return true;
            case SEND_CONTROL_KEY_ID:
                doSendControlKey();
                return true;
            case SEND_FN_KEY_ID:
                doSendFnKey();
                return true;
            default:
                return super.onContextItemSelected( item );
        }
    }

    private boolean canPaste()
    {
        ClipboardManagerCompat clip = ClipboardManagerCompatFactory
                .getManager( getActivity().getApplicationContext() );
        return clip.hasText();
    }

    private void doCopyAll()
    {
        ClipboardManagerCompat clip = ClipboardManagerCompatFactory
                .getManager( getActivity().getApplicationContext() );
        clip.setText( getCurrentTermSession().getTranscriptText().trim() );
    }

    private void doPaste()
    {
        if( !canPaste() )
        {
            return;
        }
        ClipboardManagerCompat clip = ClipboardManagerCompatFactory
                .getManager( getActivity().getApplicationContext() );
        CharSequence paste = clip.getText();
        getCurrentTermSession().write( paste.toString() );
    }

    private void doSendControlKey()
    {
        getCurrentEmulatorView().sendControlKey();
    }

    private void doSendFnKey()
    {
        getCurrentEmulatorView().sendFnKey();
    }

    /**
     * Intercepts keys before the view/terminal gets it.
     */
    private View.OnKeyListener mKeyListener = new View.OnKeyListener()
    {
        public boolean onKey( View v, int keyCode, KeyEvent event )
        {
            // Log.v( LOG_TAG, "keyCode: " + keyCode + "ev: " + event );
            return backkeyInterceptor( keyCode, event ) || keyboardShortcuts( keyCode, event );
        }

        /**
         * Keyboard shortcuts (tab management, paste)
         */
        private boolean keyboardShortcuts( int keyCode, KeyEvent event )
        {
            boolean isCursorPressed = tool.isCursorSelected();

            if( isCursorPressed )
            {
                Log.v( LOG_TAG, "isCursorPressed" );
                int key = 0;
                switch( keyCode )
                {
                    case KeyEvent.KEYCODE_J:
                    case KeyEvent.KEYCODE_A:
                        key = KeyEvent.KEYCODE_DPAD_LEFT;
                        break;

                    case KeyEvent.KEYCODE_K:
                    case KeyEvent.KEYCODE_S:
                        key = KeyEvent.KEYCODE_DPAD_DOWN;
                        break;

                    case KeyEvent.KEYCODE_L:
                    case KeyEvent.KEYCODE_D:
                        key = KeyEvent.KEYCODE_DPAD_RIGHT;
                        break;

                    case KeyEvent.KEYCODE_I:
                    case KeyEvent.KEYCODE_W:
                        key = KeyEvent.KEYCODE_DPAD_UP;
                        break;
                }

                if( key == 0 )
                    return false;
                else
                {
                    KeyEvent e = new KeyEvent( event.getAction(), key );
                    getActivity().dispatchKeyEvent( e );

                    return true;
                }
            }

            if( event.getAction() != KeyEvent.ACTION_DOWN )
            {
                return false;
            }
            if( !mUseKeyboardShortcuts )
            {
                return false;
            }
            boolean isCtrlPressed = ( event.getMetaState() & KeycodeConstants.META_CTRL_ON ) != 0;
            boolean isShiftPressed = ( event.getMetaState() & KeycodeConstants.META_SHIFT_ON ) != 0;


            if( keyCode == KeycodeConstants.KEYCODE_TAB && isCtrlPressed )
            {
                if( isShiftPressed )
                {
                    mViewFlipper.showPrevious();
                }
                else
                {
                    mViewFlipper.showNext();
                }

                return true;
            }
            else if( keyCode == KeycodeConstants.KEYCODE_N && isCtrlPressed && isShiftPressed )
            {
                //doCreateNewWindow();

                return true;
            }
            else if( keyCode == KeycodeConstants.KEYCODE_V && isCtrlPressed && isShiftPressed )
            {
                //doPaste();

                return true;
            }
            else
            {
                return false;
            }
        }

        /**
         * Make sure the back button always leaves the application.
         */
        private boolean backkeyInterceptor( int keyCode, KeyEvent event )
        {
            /*if( keyCode == KeyEvent.KEYCODE_BACK && mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES && mActionBar != null && mActionBar.isShowing() )
            {
                /* We need to intercept the key event before the view sees it,
                   otherwise the view will handle it before we get it * /
                onKeyUp( keyCode, event );
                return true;
            }
            else
            {*/
            return false;
            //}
        }
    };

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        ViewGroup root = (ViewGroup) inflater.inflate( R.layout.console_fragment, container, false );

        tool = (TermTool) root.findViewById( R.id.console_tool );
        tool.init( this );

        final SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences( getActivity() );
        mSettings = new TermSettings( getResources(), mPrefs );
        //mPrefs.registerOnSharedPreferenceChangeListener( this);

        Intent broadcast = new Intent( ACTION_PATH_BROADCAST );
        if( AndroidCompat.SDK >= 12 )
        {
            broadcast.addFlags( Intent.FLAG_INCLUDE_STOPPED_PACKAGES );
        }
        mPendingPathBroadcasts++;
        getActivity().sendOrderedBroadcast( broadcast, PERMISSION_PATH_BROADCAST, mPathReceiver, null, Activity.RESULT_OK, null, null );

        broadcast = new Intent( broadcast );
        broadcast.setAction( ACTION_PATH_PREPEND_BROADCAST );
        mPendingPathBroadcasts++;
        getActivity().sendOrderedBroadcast( broadcast, PERMISSION_PATH_PREPEND_BROADCAST, mPathReceiver, null, Activity.RESULT_OK, null, null );

        TSIntent = new Intent( getContext(), TermService.class );
        getActivity().startService( TSIntent );

        mViewFlipper = (TermViewFlipper) root.findViewById( R.id.view_flipper );
        //setFunctionKeyListener();

        /*PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TermDebug.LOG_TAG);
        WifiManager wm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if ( AndroidCompat.SDK >= 12) {
            wifiLockMode = WIFI_MODE_FULL_HIGH_PERF;
        }
        mWifiLock = wm.createWifiLock(wifiLockMode, TermDebug.LOG_TAG);*/

        /*ActionBarCompat actionBar = ActivityCompat.getActionBar(this);
        if (actionBar != null) {
            mActionBar = actionBar;
            actionBar.setNavigationMode(ActionBarCompat.NAVIGATION_MODE_LIST);
            actionBar.setDisplayOptions(0, ActionBarCompat.DISPLAY_SHOW_TITLE);
            if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES) {
                actionBar.hide();
            }
        }

        mHaveFullHwKeyboard = checkHaveFullHwKeyboard(getResources().getConfiguration());

        if (mFunctionBar == -1) mFunctionBar = mSettings.showFunctionBar() ? 1 : 0;
        if (mFunctionBar == 0) setFunctionBar(mFunctionBar);*/

        //updatePrefs();
        mAlreadyStarted = true;

        setupOpenFilesSelector( root );
        setFunctionBarSize( root );

        return root;
    }

    public void doToggleSoftKeyboard()
    {
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService( Context.INPUT_METHOD_SERVICE );
        imm.toggleSoftInput( InputMethodManager.SHOW_FORCED, 0 );
    }

    public void sendKeyStrings( String str, boolean esc )
    {
        TermSession session = getCurrentTermSession();
        if( session != null )
        {
            if( esc )
            {
                KeyEvent event = new KeyEvent( KeyEvent.ACTION_DOWN, KeycodeConstants.KEYCODE_ESCAPE );
                getActivity().dispatchKeyEvent( event );
            }
            session.write( str );
        }
    }

    private TermSession getCurrentTermSession()
    {
        SessionList sessions = mTermSessions;
        if( sessions == null )
        {
            return null;
        }
        else
        {
            return sessions.get( mViewFlipper.getDisplayedChild() );
        }
    }

    public EmulatorView getCurrentEmulatorView()
    {
        return (EmulatorView) mViewFlipper.getCurrentView();
    }

    private void setFunctionKeyListener( ViewGroup root )
    {
        /*root.findViewById( com.offsec.nhterm.R.id.button_esc ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_ctrl ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_alt ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_tab ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_up ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_down ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_left ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_right ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_backspace ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_enter ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_i ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_colon ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_slash ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_equal ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_asterisk ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_pipe ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_minus ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_vim_paste ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_vim_yank ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_softkeyboard ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_menu ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_menu_hide ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_menu_plus ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_menu_minus ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_menu_x ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_menu_user ).setOnClickListener( this );
        findViewById( com.offsec.nhterm.R.id.button_menu_quit ).setOnClickListener( this );*/
    }

    private void setupOpenFilesSelector( ViewGroup root )
    {
        LinearLayoutManager layoutManager = new LinearLayoutManager( getContext(), LinearLayoutManager.HORIZONTAL, false );
        navOpenTerms = root.findViewById( R.id.nav_open_terms );
        navOpenTerms.setLayoutManager( layoutManager );

        // navOpenTerms.setAdapter( termLineAdapter );

        // Configurando um dividr entre linhas, para uma melhor visualização.
        navOpenTerms.addItemDecoration( new DividerItemDecoration( getContext(), DividerItemDecoration.HORIZONTAL ) );
    }

    @Override
    public void onStart()
    {
        super.onStart();

        if( !getActivity().bindService( TSIntent, mTSConnection, Activity.BIND_AUTO_CREATE ) )
        {
            throw new IllegalStateException( "Failed to bind to TermService!" );
        }
    }

    @Override
    public void onStop()
    {
        mViewFlipper.onPause();
        if( mTermSessions != null )
        {
            mTermSessions.removeCallback( this );

            if( termLineAdapter != null )
            {
                mTermSessions.removeCallback( termLineAdapter );
                mTermSessions.removeTitleChangedListener( termLineAdapter );
                mViewFlipper.removeCallback( termLineAdapter );
            }
        }

        mViewFlipper.removeAllViews();

        getActivity().unbindService( mTSConnection );

        super.onStop();
    }

    private void updatePrefs()
    {
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics( metrics );

        mViewFlipper.updatePrefs( mSettings );

        for( View v : mViewFlipper )
        {
            ( (EmulatorView) v ).setDensity( metrics );
            ( (TermView) v ).updatePrefs( mSettings );
        }

        /*mUseKeyboardShortcuts = mSettings.getUseKeyboardShortcutsFlag();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        setFunctionKeyVisibility();
        mViewFlipper.updatePrefs(mSettings);

        for (View v : mViewFlipper) {
            ((EmulatorView) v).setDensity(metrics);
            ((TermView) v).updatePrefs(mSettings);
        }

        if (mTermSessions != null) {
            for (TermSession session : mTermSessions) {
                ((GenericTermSession) session).updatePrefs(mSettings);
            }
        }

        {
            Window win = getWindow();
            WindowManager.LayoutParams params = win.getAttributes();
            final int FULLSCREEN = WindowManager.LayoutParams.FLAG_FULLSCREEN;
            int desiredFlag = mSettings.showStatusBar() ? 0 : FULLSCREEN;
            if (desiredFlag != (params.flags & FULLSCREEN) || (AndroidCompat.SDK >= 11 && mActionBarMode != mSettings.actionBarMode())) {
                if (mAlreadyStarted) {
                    // Can't switch to/from fullscreen after
                    // starting the activity.
                    restart();
                } else {
                    win.setFlags(desiredFlag, FULLSCREEN);
                    if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES) {
                        if (mActionBar != null) {
                            mActionBar.hide();
                        }
                    }
                }
            }
        }

        int orientation = mSettings.getScreenOrientation();
        int o = 0;
        if (orientation == 0) {
            o = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        } else if (orientation == 1) {
            o = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else if (orientation == 2) {
            o = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else {
            /* Shouldn't be happened. * /
        }
        setRequestedOrientation(o);*/
    }

    private void setFunctionBarSize( ViewGroup root )
    {
        int size = root.findViewById( R.id.console_tool ).getMeasuredHeight();
        if( mViewFlipper != null ) mViewFlipper.setFunctionBarSize( size == 0 ? 100 : size );
    }

    private TermView createEmulatorView( TermSession session )
    {
        DisplayMetrics metrics = new DisplayMetrics();

        getActivity().getWindowManager().getDefaultDisplay().getMetrics( metrics );
        TermView emulatorView = new TermView( getContext(), session, metrics );

        emulatorView.setExtGestureListener( new TermFragment.EmulatorViewGestureListener( emulatorView ) );
        emulatorView.setOnKeyListener( mKeyListener );
        registerForContextMenu( emulatorView );

        return emulatorView;
    }

    public void selectSession( TermSession session, int position )
    {
        Log.d( "mWinListItemSelected", String.valueOf( mViewFlipper.getDisplayedChild() ) );
        /*if(alertDialog != null){
            alertDialog.dismiss();
            alertDialog = null;
        }*/
        int oldPosition = mViewFlipper.getDisplayedChild();
        if( position != oldPosition )
        {
            if( position >= mViewFlipper.getChildCount() )
            {
                mViewFlipper.addView( createEmulatorView( mTermSessions.get( position ) ) );
                Log.d( "addView cc", String.valueOf( mTermSessions.get( position ) ) );
            }
            //selectedTab = position;
            oldLength = mTermSessions.size();
            mTermSessions.setOldSize( oldLength );
            mTermSessions.setSelectedSession( selectedTab );
            mViewFlipper.setDisplayedChild( selectedTab );
            /*if( mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES )
            {
                mActionBar.hide();
            }*/
        }
        selectedTab = position;
        oldLength = mTermSessions.size();
        mTermSessions.setOldSize( oldLength );
        mTermSessions.setSelectedSession( selectedTab );
        mViewFlipper.setDisplayedChild( selectedTab );
    }

    private void populateWindowList()
    {
        /*if (mActionBar == null) {
            Log.d("populateWindowList", "in null");
            return;
        }*/

        if( mTermSessions != null )
        {
            Log.d( "populateWindowList", "in Not null" );
            int position = mViewFlipper.getDisplayedChild();
            Integer curLength;
            if( termLineAdapter == null )
            {
                //mWinListAdapter = new Term.WindowListActionBarAdapter( mTermSessions );
                termLineAdapter = new TermLineAdapter( this, mTermSessions );
                Log.d( "populateWindowList", "in mWinListAdapter = null" );
                //mActionBar.setListNavigationCallbacks( mWinListAdapter, mWinListItemSelected );
                //mActionBar.setSelectedNavigationItem(position);
                // POC sometimes not workin?
                if( mTermSessions.getSelectedSession() == 0 )
                {
                    Log.d( "populateWindowList", "curLength  == null" );
                    selectedTab = 0;
                    curLength = 1;
                    oldLength = 1;
                }
                else
                {
                    if( mTermSessions.size() > mTermSessions.getOldSize() )
                    {
                        //added 1
                        curLength = mTermSessions.size();
                        selectedTab = curLength - 1;
                        mTermSessions.setSelectedSession( selectedTab );
                        mTermSessions.setOldSize( curLength );
                        oldLength = curLength;

                        Log.d( "populateWindowList", "added 1" );
                        termLineAdapter.setSessions( mTermSessions );
                        //mActionBar.setSelectedNavigationItem( selectedTab );
                        mViewFlipper.addCallback( termLineAdapter );
                    }
                    else
                    {
                        selectedTab = mTermSessions.getSelectedSession();
                        curLength = mTermSessions.size();
                        oldLength = mTermSessions.getOldSize();
                    }

                }

                //mActionBar.setSelectedNavigationItem( selectedTab );
                mViewFlipper.addCallback( termLineAdapter );

                Log.v( LOG_TAG, "navOpenTerms.setAdapter" );
            }
            else
            {
                curLength = termLineAdapter.getItemCount();
                if( curLength > oldLength )
                {
                    //added 1
                    Log.d( "cur: " + curLength, "last: " + oldLength );
                    selectedTab = curLength - 1;
                    mTermSessions.setSelectedSession( selectedTab );
                    mTermSessions.setOldSize( curLength );
                    oldLength = curLength;

                    Log.d( "populateWindowList", "added 1" );
                    termLineAdapter.setSessions( mTermSessions );
                    //mActionBar.setSelectedNavigationItem( selectedTab );
                    mViewFlipper.addCallback( termLineAdapter );
                }
                else
                {
                    Log.d( "populateWindowList", "in selectedTab" );
                    termLineAdapter.setSessions( mTermSessions );
                    //mActionBar.setSelectedNavigationItem( selectedTab );
                    mTermSessions.setSelectedSession( selectedTab );
                    mViewFlipper.addCallback( termLineAdapter );
                }
            }

            navOpenTerms.setAdapter( termLineAdapter );
        }
    }

    @Override
    public Animation onCreateAnimation( int transit, boolean enter, int nextAnim )
    {
        try
        {
            Animation anim = AnimationUtils.loadAnimation( getActivity(), nextAnim );

            anim.setAnimationListener( new Animation.AnimationListener()
            {

                @Override
                public void onAnimationStart( Animation animation )
                {
                    isAnimating = true;
                }

                @Override
                public void onAnimationRepeat( Animation animation )
                {
                    Log.d( LOG_TAG, "Animation repeating." );
                    // additional functionality
                }

                @Override
                public void onAnimationEnd( Animation animation )
                {
                    isAnimating = false;
                }
            } );

            return anim;
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public boolean isAnimating()
    {
        return isAnimating;
    }

    public void closeSession()
    {
        if( mTermSessions == null )
        {
            return;
        }

        EmulatorView view = getCurrentEmulatorView();
        if( view == null )
        {
            return;
        }
        TermSession session = mTermSessions.remove( mViewFlipper.getDisplayedChild() );
        view.onPause();
        session.finish();
        mViewFlipper.removeView( view );
        if( mTermSessions.size() != 0 )
        {
            mViewFlipper.showNext();
        }
        else
        {
            Log.d( "NOSCREENS?", "?NOSCREENS??" );
        }

        termLineAdapter.notifyDataSetChanged();
    }

    public void addNew()
    {
        showTermSelector( DO_CREATE_NEW_WINDOW );
    }

    private void populateViewFlipper()
    {

        if( mTermService != null )
        {
            mTermSessions = mTermService.getSessions();

            if( mTermSessions.size() == 0 )
            {
                showTermSelector( POPULATE_VIEW_FLIPPER );
            }
            else
            {
                end_populateViewFlipper();
            }
        }
    }

    private void end_populateViewFlipper()
    {
        mTermSessions.addCallback( this );
        for( TermSession _session : mTermSessions )
        {
            EmulatorView view = createEmulatorView( _session );
            mViewFlipper.addView( view );
        }
        updatePrefs();
        if( onResumeSelectWindow >= 0 )
        {
            //mViewFlipper.setDisplayedChild(onResumeSelectWindow);
            onResumeSelectWindow = -1;
        }

        mViewFlipper.onResume();
    }

    private void end_doCreateNewWindow( TermSession session )
    {
        TermView view = createEmulatorView( session );
        view.updatePrefs( mSettings );
        mViewFlipper.addView( view );
        mViewFlipper.setDisplayedChild( mViewFlipper.getChildCount() - 1 );
    }

    private void showTermSelector( final int from )
    {
        final TermSettings settings = mSettings;

        AlertDialog.Builder builderSingle = new AlertDialog.Builder( getContext() );
        builderSingle.setTitle( R.string.term_fragment_new_session );

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>( getContext(), R.layout.dialog_listitem );

        arrayAdapter.add( "Android" );
        arrayAdapter.add( "Android Su" );
        arrayAdapter.add( "PRoot Ubuntu" );

        builderSingle.setNegativeButton( R.string.term_fragment_cancel, new DialogInterface.OnClickListener()
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
                TermSession session;

                switch( which )
                {
                    case 0:// Android
                        session = null;
                        try
                        {
                            session = createTermSession( getActivity().getBaseContext(), settings, "", ShellType.ANDROID_SHELL );
                            session.setFinishCallback( mTermService );
                        }
                        catch( IOException e )
                        {
                            e.printStackTrace();
                        }
                        mTermSessions.add( session );
                        if( from == DO_CREATE_NEW_WINDOW )
                        {
                            end_doCreateNewWindow( session );
                        }
                        if( from == POPULATE_VIEW_FLIPPER )
                        {
                            end_populateViewFlipper();
                        }

                        termLineAdapter.notifyDataSetChanged();
                        break;

                    case 1:
                        Log.d( LOG_TAG, "Su" );
                        session = null;

                        if( CheckRoot.isDeviceRooted() )
                        {
                            Log.d( "isDeviceRooted", "Device is rooted!" );
                            try
                            {
                                session = createTermSession( getActivity().getBaseContext(), settings, "", ShellType.ANDROID_SU_SHELL );
                                session.setFinishCallback( mTermService );
                            }
                            catch( IOException e )
                            {
                                e.printStackTrace();
                            }
                            mTermSessions.add( session );
                            if( Objects.equals( from, "doCreateNewWindow" ) )
                            {
                                end_doCreateNewWindow( session );
                            }
                            if( Objects.equals( from, "populateViewFlipper" ) )
                            {
                                end_populateViewFlipper();
                            }

                        }
                        else
                        {
                            // ALERT! WHY YOU NO ROOT!
                            Log.d( "isDeviceRooted", "Device is not rooted!" );
                            show_nosupersu();
                        }
                        break;

                    case 2://Proot ubuntu
                        File file = new File( getContext().getFilesDir(), "dist/ubuntu-start.sh" );
                        if( file.exists() )
                        {
                            session = null;
                            try
                            {
                                session = createTermSession( getActivity().getBaseContext(), settings, "", ShellType.ANDROID_PURE_SHELL + " /data/data/com.webdefault.corks/files/dist/ubuntu-start.sh" );
                                session.setFinishCallback( mTermService );
                            }
                            catch( IOException e )
                            {
                                e.printStackTrace();
                            }
                            mTermSessions.add( session );
                            if( from == DO_CREATE_NEW_WINDOW )
                            {
                                end_doCreateNewWindow( session );
                            }
                            if( from == POPULATE_VIEW_FLIPPER )
                            {
                                end_populateViewFlipper();
                            }

                            termLineAdapter.notifyDataSetChanged();
                        }
                        else
                        {
                            Dialogs.question( getContext(),
                                    getString(R.string.term_fragment_ubuntu_not_detected),
                                    getString(R.string.term_fragment_ubuntu_not_detected_info),
                                    getString(R.string.term_fragment_yes),
                                    getString(R.string.term_fragment_no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick( DialogInterface dialogInterface, int i )
                                {
                                    installUbuntu( from );
                                }
                            } );
                        }
                        break;
                }
                //Log.v( LOG_TAG, "selection: " + which );
                //String encode = arrayAdapter.getItem( which );
                //editor.saveCurrentFileWithEncode( encode );
            }
        } );
        builderSingle.show();
    }

    private void installUbuntu( final int from )
    {
        Dialogs.question( getContext(),
                getString( R.string.term_fragment_legal ),
                getString( R.string.term_fragment_legal_info ),
                getString( R.string.term_fragment_yes_accept ),
                getString( R.string.term_fragment_cancel ),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick( DialogInterface dialogInterface, int i )
            {
                TermSession session;

                try
                {
                    session = createTermSession( getActivity().getBaseContext(), mSettings, "cd ~ && curl https://webdefault.com.br/install-proot-and-ubuntu.sh | sh", ShellType.ANDROID_SHELL );
                    session.setFinishCallback( mTermService );

                    mTermSessions.add( session );
                    if( from == DO_CREATE_NEW_WINDOW )
                    {
                        end_doCreateNewWindow( session );
                    }
                    if( from == POPULATE_VIEW_FLIPPER )
                    {
                        end_populateViewFlipper();
                    }

                    termLineAdapter.notifyDataSetChanged();
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
            }
        } );
    }

    private void show_nosupersu()
    {
        AlertDialog.Builder builder1 = new AlertDialog.Builder( getActivity() );
        builder1.setMessage( R.string.term_fragment_no_su_binary );
        builder1.setCancelable( true );

        builder1.setPositiveButton(
                R.string.ok,
                new DialogInterface.OnClickListener()
                {
                    public void onClick( DialogInterface dialog, int id )
                    {
                        dialog.cancel();
                    }
                } );

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    protected void selectTerm()
    {
        mTermSessions.setOldSize( oldLength );
        mTermSessions.setSelectedSession( selectedTab );
        mViewFlipper.setDisplayedChild( selectedTab );
    }

    protected static TermSession createTermSession( Context context, TermSettings
            settings, String initialCommand, String _mShell
    ) throws IOException
    {
        Log.d( "MM createTermSession", _mShell + "cmd: " + initialCommand );
        GenericTermSession session = new ShellTermSession( settings, initialCommand, _mShell );  // called from intents
        // XXX We should really be able to fetch this from within TermSession

        session.setProcessExitMessage( context.getString( com.offsec.nhterm.R.string.process_exit_message ) );

        return session;
    }

    @Override
    public void onUpdate()
    {
        SessionList sessions = mTermSessions;
        if( sessions == null )
        {
            return;
        }

        if( sessions.size() == 0 )
        {
            mStopServiceOnFinish = true;
            // finish();
        }
        else if( sessions.size() < mViewFlipper.getChildCount() )
        {
            for( int i = 0; i < mViewFlipper.getChildCount(); ++i )
            {
                EmulatorView v = (EmulatorView) mViewFlipper.getChildAt( i );
                if( !sessions.contains( v.getTermSession() ) )
                {
                    v.onPause();
                    mViewFlipper.removeView( v );
                    --i;
                }
            }
        }
    }
}

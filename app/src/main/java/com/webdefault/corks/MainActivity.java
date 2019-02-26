package com.webdefault.corks;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.system.Os;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.offsec.nhterm.TermView;
import com.offsec.nhterm.emulatorview.EmulatorView;
import com.offsec.nhterm.emulatorview.TermSession;
import com.offsec.nhterm.emulatorview.compat.ClipboardManagerCompat;
import com.offsec.nhterm.emulatorview.compat.ClipboardManagerCompatFactory;
import com.offsec.nhterm.emulatorview.compat.KeycodeConstants;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;
import com.webdefault.corks.editor.adapter.FileLineAdapter;
import com.webdefault.corks.editor.adapter.FileObject;
import com.webdefault.corks.billing.BillingManager;
import com.webdefault.corks.billing.BillingProvider;
import com.webdefault.corks.billing.UpdateListener;
import com.webdefault.corks.editor.highlight.Syntax;
import com.webdefault.corks.term.TermFragment;
import com.webdefault.lib.Utils;
import com.webdefault.lib.db.ResultColumns;
import com.webdefault.lib.db.ResultLines;
import com.webdefault.corks.editor.adapter.FileObjectViewHolder;
import com.webdefault.corks.editor.Editor;
import com.webdefault.corks.editor.ToolsSelector;

import com.offsec.nhterm.TermViewFlipper;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, BillingProvider
{
    private static final String LOG_TAG = "MainActivity";
    
    private static final int SELECT_FOLDER = 0x4445;
    private static final int SELECT_FILE = 0x4446;
    private static final int SAVE_AS_FILE = 0x4447;
    
    private final static int SELECT_TEXT_ID = 0;
    private final static int COPY_ALL_ID = 1;
    private final static int PASTE_ID = 2;
    private final static int SEND_CONTROL_KEY_ID = 3;
    private final static int SEND_FN_KEY_ID = 4;
    
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 0x4448;
    
    private static final String NAME = "Very long name for folder";
    private AndroidTreeView tView;
    private TreeNode root;
    
    TreeNode.TreeNodeClickListener clickListener;
    TreeNode.TreeNodeLongClickListener longClickListener;
    
    private LocalDatabase db;
    
    // Long click selection
    public FileObject longClickFileObject;
    public TreeNode longClickNode;
    
    // Editor
    private FileLineAdapter mFileSelectorAdapter;
    private Editor editor;
    private ToolsSelector tools;
    
    private ScrollView scrollView;
    
    private AllowChildInterceptTouchEventDrawerLayout drawer;
    
    private boolean hasWritePermission = false;
    
    private RecyclerView openFilesListView;
    
    private InterstitialAd mInterstitialAd;
    private BillingManager mBillingManager;
    private UpdateListener updateListener;
    
    private TermViewFlipper mViewFlipper;
    private TermSession session = null;
    
    private TermFragment termFragment;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        setContentView( R.layout.activity_main );
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );
        
        updateListener = new UpdateListener( this );
        mBillingManager = new BillingManager( this, updateListener );
        
        scrollView = (ScrollView) findViewById( R.id.scroll_editor );
        
        mViewFlipper = (TermViewFlipper) findViewById( R.id.view_flipper );
        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById( R.id.fab );
        fab.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                Snackbar.make( view, "Replace with your own action", Snackbar.LENGTH_LONG )
                        .setAction( "Action", null ).show();
            }
        } );
        */
        db = LocalDatabase.Companion.getInstance( this );
        
        if( db.intOption( "app_version_code" ) != BuildConfig.VERSION_CODE )
        {
            Syntax.refreshSyntax( this );
            db.setIntOption( "app_version_code", BuildConfig.VERSION_CODE );
        }
        
        drawer = (AllowChildInterceptTouchEventDrawerLayout) findViewById( R.id.drawer_layout );
        drawer.setInterceptTouchEventChildId( R.id.nav_view );
        // drawer.requestDisallowInterceptTouchEvent( false );
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close );
        drawer.addDrawerListener( toggle );
        toggle.syncState();
        
        int width = (int) ( getResources().getDisplayMetrics().widthPixels * 0.75 );
        ViewGroup dcontainer = findViewById( R.id.drawer_container );
        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) dcontainer.getLayoutParams();
        params.width = width;
        dcontainer.setLayoutParams( params );
        
        findViewById( R.id.open_folder ).setOnClickListener( this );
        
        /// Drawer and desk files
        
        
        clickListener = new TreeNode.TreeNodeClickListener()
        {
            @Override
            public void onClick( TreeNode node, Object value )
            {
                FileObject obj = (FileObject) value;
                if( obj.isDirectory ) fillFolder( node );
                else
                {
                    openFile( node );
                    drawer.closeDrawer( Gravity.START );
                }
            }
        };
        
        longClickListener = new TreeNode.TreeNodeLongClickListener()
        {
            @Override
            public boolean onLongClick( TreeNode node, Object value )
            {
                longClickFileObject = (FileObject) value;
                if( longClickFileObject.isDirectory ) fillFolder( node );
                
                longClickNode = node;
                View t = node.getViewHolder().getView();
                registerForContextMenu( t );
                openContextMenu( t );
                
                return true;
            }
        };
        
        setupDesk();
        
        // Setup editor
        editor = (Editor) findViewById( R.id.editor );
        tools = (ToolsSelector) findViewById( R.id.tools_selector );
        
        tools.init( editor );
        
        requestPermissionToWrite();
        
        setupOpenFilesSelector();
        
        String openFile = getIntent().getStringExtra(ShareActivity.Companion.getOPEN_FILE());
        if( openFile != null )
        {
            openFile( addFolder( openFile, false ) );
            closeDrawers();
        }
        else
        {
            drawer.openDrawer( Gravity.START );
        }
        
        MobileAds.initialize( this, "ca-app-pub-4521783308582259~1242153198" );
        
        mInterstitialAd = new InterstitialAd( this );
        mInterstitialAd.setAdUnitId( "ca-app-pub-4521783308582259/1294124081" );
        mInterstitialAd.loadAd( new AdRequest.Builder().build() );
    }

    public void subscribe()
    {
        Log.d( LOG_TAG, "Purchase button clicked." );
        
        
        mBillingManager.initiatePurchaseFlow( "corks_subscription_tier_1",
                BillingClient.SkuType.SUBS );
    }
    
    public void showAd()
    {
        if( mInterstitialAd.isLoaded() )
        {
            int value = db.intOption( "last_time_shown_ad", 0 );
            
            Log.v( LOG_TAG, "showAd: " + value );
            if( value == 0 || value - ( System.currentTimeMillis() / 1000 ) > 60 * 5 )
            {
                mInterstitialAd.show();
                value = (int) ( System.currentTimeMillis() / 1000 );
                db.setIntOption( "last_time_shown_ad", value );
            }
        }
    }
    
    public void scrollTo( int y )
    {
        ObjectAnimator animator = ObjectAnimator.ofInt( scrollView, "scrollY", scrollView.getScrollY(), y );
        animator.setDuration( 250 );
        animator.start();
    }
    
    public void requestPermissionToWrite()
    {
        // Here, thisActivity is the current activity
        if( ContextCompat.checkSelfPermission( this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE )
                != PackageManager.PERMISSION_GRANTED )
        {
            
            // Should we show an explanation?
            if( ActivityCompat.shouldShowRequestPermissionRationale( this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE ) )
            {
                //Dialogs.info( this, "It needs" );
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            }
            else
            {
                
                // No explanation needed, we can request the permission.
                
                ActivityCompat.requestPermissions( this,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        PERMISSION_WRITE_EXTERNAL_STORAGE );
                
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else
        {
            hasWritePermission = true;
            
            final File directory = getFilesDir();
            File storage = new File( directory, "home/storage" );
            
            if( !storage.isDirectory() )
            {
                try
                {
                    Os.symlink( Environment.getExternalStorageDirectory().getPath(), storage.getPath() );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult( int requestCode, String permissions[],
                                            int[] grantResults
    )
    {
        if( requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE )
        {
            // If request is cancelled, the result arrays are empty.
            //hasWritePermission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            
            requestPermissionToWrite();
            /*
            {
                
            }
            else
            {
                Toast.makeText( this, "Can't open files without permission.", Toast.LENGTH_SHORT ).show();
                finish();
            }
            return;*/
        }
    }
    
    private void setupDesk()
    {
        if( tView != null )
        {
            ( (ViewGroup) findViewById( R.id.nav_view ) ).removeAllViews();
        }
        
        root = TreeNode.root();
        tView = new AndroidTreeView( this, root );
        tView.setDefaultAnimation( true );
        tView.setUse2dScroll( true );
        tView.setDefaultContainerStyle( R.style.TreeNodeStyleCustom );
        ( (ViewGroup) findViewById( R.id.nav_view ) ).addView( tView.getView() );
        
        ResultLines lines = db.selectDeskPaths();
        for( ResultColumns path : lines )
        {
            addFolder( path.get( "path" ), true );
        }
    }
    
    public void closeDrawers()
    {
        drawer.closeDrawers();
    }
    
    public ResultColumns addNewFile()
    {
        return db.addNewFile();
    }
    
    public ToolsSelector getToolsSelector()
    {
        return tools;
    }
    
    public void closeCurrentFile()
    {
        mFileSelectorAdapter.closeCurrentFile();
    }
    
    ResultColumns currentFile = null;
    
    public void selectFile( ResultColumns file )
    {
        if( currentFile != file )
        {
            editor.loadFile( file );
            tools.loadFile( file );
            currentFile = file;
            
            if( mFileSelectorAdapter != null )
                openFilesListView.smoothScrollToPosition( mFileSelectorAdapter.getFocusedItem() );
        }
    }
    
    public void closeFile( ResultColumns fileInfo )
    {
        db.closeFile( fileInfo );
        editor.closeFile( fileInfo );
    }
    
    public static String getMimeType( String url )
    {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl( url );
        if( extension != null )
        {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension( extension.toLowerCase() );
            
            if( type == null )
            {
                try
                {
                    type = Utils.isBinaryFile( url ) ? "application/unknow" : "text/plain";
                }
                catch( Exception e )
                {
                    type = "text/plain";
                }
            }
        }
        
        return type;
    }
    
    public void hideTool()
    {
        tools.hideToolView();
    }
    
    public void saveAs( String path )
    {
        // Log.v( LOG_TAG, "saveAs" );
        Intent intent = new Intent( this, FileBrowser.class );
        intent.putExtra( FileBrowser.DIALOG_TYPE, FileBrowser.SAVE_FILE );
        
        if( !path.isEmpty() )
        {
            intent.putExtra( FileBrowser.DEFAULT_NAME, new File( path ).getName() );
            intent.putExtra( FileBrowser.PATH, path );
        }
        
        intent.putExtra( FileBrowser.FOLDER_RESOURCE, R.drawable.folder_icon );
        intent.putExtra( FileBrowser.FILE_RESOURCE, R.drawable.file_icon );
        intent.putExtra( FileBrowser.BACK_FOLDER_RESOURCE, R.drawable.back_folder_icon );
        startActivityForResult( intent, SAVE_AS_FILE );
    }
    /*
    public void saveWithEncode( String path, String encode )
    {
        // Log.v( LOG_TAG, "saveAs" );
        Intent intent = new Intent( this, FileBrowser.class );
        intent.putExtra( FileBrowser.DIALOG_TYPE, FileBrowser.SAVE_FILE );
        intent.putExtra( FileBrowser.DEFAULT_NAME, new File( path ).getName() );
        intent.putExtra( FileBrowser.PATH, path );
        
        intent.putExtra( FileBrowser.FOLDER_RESOURCE, R.drawable.folder_icon );
        intent.putExtra( FileBrowser.FILE_RESOURCE, R.drawable.file_icon );
        intent.putExtra( FileBrowser.BACK_FOLDER_RESOURCE, R.drawable.back_folder_icon );
        startActivityForResult( intent, SAVE_AS_FILE );
    }
    */
    private void setupOpenFilesSelector()
    {
        LinearLayoutManager layoutManager = new LinearLayoutManager( this, LinearLayoutManager.HORIZONTAL, false );
        openFilesListView = findViewById( R.id.nav_open_files );
        openFilesListView.setLayoutManager( layoutManager );
        
        ResultLines files = db.selectOpenFiles();
        mFileSelectorAdapter = new FileLineAdapter( this, files, db );
        openFilesListView.setAdapter( mFileSelectorAdapter );
        
        // Configurando um dividr entre linhas, para uma melhor visualização.
        openFilesListView.addItemDecoration( new DividerItemDecoration( this, DividerItemDecoration.HORIZONTAL ) );
    }
    
    public void updateFileList()
    {
        mFileSelectorAdapter.notifyDataSetChanged();
    }
    
    @Override
    public boolean onTouchEvent( MotionEvent event )
    {
        Log.v( LOG_TAG, "event: " + event );
        
        
        return super.onTouchEvent( event );
    }
    
    private void openFile( TreeNode node )
    {
        FileObject obj = (FileObject) node.getValue();
        
        String mime = getMimeType( obj.path );
        Log.v( LOG_TAG, "the mime: " + mime );
        if( mime.startsWith( "text/" ) )
        {
            ResultColumns file = db.openFile( obj.name, obj.path );
            
            if( file != null )
            {
                mFileSelectorAdapter.add( file );
            }
        }
        else
        {
            Toast.makeText( this, "This kind of file is not supported yet.", Toast.LENGTH_LONG ).show();
        }
    }
    
    private void addFolder( String path )
    {
        addFolder( path, false );
    }
    
    private TreeNode addFolder( String path, boolean alreadyOpen )
    {
        File file = new File( path );
        
        if( alreadyOpen || !db.isPathShowAtDesk( file.getPath() ) )
        {
            TreeNode node;
            
            if( file.isDirectory() )
            {
                node = new TreeNode( new FileObject( R.string.ic_folder, file.getName(), file.getPath(), file.isDirectory() ) )
                        .setViewHolder( new FileObjectViewHolder( this ) )
                        .setClickListener( clickListener )
                        .setLongClickListener( longClickListener );
                
                fillFolder( node );
            }
            else
            {
                node = new TreeNode( new FileObject( R.string.ic_drive_file, file.getName(), file.getPath(), file.isDirectory() ) )
                        .setViewHolder( new FileObjectViewHolder( this ) )
                        .setClickListener( clickListener )
                        .setLongClickListener( longClickListener );
            }
            
            db.showPathAtDesk( file.getName(), file.getPath() );
            
            // root.addChild( longClickNode );
            tView.addNode( root, node );
            
            return node;
        }
        else
            return null;
    }
    
    private void fillFolder( TreeNode node )
    {
        FileObject obj = (FileObject) node.getValue();
        if( obj.isDirectory && !obj.isRendered )
        {
            File file = new File( obj.path );
            File[] files = file.listFiles();
            Arrays.sort( files );
            
            for( int i = 0; i < files.length; i++ )
            {
                File f = files[i];
                
                if( f.getName().charAt( 0 ) != '.' )
                {
                    boolean isDir = f.isDirectory();
                    
                    TreeNode n = new TreeNode( new FileObject(
                            isDir ? R.string.ic_folder : R.string.ic_drive_file,
                            f.getName(),
                            f.getPath(),
                            f.isDirectory() ) )
                            .setViewHolder( new FileObjectViewHolder( this ) )
                            .setClickListener( clickListener )
                            .setLongClickListener( longClickListener );
                    node.addChild( n );
                }
            }
            
            obj.isRendered = true;
            
            if( files.length > 0 && node.getParent() != null )
            {
                ( (FileObjectViewHolder) node.getViewHolder() ).update( node, (FileObject) node.getValue() );
            }
        }
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        
        editor.pause();
    }
    
    @Override
    public void onBackPressed()
    {
        if( drawer.isDrawerOpen( GravityCompat.START ) || drawer.isDrawerOpen( GravityCompat.END ) )
        {
            drawer.closeDrawer( GravityCompat.START );
            drawer.closeDrawer( GravityCompat.END );
        }
        else
        {
            super.onBackPressed();
        }
    }
    
    final int CONTEXT_MENU_RENAME = 1;
    final int CONTEXT_MENU_REMOVE_FROM_DESK = 2;
    final int CONTEXT_MENU_DELETE = 3;
    
    private boolean isViewFlipper = false;
    
    @Override
    public void onCreateContextMenu( ContextMenu menu, View
            v, ContextMenu.ContextMenuInfo menuInfo
    )
    {
        if( v == mViewFlipper )
        {
            isViewFlipper = true;
            super.onCreateContextMenu( menu, v, menuInfo );
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
        else
        {
            isViewFlipper = false;
            //Context menu
            menu.setHeaderTitle( longClickFileObject.name );
            if( menu.findItem( CONTEXT_MENU_RENAME ) == null )
                menu.add( Menu.NONE, CONTEXT_MENU_RENAME, Menu.NONE, "Rename" );
            
            if( longClickNode.getLevel() == 1 && menu.findItem( CONTEXT_MENU_REMOVE_FROM_DESK ) == null )
                menu.add( Menu.NONE, CONTEXT_MENU_REMOVE_FROM_DESK, Menu.NONE, "Remove from desk" );
            else
                menu.removeItem( CONTEXT_MENU_REMOVE_FROM_DESK );
            
            if( menu.findItem( CONTEXT_MENU_DELETE ) == null )
                menu.add( Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, "Delete" );
        }
    }
    
    private boolean canPaste()
    {
        ClipboardManagerCompat clip = ClipboardManagerCompatFactory
                .getManager( getApplicationContext() );
        return clip.hasText();
    }
    
    private EmulatorView getCurrentEmulatorView()
    {
        return (EmulatorView) mViewFlipper.getCurrentView();
    }
    
    private void doCopyAll()
    {
        ClipboardManagerCompat clip = ClipboardManagerCompatFactory
                .getManager( getApplicationContext() );
        clip.setText( session.getTranscriptText().trim() );
    }
    
    private void doPaste()
    {
        if( !canPaste() )
        {
            return;
        }
        ClipboardManagerCompat clip = ClipboardManagerCompatFactory
                .getManager( getApplicationContext() );
        CharSequence paste = clip.getText();
        session.write( paste.toString() );
    }
    
    @Override
    public boolean onContextItemSelected( MenuItem item )
    {
        DialogInterface.OnClickListener listener;
        
        if( isViewFlipper )
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
                    //doSendControlKey();
                    return true;
                case SEND_FN_KEY_ID:
                    //doSendFnKey();
                    return true;
                default:
                    return super.onContextItemSelected( item );
            }
        }
        else
        {
            switch( item.getItemId() )
            {
                case CONTEXT_MENU_RENAME:
                    listener = new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dialogInterface, int i )
                        {
                            final AlertDialog dialog = (AlertDialog) dialogInterface;
                            
                            EditText field = (EditText) dialog.findViewById( R.id.input );
                            String name = field.getText().toString();
                            if( name.equals( "" ) )
                            {
                                Toast.makeText( MainActivity.this, "Fill with a name.", Toast.LENGTH_SHORT ).show();
                                Dialogs.inputText( MainActivity.this, "Rename", longClickFileObject.name, longClickFileObject.name, "Ok", "Cancel", this );
                            }
                            else if( name.substring( 0, 1 ).equals( "." ) )
                            {
                                Toast.makeText( MainActivity.this, "Files starting with '.' is not allowed.", Toast.LENGTH_SHORT ).show();
                                Dialogs.inputText( MainActivity.this, "Rename", longClickFileObject.name, longClickFileObject.name, "Ok", "Cancel", this );
                            }
                            else
                            {
                                File file = new File( longClickFileObject.path );
                                if( !file.getParentFile().exists() )
                                {
                                    Toast.makeText( MainActivity.this, "You don't have permission to write this folder.", Toast.LENGTH_SHORT ).show();
                                }
                                else if( !file.exists() )
                                {
                                    Toast.makeText( MainActivity.this, "This file may no longer exist.", Toast.LENGTH_SHORT ).show();
                                }
                                else if( file.renameTo( new File( file.getParentFile(), name ) ) )
                                {
                                    // Toast.makeText( MainActivity.this, "Files starting with '.' is not allowed.", Toast.LENGTH_SHORT ).show();
                                    longClickFileObject.name = name;
                                    ( (FileObjectViewHolder) longClickNode.getViewHolder() ).update( longClickNode, longClickFileObject );
                                }
                                else
                                {
                                    Toast.makeText( MainActivity.this, "Could not rename this file. You may not have permission for it.", Toast.LENGTH_SHORT ).show();
                                    Dialogs.inputText( MainActivity.this, "Rename", longClickFileObject.name, longClickFileObject.name, "Ok", "Cancel", this );
                                }
                                dialog.cancel();
                            }
                        }
                    };
                    
                    Dialogs.inputText( this, "Rename", longClickFileObject.name, longClickFileObject.name, "Ok", "Cancel", listener );
                    break;
                case CONTEXT_MENU_REMOVE_FROM_DESK:
                {
                    tView.removeNode( longClickNode );
                    // .getParent().deleteChild( longClickNode );
                    db.removePathFromDesk( longClickFileObject.path );
                }
                break;
                case CONTEXT_MENU_DELETE:
                {
                    listener = new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dialogInterface, int i )
                        {
                            File file = new File( longClickFileObject.path );
                            if( file.delete() )
                            {
                                tView.removeNode( longClickNode );
                            }
                            else
                            {
                                Toast.makeText( MainActivity.this, "Could not delete this file. You may not have permission for it.", Toast.LENGTH_SHORT ).show();
                            }
                        }
                    };
                    
                    Dialogs.question( this, "Warning", "Are you sure to delete '" + longClickFileObject.name + "'?", "Yes", "Cancel", listener );
                }
                break;
            }
        }
        return super.onContextItemSelected( item );
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate( R.menu.main, menu );
        
        if( BuildConfig.DEBUG )
        {
            menu.findItem( R.id.action_refresh_syntax ).setVisible( true );
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        
        Intent intent;
        switch( id )//== R.id.action_settings )
        {
            case R.id.action_open_folder:
                if( hasWritePermission )
                {
                    intent = new Intent( this, FileBrowser.class );
                    intent.putExtra( FileBrowser.DIALOG_TYPE, FileBrowser.OPEN_FOLDER );
                    intent.putExtra( FileBrowser.FOLDER_RESOURCE, R.drawable.folder_icon );
                    intent.putExtra( FileBrowser.FILE_RESOURCE, R.drawable.file_icon );
                    intent.putExtra( FileBrowser.BACK_FOLDER_RESOURCE, R.drawable.back_folder_icon );
                    startActivityForResult( intent, SELECT_FOLDER );
                }
                else
                {
                    requestPermissionToWrite();
                }
                break;
            
            case R.id.action_open_file:
                openFile();
                break;
                
            case R.id.action_refresh_syntax:
                Syntax.refreshSyntax( this );
                break;
            
            case R.id.action_about:
                Dialogs.info(
                        this,
                        getResources().getString( R.string.app_name ),
                        "Version: " + getResources().getString( R.string.versionName ) + "\n\n" +
                                "Terminal based on:\nAndroid Terminal emulator\n" +
                                "https://github.com/jackpal/Android-Terminal-Emulator\n\n" +
                                "Webdefault 2018\n" );
        }
        
        return super.onOptionsItemSelected( item );
    }
    
    public void openFile()
    {
        Intent intent;
        
        if( hasWritePermission )
        {
            intent = new Intent( this, FileBrowser.class );
            intent.putExtra( FileBrowser.DIALOG_TYPE, FileBrowser.OPEN_FILE );
            intent.putExtra( FileBrowser.FOLDER_RESOURCE, R.drawable.folder_icon );
            intent.putExtra( FileBrowser.FILE_RESOURCE, R.drawable.file_icon );
            intent.putExtra( FileBrowser.BACK_FOLDER_RESOURCE, R.drawable.back_folder_icon );
            startActivityForResult( intent, SELECT_FILE );
        }
        else
        {
            requestPermissionToWrite();
        }
    }
    
    public void showConsoleFragment()
    {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        
        if( termFragment == null )
            termFragment = new TermFragment();
        
        if( !termFragment.isAnimating() )
        {
            ft.setCustomAnimations( R.anim.enter_from_bottom, R.anim.enter_from_bottom );
            ft.add( R.id.drawer_layout, termFragment );
            ft.commit();
        }
    }
    
    public void hideConsoleFragment()
    {
        if( !termFragment.isAnimating() )
        {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations( R.anim.leave_from_bottom, R.anim.leave_from_bottom );
            ft.remove( termFragment );
            ft.commit();
        }
    }
    
    @SuppressWarnings( "StatementWithEmptyBody" )
    @Override
    public boolean onNavigationItemSelected( MenuItem item )
    {
        // Handle navigation view item clicks here.
        // int id = item.getItemId();
        
        DrawerLayout drawer = (DrawerLayout) findViewById( R.id.drawer_layout );
        drawer.closeDrawer( GravityCompat.START );
        drawer.closeDrawer( GravityCompat.END );
        return true;
    }
    
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if( requestCode == SELECT_FOLDER )
        {
            if( resultCode == RESULT_OK )
            {
                Log.v( LOG_TAG, "path: " + data.getStringExtra( FileBrowser.FILENAME ) );
                addFolder( data.getStringExtra( FileBrowser.FILENAME ) );
            }
        }
        else if( requestCode == SELECT_FILE )
        {
            if( resultCode == RESULT_OK )
            {
                Log.v( LOG_TAG, "path: " + data.getStringExtra( FileBrowser.FILENAME ) );
                openFile( addFolder( data.getStringExtra( FileBrowser.FILENAME ), false ) );
            }
        }
        else if( requestCode == SAVE_AS_FILE )
        {
            if( resultCode == RESULT_OK )
            {
                Log.v( LOG_TAG, "path: " );
                String oldPath = data.getStringExtra( FileBrowser.OLD_FILENAME );
                String newPath = data.getStringExtra( FileBrowser.FILENAME );
                
                editor.saveAs( newPath );
                
                File file = new File( newPath );
                ResultColumns fileObject = db.renameOpenFile( oldPath, file.getName(), file.getAbsolutePath() );
                mFileSelectorAdapter.set( oldPath, fileObject );
                
                setupDesk();
                
                //
                //tools.saveAs( newPath );
                // addFolder( data.getStringExtra( FileBrowser.FILENAME ) );
            }
        }
    }
    
    @Override
    public void onClick( View view )
    {
        switch( view.getId() )
        {
            case R.id.open_folder:
                if( hasWritePermission )
                {
                    Intent intent = new Intent( this, FileBrowser.class );
                    intent.putExtra( FileBrowser.DIALOG_TYPE, FileBrowser.OPEN_FOLDER );
                    intent.putExtra( FileBrowser.FOLDER_RESOURCE, R.drawable.folder_icon );
                    intent.putExtra( FileBrowser.FILE_RESOURCE, R.drawable.file_icon );
                    intent.putExtra( FileBrowser.BACK_FOLDER_RESOURCE, R.drawable.back_folder_icon );
                    startActivityForResult( intent, SELECT_FOLDER );
                }
                else
                {
                    requestPermissionToWrite();
                }
                break;
        }
    }
    
    @Override
    public BillingManager getBillingManager()
    {
        return null;
    }
    
    @Override
    public boolean hasSubscriptionPurchased()
    {
        return false;
    }
    
    /**
     * Should we use keyboard shortcuts?
     */
    private boolean mUseKeyboardShortcuts;
    
    /**
     * Intercepts keys before the view/terminal gets it.
     */
    private View.OnKeyListener mKeyListener = new View.OnKeyListener()
    {
        public boolean onKey( View v, int keyCode, KeyEvent event )
        {
            return backkeyInterceptor( keyCode, event ) || keyboardShortcuts( keyCode, event );
        }
        
        /**
         * Keyboard shortcuts (tab management, paste)
         */
        private boolean keyboardShortcuts( int keyCode, KeyEvent event )
        {
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
                // doCreateNewWindow();
                
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
            if( false )//keyCode == KeyEvent.KEYCODE_BACK && mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES && mActionBar != null && mActionBar.isShowing() )
            {
                /* We need to intercept the key event before the view sees it,
                   otherwise the view will handle it before we get it */
                onKeyUp( keyCode, event );
                return true;
            }
            else
            {
                return false;
            }
        }
    };
}

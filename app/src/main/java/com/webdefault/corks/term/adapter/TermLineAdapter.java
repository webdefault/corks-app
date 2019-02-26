package com.webdefault.corks.term.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.offsec.nhterm.GenericTermSession;
import com.offsec.nhterm.emulatorview.TermSession;
import com.offsec.nhterm.emulatorview.UpdateCallback;
import com.offsec.nhterm.util.SessionList;
import com.webdefault.corks.LocalDatabase;
import com.webdefault.corks.MainActivity;
import com.webdefault.corks.R;
import com.webdefault.corks.term.TermFragment;
import com.webdefault.lib.db.ResultColumns;

public class TermLineAdapter extends RecyclerView.Adapter<TermHolder> implements UpdateCallback, View.OnClickListener
{
    private static final String LOG_TAG = "TermLineAdapter";
    
    private SessionList mSessions;
    private TermFragment parent;
    private int focusedItem = 0;
    
    public TermLineAdapter( TermFragment parent, SessionList sessions )
    {
        this.parent = parent;
        setSessions( sessions );
    }
    
    public void setSessions( SessionList sessions )
    {
        Log.v( LOG_TAG, "setSession: " + sessions.size() );
        mSessions = sessions;
        
        if( sessions != null )
        {
            sessions.addCallback( this );
            sessions.addTitleChangedListener( this );
        }
        else
        {
            onUpdate();
        }
    }
    
    @Override
    public TermHolder onCreateViewHolder( ViewGroup parent, int viewType )
    {
        return new TermHolder( LayoutInflater.from( parent.getContext() )
                .inflate( 
                        viewType == 1 ? R.layout.open_term_listitem : R.layout.new_file_listitem, 
                        parent, 
                        false ), viewType, this );
    }
    
    @Override
    public void onBindViewHolder( TermHolder holder, final int position )
    {
        if( holder.type == 1 )
        {
            final TermSession term = mSessions.get( position );
            
            holder.itemView.setSelected( focusedItem == position );
            if( focusedItem == position )
            {
                parent.selectSession( mSessions.get( position ), position );
            }
            
            // Log.v( LOG_TAG, "onBindViewHolder: " + term );
            String defaultTitle = parent.getActivity().getString( R.string.window_title, position + 1 );
            
            holder.title.setText( getSessionTitle( position, defaultTitle ) );
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
    }
    
    private void addNew()
    {
        parent.addNew();
    }
    
    protected String getSessionTitle( int position, String defaultTitle )
    {
        TermSession session = mSessions.get( position );
        if( session != null && session instanceof GenericTermSession )
        {
            return ( (GenericTermSession) session ).getTitle( defaultTitle );
        }
        else
        {
            return defaultTitle;
        }
    }
    
    @Override
    public int getItemCount()
    {
        Log.v( LOG_TAG, "getItemCount" );
        if( mSessions != null )
        {
            return mSessions.size() + 1;
        }
        else
        {
            return 1;
        }
    }
    
    @Override
    public int getItemViewType( int position )
    {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous
        return position == mSessions.size() ? 2 : 1;
    }
    
    @Override
    public void onUpdate()
    {
        // notifyDataSetChanged();
    }
    
    @Override
    public void onClick( View view )
    {
        switch( view.getId() )
        {
            case R.id.close_btn:
                parent.closeSession();
                break;
        }
    }
}

package com.webdefault.corks.editor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.github.johnkil.print.PrintView;
import com.unnamed.b.atv.model.TreeNode;
import com.webdefault.corks.R;

/**
 * Created by Bogdan Melnychuk on 2/15/15.
 */
public class FileObjectViewHolder extends TreeNode.BaseNodeViewHolder<FileObject>
{
    private TextView tvValue;
    private PrintView arrowView;
    private CheckBox nodeSelector;
    private PrintView iconView;
    
    public FileObjectViewHolder( Context context )
    {
        super( context );
    }
    
    @Override
    public View createNodeView( final TreeNode node, FileObject value )
    {
        final LayoutInflater inflater = LayoutInflater.from( context );
        final View view = inflater.inflate( R.layout.drawer_file_listitem, null, false );
        
        tvValue = (TextView) view.findViewById( R.id.node_value );
        iconView = (PrintView) view.findViewById( R.id.icon );
        arrowView = (PrintView) view.findViewById( R.id.arrow_icon );
        nodeSelector = (CheckBox) view.findViewById( R.id.node_selector );
        
        nodeSelector.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
            {
                node.setSelected( isChecked );
                for( TreeNode n : node.getChildren() )
                {
                    getTreeView().selectNode( n, isChecked );
                }
            }
        } );
        
        nodeSelector.setChecked( node.isSelected() );
        
        update( node, value );
        
        return view;
    }
    
    public void update( final TreeNode node, FileObject value )
    {
        tvValue.setText( value.name );
        iconView.setIconText( context.getResources().getString( value.icon ) );
        
        if( node.isLeaf() )
        {
            arrowView.setVisibility( View.INVISIBLE );
        }
        else
        {
            arrowView.setVisibility( View.VISIBLE );
        }
    }
    
    @Override
    public void toggle( boolean active )
    {
        arrowView.setIconText( context.getResources().getString( active ? R.string.ic_keyboard_arrow_down : R.string.ic_keyboard_arrow_right ) );
    }
    
    @Override
    public void toggleSelectionMode( boolean editModeEnabled )
    {
        nodeSelector.setVisibility( editModeEnabled ? View.VISIBLE : View.GONE );
        nodeSelector.setChecked( mNode.isSelected() );
    }
}

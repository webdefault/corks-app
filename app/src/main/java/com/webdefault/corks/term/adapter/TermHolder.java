package com.webdefault.corks.term.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.github.johnkil.print.PrintImageButton;
import com.webdefault.corks.R;

public class TermHolder extends RecyclerView.ViewHolder
{
    public int type;
    public TextView title;
    public PrintImageButton closeBtn;
    public int position;

    public TermHolder( View itemView, int type, View.OnClickListener listener )
    {
        super( itemView );

        this.type = type;
        title = (TextView) itemView.findViewById( R.id.name );
        closeBtn = (PrintImageButton) itemView.findViewById( R.id.close_btn );
        closeBtn.setOnClickListener( listener );
        
        
        //moreButton = (ImageButton) itemView.findViewById(R.id.main_line_more);
        //deleteButton = (ImageButton) itemView.findViewById(R.id.main_line_delete);
    }
}

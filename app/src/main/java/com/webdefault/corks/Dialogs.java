package com.webdefault.corks;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by orlandoleite on 2/4/16.
 */
public class Dialogs
{
	public static DialogInterface.OnClickListener DEFAULT_LISTENER = new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick( DialogInterface dialog, int which )
		{
			dialog.dismiss();
		}
	};

	public static void info( Context context, String title, String message )
	{
		info( context, title, message, "Ok", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick( DialogInterface dialog, int which )
			{
				dialog.dismiss();
			}
		} );
	}

	public static void info( Context context, String title, String message, String posBtn, DialogInterface.OnClickListener listener )
	{
		AlertDialog.Builder builder = new AlertDialog.Builder( context );
		builder.setTitle( title );

		// Linkify the message
		final SpannableString s = new SpannableString( message );
		Linkify.addLinks( s, Linkify.ALL );

		builder.setMessage( s );
		builder.setPositiveButton( posBtn, listener );

		AlertDialog d = builder.show();

		((TextView)d.findViewById(android.R.id.message)).setMovementMethod( LinkMovementMethod.getInstance());
	}

	public static void question( Context context, String title, String message, String posBtn, String negBtn, DialogInterface.OnClickListener listener )
	{
		AlertDialog.Builder builder = new AlertDialog.Builder( context );
		builder.setTitle( title );

		// Linkify the message
		final SpannableString s = new SpannableString( message );
		Linkify.addLinks( s, Linkify.ALL );
		builder.setMessage( s );

		builder.setPositiveButton( posBtn, listener );
		builder.setNegativeButton( negBtn, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick( DialogInterface dialog, int which )
			{
				dialog.cancel();
			}
		} );

		AlertDialog d = builder.show();

		((TextView)d.findViewById(android.R.id.message)).setMovementMethod( LinkMovementMethod.getInstance());
	}

	public static void inputText( Context context, String title, String hint, String value, String posBtn, String negBtn, DialogInterface.OnClickListener listener )
	{
		AlertDialog.Builder builder = new AlertDialog.Builder( context, R.style.DialogAlert );
		builder.setTitle( title );

		LinearLayout layout = (LinearLayout) View.inflate( context, R.layout.dialog_input_text, null );
		final EditText input = (EditText) layout.findViewById( R.id.input );
		input.setHint( hint );
		input.setText( value );
		//input.addTextChangedListener( new MoneyTextWatcher( input ) );
		builder.setView( layout );

		//InputMethodManager imm = (InputMethodManager) getContext().getSystemService( Context.INPUT_METHOD_SERVICE );
		//imm.toggleSoftInput( InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY );

		builder.setPositiveButton( posBtn, listener );
		builder.setNegativeButton( negBtn, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick( DialogInterface dialog, int which )
			{
				dialog.cancel();
			}
		} );

		final Dialog dialog = builder.show();

		input.setOnFocusChangeListener( new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange( View v, boolean hasFocus )
			{
				if( hasFocus )
				{
					dialog.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE );
				}
			}
		} );
	}
	/*
	public static void inputNumber( Context context, String title, String hint, String posBtn, String negBtn, DialogInterface.OnClickListener listener )
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle( title );
		
		LinearLayout layout = (LinearLayout) View.inflate( context, R.layout.dialog_input_money, null );
		final EditText input = (EditText) layout.findViewById( R.id.input );
		input.setHint( hint );
		//input.addTextChangedListener( new MoneyTextWatcher( input ) );
		builder.setView( layout );
		
		//InputMethodManager imm = (InputMethodManager) getContext().getSystemService( Context.INPUT_METHOD_SERVICE );
		//imm.toggleSoftInput( InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY );
		
		builder.setPositiveButton( posBtn, listener );
		builder.setNegativeButton( negBtn, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick( DialogInterface dialog, int which )
			{
				dialog.cancel();
			}
		} );
		
		final Dialog dialog = builder.show();
		
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					dialog.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
	}
	*/
}

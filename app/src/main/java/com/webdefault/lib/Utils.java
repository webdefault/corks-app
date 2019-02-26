package com.webdefault.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

// import com.hizzo.app.MainPhone;

public class Utils
{
    public static boolean fileExistsAtPath( String filePath )
    {
        java.io.File file = new java.io.File( filePath );
        return file.exists();
    }

    public static int indexOfStringInList( List<String> list, String value )
    {
        int result = -1;
        int total = list.size();
        for( int i = 0; i < total; i++ )
        {
            if( list.get( i ).equals( value ) )
            {
                result = i;
                break;
            }
        }

        return result;
    }

    public static final String md5( final String s )
    {
        final String MD5 = "MD5";
        try
        {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest
                    .getInstance( MD5 );
            digest.update( s.getBytes() );
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for( byte aMessageDigest : messageDigest )
            {
                String h = Integer.toHexString( 0xFF & aMessageDigest );
                while( h.length() < 2 )
                    h = "0" + h;
                hexString.append( h );
            }
            return hexString.toString();

        }
        catch( NoSuchAlgorithmException e )
        {
            e.printStackTrace();
        }
        return "";
    }

    public static String sha1( String text )
    {
        byte[] bytes = null;
        String result = null;

        try
        {
            bytes = text.getBytes( "utf-8" );

            MessageDigest md = MessageDigest.getInstance( "SHA-1" );
            md.update( bytes, 0, bytes.length );
            byte[] sha1hash = md.digest();

            Formatter fmt = new Formatter();
            for( byte b : sha1hash )
            {
                fmt.format( "%02X", b );
            }

            result = fmt.toString();
            fmt.close();
        }
        catch( Exception e )
        {
            System.out.println( e );
        }

        return result;
    }

    public static byte[] sha1Bytes( String text )
    {
        byte[] bytes = null;

        try
        {
            bytes = text.getBytes( "utf-8" );

            MessageDigest md = MessageDigest.getInstance( "SHA-1" );
            md.update( bytes, 0, bytes.length );
            return md.digest();
        }
        catch( Exception e )
        {
            System.out.println( e );
        }

        return null;
    }

    public static String sha1( InputStream target )
    {
        byte[] sha1hash = null;

        try
        {
            MessageDigest md = MessageDigest.getInstance( "SHA-1" );
            byte[] dataBytes = new byte[1024];

            int nread = 0;

            while( ( nread = target.read( dataBytes ) ) != -1 )
            {
                md.update( dataBytes, 0, nread );
            }
            ;

            sha1hash = md.digest();
        }
        catch( Exception e )
        {
            System.out.println( e );
        }

        // convert the byte to hex format
        StringBuffer sb = new StringBuffer( "" );
        for( int i = 0; i < sha1hash.length; i++ )
        {
            sb.append( Integer.toString( ( sha1hash[i] & 0xff ) + 0x100, 16 )
                    .substring( 1 ) );
        }

        return sb.toString();
    }

    public static String removeAccent( String str, boolean isLowerCase )
    {
        if( isLowerCase == false )
        {
            str = str.replaceAll( "[ÂÀÁÄÃ]", "A" );
            str = str.replaceAll( "[ÊÈÉË]", "E" );
            str = str.replaceAll( "ÎÍÌÏ", "I" );
            str = str.replaceAll( "[ÔÕÒÓÖ]", "O" );
            str = str.replaceAll( "[ÛÙÚÜ]", "U" );
            str = str.replaceAll( "Ç", "C" );
            str = str.replaceAll( "Ý", "Y" );
            str = str.replaceAll( "Ñ", "N" );
        }

        str = str.replaceAll( "[âãàáä]", "a" );
        str = str.replaceAll( "[êèéë]", "e" );
        str = str.replaceAll( "îíìï", "i" );
        str = str.replaceAll( "[ôõòóö]", "o" );
        str = str.replaceAll( "[ûúùü]", "u" );
        str = str.replaceAll( "ç", "c" );
        str = str.replaceAll( "[ýÿ]", "y" );
        str = str.replaceAll( "ñ", "n" );

        return str;
    }

    public static String slugfyString( String str )
    {
        String newStr = removeAccent( str.toLowerCase( Locale.getDefault() ), true );
        return newStr.replaceAll( "\\W", "-" );
    }

    public static String floatToMoney( Float value )
    {
        Currency currency = Currency.getInstance( "BRL" );
        DecimalFormat formato = new DecimalFormat( currency.getSymbol() + " #,##0.00" );
        return formato.format( value );
    }

    public static DisplayMetrics getMetrics( Context ctx )
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) ctx.getSystemService( Context.WINDOW_SERVICE ); // the results will be higher than using the activity context object or the getWindowManager() shortcut
        wm.getDefaultDisplay().getMetrics( displayMetrics );

        return displayMetrics;
    }


    public static float distanceFromLocation( double latA, double logA, double latB, double logB )
    {
        Location locationA = new Location( "point A" );
        locationA.setLatitude( latA );
        locationA.setLongitude( logA );

        Location locationB = new Location( "point B" );
        locationB.setLatitude( latB );
        locationB.setLongitude( logB );

        return locationA.distanceTo( locationB );
    }

    public static void openWebURL( String inURL, Activity act )
    {
        Intent browse = new Intent( Intent.ACTION_VIEW, Uri.parse( inURL ) );
        act.startActivity( browse );
    }

    public static void setListViewHeightBasedOnChildren( ListView listView )
    {
        ListAdapter listAdapter = listView.getAdapter();
        if( listAdapter == null )
            return;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec( listView.getWidth(), View.MeasureSpec.UNSPECIFIED );
        int totalHeight = 0;
        View view = null;
        for( int i = 0; i < listAdapter.getCount(); i++ )
        {
            view = listAdapter.getView( i, view, listView );
            if( i == 0 )
                view.setLayoutParams( new ViewGroup.LayoutParams( desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT ) );

            view.measure( desiredWidth, View.MeasureSpec.UNSPECIFIED );
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + ( listView.getDividerHeight() * ( listAdapter.getCount() - 1 ) );
        listView.setLayoutParams( params );
    }

    public static String differenceDateToString( Date date )
    {
        long diff = ( new Date().getTime() / 1000 ) - date.getTime();

        int hours = (int) diff / 3600;
        int minutes = (int) ( diff - hours * 3600 ) / 60;
        // int seconds = (int) ( diff - hours * 3600 - minutes * 60 );
        // super.setText( ( hours > 0 ? hours + "h" : "" ) + minutes + "m" + seconds + "s" );

        if( minutes == 0 )
        {
            return "0 minuto";
        }
        else
        {
            String h = " horas e ";
            String m = " minutos";
            if( hours == 1 ) h = " hora e ";
            if( minutes == 1 ) m = " minuto";

            return ( hours > 0 ? hours + h : "" ) + minutes + m;
        }
    }

    public static String shortFileSize( long size )
    {
        if( size < 1024 )
            return size + " Bytes";
        else if( ( size = size / 1024 ) < 1024 )
            return size + " kB";
        else if( ( size = size / 1024 ) < 1024 )
            return size + " mB";
        else if( ( size = size / 1024 ) < 1024 )
            return size + " gB";
        else
            return size + " tB";
    }

    /**
     * https://stackoverflow.com/questions/620993/determining-binary-text-file-type-in-java
     * Guess whether given file is binary. Just checks for anything under 0x09.
     */
    public static boolean isBinaryFile( String url ) throws FileNotFoundException, IOException
    {
        File f = new File(url);
        if(!f.exists())
            return false;
        FileInputStream in = new FileInputStream(f);
        int size = in.available();
        if(size > 1000)
            size = 1000;
        byte[] data = new byte[size];
        in.read(data);
        in.close();
        String s = new String(data, "ISO-8859-1");
        String s2 = s.replaceAll(
                "[a-zA-Z0-9ßöäü\\.\\*!\"§\\$\\%&/()=\\?@~'#:,;\\"+
                        "+><\\|\\[\\]\\{\\}\\^°²³\\\\ \\n\\r\\t_\\-`´âêîô"+
                        "ÂÊÔÎáéíóàèìòÁÉÍÓÀÈÌÒ©‰¢£¥€±¿»«¼½¾™ª]", "");
        // will delete all text signs

        double d = (double)(s.length() - s2.length()) / (double)(s.length());
        // percentage of text signs in the text
        return d < 0.95;
    }

    public static void copyFile( InputStream in, OutputStream out ) throws IOException
    {
        byte[] buffer = new byte[1024];
        int read;
        while( ( read = in.read( buffer ) ) != -1 )
        {
            out.write( buffer, 0, read );
        }
    }

    public static int between( int i, int min, int max )
    {
        return i < min ? min : ( i > max ? max : i );
    }

    public static boolean deleteRecursive( File fileOrDirectory )
    {

        if( fileOrDirectory.isDirectory() )
            for( File child : fileOrDirectory.listFiles() )
                deleteRecursive( child );

        return fileOrDirectory.delete();
    }
}

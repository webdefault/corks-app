package com.webdefault.corks.term

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout

import com.github.johnkil.print.PrintImageButton
import com.offsec.nhterm.emulatorview.EmulatorView
import com.offsec.nhterm.emulatorview.compat.KeycodeConstants
import com.webdefault.corks.MainActivity
import com.webdefault.corks.R

class TermTool(context:Context, attrs:AttributeSet?) : LinearLayout(context, attrs),
        View.OnClickListener
{
    
    private lateinit var escBtn:Button
    private lateinit var ctrlBtn:Button
    private lateinit var altBtn:Button
    private lateinit var tabBtn:Button
    
    private lateinit var cursorBtn:PrintImageButton
    private lateinit var verBarBtn:Button
    private lateinit var dashBtn:Button
    private lateinit var slashBtn:Button
    
    private lateinit var parent:TermFragment
    
    val isCursorSelected:Boolean
        get() = cursorBtn.isSelected
    
    fun init(parent:TermFragment)
    {
        this.parent = parent
        
        findViewById<View>(R.id.text_edit_btn).setOnClickListener(this)
        
        escBtn = findViewById<View>(R.id.esc_btn) as Button
        escBtn.setOnClickListener(this)
        
        ctrlBtn = findViewById<View>(R.id.control_btn) as Button
        ctrlBtn.setOnClickListener(this)
        
        altBtn = findViewById<View>(R.id.alt_btn) as Button
        altBtn.setOnClickListener(this)
        
        tabBtn = findViewById<View>(R.id.tab_btn) as Button
        tabBtn.setOnClickListener(this)
        
        cursorBtn = findViewById<View>(R.id.cursor_btn) as PrintImageButton
        cursorBtn.setOnClickListener(this)
        
        verBarBtn = findViewById<View>(R.id.vertical_bar_btn) as Button
        verBarBtn.setOnClickListener(this)
        
        dashBtn = findViewById<View>(R.id.dash_btn) as Button
        dashBtn.setOnClickListener(this)
        
        slashBtn = findViewById<View>(R.id.slash_btn) as Button
        slashBtn.setOnClickListener(this)
    }
    
    override fun onClick(view:View)
    {
        when(view.id)
        {
            R.id.esc_btn          -> doSendActionBarKey(KeycodeConstants.KEYCODE_ESCAPE)
            
            R.id.control_btn      ->
            {
                doSendActionBarKey(KeycodeConstants.KEYCODE_CTRL_LEFT)
                cursorBtn.isSelected = false
            }
            
            R.id.alt_btn          -> doSendActionBarKey(KeycodeConstants.KEYCODE_ALT_LEFT)
            
            R.id.tab_btn          -> doSendActionBarKey(KeycodeConstants.KEYCODE_TAB)
            
            R.id.vertical_bar_btn -> parent.sendKeyStrings("|", false)
            
            R.id.dash_btn         -> parent.sendKeyStrings("-", false)
            
            R.id.slash_btn        -> parent.sendKeyStrings("/", false)
            
            R.id.cursor_btn       -> cursorBtn.isSelected = !cursorBtn.isSelected
            
            R.id.text_edit_btn    -> (context as MainActivity).hideConsoleFragment()
        }
    }
    
    private fun doSendActionBarKey(key:Int):Boolean
    {
        val view = parent.currentEmulatorView
        
        if(key == 999)
        {
            // do nothing
        }
        else if(key == 1002)
        {
            parent.doToggleSoftKeyboard()
        }
        else if(key == 1249)
        {
            //doPaste();
        }
        else if(key == 1250)
        {
            parent.addNew()
        }
        else if(key == 1251)
        {
            /*if( mVimApp && mSettings.getInitialCommand().matches( "(.|\n)*(^|\n)-vim\\.app(.|\n)*" ) && mTermSessions.size() == 1 )
            {
                sendKeyStrings( ":confirm qa\r", true );
            }
            else
            {*/
            parent.closeSession()
            //}
        }
        else if(key == 1252)
        {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        else if(key == 1253)
        {
            parent.sendKeyStrings(":confirm qa\r", true)
        }
        else if(key == 1254)
        {
            view.sendFnKeyCode()
        }
        else if(key == KeycodeConstants.KEYCODE_ALT_LEFT)
        {
            view.sendAltKeyCode()
        }
        else if(key == KeycodeConstants.KEYCODE_CTRL_LEFT)
        {
            view.sendControlKeyCode()
        }
        else if(key == 1247)
        {
            parent.sendKeyStrings(":", false)
        }
        else if(key == 1255)
        {
            Log.v(LOG_TAG, "key: 1255")
            // parent.setFunctionBar( 2 );
        }
        else if(key > 0)
        {
            var event = KeyEvent(KeyEvent.ACTION_DOWN, key)
            dispatchKeyEvent(event)
            event = KeyEvent(KeyEvent.ACTION_UP, key)
            dispatchKeyEvent(event)
        }
        return true
    }
    
    companion object
    {
        private val LOG_TAG = "TermTool"
    }
}

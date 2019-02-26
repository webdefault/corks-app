package com.webdefault.corks

import android.content.Context
import android.graphics.Rect
import android.support.v4.widget.DrawerLayout
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View

/**
 * Created by orlandoleite on 2/12/18.
 */

class AllowChildInterceptTouchEventDrawerLayout : DrawerLayout
{

	private var mInterceptTouchEventChildId:Int = 0

	fun setInterceptTouchEventChildId(id:Int)
	{
		this.mInterceptTouchEventChildId = id
	}

	constructor(context:Context) : super(context)
	{
	}

	constructor(context:Context, attrs:AttributeSet) : super(context, attrs)
	{
	}

	override fun onInterceptTouchEvent(ev:MotionEvent):Boolean
	{
		if(mInterceptTouchEventChildId > 0 && isDrawerOpen(Gravity.LEFT))
		{
			val scroll = findViewById<View>(mInterceptTouchEventChildId)
			if(scroll != null)
			{
				val rect = Rect()
				scroll.getHitRect(rect)
				if(rect.contains(ev.x.toInt(), ev.y.toInt()))
				{
					return false
				}
			}
		}

		try
		{
			return super.onInterceptTouchEvent(ev)
		}
		catch(e:Exception)
		{
			e.printStackTrace()
		}
		finally
		{
			//return false;
		}

		return false
	}
}
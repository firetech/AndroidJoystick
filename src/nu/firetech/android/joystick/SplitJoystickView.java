/*
 * Copyright (c) 2011, olberg(at)gmail(dot)com, 
 *                     http://mobile-anarchy-widgets.googlecode.com
 * Copyright (c) 2011, Joakim Andersson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * # Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * # Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * As can be seen in the copyright above, this class is a heavily modified
 * version of the DualJoystickView class available from the
 * mobile-anarchy-widgets project on Google Code.
 * 
 * 		/Joakim Andersson, 2011-11-11
 */

package nu.firetech.android.joystick;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * The {@link SplitJoystickView} class uses two {@link JoystickView}s to
 * provide a multi touch interface similar to the two sticks that can be found
 * on most radio controlled car toys.
 * 
 * That is, one stick for vertical movement and one for horizontal movement.
 */
public class SplitJoystickView extends LinearLayout {
	@SuppressWarnings("unused")
	private static final String TAG = SplitJoystickView.class.getSimpleName();
	
	// =========================================
	// Private Members
	// =========================================

	private JoystickView stickVertical;
	private JoystickView stickHorizontal;

	private boolean leftControls;
	private View spacer;
	private double spacerWidth;
	private Bitmap spacerBitmap;

	// =========================================
	// Constructors
	// =========================================

	public SplitJoystickView(Context context) {
		super(context);
		initView();
	}

	public SplitJoystickView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SplitJoystickView);
		if (a != null) {
			int maxSize = a.getDimensionPixelSize(R.styleable.SplitJoystickView_maxJoystickSize, 0);
			this.setLeftControls(a.getBoolean(R.styleable.SplitJoystickView_leftControls, false));
			stickVertical.setBackground(a.getResourceId(R.styleable.SplitJoystickView_vertBgSrc, R.drawable.joystick_bg_vert));
			stickVertical.setHandle(a.getResourceId(R.styleable.SplitJoystickView_vertHandleSrc, R.drawable.joystick_handle));
			stickVertical.setMaxSize(maxSize);
			this.setSpacer(a.getResourceId(R.styleable.SplitJoystickView_spacerSrc, 0));
			stickHorizontal.setBackground(a.getResourceId(R.styleable.SplitJoystickView_horizBgSrc, R.drawable.joystick_bg_horiz));
			stickHorizontal.setHandle(a.getResourceId(R.styleable.SplitJoystickView_horizHandleSrc, R.drawable.joystick_handle));
			stickHorizontal.setMaxSize(maxSize);
		}
	}

	// =========================================
	// Initialization
	// =========================================

	private void initView() {
		setOrientation(LinearLayout.HORIZONTAL);
		
		stickVertical = new JoystickView(getContext());
		stickVertical.setOrientation(JoystickView.OR_VERTICAL);
		stickHorizontal = new JoystickView(getContext());
		stickHorizontal.setOrientation(JoystickView.OR_HORIZONTAL);
		spacer = new View(getContext());
	}
	
	// =========================================
	// Public Methods 
	// =========================================

	public void setOnJostickMovedListener(OnJoystickMovedListener listener) {
		if (listener != null) {
			HorizJoystickMovedListener horiz = new HorizJoystickMovedListener(listener);
			VertJoystickMovedListener vert = new VertJoystickMovedListener(listener);
			horiz.setPartner(vert);
			vert.setPartner(horiz);
			stickVertical.setOnJostickMovedListener(vert);
			stickHorizontal.setOnJostickMovedListener(horiz);
		} else {
			stickVertical.setOnJostickMovedListener(null);
			stickHorizontal.setOnJostickMovedListener(null);
		}
	}
	
	public void setLeftControls(boolean leftControls) {
		this.leftControls = leftControls;
		requestLayout();
	}

	public void setVertBackground(int resId) {
		stickVertical.setBackground(resId);
	}
	
	public void setVertHandle(int resId) {
		stickVertical.setHandle(resId);
	}

	public void setSpacer(int resId) {
		if (resId != 0) {
			spacerBitmap = BitmapFactory.decodeResource(getResources(), resId);
		} else {
			spacerBitmap = null;
		}
	}

	public void setHorizBackground(int resId) {
		stickHorizontal.setBackground(resId);
	}
	
	public void setHorizHandle(int resId) {
		stickHorizontal.setHandle(resId);
	}
	
	// =========================================
	// Drawing Functionality
	// =========================================

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		removeView(stickVertical);
		removeView(spacer);
		removeView(stickHorizontal);

		spacerWidth = getMeasuredWidth()-(getMeasuredHeight() * 2);
		int joyWidth = (int)((getMeasuredWidth() - spacerWidth)/2);
		LayoutParams joyLParams = new LayoutParams(joyWidth,getMeasuredHeight());

		stickVertical.setLayoutParams(joyLParams);
		stickHorizontal.setLayoutParams(joyLParams);
		
		stickVertical.setPointerId(JoystickView.INVALID_POINTER_ID);
		stickHorizontal.setPointerId(JoystickView.INVALID_POINTER_ID);

		addView(leftControls ? stickHorizontal : stickVertical);
		
		if (spacerWidth > 0) {
			ViewGroup.LayoutParams spacerLParams = new ViewGroup.LayoutParams((int)spacerWidth, getMeasuredHeight());
			spacer.setLayoutParams(spacerLParams);
			addView(spacer);
		}

		addView(leftControls ? stickVertical : stickHorizontal);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		JoystickView rightStick = (leftControls ? stickVertical : stickHorizontal);
		rightStick.setTouchOffset(rightStick.getLeft(), rightStick.getTop());
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		
		if (spacerWidth > 0 && spacerBitmap != null && spacer.getVisibility() == View.VISIBLE) {
			double bitmapWidth = spacerBitmap.getWidth(), spacerWidth = spacer.getWidth();
			double bitmapHeight = spacerBitmap.getHeight(), spacerHeight = spacer.getHeight();
			double scale = Math.min((spacerWidth*0.75)/bitmapWidth, (spacerHeight*0.75)/bitmapHeight);
			if (scale < 1.0) {
				bitmapWidth *= scale;
				bitmapHeight *= scale;
			}
			int widthOffset = (int)((spacerWidth - bitmapWidth)/2);
			int heightOffset = (int)((spacerHeight - bitmapHeight)/2);
			Rect r = new Rect(spacer.getLeft() + widthOffset, spacer.getTop() + heightOffset,
							  spacer.getRight() - widthOffset, spacer.getBottom() - heightOffset);
			canvas.drawBitmap(spacerBitmap, null, r, null);
		}
	}
	
	// =========================================
	// Movement Functionality 
	// =========================================

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean v = stickVertical.dispatchTouchEvent(ev);
		boolean h = stickHorizontal.dispatchTouchEvent(ev);
		return v || h;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		boolean v = stickVertical.onTouchEvent(ev);
		boolean h = stickHorizontal.onTouchEvent(ev);
		return v || h;
	}
	

	// =========================================
	// Helpers for merging of Listener output
	// =========================================
	
	private class HorizJoystickMovedListener implements OnJoystickMovedListener {
		int x = 0;
		private OnJoystickMovedListener subListener;
		private VertJoystickMovedListener partner;
		
		public HorizJoystickMovedListener(OnJoystickMovedListener subListener) {
			this.subListener = subListener;
		}
		
		public void setPartner(VertJoystickMovedListener partner) {
			this.partner = partner;
		}
		
		@Override
		public void onMoved(int x, int y) {
			this.x = x;
			subListener.onMoved(x, partner.y);
		}
	}
	
	private class VertJoystickMovedListener implements OnJoystickMovedListener {
		int y = 0;
		private OnJoystickMovedListener subListener;
		private HorizJoystickMovedListener partner;
		
		public VertJoystickMovedListener(OnJoystickMovedListener subListener) {
			this.subListener = subListener;
		}
		
		public void setPartner(HorizJoystickMovedListener partner) {
			this.partner = partner;
		}
		
		@Override
		public void onMoved(int x, int y) {
			this.y = y;
			subListener.onMoved(partner.x, y);
		}
	}
}

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
 * As can be seen in the copyright above, this class is a modified version of
 * the JoystickView class available from the mobile-anarchy-widgets project on
 * Google Code. I've stripped the code to my needs and added some customization
 * options that I needed for my own project.
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

/**
 * The {@link JoystickView} class provides a widget that imitates the
 * functionality of a joystick.
 */
public class JoystickView extends View {
	public static final int INVALID_POINTER_ID = -1;

	public static final int OR_BOTH = 0;
	public static final int OR_VERTICAL = 1;
	public static final int OR_HORIZONTAL = 2;
	
	// =========================================
	// Private Members
	// =========================================
	private Bitmap bg;
	private Bitmap handle;
	
	private int bgRadius;
	private int handleRadius;
	private int movementRadius;
	
	private OnJoystickMovedListener moveListener;
	private int orientation = OR_BOTH;
	
	//Last touch point in view coordinates
	private int pointerId = INVALID_POINTER_ID;
	private float touchX, touchY;
	
	//Last reported position in view coordinates (allows different reporting sensitivities)
	private float reportX, reportY;
	
	//Handle center in view coordinates
	private float handleX, handleY;
	
	//Center of the view in view coordinates
	private int cX, cY;

	//Size of the view in view coordinates
	private int dim;
	
	//User coordinates of last touch point
	private int userX, userY;

	//Offset co-ordinates (used when touch events are received from parent's coordinate origin)
	private int offsetX;
	private int offsetY;

	// =========================================
	// Constructors
	// =========================================

	public JoystickView(Context context) {
		super(context);
		initView();
	}

	public JoystickView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.JoystickView);
		if (a != null) {
			setBackground(a.getResourceId(R.styleable.JoystickView_bgSrc, R.drawable.joystick_bg));
			setHandle(a.getResourceId(R.styleable.JoystickView_handleSrc, R.drawable.joystick_handle));
			int orientation = OR_BOTH;
			String s = a.getString(R.styleable.JoystickView_orientation);
			if (s != null) {
				s = s.toLowerCase();
				if (s.equals("both")) {
					orientation = OR_BOTH;
				} else if (s.equals("vertical")) {
					orientation = OR_VERTICAL;
				} else if (s.equals("horizontal")) {
					orientation = OR_HORIZONTAL;
				} else {
					throw new IllegalArgumentException("No such orientation: " + s);
				}
				setOrientation(orientation);
			}
		}
		
		initView();
	}

	// =========================================
	// Initialization
	// =========================================

	private void initView() {
		setFocusable(true);
	}
	
	// =========================================
	// Public Methods 
	// =========================================

	public void setOrientation(int orientation) {
		switch(orientation) {
		case OR_BOTH:
		case OR_VERTICAL:
		case OR_HORIZONTAL:
			this.orientation = orientation;
			break;
		default:
			throw new IllegalArgumentException("Unknown orientation");
		}
	}

	public void setBackground(int resId) {
		if (resId != 0) {
			bg = BitmapFactory.decodeResource(getResources(), resId);
		} else {
			bg = null;
		}
	}
	
	public void setHandle(int resId) {
		if (resId != 0) {
			handle = BitmapFactory.decodeResource(getResources(), resId);
		} else {
			handle = null;
		}
	}

	public void setOnJostickMovedListener(OnJoystickMovedListener listener) {
		this.moveListener = listener;
	}
	
	public void setPointerId(int id) {
		this.pointerId = id;
	}
	
	public int getPointerId() {
		return pointerId;
	}

	public void setTouchOffset(int x, int y) {
		offsetX = x;
		offsetY = y;
	}
	
	// =========================================
	// Drawing Functionality 
	// =========================================

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Here we make sure that we have a perfect circle
		int measuredWidth = measure(widthMeasureSpec);
		int measuredHeight = measure(heightMeasureSpec);
		if (measuredWidth < measuredHeight) {
			measuredHeight = (measuredWidth * 3) / 4; 
		}
		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		int d = Math.min(width, height);

		dim = d;

		cX = width / 2;
		cY = height / 2;
		
		bgRadius = dim/2 - 10;
		handleRadius = d / 4;
		movementRadius = Math.min(cX, cY) - handleRadius;
	}

	private int measure(int measureSpec) {
		int result = 0;
		// Decode the measurement specifications.
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		if (specMode == MeasureSpec.UNSPECIFIED) {
			// Return a default size of 200 if no bounds are specified.
			result = 200;
		} else {
			// As you want to fill the available space
			// always return the full available bounds.
			result = specSize;
		}
		return result;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.save();
		// Draw the background
		if (bg != null) {
			double bgWidth = bg.getWidth();
			double bgHeight = bg.getHeight();
			double scale = Math.min((bgRadius*2)/bgWidth, (bgRadius*2)/bgHeight);
			bgWidth *= scale;
			bgHeight *= scale;
			int widthOffset = (int)(bgWidth/2);
			int heightOffset = (int)(bgHeight/2);
			Rect bgPos = new Rect(cX - widthOffset, cY - heightOffset,
								  cX + widthOffset, cY + heightOffset);
			canvas.drawBitmap(bg, null, bgPos, null);
		}

		// Draw the handle
		if (handle != null) {
			handleX = touchX + cX;
			handleY = touchY + cY;
			Rect handlePos = new Rect((int)(handleX - handleRadius), (int)(handleY - handleRadius),
									  (int)(handleX + handleRadius), (int)(handleY + handleRadius));
			canvas.drawBitmap(handle, null, handlePos, null);
		}
		canvas.restore();
	}
	
	// =========================================
	// Movement Functionality 
	// =========================================

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
	    final int action = ev.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		    case MotionEvent.ACTION_MOVE: {
	    		return processMoveEvent(ev);
		    }	    
		    case MotionEvent.ACTION_CANCEL: 
		    case MotionEvent.ACTION_UP: {
		    	if ( pointerId != INVALID_POINTER_ID ) {
			    	returnHandleToCenter();
		        	setPointerId(INVALID_POINTER_ID);
		    	}
		        break;
		    }
		    case MotionEvent.ACTION_POINTER_UP: {
		    	if ( pointerId != INVALID_POINTER_ID ) {
			        final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			        final int pointerId = ev.getPointerId(pointerIndex);
			        if ( pointerId == this.pointerId ) {
			        	returnHandleToCenter();
			        	setPointerId(INVALID_POINTER_ID);
			    		return true;
			        }
		    	}
		        break;
		    }
		    case MotionEvent.ACTION_DOWN: {
		    	if ( pointerId == INVALID_POINTER_ID ) {
		    		int x = (int) ev.getX();
		    		if ( x >= offsetX && x < offsetX + dim ) {
			        	setPointerId(ev.getPointerId(0));
			    		return true;
		    		}
		    	}
		        break;
		    }
		    case MotionEvent.ACTION_POINTER_DOWN: {
		    	if ( pointerId == INVALID_POINTER_ID ) {
			        final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			        final int pointerId = ev.getPointerId(pointerIndex);
		    		int x = (int) ev.getX(pointerId);
		    		if ( x >= offsetX && x < offsetX + dim ) {
			        	setPointerId(pointerId);
			    		return true;
		    		}
		    	}
		        break;
		    }
	    }
		return false;
	}
	
	private boolean processMoveEvent(MotionEvent ev) {
		if ( pointerId != INVALID_POINTER_ID ) {
			final int pointerIndex = ev.findPointerIndex(pointerId);
			
			// Translate touch position to center of view
			float x = ev.getX(pointerIndex);
			touchX = x - cX - offsetX;
			float y = ev.getY(pointerIndex);
			touchY = y - cY - offsetY;
        	
			reportOnMoved();
			invalidate();
			
			return true;
		}
		return false;
	}

	private void reportOnMoved() {
		if (orientation != OR_VERTICAL) {
			touchX = Math.max(Math.min(touchX, movementRadius), -movementRadius);
		} else {
			touchX = 0;
		}
		if (orientation != OR_HORIZONTAL) {
        	touchY = Math.max(Math.min(touchY, movementRadius), -movementRadius);
		} else {
			touchY = 0;
		}

		if (moveListener != null) {
			boolean rx = Math.abs(touchX - reportX) >= 1.0f;
			boolean ry = Math.abs(touchY - reportY) >= 1.0f;
			if (rx || ry) {
				this.reportX = touchX;
				this.reportY = touchY;

				userX = (int)(touchX / movementRadius * 10);
				userY = -(int)(touchY / movementRadius * 10);
				
				moveListener.onMoved(userX, userY);
			}
		}
	}

	private void returnHandleToCenter() {
		final int numberOfFrames = 2;
		final double intervalsX = (0 - touchX) / numberOfFrames;
		final double intervalsY = (0 - touchY) / numberOfFrames;

		for (int i = 0; i < numberOfFrames; i++) {
			postDelayed(new Runnable() {
				@Override
				public void run() {
					touchX += intervalsX;
					touchY += intervalsY;
					
					invalidate();
				}
			}, i * 40);
		}

		if (moveListener != null) {
			// Ignore the animated movements
			this.reportX = 0;
			this.reportY = 0;
			
			moveListener.onMoved(0, 0);
		}
	}
}

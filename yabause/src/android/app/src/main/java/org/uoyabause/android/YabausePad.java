/*  Copyright 2013 Guillaume Duhamel

    This file is part of Yabause.

    Yabause is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Yabause is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Yabause; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

package org.uoyabause.android;

import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View.OnTouchListener;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;

import java.util.HashMap;

import android.util.DisplayMetrics;

import org.uoyabause.android.PadEvent;

class PadButton {
    protected RectF rect;
    protected int isOn;
    Paint back;
    float scale;

    PadButton() {
        isOn = -1;
        rect = new RectF();
        scale = 1.0f;
    }

    public void updateRect(int x1, int y1, int x2, int y2) {
        rect.set(x1, y1, x2, y2);
    }

    public void updateRect(Matrix matrix, int x1, int y1, int x2, int y2) {
        rect.set(x1, y1, x2, y2);
        matrix.mapRect(rect);
    }
    
    public void updateScale( float scale ){
    	this.scale = scale;
    }
    
    public boolean contains(int x, int y ) {
        return rect.contains(x, y);
    }

    public boolean intersects( RectF r ){
      return RectF.intersects(rect,r);
    }

    public void draw(Canvas canvas, Paint nomal_back, Paint active_back, Paint front) {
      if( isOn != -1 ){
        back = active_back;
      }else{
        back = nomal_back;
      }
    }

    void On( int index ){
      isOn = index;
    }

    void Off(){
      isOn = -1;
    }

    boolean isOn( int index ){
      if( isOn == index ){
        return true;
      }else{
        return false;
      }
    }
    
    boolean isOn(){
        if( isOn != -1 ){
            return true;
          }else{
        	return false;
          }	
    }
}



class DPadButton extends PadButton {
    public void draw(Canvas canvas, Paint nomal_back, Paint active_back, Paint front) {
        super.draw(canvas,nomal_back,active_back,front);
        canvas.drawRect(rect, back);
    }
}

class StartButton extends PadButton {
    public void draw(Canvas canvas, Paint nomal_back, Paint active_back, Paint front) {
      super.draw(canvas,nomal_back,active_back,front);
      canvas.drawOval(new RectF(rect), back);
    }
}

class ActionButton extends PadButton {
    private int width;
    private String text;
    private int textsize;

    ActionButton(int w, String t, int ts) {
        super();
        width = w;
        text = t;
        textsize = ts;
    }
    

    public void draw(Canvas canvas, Paint nomal_back, Paint active_back, Paint front) {
        super.draw(canvas,nomal_back,active_back,front);
        canvas.drawCircle(rect.centerX(), rect.centerY(), width * this.scale, back);
        //front.setTextSize(textsize);
        //front.setTextAlign(Paint.Align.CENTER);
        //canvas.drawText(text, rect.centerX() , rect.centerY() , front);
    }
}


interface OnPadListener {
    public abstract boolean onPad(PadEvent event);
}

class YabausePad extends View implements OnTouchListener {
    private PadButton buttons[];
    private OnPadListener listener = null;
    private HashMap<Integer, Integer> active;
    private DisplayMetrics metrics = null;
    
    Bitmap bitmap_pad_left = null;
    Bitmap bitmap_pad_right = null;
    
    private Paint mPaint = new Paint();
    private Matrix matrix_left = new Matrix();
    private Matrix matrix_right = new Matrix();    
    private Paint paint = new Paint();
    private Paint apaint = new Paint();
    private Paint tpaint = new Paint();
    
    private float base_scale = 1.0f; 
    final float basewidth = 1920.0f;
    final float baseheight = 1080.0f;
    private float wscale; 
    private float hscale;  
    boolean testmode = false;
    private String status;
    
    public YabausePad(Context context) {
        super(context);
        init();
    }

    public YabausePad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public YabausePad(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    public void setScale( float scale ){
    	this.base_scale = scale;
    }
    
    public float getScale(){
    	return base_scale;
    }
    
    public void setTestmode( boolean test ){
    	this.testmode = test;
    }
    
    public String getStatusString(){
    	return status;
    }

    private void init() {

    	metrics = new DisplayMetrics();
    	Activity current = (Activity)getContext();
    	current.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(current);
        base_scale= sharedPref.getFloat("pref_pad_scale", 0.5f);
        
	    paint.setARGB(0x00, 0x0, 0x0, 0x0);
	    apaint.setARGB(0x00, 0x0, 0x00, 0x00);
	    tpaint.setARGB(0x80, 0xFF, 0xFF, 0xFF);
	
        setOnTouchListener(this);

         
        buttons = new PadButton[PadEvent.BUTTON_LAST];

        buttons[PadEvent.BUTTON_UP]    = new DPadButton();
        buttons[PadEvent.BUTTON_RIGHT] = new DPadButton();
        buttons[PadEvent.BUTTON_DOWN]  = new DPadButton();
        buttons[PadEvent.BUTTON_LEFT]  = new DPadButton();

        buttons[PadEvent.BUTTON_RIGHT_TRIGGER] = new DPadButton();
        buttons[PadEvent.BUTTON_LEFT_TRIGGER]  = new DPadButton();

        buttons[PadEvent.BUTTON_START] = new StartButton();

        buttons[PadEvent.BUTTON_A] = new ActionButton((int)(100), "", 40);
        buttons[PadEvent.BUTTON_B] = new ActionButton((int)(100), "", 40);
        buttons[PadEvent.BUTTON_C] = new ActionButton((int)(100), "", 40);

        buttons[PadEvent.BUTTON_X] = new ActionButton((int)(72), "", 25);
        buttons[PadEvent.BUTTON_Y] = new ActionButton((int)(72), "", 25);
        buttons[PadEvent.BUTTON_Z] = new ActionButton((int)(72), "", 25);

        active = new HashMap<Integer, Integer>();
        
        bitmap_pad_left = BitmapFactory.decodeResource(getResources(), R.drawable.pad_l);
        bitmap_pad_right= BitmapFactory.decodeResource(getResources(), R.drawable.pad_r);    
        
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mPaint.setDither(true);        
        
        
    }

    @Override public void onDraw(Canvas canvas) {

    	
       
        canvas.drawBitmap(bitmap_pad_left, matrix_left, mPaint);
        canvas.drawBitmap(bitmap_pad_right, matrix_right, mPaint);
        
        canvas.setMatrix(null);
        
         //canvas.save();
    	//canvas.concat(matrix_left);
        buttons[PadEvent.BUTTON_UP].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_DOWN].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_LEFT].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_RIGHT].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_START].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_LEFT_TRIGGER].draw(canvas, paint, apaint, tpaint);
        
        //canvas.restore();
        //canvas.concat(matrix_right);
        buttons[PadEvent.BUTTON_A].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_B].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_C].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_X].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_Y].draw(canvas, paint, apaint, tpaint);
        buttons[PadEvent.BUTTON_Z].draw(canvas, paint, apaint, tpaint);        
        buttons[PadEvent.BUTTON_RIGHT_TRIGGER].draw(canvas, paint, apaint, tpaint);

        
    }

    public void setOnPadListener(OnPadListener listener) {
        this.listener = listener;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int posx = (int) event.getX(index);
        int posy = (int) event.getY(index);
        PadEvent pe = null;

        float hitsize = 15.0f * wscale*base_scale; 
        RectF hittest = new RectF( (int)(posx - hitsize), (int)(posy - hitsize), (int)(posx+hitsize), (int)(posy+hitsize) );

        if ((action == event.ACTION_DOWN) || (action == event.ACTION_POINTER_DOWN) || (action == event.ACTION_MOVE) ) {
            for(int i = 0;i < PadEvent.BUTTON_LAST;i++) {
                if (buttons[i].intersects(hittest)) {
                    if(!testmode) YabauseRunnable.press(i,0);
                    buttons[i].On(index);
                    //invalidate();
                }else if( buttons[i].isOn(index) ){
                	if(!testmode) YabauseRunnable.release(i,0);
                  buttons[i].Off();
                  //invalidate();
                }
            }
        }

        if ( ((action == event.ACTION_UP) || (action == event.ACTION_POINTER_UP))) {
          for(int i = 0;i < PadEvent.BUTTON_LAST;i++) {
              if( buttons[i].isOn(index) ){
                buttons[i].Off();
                if(!testmode) YabauseRunnable.release(i,0);
                //invalidate();
              }
          }
        }
/*        
        for(int i = 0;i < PadEvent.BUTTON_LAST;i++) {
            if( buttons[i].isOn(index) ){
              buttons[i].Off();
              if(!testmode) YabauseRunnable.release(i);
              invalidate();
            }
        }       
*/        
        if( testmode ){
        	status = "";
        	status += "UP:";
        	if( buttons[PadEvent.BUTTON_UP].isOn() ) status += "ON "; else status += "OFF "; 
        	status += "DOWN:";
        	if( buttons[PadEvent.BUTTON_DOWN].isOn() ) status += "ON "; else status += "OFF "; 
        	status += "LEFT:";
        	if( buttons[PadEvent.BUTTON_LEFT].isOn() ) status += "ON "; else status += "OFF "; 
        	status += "RIGHT:";
        	if( buttons[PadEvent.BUTTON_RIGHT].isOn() ) status += "ON "; else status += "OFF "; 

        	status += "\nA:";
        	if( buttons[PadEvent.BUTTON_A].isOn() ) status += "ON "; else status += "OFF "; 
        	status += "B:";
        	if( buttons[PadEvent.BUTTON_B].isOn() ) status += "ON "; else status += "OFF "; 
        	status += "C:";
        	if( buttons[PadEvent.BUTTON_C].isOn() ) status += "ON "; else status += "OFF "; 
        	
        	status += "\nX:";
        	if( buttons[PadEvent.BUTTON_X].isOn() ) status += "ON "; else status += "OFF "; 
        	status += "Y:";
        	if( buttons[PadEvent.BUTTON_Y].isOn() ) status += "ON "; else status += "OFF "; 
        	status += "Z:";
        	if( buttons[PadEvent.BUTTON_Z].isOn() ) status += "ON "; else status += "OFF "; 
        	
        	status += "\nLT:";
        	if( buttons[PadEvent.BUTTON_LEFT_TRIGGER].isOn() ) status += "ON "; else status += "OFF "; 
        	status += "RT:";
        	if( buttons[PadEvent.BUTTON_RIGHT_TRIGGER].isOn() ) status += "ON "; else status += "OFF ";         	
        	
        }
 
        if ((listener != null) ) {
             listener.onPad(null);
            return true;
        }

        return true;
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        
        float dens = getResources().getDisplayMetrics().density;
        dens /= 2.0;
        
        wscale = (float)width / basewidth ; 
        hscale = (float)height / baseheight;    
        
        int bitmap_height = bitmap_pad_right.getHeight();
    	
        matrix_right.reset();
        matrix_right.postTranslate(-780, -baseheight);
        matrix_right.postScale(base_scale*wscale, base_scale*hscale);
        matrix_right.postTranslate(width, height);
        
        matrix_left.reset();
        matrix_left.postTranslate(0, -baseheight);
        matrix_left.postScale(base_scale*wscale, base_scale*hscale);
        matrix_left.postTranslate(0, height);

        // Left Part
        //buttons[PadEvent.BUTTON_UP].updateRect(matrix_left, 303, 497, 303+ 89,497+180);
        buttons[PadEvent.BUTTON_UP].updateRect(matrix_left, 130, 512, 130+ 429,512+151);
        //buttons[PadEvent.BUTTON_DOWN].updateRect(matrix_left,303,752,303+89,752+180);
        buttons[PadEvent.BUTTON_DOWN].updateRect(matrix_left,130,784,130+429,784+151);        
        //buttons[PadEvent.BUTTON_RIGHT].updateRect(matrix_left,392,671,392+162,671+93);
        buttons[PadEvent.BUTTON_RIGHT].updateRect(matrix_left,420,533,420+144,533+378);
        //buttons[PadEvent.BUTTON_LEFT].updateRect(matrix_left,141,671,141+162,671+93);
        buttons[PadEvent.BUTTON_LEFT].updateRect(matrix_left,148,533,148+144,533+378);
        buttons[PadEvent.BUTTON_LEFT_TRIGGER].updateRect(matrix_left,56,57,56+376,57+92);
        buttons[PadEvent.BUTTON_START].updateRect(matrix_left,510,1013,510+182,1013+57);

        // Right Part
        buttons[PadEvent.BUTTON_A].updateRect(matrix_right,59,801,59+213,801+225);
        buttons[PadEvent.BUTTON_A].updateScale(base_scale*wscale);
        buttons[PadEvent.BUTTON_B].updateRect(matrix_right,268,672,268+229,672+221);
        buttons[PadEvent.BUTTON_B].updateScale(base_scale*wscale);        
        buttons[PadEvent.BUTTON_C].updateRect(matrix_right,507,577,507+224,577+229);
        buttons[PadEvent.BUTTON_C].updateScale(base_scale*wscale);
        buttons[PadEvent.BUTTON_X].updateRect(matrix_right,15,602,15+149,602+150);
        buttons[PadEvent.BUTTON_X].updateScale(base_scale*wscale);
        buttons[PadEvent.BUTTON_Y].updateRect(matrix_right,202,481,202+149,481+148);
        buttons[PadEvent.BUTTON_Y].updateScale(base_scale*wscale);
        buttons[PadEvent.BUTTON_Z].updateRect(matrix_right,397,409,397+151,409+152);
        buttons[PadEvent.BUTTON_Z].updateScale(base_scale*wscale);
        buttons[PadEvent.BUTTON_RIGHT_TRIGGER].updateRect(matrix_right,350,59,350+379,59+91);
        
        matrix_right.reset();
        matrix_right.postTranslate(- bitmap_pad_right.getWidth(), - bitmap_pad_right.getHeight());
        matrix_right.postScale(base_scale*wscale/dens, base_scale*hscale/dens);
        matrix_right.postTranslate(width, height);
        
        matrix_left.reset();
        matrix_left.postTranslate(0, - bitmap_pad_right.getHeight());
        matrix_left.postScale(base_scale*wscale/dens, base_scale*hscale/dens);
        matrix_left.postTranslate(0, height);        
        
        setMeasuredDimension(width, height);
    }
}


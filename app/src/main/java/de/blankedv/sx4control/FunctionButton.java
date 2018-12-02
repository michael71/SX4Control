/*  (C) 2011-2015, Michael Blank
 * 
 *  This file is part of Lanbahn Throttle.

    Lanbahn Throttle is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Lanbahn Throttle is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with LanbahnThrottle.  If not, see <http://www.gnu.org/licenses/>.

*/
package de.blankedv.sx4control;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * adds an "LED" indicator or an Image with an ON and and OFF state
 * to the standard button
 */
public class FunctionButton extends AppCompatButton {

    public boolean active = true;
    public Bitmap im_on = null;
    public Bitmap im_off = null;

    protected boolean darken = false;
    protected boolean ON = true;


    public FunctionButton(Context context) {
        super(context);
    }

    public FunctionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (darken) return; // draw no image/LED in this case

        int w = this.getWidth();
        int h = this.getHeight();

        if ((im_on == null) || (im_off == null)) {

                if (ON) {
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setColor(Color.GRAY);
                    canvas.drawCircle(w / 5, h / 5, w / 10, paint);
                    paint.setColor(Color.YELLOW);
                    canvas.drawCircle(w / 5, h / 5, (w / 10 - 2), paint);
                } else {
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setColor(Color.GRAY);
                    canvas.drawCircle(w / 5, h / 5, w / 10, paint);
                }
           } else {
            float x = w / 2 - im_on.getWidth() / 2;
            float y = h / 2 - im_on.getHeight() / 2;
            if (ON) {
                canvas.drawBitmap(im_on, (float) x, (float) y, null);
            } else {
                canvas.drawBitmap(im_off, (float) x, (float) y, null);
            }
        }

    }

    public void deactivate() {
        setTextColor(Color.DKGRAY);
        darken = true;
    }

    public void activate() {
        setTextColor(Color.WHITE);
        darken = false;
    }

    public void setON(boolean state) {
        active = true;
        ON = state;
        invalidate();
    }


}

package de.blankedv.sx4control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SpeedBarView extends android.support.v7.widget.AppCompatImageView {

    Paint myPaint = new Paint();
    private int width;
    private int height;
    private float speed = 0;
    private String title;


    public SpeedBarView(Context context) {
        super(context);
    }

    public SpeedBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public SpeedBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        initialize();
    }


    public void initialize() {
        myPaint.setColor(Color.rgb(50, 100, 255));
        myPaint.setStrokeWidth(10);

    }

    public void diffSpeed(float diff) {
        if (Math.abs(diff) > 30) return;  // maybe erratic moves

        speed = speed + diff; /// or (diff/2f); =more turns needed
        if (speed > 1000f) speed = 1000f;
        if (speed < 0) speed = 0;
        invalidate();
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float newspeed) {
        speed = newspeed;
        if (speed > 1000f) speed = 1000f;
        if (speed < 0) speed = 0;
        invalidate();
    }

    public int getIntSpeed() {
        return (int) (speed / 10);
    }

    public void setIntSpeed(int ispeed) {
        speed = ispeed * 10f;
        if (speed > 1000f) speed = 1000f;
        if (speed < 0) speed = 0;
        invalidate();
    }

    protected void onDraw(Canvas c) {
        myPaint.setColor(Color.rgb(30, 30, 30));
        c.drawRect(0, 0, width, height, myPaint);
        int len = (int) Math.floor(speed);
        myPaint.setColor(Color.rgb(50, 100, 255));
        c.drawRect(0, (height * 0) / 10, ((15 + len) * width) / 1015, (height * 9) / 10, myPaint);
        c.drawRect(0, (height * 9) / 10, width, height, myPaint);

        myPaint.setColor(Color.rgb(255, 255, 255));
        myPaint.setTextSize(48f);
        int yPos = (int) ((height / 2) - ((myPaint.descent() + myPaint.ascent()) / 2));
        c.drawText(title, width * 1 / 3, yPos, myPaint);
        super.onDraw(c);
    }

    public void setTitle(String title) {
        this.title = title.substring(0, Math.min(title.length(), 15));
    }
}

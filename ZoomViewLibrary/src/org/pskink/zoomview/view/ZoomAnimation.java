package org.pskink.zoomview.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

public class ZoomAnimation extends Animation {
	private final static String TAG = "ZoomAnimation";
	
    private static final float MAX_ALPHA = 0.75f;
    private float mFrom;
    private float mTo;
    private int mAlpha;
	private ZoomView mScroll;
    private Path mPath;
    private Paint mPathPaint;

    public ZoomAnimation(ZoomView scroll) {
    	mScroll = scroll;
		int duration = 2 * scroll.getResources().getInteger(android.R.integer.config_longAnimTime);
        setDuration(duration);
        
		mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPathPaint.setStyle(Style.STROKE);
        mPathPaint.setStrokeCap(Cap.SQUARE);
        mPath = new Path();
        mPath.addCircle(0, 0, 16, Direction.CCW);
    }
    
    public void start(float scale, boolean zoomIn, float minScale, float maxScale) {
        mFrom = scale;
        mTo = scale < (maxScale + minScale) / 2? maxScale : minScale;
        start();
        long t = AnimationUtils.currentAnimationTimeMillis();
        getTransformation(t, null);
    }
    
    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float time = interpolatedTime;
        float alpha = (float) Math.sin(time * Math.PI);
        alpha = Math.min(alpha, MAX_ALPHA);
        mAlpha = (int) (255 * alpha / MAX_ALPHA);
        // i could use setInterpolator() but i also need to have linear alpha
        mScroll.setZoom(mFrom + (mTo - mFrom) * interpolatedTime, false);
    }

	public void drawFrame(Canvas canvas, float pivotX, float pivotY) {
        canvas.save();
        canvas.translate(pivotX, pivotY);
        // draw white outline
        mPathPaint.setStrokeWidth(10);
        mPathPaint.setColor(0xffffffff);
        mPathPaint.setAlpha(mAlpha);
        canvas.drawPath(mPath, mPathPaint);
        // draw black interior
        mPathPaint.setStrokeWidth(4);
        mPathPaint.setColor(0xff000000);
        mPathPaint.setAlpha(mAlpha);
        canvas.drawPath(mPath, mPathPaint);
        canvas.restore();
    }
}

package org.pskink.zoomview.view;

import org.pskink.zoomview.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Scroller;
import android.widget.ZoomButtonsController.OnZoomListener;

public class ZoomView extends View implements OnZoomListener {
    private final static String TAG = "ZoomView";
    private static final float MAX_SCALE = 1;

    private Scroller mScroller;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScale;
    private ScrollZoomButtonsController mZoomController;
    private Matrix mMatrix;
    private OnDrawListener mListener;
    private ZoomAnimation mZoomAnimation;
    private RectF mContent;
    private RectF mWindow;
    private RectF mScreen;
    private RectF mMappedContent;
    private RectF mDummy;
    private float[] mFocalPoints;
    private Drawable mDrawable;
    private float mMinScale;
    private boolean mSmall;
    
    public interface OnDrawListener {
        public void onDraw(Canvas canvas, Drawable d, Matrix matrix, RectF window);
    }
    
    public ZoomView(Context context) {
        super(context);
        init(context);
    }
    
    private void init(Context context) {
        mScroller = new Scroller(context);
        mGestureDetector = new GestureDetector(context, mGestureListener, null, true);
        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
        mZoomController = new ScrollZoomButtonsController(this);
        mZoomController.setText("Zoom: ", mScale);
        
        setVerticalScrollBarEnabled(true);
        setHorizontalScrollBarEnabled(true);
        
        mMatrix = new Matrix();
        mZoomAnimation = new ZoomAnimation(this);

        mContent = new RectF();
        mWindow = new RectF();
        mScreen = new RectF();
        mMappedContent = new RectF();
        mDummy = new RectF();
        mFocalPoints = new float[4];
    }

    public ZoomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XoomView);
        Drawable d = a.getDrawable(R.styleable.XoomView_src);
        if (d != null) {
            setImageDrawable(d);
        }
        a.recycle();
    }

    public void setImageDrawable(Drawable d) {
        if (d == null) {
            throw new NullPointerException("Drawable == null");
        }
        mDrawable = d;
        mDrawable.setCallback(this);
        requestLayout();
        invalidate();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mDrawable || super.verifyDrawable(who);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float w = getWidth();
        float h = getHeight();
        if (mDrawable != null && w != 0 && h != 0) {
            Rect bounds = mDrawable.getBounds();
            int dW = bounds.width();
            int dH = bounds.height();
            if (dW <= 0 || dH <= 0) {
                dW = mDrawable.getIntrinsicWidth();
                dH = mDrawable.getIntrinsicHeight();
            }
            if (dW <= 0 || dH <= 0) {
                String msg = "drawable bounds not set (setBounds method) or (getIntrinsicWidth() or getIntrinsicHeight()) returns <= 0";
                throw new IllegalArgumentException(msg);
            }

            float scale = Math.min(w / dW, h / dH);

            mScale = mMinScale = scale;

            // if mSmall = true: Drawable is smaller than View
            mSmall = mScale >= MAX_SCALE;

            if (bounds.isEmpty()) {
                mDrawable.setBounds(0, 0, dW, dH);
            }

            mContent.set(0, 0, dW, dH);
            mWindow.set(0, 0, w / scale, h / scale);
            mScreen.set(0, 0, w, h);
            // initialize mMatrix, equal to: setScale(scale, scale)
            mMatrix.setRectToRect(mWindow, mScreen, ScaleToFit.FILL);
            mMatrix.mapRect(mMappedContent, mContent);

            updateMatrix(mWindow);
        }
    }

    public void setOnDrawListener(OnDrawListener listener) {
        mListener = listener;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mZoomController.setVisible(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mSmall) {
            return false;
        }
        boolean res = mScaleGestureDetector.onTouchEvent(event);
        if (!mScaleGestureDetector.isInProgress()) {
            res = mGestureDetector.onTouchEvent(event);
        }
        return res;
    }
    
    @SuppressLint("WrongCall")
    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawable == null) {
            return;
        }
        
        boolean isAnimating = false;
        if (mZoomAnimation.hasStarted() && !mZoomAnimation.hasEnded()) {
            long t = AnimationUtils.currentAnimationTimeMillis();
            mZoomAnimation.getTransformation(t, null);
            isAnimating = true;
        }
        
        if (mListener != null) {
            mDummy.set(mWindow);
            canvas.save();
            mListener.onDraw(canvas, mDrawable, mMatrix, mDummy);
            canvas.restore();
        }

        canvas.save();
        canvas.concat(mMatrix);
        if (mListener == null) {
            mDrawable.draw(canvas);
        }
        if (isAnimating) {
            mZoomAnimation.drawFrame(canvas, mFocalPoints[0], mFocalPoints[1]);
        }
        canvas.restore();
    }


    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mWindow.offsetTo(mScroller.getCurrX(), mScroller.getCurrY());
            updateMatrix(mDummy);
            invalidate();
        }
    }

    private boolean updateMatrix(RectF out) {
        boolean check = false;
        float delta;
        final RectF c = mContent;
        final RectF w = mWindow;

        if (mMappedContent.width() > mScreen.width()) {
            // check if mWindow extends on the left or right
            if ((delta = c.left - w.left) > 0 || ((delta = c.right - w.right) < 0)) {
                check = true;
                w.offset(delta, 0);
            }
        } else {
            // center horizontally
            check = true;
            w.offsetTo((c.width() - w.width()) / 2, w.top);
        }
        if (mMappedContent.height() > mScreen.height()) {
            // check if mWindow extends on the top or bottom
            if ((delta = c.top - w.top) > 0 || ((delta = c.bottom - w.bottom) < 0)) {
                check = true;
                w.offset(0, delta);
            }
        } else {
            // center vertically
            check = true;
            w.offsetTo(w.left, (c.height() - w.height()) / 2);
        }
        mMatrix.setRectToRect(mWindow, mScreen, ScaleToFit.FILL);
        return check;
    }

    private SimpleOnScaleGestureListener mScaleGestureListener = new SimpleOnScaleGestureListener() {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            float x = mScaleGestureDetector.getFocusX();
            float y = mScaleGestureDetector.getFocusY();
            setFocalPoints(
                    mWindow.left + x / mScale, 
                    mWindow.top + y / mScale,
                    x / mScreen.width(),
                    y / mScreen.height());
            return true;
        }
        
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = mScaleGestureDetector.getScaleFactor();
            float scale = mScale;
            scale *= scaleFactor;
            if (mMinScale <= scale && scale <= MAX_SCALE) {
                setZoom(scale, true);
            }
            return true;
        }
    };
    
    private SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            mZoomController.setVisible(false);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            velocityX = mMappedContent.width() > mScreen.width()? -velocityX : 0;
            velocityY = mMappedContent.height() > mScreen.height()? -velocityY : 0;
            if (velocityX == 0 && velocityY == 0) {
                return false;
            }

            int minX, maxX, minY, maxY;
            minX = minY = Integer.MIN_VALUE;
            maxX = maxY = Integer.MAX_VALUE;
            if (velocityX != 0) {
                minX = 0;
                maxX = (int) (mContent.width() - mWindow.width());
            }
            if (velocityY != 0) {
                minY = 0;
                maxY = (int) (mContent.height() - mWindow.height());
            }
            mScroller.fling((int) mWindow.left, (int) mWindow.top, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY);
            invalidate();
            return true;
        }
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            distanceX = mMappedContent.width() > mScreen.width()? distanceX / mScale : 0;
            distanceY = mMappedContent.height() > mScreen.height()? distanceY / mScale : 0;
            if (distanceX == 0 && distanceY == 0) {
                return false;
            }
            mWindow.offset(distanceX, distanceY);
            updateMatrix(mWindow);
            invalidate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
//            Matrix inverse = new Matrix();
//            mMatrix.invert(inverse);
//            mPoints[0] = e.getX();
//            mPoints[1] = e.getY();
//            mPoints[2] = e.getX() / mScreen.width(); 
//            mPoints[3] = e.getY() / mScreen.height();
//            inverse.mapPoints(mPoints, 0, mPoints, 0, 1);
            setFocalPoints(
                    mWindow.left + e.getX() / mScale,
                    mWindow.top + e.getY() / mScale, 
                    e.getX() / mScreen.width(),
                    e.getY() / mScreen.height());

            mZoomAnimation.start(mScale, mScale < (MAX_SCALE + mMinScale) / 2, mMinScale, MAX_SCALE);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (!mScaleGestureDetector.isInProgress()) {
                mZoomController.setVisible(true);
            }
        }
    };

    @Override
    public void onVisibilityChanged(boolean visible) {
    }

    @Override
    public void onZoom(boolean zoomIn) {
        final float FACTOR = 0.5f;
        final float NUM_STEPS = 10;

        setFocalPoints(
                mWindow.left + mWindow.width() * FACTOR,
                mWindow.top + mWindow.height() * FACTOR,
                FACTOR,
                FACTOR);
        float delta = (MAX_SCALE - mMinScale) / NUM_STEPS;
        setZoom(mScale + (zoomIn? delta : -delta), true);
    }

    private void setFocalPoints(float x, float y, float fractionX, float fractionY) {
        mFocalPoints[0] = x;
        mFocalPoints[1] = y;
        mFocalPoints[2] = fractionX;
        mFocalPoints[3] = fractionY;
    }

    public void setZoom(float zoom, boolean adjust) {
        mScale = zoom;
        if (adjust) {
            mScale = Math.min(MAX_SCALE, Math.max(mMinScale, mScale));
        }
        float w = mScreen.width() / mScale;
        float h = mScreen.height() / mScale;
        // mFocalPoints[0, 1] focal in absolute pixels
        // mFocalPoints[2, 3] focal fraction of window size [0..1]
        float left = mFocalPoints[0] - w * mFocalPoints[2];
        float top = mFocalPoints[1] - h * mFocalPoints[3];
        mWindow.set(left, top, left + w, top + h);

        // set mMatrix scaling
        mMatrix.setRectToRect(mWindow, mScreen, ScaleToFit.FILL);
        mMatrix.mapRect(mMappedContent, mContent);
        
        updateMatrix(mWindow);
        invalidate();
        
        mZoomController.setText("Zoom: ", mScale);
        float EPSILON = 0.001f;
        mZoomController.setZoomInEnabled(Math.abs(mScale - MAX_SCALE) > EPSILON);
        mZoomController.setZoomOutEnabled(Math.abs(mScale - mMinScale) > EPSILON);
        invalidate();
    }
}

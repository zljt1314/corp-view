package com.ljt.lib_crop_view.crop;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.Scroller;

class ZoomImageViewGestureDetector {

    private PointF mLastPoint = new PointF();

    private PointF mMid = new PointF();

    private float mLastScale = 1f;

    private Scroller mScroller;

    private Runnable mScrollRunnable;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private ZoomImageView mImageView;

    public ZoomImageViewGestureDetector(Context context, ZoomImageView imageView) {
        mImageView = imageView;
        mScroller = new Scroller(context);
        initGestureDetector(context);
    }

    private void initGestureDetector(Context context) {
        mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                float scale = mImageView.getScale();
                if (scale < 1f) {
                    mImageView.zoomTo(1f, mMid.x, mMid.y, 200);
                } else if (scale >= mImageView.getMaxZoom()) {
                    mImageView.zoomTo(mImageView.getMaxZoom(), mMid.x, mMid.y, 200);
                }
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                mLastScale = mImageView.getScale();
                mMid.set(detector.getFocusX(), detector.getFocusY());
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mImageView.zoomTo(mLastScale * detector.getScaleFactor(), mMid.x, mMid.y);
                return false;
            }
        });
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                mImageView.onClick();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mImageView.getScale() > 1.0f) {
                    mImageView.zoomTo(1.0f);
                } else {
                    mImageView.zoomTo(2.0f);
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                int edge = mImageView.postTranslateCenter(-distanceX, -distanceY);
                if (!mImageView.inViewPager()) {
                    return true;
                }
                boolean beginDrag = false;
                if ((edge & (ZoomImageView.LEFT_EDGE | ZoomImageView.RIGHT_EDGE)) != ZoomImageView.NONE) {
                    beginDrag = true;
                } else if ((edge & ZoomImageView.LEFT_EDGE) != ZoomImageView.NONE && distanceX < 0) {
                    beginDrag = true;
                } else if ((edge & ZoomImageView.RIGHT_EDGE) != ZoomImageView.NONE && distanceX > 0) {
                    beginDrag = true;
                }
                // 让ViewPager去处理拖动事件
                if (beginDrag) {
                    ((ViewGroup) mImageView.getParent()).requestDisallowInterceptTouchEvent(false);
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                fling(mImageView, (int) -velocityX, (int) -velocityY);
                return false;
            }
        });
    }

    public void computeScroll(final ZoomImageView view) {

    }

    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        return true;
    }

    private void fling(final ZoomImageView view, int xVelocity, int yVelocity) {
        RectF r = view.getMapRect();
        int initialX = 0;
        int initialY = 0;
        // 当xVelocity >= 0时minX为0，当xVelocity < 0时minX为view宽-图片宽的负值(view宽<图片宽)
        int minX = xVelocity >= 0 ? 0 : -(int) Math.max(0, r.width() - view.getWidth());
        // 当xVelocity <= 0时maxX为0，当xVelocity > 0时minX为view宽-图片宽(view宽<图片宽)
        int maxX = xVelocity > 0 ? (int) Math.max(0, r.width() - view.getWidth()) : 0;
        // 同minX
        int minY = yVelocity > 0 ? 0 : -(int) Math.max(0, r.height() - view.getHeight());
        // 同maxX
        int maxY = yVelocity > 0 ? (int) Math.max(0, r.height() - view.getHeight()) : 0;
        mLastPoint.set(initialX, initialY);
        mScroller.fling(initialX, initialY, xVelocity, yVelocity, minX, maxX, minY, maxY);
        mScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (mScroller.computeScrollOffset()) {
                    final float transX = mScroller.getCurrX() - mLastPoint.x;
                    final float transY = mScroller.getCurrY() - mLastPoint.y;
                    mLastPoint.set(mScroller.getCurrX(), mScroller.getCurrY());
                    view.postTranslateCenter(-transX, -transY);
                    view.post(this);
                } else {
                    endFling(view);
                }
            }
        };
        view.post(mScrollRunnable);
        view.invalidate();
    }

    private void endFling(final ZoomImageView view) {
        mScroller.forceFinished(true);
        if (mScrollRunnable != null) {
            view.removeCallbacks(mScrollRunnable);
            mScrollRunnable = null;
        }
    }

}

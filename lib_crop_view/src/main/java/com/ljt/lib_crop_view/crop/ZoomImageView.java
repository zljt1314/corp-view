/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ljt.lib_crop_view.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * 可缩放ImageView
 *
 * @author lijintao <br/>
 */
public class ZoomImageView extends AppCompatImageView {

    @SuppressWarnings("unused")
    private static final String TAG = "ZoomImageView";

    protected static final int NONE = 0;
    protected static final int LEFT_EDGE = 1;
    protected static final int RIGHT_EDGE = 2;
    protected static final int TOP_EDGE = 4;
    protected static final int BOTTOM_EDGE = 8;

    //这是最初用来显示图像的基变换
    //当前的计算显示了图像的全貌，
    //根据需要进行邮件装箱。可以选择以裁剪的方式显示图像。
    //当我们从缩略图图像到全尺寸图像时，这个矩阵被重新计算。
    protected Matrix mBaseMatrix = new Matrix();

    //这是一个补充变换，它反映了用户在缩放和平移方面所做的工作。
    //当我们从缩略图到全尺寸图像时，这个矩阵保持不变。
    protected Matrix mSuppMatrix = new Matrix();

    // 这是最后一个矩阵，它被计算为基本矩阵和补充矩阵的合并。
    private final Matrix mDisplayMatrix = new Matrix();

    // 从矩阵中得到尺度因子。用于从矩阵中提取值的临时缓冲区。
    private final float[] mMatrixValues = new float[9];

    // 正在显示的当前位图。
    protected final RotateBitmap mBitmapDisplayed = new RotateBitmap(null);

    private int mThisWidth = -1;
    private int mThisHeight = -1;

    private float mMaxZoom;

    private ZoomImageViewGestureDetector mTouchDetector;

    private boolean mInViewPager;

    private RectF mMapRect = new RectF();

    private View.OnClickListener mClickListener;

    private SetImageTask mSetImageTask = null;

    private boolean mZoomable = true;

    /**
     * 当ImageView宽高为0时，先保存下操作，在layout时进行设置 com.baidu.netdisk.ui.widget.SetImageBitmapTask
     */
    private interface SetImageTask {
        public void setImageAfterLayout();
    }

    // ZoomImageView会将一个位图传递给回收商，如果它已经完成了对这个位图的使用。
    public interface Recycler {
        public void recycle(Bitmap b);
    }

    public void setRecycler(Recycler r) {
        mRecycler = r;
    }

    private Recycler mRecycler;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mThisWidth = right - left;
        mThisHeight = bottom - top;
        SetImageTask task = mSetImageTask;
        if (task != null) {
            mSetImageTask = null;
            task.setImageAfterLayout();
        }
        if (mBitmapDisplayed.getBitmap() != null) {
            getProperBaseMatrix(mBitmapDisplayed, mBaseMatrix);
            setImageMatrix(getImageViewMatrix());
            center(true, true);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            boolean inZoom = event.getPointerCount() >= 2 || getScale() != 1;
            ((ViewGroup) getParent()).requestDisallowInterceptTouchEvent(inZoom);
        }

        return mTouchDetector.onTouchEvent(event);
    }

    @Override
    public void setOnClickListener(View.OnClickListener listener) {
        mClickListener = listener;
    }

    /**
     * 单击事件
     */
    public void onClick() {
        if (mClickListener != null) {
            mClickListener.onClick(this);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        mTouchDetector.computeScroll(this);
    }

    public void setInViewPager(boolean inViewPager) {
        mInViewPager = inViewPager;
    }

    public boolean inViewPager() {
        return mInViewPager;
    }

    public void setZoomable(boolean enabled) {
        mZoomable = enabled;
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        setImageBitmapResetBase(bitmap, true);
    }

    /**
     * 为解决ImageLoader模块根据scaleType确定decode图片的大小，如果为Matrix，则返回更大的图片，并不影响真实ImageView 的scaleType
     *
     * @return
     * @see ImageView#getScaleType()
     */
    @Override
    public ImageView.ScaleType getScaleType() {
        return ImageView.ScaleType.FIT_CENTER;
    }

    @Override
    public void setImageResource(int resId) {
        setImageBitmap(BitmapFactory.decodeResource(getResources(), resId));
    }

    private void setImageBitmap(Bitmap bitmap, int rotation) {
        super.setImageBitmap(bitmap);
        Drawable d = getDrawable();
        if (d != null) {
            d.setDither(true);
        }

        Bitmap old = mBitmapDisplayed.getBitmap();
        mBitmapDisplayed.setBitmap(bitmap);
        mBitmapDisplayed.setRotation(rotation);

        if (old != null && old != bitmap && mRecycler != null) {
            mRecycler.recycle(old);
        }
    }

    public void clear() {
        setImageBitmapResetBase(null, true);
    }

    /**
     * 重置ImageView
     */
    public void resetImageView() {
        setImageBitmap(getDisplayBitmap());
    }

    // 该函数改变位图，根据位图的大小重置基矩阵，并可选择重置补充矩阵。
    private void setImageBitmapResetBase(final Bitmap bitmap, final boolean resetSupp) {
        setImageRotateBitmapResetBase(new RotateBitmap(bitmap), resetSupp);
    }

    private void setImageRotateBitmapResetBase(final RotateBitmap bitmap, final boolean resetSupp) {
        final int viewWidth = getWidth();
        if (viewWidth <= 0) {
            createSetImageTask(bitmap, resetSupp);
            return;
        }

        if (bitmap.getBitmap() != null) {
            getProperBaseMatrix(bitmap, mBaseMatrix);
            setImageBitmap(bitmap.getBitmap(), bitmap.getRotation());
        } else {
            mBaseMatrix.reset();
            setImageBitmap(null, 0);
        }

        if (resetSupp) {
            mSuppMatrix.reset();
        }
        setImageMatrix(getImageViewMatrix());
        mMaxZoom = maxZoom();
    }

    private void createSetImageTask(final RotateBitmap bitmap, final boolean resetSupp) {
        mSetImageTask = new SetImageTask() {

            @Override
            public void setImageAfterLayout() {
                setImageRotateBitmapResetBase(bitmap, resetSupp);
            }
        };
    }

    // 尽可能在一个或两个轴上居中。定心的定义如下:如果图像按比例缩小到视图的尺寸以下，那么将其居中(字面意思)。
    // 如果图像缩放到比视图大，并被转换为视图，则将其转换回视图(即消除黑条)。
    protected int center(boolean horizontal, boolean vertical) {
        if (mBitmapDisplayed.getBitmap() == null) {
            return TOP_EDGE | LEFT_EDGE | BOTTOM_EDGE | RIGHT_EDGE;
        }

        RectF rect = getMapRect();

        float height = rect.height();
        float width = rect.width();

        float deltaX = 0, deltaY = 0;

        int edge = NONE;
        if (vertical) {
            int viewHeight = getHeight();
            if (height <= viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top;
                edge |= (TOP_EDGE | BOTTOM_EDGE);
            } else if (rect.top >= 0) {
                deltaY = -rect.top;
                edge |= TOP_EDGE;
            } else if (rect.bottom <= viewHeight) {
                deltaY = getHeight() - rect.bottom;
                edge |= BOTTOM_EDGE;
            }
        }

        if (horizontal) {
            int viewWidth = getWidth();
            if (width <= viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left;
                edge |= (LEFT_EDGE | RIGHT_EDGE);
            } else if (rect.left >= 0) {
                deltaX = -rect.left;
                edge |= LEFT_EDGE;
            } else if (rect.right <= viewWidth) {
                deltaX = viewWidth - rect.right;
                edge |= RIGHT_EDGE;
            }
        }

        postTranslate(deltaX, deltaY);
        setImageMatrix(getImageViewMatrix());
        return edge;
    }

    protected RectF getMapRect() {
        mMapRect.setEmpty();

        if (mBitmapDisplayed.getBitmap() == null) {
            return mMapRect;
        }

        Matrix m = getImageViewMatrix();

        mMapRect.set(0, 0, mBitmapDisplayed.getBitmap().getWidth(), mBitmapDisplayed.getBitmap().getHeight());

        m.mapRect(mMapRect);
        return mMapRect;
    }

    protected void center(final boolean horizontal, final boolean vertical, final float durationMs) {
        if (mBitmapDisplayed.getBitmap() == null) {
            return;
        }

        Matrix m = getImageViewMatrix();

        RectF rect = new RectF(0, 0, mBitmapDisplayed.getBitmap().getWidth(), mBitmapDisplayed.getBitmap().getHeight());

        m.mapRect(rect);

        float height = rect.height();
        float width = rect.width();

        float deltaX = 0, deltaY = 0;

        final PointF orig = new PointF();
        final PointF old = new PointF();
        if (vertical) {
            int viewHeight = getHeight();
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top;
                orig.y = rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
                orig.y = rect.top;
            } else if (rect.bottom < viewHeight) {
                deltaY = getHeight() - rect.bottom;
                orig.y = rect.bottom;
            }
        }

        if (horizontal) {
            int viewWidth = getWidth();
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left;
                orig.x = rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
                orig.x = rect.left;
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right;
                orig.x = rect.right;
            }
        }

        old.set(orig);
        final float incrementXPerMs = deltaX / durationMs;
        final float incrementYPerMs = deltaY / durationMs;

        final long startTime = System.currentTimeMillis();

        post(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                float currentMs = Math.min(durationMs, now - startTime);
                float targetX = orig.x + (incrementXPerMs * currentMs) - old.x;
                float targetY = orig.y + (incrementYPerMs * currentMs) - old.y;
                postTranslate(targetX, targetY);
                setImageMatrix(getImageViewMatrix());
                // postTranslateCenter(targetX, targetY);

                old.x += targetX;
                old.y += targetY;

                if (currentMs < durationMs) {
                    post(this);
                }
            }
        });

    }

    public ZoomImageView(Context context) {
        super(context);
        init();
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setScaleType(ImageView.ScaleType.MATRIX);
        mTouchDetector = new ZoomImageViewGestureDetector(getContext(), this);
    }

    protected float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    // 从矩阵中得到尺度因子。
    protected float getScale(Matrix matrix) {
        return getValue(matrix, Matrix.MSCALE_X);
    }

    public float getScale() {
        return getScale(mSuppMatrix);
    }

    protected float getTransX(Matrix matrix) {
        return getValue(matrix, Matrix.MTRANS_X);
    }

    protected float getTransY(Matrix matrix) {
        return getValue(matrix, Matrix.MTRANS_Y);
    }

    protected float getTransX() {
        return getTransX(mSuppMatrix);
    }

    protected float getTransY() {
        return getTransY(mSuppMatrix);
    }

    // todo 设置基本矩阵，使图像的中心和缩放适当。
    protected void getProperBaseMatrix(RotateBitmap bitmap, Matrix matrix) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float w = bitmap.getWidth();
        float h = bitmap.getHeight();
        matrix.reset();

        // 我们将缩放比例限制为3x，否则如果是小图标，结果可能会很糟糕。
        float scale;
        if (h * viewWidth > w * viewHeight) { // 当图片高宽比大于屏幕高宽比，为保证图片显示完整，
            scale = viewHeight / h;
        } else {
            scale = viewWidth / w;
        }

        matrix.postConcat(bitmap.getRotateMatrix());
        matrix.postScale(scale, scale);

        float transX = (viewWidth - w * scale) / 2F;
        float transY = (viewHeight - h * scale) / 2F;
        if (transY < 0) {
            transY = 0;
        }

        matrix.postTranslate(transX, transY);
    }

    // 将基本矩阵与supp矩阵组合成最终矩阵。
    protected Matrix getImageViewMatrix() {
        // 最后的矩阵被计算为基矩阵和补充矩阵的合并。
        mDisplayMatrix.set(mBaseMatrix);
        mDisplayMatrix.postConcat(mSuppMatrix);
        return mDisplayMatrix;
    }

    static final float SCALE_RATE = 1.25F;

    // 设置最大缩放，这是一个相对于基本矩阵的缩放比例。它的计算显示图像在400%缩放，无论屏幕或图像方向。
    // 如果我们在未来解码完整的300万像素图像，而不是目前的1024x768，这应该被修改为200%。
    protected float maxZoom() {
        if (mMaxZoom > 0) {
            return mMaxZoom;
        }
        if (mBitmapDisplayed.getBitmap() == null) {
            return 1F;
        }

        float fw = (float) mBitmapDisplayed.getWidth() / (float) mThisWidth;
        float fh = (float) mBitmapDisplayed.getHeight() / (float) mThisHeight;
        float max = Math.max(fw, fh) * 4;
        return Math.max(max, 1.0f);
    }

    public void setMaxZoom(float maxZoom) {
        mMaxZoom = maxZoom;
    }

    protected void zoomTo(float scale, float centerX, float centerY) {
        // 稍后我们将缩放到maxzoom
        if (!mZoomable) {
            return;
        }
        float oldScale = getScale();
        float deltaScale = scale / oldScale;

        mSuppMatrix.postScale(deltaScale, deltaScale, centerX, centerY);
        setImageMatrix(getImageViewMatrix());
        center(true, true);
    }

    void zoomTo(final float scale, final float centerX, final float centerY, final float durationMs) {
        final float incrementPerMs = (scale - getScale()) / durationMs;
        final float oldScale = getScale();
        final long startTime = System.currentTimeMillis();

        post(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                float currentMs = Math.min(durationMs, now - startTime);
                float target = oldScale + (incrementPerMs * currentMs);
                zoomTo(target, centerX, centerY);

                if (currentMs < durationMs) {
                    post(this);
                }
            }
        });
    }

    public void zoomTo(float scale) {
        float cx = getWidth() / 2F;
        float cy = getHeight() / 2F;

        zoomTo(scale, cx, cy, 200);
    }

    protected void zoomIn() {
        zoomIn(SCALE_RATE);
    }

    protected void zoomOut() {
        zoomOut(SCALE_RATE);
    }

    protected void zoomIn(float rate) {
        if (getScale() >= mMaxZoom) {
            return; // 不要让用户放大到分子级别。
        }
        if (mBitmapDisplayed.getBitmap() == null) {
            return;
        }

        float cx = getWidth() / 2F;
        float cy = getHeight() / 2F;

        mSuppMatrix.postScale(rate, rate, cx, cy);
        setImageMatrix(getImageViewMatrix());
    }

    protected void zoomOut(float rate) {
        if (mBitmapDisplayed.getBitmap() == null) {
            return;
        }

        float cx = getWidth() / 2F;
        float cy = getHeight() / 2F;

        // 缩小到最多1x。
        Matrix tmp = new Matrix(mSuppMatrix);
        tmp.postScale(1F / rate, 1F / rate, cx, cy);

        if (getScale(tmp) < 1F) {
            mSuppMatrix.setScale(1F, 1F, cx, cy);
        } else {
            mSuppMatrix.postScale(1F / rate, 1F / rate, cx, cy);
        }
        setImageMatrix(getImageViewMatrix());
        center(true, true);
    }

    protected void postTranslate(float dx, float dy) {
        mSuppMatrix.postTranslate(dx, dy);
    }

    protected void panBy(float dx, float dy) {
        postTranslate(dx, dy);
        setImageMatrix(getImageViewMatrix());
    }

    protected int postTranslateCenter(float dx, float dy) {
        postTranslate(dx, dy);
        return center(true, true);
    }

    protected float getMaxZoom() {
        return mMaxZoom;
    }

    protected boolean isReady() {
        return mBitmapDisplayed.getBitmap() != null;
    }

    public Bitmap getDisplayBitmap() {
        return mBitmapDisplayed.getBitmap();
    }

    public static class RotateBitmap {
        public static final String TAG = "RotateBitmap";
        private Bitmap mBitmap;
        private int mRotation;

        public RotateBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            mRotation = 0;
        }

        public RotateBitmap(Bitmap bitmap, int rotation) {
            mBitmap = bitmap;
            mRotation = rotation % 360;
        }

        public void setRotation(int rotation) {
            mRotation = rotation;
        }

        public int getRotation() {
            return mRotation;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public void setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public Matrix getRotateMatrix() {
            // 默认情况下，这是一个单位矩阵
            Matrix matrix = new Matrix();
            if (mRotation != 0 && mBitmap != null) {
                //我们想在原点做旋转，但是因为边界
                //矩形在旋转后会改变，所以值
                //按新旧宽度/高度分别计算。
                int cx = mBitmap.getWidth() / 2;
                int cy = mBitmap.getHeight() / 2;
                matrix.preTranslate(-cx, -cy);
                matrix.postRotate(mRotation);
                matrix.postTranslate(getWidth() / 2f, getHeight() / 2f);
            }
            return matrix;
        }

        public boolean isOrientationChanged() {
            return (mRotation / 90) % 2 != 0;
        }

        public int getHeight() {
            if (isOrientationChanged()) {
                return mBitmap.getWidth();
            } else {
                return mBitmap.getHeight();
            }
        }

        public int getWidth() {
            if (isOrientationChanged()) {
                return mBitmap.getHeight();
            } else {
                return mBitmap.getWidth();
            }
        }

        public void recycle() {
            if (mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }
        }
    }
}

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
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * CropImageView
 * 
 * @author lijintao <br/>
 */
public class CropImageView extends ZoomImageView {

    public CropImageView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected float maxZoom() {
        super.maxZoom();
        if (mBitmapDisplayed.getBitmap() == null) {
            return 1F;
        }

        float fw = (float) mBitmapDisplayed.getWidth() / 250.0f;
        float fh = (float) mBitmapDisplayed.getHeight() / 250.0f;
        float max = Math.min(fw, fh);
        return Math.max(max, 1.0f);
    }

    @Override
    protected void getProperBaseMatrix(RotateBitmap bitmap, Matrix matrix) {
        float viewHeight = getHeight();
        float viewWidth = getCropRight() - getCropLeft();// 取剪裁框的宽度
        if (viewWidth == 0) {
            viewWidth = getWidth();
        }

        float w = bitmap.getWidth();
        float h = bitmap.getHeight();
        matrix.reset();
        float ratio = Math.min(w, h);

        // 我们将缩放比例限制为3x，否则如果是小图标，结果可能会很糟糕。
        float scale = viewWidth / ratio;

        matrix.postConcat(bitmap.getRotateMatrix());
        matrix.postScale(scale, scale);

        float transX = (viewWidth - w * scale) / 2F;
        float transY = (viewHeight - h * scale) / 2F;
        if (transY < 0) {
            transY = 0;
        }

        matrix.postTranslate(transX, transY);
    }

    private ClipView mCropView;

    @Override
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
            int viewHeight = getCropBottom() - getCropTop();
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top + getCropTop();
                edge |= (TOP_EDGE | BOTTOM_EDGE);
            } else if (rect.top > getCropTop()) {
                deltaY = getCropTop() - rect.top;
                edge |= TOP_EDGE;
            } else if (rect.bottom < getCropBottom()) {
                deltaY = getCropBottom() - rect.bottom;
                edge |= BOTTOM_EDGE;
            }
        }

        if (horizontal) {
            int viewWidth = getCropRight() - getCropLeft();
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left + getCropLeft();
                edge |= (LEFT_EDGE | RIGHT_EDGE);
            } else if (rect.left > getCropLeft()) {
                deltaX = -rect.left + getCropLeft();
                edge |= LEFT_EDGE;
            } else if (rect.right < getCropRight()) {
                deltaX = -rect.right + getCropRight();
                edge |= RIGHT_EDGE;
            }
        }

        postTranslate(deltaX, deltaY);
        setImageMatrix(getImageViewMatrix());
        return edge;
    }

    public Rect getMapCropRect() {
        Matrix m = getImageViewMatrix();
        Matrix m2 = new Matrix();
        m2.reset();
        m.invert(m2);
        RectF rectf = new RectF(getCropLeft(), getCropTop(), getCropRight(), getCropBottom());
        m2.mapRect(rectf);
        Rect rect = new Rect((int) rectf.left, (int) rectf.top, (int) rectf.right, (int) rectf.bottom);
        rect.bottom = rect.top + rect.right - rect.left;
        return rect;

    }

    @Override
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

            int viewHeight = getCropBottom() - getCropTop();
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top + getCropTop();
                orig.y = rect.top;
            } else if (rect.top > getCropTop()) {
                deltaY = getCropTop() - rect.top;
                orig.y = rect.top;
            } else if (rect.bottom < getCropBottom()) {
                deltaY = getCropBottom() - rect.bottom;
                orig.y = rect.bottom;
            }
        }

        if (horizontal) {
            int viewWidth = getCropRight() - getCropLeft();
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left + getCropLeft();
                orig.x = rect.left;
            } else if (rect.left > getCropLeft()) {
                deltaX = -rect.left + getCropLeft();
                orig.x = rect.left;
            } else if (rect.right < getCropRight()) {
                deltaX = getCropRight() - rect.right;
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

                old.x += targetX;
                old.y += targetY;

                if (currentMs < durationMs) {
                    post(this);
                }
            }
        });

    }

    /**
     * 设置裁剪框view
     * @param view
     */
    public void setCropView(ClipView view) {
        mCropView = view;
    }

    /**
     * 由于top 是相对parent的位置，如果其parent不是充满到顶部，取parent的top
     * 
     * @return
     */
    protected int getCropTop() {
        View view = (View) mCropView.getParent();
        return view.getTop();
    }

    /**
     * 由于bottom 是相对parent的位置，如果其parent不是充满到底部，取parent的bottom
     * 
     * @return
     */
    protected int getCropBottom() {
        View view = (View) mCropView.getParent();
        return view.getBottom();
    }

    protected int getCropLeft() {
        return mCropView.getLeft();
    }

    protected int getCropRight() {
        return mCropView.getRight();
    }

    public int dip2px(Context context, double dpValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5);
    }

}

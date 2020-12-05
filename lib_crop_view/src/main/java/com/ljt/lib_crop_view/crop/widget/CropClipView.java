package com.ljt.lib_crop_view.crop.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.ljt.lib_crop_view.R;
import com.ljt.lib_crop_view.crop.ClipView;
import com.ljt.lib_crop_view.crop.CropImageView;

/**
 * @author lijintao
 */
public class CropClipView extends RelativeLayout {

    private CropImageView cropImageView;
    private ClipView clipView;

    public CropClipView(Context context) {
        this(context, null, 0);
    }

    public CropClipView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropClipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.view_crop_clip, this);

        cropImageView = findViewById(R.id.v_crop_iv);
        clipView = findViewById(R.id.v_clip_v);

        clipView.post(new Runnable() {
            @Override
            public void run() {
                cropImageView.setCropView(clipView);
                clipView.setClipBorderWidth(4);
                cropImageView.setImageResource(R.mipmap.test);
            }
        });
    }

}

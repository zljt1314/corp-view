/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */
package com.ljt.lib_crop_view.crop;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ImageUtil
 *
 * @author lijintao <br/>
 */
public class ImageUtil {

    private static final String TAG = "ImageUtil";

    public static Bitmap zoomImage(Context context, Bitmap bgimage, double newWidth, double newHeight) {
        // 获取这个图片的宽和高
        final float width = bgimage.getWidth();
        final float height = bgimage.getHeight();
        // 创建操作图片用的matrix对象
        final Matrix matrix = new Matrix();
        // 计算宽高缩放率
        final float scaleWidth = (float) newWidth / width;
        final float scaleHeight = (float) newHeight / height;
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        final Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width, (int) height, matrix, true);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        bitmap.setDensity(dm.densityDpi);
        return bitmap;
    }

    /**
     * @return 创建图片文件名，以日期作为文件名
     */
    public static String createImageName() {
        long dateTaken = System.currentTimeMillis();
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return dateFormat.format(date);
    }

    /**
     * 缩放图片
     *
     * @param bitmap
     * @param w
     * @param h
     * @return
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int w, int h) {

        if (bitmap == null) {
            return null;
        }
        return Bitmap.createScaledBitmap(bitmap, w, h, false);
    }

    /**
     * 对图片进行混合处理
     *
     * @param top
     * @param bottom
     * @return
     */
    public static Bitmap mixBitmap(Bitmap top, int topWidth, int topHeight, Bitmap bottom, int bottomWidth,
                                   int bottomHeight) {
        try {
            top = zoomBitmap(top, topWidth, topHeight);
            bottom = zoomBitmap(bottom, bottomWidth, bottomHeight);
            Bitmap bmp = createBitmap(bottomWidth, bottomHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = null;
            if (bmp != null) {
                canvas = new Canvas(bmp);
            }
            if (canvas != null) {
                canvas.drawColor(0x00ffffff);
                RectF dst = new RectF(0, 0, bottomWidth, bottomHeight);
                Rect src = new Rect(0, 0, bottomWidth, bottomHeight);
                canvas.drawBitmap(bottom, src, dst, new Paint());
                bottom.recycle();
                int xOffset = (bottomWidth - topWidth) / 2;
                int yOffset = (bottomHeight - topHeight) / 2;
                dst = new RectF(xOffset, yOffset, topWidth + xOffset, topHeight + yOffset);
                src = new Rect(0, 0, topWidth, topHeight);
                canvas.drawBitmap(top, src, dst, new Paint());
            }
            if (top != null) {
                top.recycle();
            }
            return bmp;
        } catch (OutOfMemoryError e) {
            // TODO: handle exception

        }
        return bottom;
    }

    /**
     * 剪裁图片
     *
     * @param bitmap
     * @param r
     * @return
     */
    public static Bitmap cropBitmap(Bitmap bitmap, Rect r) {
        int width = r.right - r.left;
        int height = r.bottom - r.top;
        Bitmap bmp = createBitmap(width, height, Bitmap.Config.RGB_565);
        RectF dst = new RectF(0, 0, width, height);
        Canvas canvas = new Canvas(bmp);
        final Paint paint = new Paint();
        canvas.drawBitmap(bitmap, r, dst, paint);
        return bmp;
    }

    /**
     * 对图片进行居中剪裁
     *
     * @param bitmap
     * @return
     */
    public static Bitmap centerCropBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w == h) {
            return bitmap;
        }
        Rect rect = null;
        if (w > h) {
            rect = new Rect((w - h) / 2, 0, (w + h) / 2, h);
        } else {
            rect = new Rect(0, (h - w) / 2, w, (h + w) / 2);
        }
        return cropBitmap(bitmap, rect);

    }

    /**
     * 返回圆角图片
     *
     * @param imagePath
     * @param round
     * @param size
     * @return
     */
    public static Bitmap getRoundCorner(String imagePath, int round, int size) {
        Bitmap bitmap = decodeFile(imagePath, size * 2);
        bitmap = centerCropBitmap(bitmap);
        bitmap = getZoomRoundedCornerBitmap(bitmap, round, size, size);
        return bitmap;
    }

    /**
     * 对图片进行旋转处理
     *
     * @param bitmap
     * @param degrees
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        if (bitmap == null || degrees == 0) {
            return bitmap;
        }
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            return createBitmap(bitmap, 0, 0, width, height, matrix, true);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 对图片进行镜像处理
     *
     * @param bitmap
     * @param degrees
     * @return
     */
    public static Bitmap rotateMirrorBitmap(Bitmap bitmap, float degrees) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        matrix.postScale(-1, 1);
        return createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    /**
     * 旋转并缩放图片
     *
     * @param bitmap
     * @param degrees
     * @param w
     * @param h
     * @return
     */
    public static Bitmap zoomRoateBitmap(Bitmap bitmap, float degrees, int w, int h) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidht = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidht, scaleHeight);
        matrix.postRotate(degrees);
        return createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    /**
     * 将Drawable转化为Bitmap
     *
     * @param drawable
     * @return
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = createBitmap(width, height,
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * 获取圆形图片
     *
     * @param bitmap
     * @return
     */
    public static Bitmap getRoundedBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff000000;
        final Paint paint = new Paint(1);
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * 获取圆形图片同时进行缩放
     *
     * @param bitmap
     * @param roundPx
     * @param w
     * @param h
     * @return
     */
    public static Bitmap getZoomRoundedCornerBitmap(Bitmap bitmap, float roundPx, int w, int h) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidht = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidht, scaleHeight);
        Bitmap bm = createBitmap(bitmap, 0, 0, width, height, matrix, true);
        int bmW = 0;
        int bmH = 0;
        if (bm != null) {
            bmW = bm.getWidth();
            bmH = bm.getHeight();
        }
        Bitmap output = createBitmap(bmW, bmH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bmW, bmH);
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bm, rect, rect, paint);
        return output;
    }

    /**
     * 裁切图片
     *
     * @param src 原图
     * @param w   新图的宽
     * @param h   新图的高
     * @return Bitmap
     */
    public static Bitmap cutBitmap(Bitmap src, int w, int h) {
        if (src == null) {
            return null;
        }

        int imgW = src.getWidth();
        int imgH = src.getHeight();
        int sx = w >= imgW ? 0 : ((imgW - w) / 2);
        int sy = h >= imgH ? 0 : ((imgH - h) / 2);
        return createBitmap(src, sx, sy, sx == 0 ? imgW : w, sy == 0 ? imgH : h);
    }

    /**
     * 以最省内存的方式读取本地资源的图片
     *
     * @param context
     * @param resId
     * @return
     */
    public static Bitmap readBitMap(Context context, int resId) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        // 获取资源图片
        InputStream is = context.getResources().openRawResource(resId);
        Bitmap bitmap = decodeStream(is, null, opt);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap decodeStream(InputStream is, Rect outPadding, BitmapFactory.Options opts) {
        try {
            return BitmapFactory.decodeStream(is, outPadding, opts);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public static Bitmap decodeResource(Resources res, int id) {
        try {
            return BitmapFactory.decodeResource(res, id);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height, Matrix m, boolean filter) {
        try {
            return Bitmap.createBitmap(source, x, y, width, height, m, filter);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
        try {
            return Bitmap.createBitmap(width, height, config);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        try {
            return Bitmap.createBitmap(source, x, y, width, height);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public static Bitmap getBitmap(ImageView imgView) {
        Drawable drawable = imgView.getDrawable();
        Bitmap bitmap = getBitmap(drawable);
        return bitmap;
    }

    public static Bitmap getBitmap(Drawable drawable) {
        Bitmap bitmap = null;
        if (drawable instanceof TransitionDrawable) {
            bitmap = ((BitmapDrawable) ((TransitionDrawable) drawable).getDrawable(1)).getBitmap();
        } else if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof NinePatchDrawable) {
            bitmap = drawableToBitmap(drawable);
        }
        return bitmap;
    }

    public static String saveBitmap(Bitmap bitmap, String fileName) {
        try {
            FileOutputStream os = new FileOutputStream(fileName);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    public static void saveBitmap(Bitmap bitmap, String fileName, int w, int h) {
        Bitmap bmp = zoomBitmap(bitmap, w, h);
        try {
            FileOutputStream os = new FileOutputStream(fileName);
            if (bmp != null) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, os);
            }
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap decodeFile(String filePath, int maxResolution) {
        if (filePath == null) {
            return null;
        }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            if (maxResolution != 0) {
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(filePath, options);
                options.inSampleSize = calculateInSampleSize(options, maxResolution, maxResolution);
            }
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            int degree = getExifOrientation(filePath);
            return rotateBitmap(bitmap, degree);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        return null;

    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if ((width > height && width < 2 * height) || (height > 2 * width)) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    private static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }

            }
        }
        return degree;
    }

    public static InputStream getImageInputStream(String imagePath) throws Exception {
        int degree = getExifOrientation(imagePath);
        if (degree == 0) {
            return new FileInputStream(imagePath);
        }
        Bitmap bitmap = decodeFile(imagePath, 1024);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, os);
        }
        byte[] bytes = os.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayInputStream;
    }

    /**
     * 将byte[]转换成InputStream
     *
     * @param b
     * @return
     */
    public static InputStream byte2InputStream(byte[] b) {
        return new ByteArrayInputStream(b);
    }

    /**
     * 将InputStream转换成byte
     *
     * @param is
     * @return
     */
    public static byte[] inputStream2Bytes(InputStream is) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] readByte = new byte[1024];
        int readCount = -1;
        try {
            while ((readCount = is.read(readByte, 0, 1024)) != -1) {
                bos.write(readByte, 0, readCount);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 将Bitmap转换成InputStream
     *
     * @param bm
     * @return
     */
    public static InputStream bitmap2InputStream(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is;
    }

    /**
     * 将Bitmap转换成InputStream
     *
     * @param bm
     * @param quality
     * @return
     */
    public static InputStream bitmap2InputStream(Bitmap bm, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, quality, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is;
    }

    /**
     * 将InputStream转换成Bitmap
     *
     * @param is
     * @return
     */
    public static Bitmap inputStream2Bitmap(InputStream is) {
        return BitmapFactory.decodeStream(is);
    }

    /**
     * Drawable转换成InputStream
     *
     * @param d
     * @return
     */
    public static InputStream drawable2InputStream(Drawable d) {
        Bitmap bitmap = drawable2Bitmap(d);
        return bitmap2InputStream(bitmap);
    }

    // InputStream转换成Drawable
    public static Drawable inputStream2Drawable(InputStream is) {
        Bitmap bitmap = inputStream2Bitmap(is);
        return bitmap2Drawable(bitmap);
    }

    // Drawable转换成byte[]
    public static byte[] drawable2Bytes(Drawable d) {
        Bitmap bitmap = drawable2Bitmap(d);
        return bitmap2Bytes(bitmap);
    }

    // byte[]转换成Drawable
    public static Drawable bytes2Drawable(byte[] b) {
        Bitmap bitmap = bytes2Bitmap(b);
        return bitmap2Drawable(bitmap);
    }

    // Bitmap转换成byte[]
    public static byte[] bitmap2Bytes(Bitmap bm) {
        if (bm == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 75, baos);
        byte[] bytes = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    // byte[]转换成Bitmap
    public static Bitmap bytes2Bitmap(byte[] b) {
        if (b != null && b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        }
        return null;
    }

    // Drawable转换成Bitmap
    public static Bitmap drawable2Bitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // Bitmap转换成Drawable
    public static Drawable bitmap2Drawable(Bitmap bitmap) {
        BitmapDrawable bd = new BitmapDrawable(bitmap);
        Drawable d = (Drawable) bd;
        return d;
    }

    /**
     * 回收bitmap
     *
     * @param bmp
     */
    public static void safeRecycle(Bitmap bmp) {
        if (bmp != null && !bmp.isRecycled()) {
            bmp.recycle();
        }
    }

    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        return bmpToByteArray(bmp, 75, needRecycle);
    }

    public static byte[] bmpToByteArray(final Bitmap bmp, int quality, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, output);
        if (needRecycle) {
            bmp.recycle();
        }
        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Bitmap转换成byte[]并且进行压缩,压缩到不大于maxkb
     *
     * @param bitmap
     * @param maxkb
     * @return
     */
    public static byte[] bitmap2Bytes(Bitmap bitmap, int maxkb) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        int options = 100;
        while (output.toByteArray().length > maxkb && options != 10) {
            output.reset(); //清空output
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, output);//这里压缩options%，把压缩后的数据存放到output中
            options -= 10;
        }
        return output.toByteArray();
    }

}

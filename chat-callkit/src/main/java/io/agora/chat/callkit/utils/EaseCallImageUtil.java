package io.agora.chat.callkit.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;

/**
 * Function: Implement gaussian blur tool class
 */
public class EaseCallImageUtil {
    private static final int BITMAP_SCALE = 5;

    private static EaseCallImageUtil sInstance;

    private EaseCallImageUtil() {
    }

    public static EaseCallImageUtil instance() {
        if (sInstance == null) {
            synchronized (EaseCallImageUtil.class) {
                if (sInstance == null) {
                    sInstance = new EaseCallImageUtil();
                }
            }
        }
        return sInstance;
    }

    public Bitmap blurBitmap(Context context, Bitmap image, float blurRadius, int outWidth, int outHeight) {
        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, outWidth / BITMAP_SCALE, outHeight / BITMAP_SCALE, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            blurScript.setRadius(blurRadius);
        }
        blurScript.setInput(tmpIn);
        blurScript.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        return outputBitmap;
    }

    public static void setImage(Context context, ImageView imageView, String path) {
        if (imageView != null) {
            try {
                int resourceId = Integer.parseInt(path);
                imageView.setImageResource(resourceId);
            } catch (NumberFormatException e) {
                Glide.with(context).load(path).into(imageView);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setBgRadius(View view, int bgRadius) {
        if (Build.VERSION.SDK_INT >= 21) {
            //Set the rounded corner size
            view.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), bgRadius);
                }
            });
            //set shadow
            view.setElevation(10);
            //set rounded corners Clip
            view.setClipToOutline(true);
        }
    }

    public static <T extends View> void setViewGaussianBlur(T view, String path) {
        if (view != null) {
            try {
                int resourceId = Integer.parseInt(path);
                Glide.with(view)
                        .load(resourceId)
                        .apply(RequestOptions.bitmapTransform(new EaseCallGlideBlurTransformation(view.getContext())))
                        .into(new ViewTarget<T, Drawable>(view) {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                Drawable current = resource.getCurrent();
                                //set background image
                                view.setBackground(current);
                            }
                        });
            } catch (NumberFormatException e) {
                Glide.with(view)
                        .load(path)
                        .apply(RequestOptions.bitmapTransform(new EaseCallGlideBlurTransformation(view.getContext())))
                        .into(new ViewTarget<T, Drawable>(view) {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                Drawable current = resource.getCurrent();
                                //set background image
                                view.setBackground(current);
                            }
                        });
            }
        }
    }

}

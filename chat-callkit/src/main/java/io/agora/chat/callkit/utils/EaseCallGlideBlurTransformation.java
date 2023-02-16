package io.agora.chat.callkit.utils;


import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;

import java.security.MessageDigest;

public class EaseCallGlideBlurTransformation extends CenterCrop {
    private Context context;

    public EaseCallGlideBlurTransformation(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        return EaseCallImageUtil.instance().blurBitmap(context, toTransform, 15, outWidth, outHeight);
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
    }
}

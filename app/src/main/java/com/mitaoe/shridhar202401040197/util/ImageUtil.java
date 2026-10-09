package com.mitaoe.shridhar202401040197.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtil {

    public static class EncodedImage {
        public final String base64;
        public final String mimeType;

        public EncodedImage(String base64, String mimeType) {
            this.base64 = base64;
            this.mimeType = mimeType;
        }
    }

    public static EncodedImage encodeUriToBase64(Context context, Uri imageUri) {
        if (context == null || imageUri == null) return null;
        try {
            String mimeType = context.getContentResolver().getType(imageUri);
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "image/jpeg";
            }

            // Decode dimensions first to calculate downsampling
            InputStream isCheck = context.getContentResolver().openInputStream(imageUri);
            if (isCheck == null) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(isCheck, null, opts);
            isCheck.close();

            int maxDim = 1024;
            int sampleSize = 1;
            while (opts.outWidth / sampleSize > maxDim || opts.outHeight / sampleSize > maxDim) {
                sampleSize *= 2;
            }

            // Decode actual downsampled bitmap
            InputStream isLoad = context.getContentResolver().openInputStream(imageUri);
            if (isLoad == null) return null;

            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeStream(isLoad, null, decodeOpts);
            isLoad.close();

            if (bitmap == null) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Bitmap.CompressFormat format = "image/png".equalsIgnoreCase(mimeType) 
                    ? Bitmap.CompressFormat.PNG 
                    : Bitmap.CompressFormat.JPEG;

            bitmap.compress(format, 80, baos);
            bitmap.recycle();

            byte[] bytes = baos.toByteArray();
            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            return new EncodedImage(base64, mimeType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

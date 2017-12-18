package com.xzfg.app.util;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;

import com.xzfg.app.BuildConfig;
import com.xzfg.app.R;
import com.xzfg.app.model.AgentSettings;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 *
 */
@SuppressWarnings("deprecation")
public class ImageUtil {
    private ImageUtil() {
    }

    /**
     * Returns a sample size that will give an image equal to, or greater than, the requested width and height.
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }


        return inSampleSize;
    }

    public static int getPixels(int dp) {
        return (int) Math.ceil(dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int getDp(int pixels) {
        return (int) Math.ceil(pixels / Resources.getSystem().getDisplayMetrics().density);
    }

    public static String[] getVideoDetails(ContentResolver resolver, Uri photoUri) {
        String[] details = new String[4];

        Cursor cursor = resolver.query(photoUri, new String[]{
                MediaStore.Video.VideoColumns.SIZE,
                MediaStore.Video.VideoColumns.DATE_TAKEN,
                MediaStore.Video.VideoColumns.LATITUDE,
                MediaStore.Video.VideoColumns.LONGITUDE,
        }, null, null, null);

        if (cursor == null) {
            return details;
        }

        if (cursor.getCount() != 1) {
            return details;
        }

        cursor.moveToFirst();

        details[0] = String.valueOf(cursor.getLong(0));
        details[1] = String.valueOf(cursor.getLong(1));
        details[2] = String.valueOf(cursor.getDouble(2));
        details[3] = String.valueOf(cursor.getDouble(3));

        cursor.close();

        return details;
    }

    public static String[] getPictureDetails(ContentResolver resolver, Uri photoUri) {
        String[] details = new String[4];

        Cursor cursor = resolver.query(photoUri, new String[]{
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.LATITUDE,
                MediaStore.Images.ImageColumns.LONGITUDE
        }, null, null, null);

        if (cursor == null) {
            return details;
        }

        if (cursor.getCount() != 1) {
            return details;
        }

        cursor.moveToFirst();

        details[0] = String.valueOf(cursor.getInt(0));
        details[1] = String.valueOf(cursor.getLong(1));
        details[2] = String.valueOf(cursor.getDouble(2));
        details[3] = String.valueOf(cursor.getDouble(3));

        cursor.close();

        return details;
    }

    public static int[] getRequestedSize(int requestedSize, ConnectivityManager connectivityManager) {
        int targetWidth = -1;
        int targetHeight = -1;

        switch (requestedSize) {
            case -1:
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    targetWidth = 480;
                    targetHeight = 360;
                } else {
                    targetWidth = 192;
                    targetHeight = 144;
                }
                break;
            case 2:
                targetWidth = 480;
                targetHeight = 360;
                break;
            case 3:
                targetWidth = -2;
                targetHeight = -2;
                break;
            case 4:
                targetWidth = 352;
                targetHeight = 288;
                break;
            case 5:
                targetWidth = 640;
                targetHeight = 480;
                break;
            case 6:
                targetWidth = 968;
                targetHeight = 548;
                break;
            case 7:
                targetWidth = 1288;
                targetHeight = 728;
                break;
            case 8:
                targetWidth = 1928;
                targetHeight = 1088;
                break;
        }
        return new int[]{targetWidth, targetHeight};

    }

    public static Camera.Size getBestPhotoSize(AgentSettings agentSettings, ConnectivityManager connectivityManager, List<Camera.Size> supportedSizes) {
        Camera.Size outSize = supportedSizes.get(supportedSizes.size() - 1);

        int targetWidth = outSize.width;
        int targetHeight = outSize.height;
        int[] requested = getRequestedSize(agentSettings.getPhotoSize(), connectivityManager);

        if (requested[0] == -2) {
            Camera.Size max = supportedSizes.get(0);
            targetWidth = max.width;
            targetHeight = max.height;
        } else {
            if (requested[0] != -1 && requested[1] != -1) {
                targetWidth = requested[0];
                targetHeight = requested[1];
            }
        }


        for (Camera.Size size : supportedSizes) {
            if (size.width <= targetWidth && size.height <= targetHeight && size.width > outSize.width && size.height > outSize.height) {
                outSize = size;
            }
        }

        return outSize;

    }

    public static Camera.Size getBestVideoSize(AgentSettings agentSettings, ConnectivityManager connectivityManager, List<Camera.Size> supportedSizes) {

        int[] requested = getRequestedSize(agentSettings.getVideoCasesSize(), connectivityManager);

        if (requested[0] == -2) {
            return supportedSizes.get(0);
        }

        Camera.Size outSize = null;
        for (Camera.Size size : supportedSizes) {
            if (outSize == null && (size.width <= requested[0] + 8 & size.height <= requested[1] + 8)) {
                outSize = size;
                break;
            }
        }
        if (outSize == null) {
            outSize = supportedSizes.get(supportedSizes.size() - 1);
        }

        return outSize;

    }

    public static Camera.Size getBestStreamingSize(AgentSettings agentSettings, ConnectivityManager connectivityManager, List<Camera.Size> supportedSizes) {
        int[] requested = getRequestedSize(agentSettings.getVideoStreamSize(), connectivityManager);

        if (requested[0] == -2) {
            return supportedSizes.get(0);
        }
        Camera.Size outSize = null;
        for (Camera.Size size : supportedSizes) {
            if (outSize == null && (size.width <= requested[0] + 8 & size.height <= requested[1] + 8)) {
                outSize = size;
                break;
            }
        }
        if (outSize == null) {
            outSize = supportedSizes.get(supportedSizes.size() - 1);
        }
        return outSize;

    }


    // ******************************************************


    // Set ImageView image
    public static void setImageView(ImageView image, byte[] data, int displayWidth, boolean recycle, boolean rounded) {
        try {
            if (image != null && data != null) {
                Size is = getImageSize(data);
                Size s = new Size(is.width, is.height);
                DisplayMetrics displayMetrics = image.getResources().getDisplayMetrics();
                if (displayWidth <= 1) displayWidth = displayMetrics.widthPixels;

                if (is.width > displayWidth) {
                    s.width = displayWidth;
                    double h = is.width;
                    h = h / displayWidth;
                    h = s.height / h;
                    s.height = (int) Math.round(h);
                }

                // Cleanup
                if (recycle) clearImageView(image);
                // Fix orientation
                int orientation = Exif.getOrientation(data);
                Matrix matrix = new Matrix();
                if (orientation == 6) {
                    matrix.postRotate(90);
                } else if (orientation == 3) {
                    matrix.postRotate(180);
                } else if (orientation == 8) {
                    matrix.postRotate(270);
                }

                Bitmap bm = decodeSampledBitmap(data, s.width, s.height);
                bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

                if (rounded) {
                    bm = getCircularBitmap(bm);
                }
                image.setImageBitmap(bm);
            }
        } catch (OutOfMemoryError e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
                //Utils.Log("Could not set image bitmap - OutOfMemoryError");
            }
        }
    }

    // Set ImageView image
    public static void setThumbnailView(ImageView image, byte[] data, int displayWidth, boolean recycle) {
        try {
            if (image != null && data != null) {
                Size is = getImageSize(data);
                Size s = new Size(is.width, is.height);
                DisplayMetrics displayMetrics = image.getResources().getDisplayMetrics();
                if (displayWidth <= 1) displayWidth = displayMetrics.widthPixels;

                if (is.width > displayWidth) {
                    s.width = displayWidth;
                    double h = is.width;
                    h = h / displayWidth;
                    h = s.height / h;
                    s.height = (int) Math.round(h);
                }

                // Cleanup
                if (recycle) clearImageView(image);
                // Fix orientation
                int orientation = Exif.getOrientation(data);
                Matrix matrix = new Matrix();
                if (orientation == 6) {
                    matrix.postRotate(90);
                } else if (orientation == 3) {
                    matrix.postRotate(180);
                } else if (orientation == 8) {
                    matrix.postRotate(270);
                }

                Bitmap bm = decodeSampledBitmap(data, s.width, s.height);
                bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                bm = getThumbnailBitmap(bm, image.getResources());
                image.setImageBitmap(bm);
            }
        } catch (OutOfMemoryError e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
                //Utils.Log("Could not set image bitmap - OutOfMemoryError");
            }
        }
    }


    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int minDx = Math.min(width, height);
        float radius = minDx / 2;
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect srcRect = new Rect((width - minDx) / 2, (height - minDx) / 2,
                (width + minDx) / 2, (height + minDx) / 2);
        final Rect dstRect = new Rect(0, 0, minDx, minDx);

        Bitmap output = Bitmap.createBitmap(minDx, minDx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(minDx / 2, minDx / 2, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);

        return output;
    }

    public static Bitmap getThumbnailBitmap(Bitmap bitmap, Resources resources) {
        // Resize source bitmap
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        int scaleWidth = Math.round(80 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))*2;
        Bitmap oldBitmap = bitmap;
        bitmap = Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleWidth, false);
        oldBitmap.recycle();
        oldBitmap = null;

        // Cross icon to put to the top right corner
        Bitmap closeBitmap = BitmapFactory.decodeResource(resources, R.drawable.close);
        int closeWidth = closeBitmap.getWidth();
        final int padding = closeWidth / 2;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int minDx = Math.min(width, height);
        int radiusPx = 32;
        final Rect srcRect = new Rect((width - minDx) / 2, (height - minDx) / 2,
                (width + minDx) / 2, (height + minDx) / 2);
        final Rect dstRectOrg = new Rect(0, 0, minDx, minDx);
        // Add padding to top righ corner for cross icon
        final Rect dstRect = new Rect(dstRectOrg.left, dstRectOrg.top + padding, dstRectOrg.right - padding, dstRectOrg.bottom);

        // Create destination bitmap
        Bitmap output = Bitmap.createBitmap(dstRectOrg.width(), dstRectOrg.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(resources.getColor(R.color.actionBarBackColor));
        canvas.drawRoundRect(new RectF(dstRect), radiusPx, radiusPx, paint);
        dstRect.inset(8, 8);

        Path clipPath = new Path();
        clipPath.addRoundRect(new RectF(dstRect), radiusPx, radiusPx, Path.Direction.CW);
        canvas.clipPath(clipPath);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);
        canvas.clipRect(dstRectOrg, Region.Op.UNION);
        canvas.drawBitmap(closeBitmap, dstRect.right - closeWidth / 2, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

        return output;
    }

    public static Bitmap createVideoThumbnail(ContentResolver cr, Uri uri) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            ParcelFileDescriptor fd = cr.openFileDescriptor(uri, "r");
            retriever.setDataSource(fd.getFileDescriptor(), 0, 0x7ffffffffffffffL);
            bitmap = retriever.getFrameAtTime(-1);
        } catch (FileNotFoundException ex) {
            // Assume this is a corrupt video file
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }

        if (bitmap == null) return null;

        // Scale down the bitmap if it's too large.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int max = Math.max(width, height);
        if (max > 512) {
            float scale = 512f / max;
            int w = Math.round(scale * width);
            int h = Math.round(scale * height);
            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        }

        return bitmap;
    }

    public static Bitmap decodeSampledBitmap(byte[] data, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        options.inInputShareable = true;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    // Cleanup ImageView
    public static void clearImageView(ImageView v) {
        if (v != null) {
            Drawable drawable = v.getDrawable();
            if (drawable != null && drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                v.setImageDrawable(null);
                if (!bitmap.isRecycled())
                    bitmap.recycle();
            }
        }
    }

    // Gets image size
    public static Size getImageSize(byte[] data) {
        // Decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        return new Size(options.outWidth, options.outHeight);
    }

    public static byte[] loadFileFromStorage(String filePath) throws IOException {
        byte[] result = null;

        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                result = readFileAsBytes(file.getPath());
            } else {
                InputStream is = null;
                try {
                    is = new URL(filePath).openStream();
                } catch (Exception ex) {
                    // May throw MalformedUrlException
                }
                if (is != null) {
                    result = readStreamAsBytes(is);
                    is.close();
                }
            }
        }

        return result;
    }

    // Read stream as bytes
    public static byte[] readStreamAsBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            bos.write(data, 0, nRead);
        }
        bos.flush();

        return bos.toByteArray();
    }

    // Read file as bytes
    public static byte[] readFileAsBytes(String pathName) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(pathName));
        return readStreamAsBytes(is);
    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        //v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
        v.draw(c);

        return b;
    }

}

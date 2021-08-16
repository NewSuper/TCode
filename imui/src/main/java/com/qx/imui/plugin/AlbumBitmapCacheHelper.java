package com.qx.imui.plugin;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.LruCache;
import android.view.WindowManager;

import com.qx.imlib.qlog.QLog;
import com.qx.imui.util.FileUtils;
import com.qx.imui.util.KitStorageUtils;
import com.qx.imui.util.QXUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;

public class AlbumBitmapCacheHelper {
    private static final String TAG = "AlbumBitmapCacheHelper";
    private static volatile AlbumBitmapCacheHelper instance = null;
    private LruCache<String, Bitmap> cache;
    private static int cacheSize;
    private ArrayList<String> currentShowString;
    private Context mContext;
    ThreadPoolExecutor tpe;

    private AlbumBitmapCacheHelper() {
        this.tpe = new ThreadPoolExecutor(2, 5, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        this.cache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                int result;
                if (Build.VERSION.SDK_INT >= 19) {
                    result = value.getAllocationByteCount();
                } else if (Build.VERSION.SDK_INT >= 12) {
                    result = value.getByteCount();
                } else {
                    result = value.getRowBytes() * value.getHeight();
                }
                return result;
            }
        };
        this.currentShowString = new ArrayList<>();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void releaseAllSizeCache() {
        this.cache.evictAll();
        this.cache.resize(1);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void releaseHalfSizeCache() {
        this.cache.resize((int) (Runtime.getRuntime().maxMemory() / 1024L / 8L));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void resizeCache() {
        this.cache.resize((int) (Runtime.getRuntime().maxMemory() / 1024L / 4L));
    }

    private void clearCache() {
        this.cache.evictAll();
        this.cache = null;
        this.tpe = null;
        instance = null;
    }

    public static AlbumBitmapCacheHelper getInstance() {
        if (instance == null) {
            Class var0 = AlbumBitmapCacheHelper.class;
            synchronized (AlbumBitmapCacheHelper.class) {
                if (instance == null) {
                    instance = new AlbumBitmapCacheHelper();
                }
            }
        }

        return instance;
    }

    public static void init(Context context) {
        QLog.d(TAG, "AlbumBitmapCacheHelper" + "init");
        cacheSize = calculateMemoryCacheSize(context);
        AlbumBitmapCacheHelper helper = getInstance();
        helper.mContext = context.getApplicationContext();
    }

    public void uninit() {
        QLog.d(TAG, "uninit");
        this.tpe.shutdownNow();
        this.clearCache();
    }

    public Bitmap getBitmap(String path, int width, int height, AlbumBitmapCacheHelper.ILoadImageCallback callback, Object... objects) {
        Bitmap bitmap = this.getBitmapFromCache(path, width, height);
        if (bitmap != null) {
            Log.e("AlbumBitmapCacheHelper", "getBitmap from cache");
        } else {
            this.decodeBitmapFromPath(path, width, height, callback, objects);
        }

        return bitmap;
    }

    private void decodeBitmapFromPath(final String path, final int width, final int height, final AlbumBitmapCacheHelper.ILoadImageCallback callback, final Object... objects) throws OutOfMemoryError {
        final Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                if (callback != null) {
                    callback.onLoadImageCallBack((Bitmap) msg.obj, path, objects);
                }

            }
        };
        this.tpe.execute(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run() {
                if (AlbumBitmapCacheHelper.this.currentShowString.contains(path) && AlbumBitmapCacheHelper.this.cache != null) {
                    Bitmap bitmap = null;
                    if (width != 0 && height != 0) {
                        String hash = QXUtils.md5(path + "_" + width + "_" + height);
                        String tempPath = FileUtils.getInternalCachePath(AlbumBitmapCacheHelper.this.mContext, "image") + "/" + hash + ".temp";
                        File picFile = new File(path);
                        File tempFile = new File(tempPath);
                        if (tempFile.exists() && picFile.lastModified() <= tempFile.lastModified()) {
                            bitmap = BitmapFactory.decodeFile(tempPath);
                        }

                        if (bitmap == null) {
                            try {
                                bitmap = AlbumBitmapCacheHelper.this.getBitmap(path, width, height);
                            } catch (OutOfMemoryError var23) {
                                bitmap = null;
                            }

                            if (bitmap != null && AlbumBitmapCacheHelper.this.cache != null) {
                                bitmap = AlbumBitmapCacheHelper.centerSquareScaleBitmap(bitmap, bitmap.getWidth() > bitmap.getHeight() ? bitmap.getHeight() : bitmap.getWidth());
                            }

                            if (bitmap != null) {
                                FileOutputStream fos = null;

                                try {
                                    File file = new File(tempPath);
                                    if (!file.exists()) {
                                        file.createNewFile();
                                    } else {
                                        file.delete();
                                        file.createNewFile();
                                    }

                                    fos = new FileOutputStream(file);
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                                    fos.write(baos.toByteArray());
                                    fos.flush();
                                } catch (FileNotFoundException var21) {
                                    QLog.e(TAG, "decodeBitmapFromPath" + var21);
                                } catch (IOException var22) {
                                    QLog.e(TAG, "decodeBitmapFromPath" + var22);
                                } finally {
                                    if (fos != null) {
                                        try {
                                            fos.close();
                                        } catch (IOException var20) {
                                            QLog.e(TAG, "decodeBitmapFromPath" + var20);
                                        }
                                    }

                                }
                            }
                        } else if (AlbumBitmapCacheHelper.this.cache != null) {
                            bitmap = AlbumBitmapCacheHelper.centerSquareScaleBitmap(bitmap, bitmap.getWidth() > bitmap.getHeight() ? bitmap.getHeight() : bitmap.getWidth());
                        }
                    } else {
                        try {
                            bitmap = AlbumBitmapCacheHelper.this.getBitmap(path, width, height);
                        } catch (OutOfMemoryError var24) {
                            QLog.e(TAG, "decodeBitmapFromPath" + var24);
                        }
                    }

                    if (bitmap != null && AlbumBitmapCacheHelper.this.cache != null) {
                        AlbumBitmapCacheHelper.this.cache.put(path + "_" + width + "_" + height, bitmap);
                    }

                    Message msg = Message.obtain();
                    msg.obj = bitmap;
                    handler.sendMessage(msg);
                }
            }
        });
    }

    public static Bitmap centerSquareScaleBitmap(Bitmap bitmap, int edgeLength) {
        if (null != bitmap && edgeLength > 0) {
            int widthOrg = bitmap.getWidth();
            int heightOrg = bitmap.getHeight();
            int xTopLeft = (widthOrg - edgeLength) / 2;
            int yTopLeft = (heightOrg - edgeLength) / 2;
            if (xTopLeft == 0 && yTopLeft == 0) {
                return bitmap;
            } else {
                try {
                    Bitmap result = Bitmap.createBitmap(bitmap, xTopLeft, yTopLeft, edgeLength, edgeLength);
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }

                    return result;
                } catch (OutOfMemoryError var8) {
                    return bitmap;
                }
            }
        } else {
            return null;
        }
    }

    private int computeScale(BitmapFactory.Options options, int width, int height) {
        if (options == null) {
            return 1;
        } else {
            int widthScale = (int) ((float) options.outWidth / (float) width);
            int heightScale = (int) ((float) options.outHeight / (float) height);
            int scale = widthScale > heightScale ? widthScale : heightScale;
            if (scale < 1) {
                scale = 1;
            }

            return scale;
        }
    }

    private Bitmap getBitmapFromCache(String path, int width, int height) {
        return (Bitmap) this.cache.get(path + "_" + width + "_" + height);
    }

    public void addPathToShowlist(String path) {
        this.currentShowString.add(path);
    }

    public void removePathFromShowlist(String path) {
        this.currentShowString.remove(path);
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(
            api = 24
    )
    private Bitmap getBitmap(String path, int widthLimit, int heightLimit) throws OutOfMemoryError {
        Bitmap bitmap = null;

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            ExifInterface exifInterface = null;
            if (KitStorageUtils.isBuildAndTargetForQ(this.mContext)) {
                if (!FileUtils.uriStartWithContent(Uri.parse(path))) {
                    exifInterface = new ExifInterface(path);
                } else {
                    ParcelFileDescriptor pfd = this.mContext.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
                    exifInterface = new ExifInterface(pfd.getFileDescriptor());
                }
            } else {
                exifInterface = new ExifInterface(path);
            }

            int orientation = exifInterface.getAttributeInt("Orientation", 0);
            int sampleSize;
            if (widthLimit == 0 && heightLimit == 0) {
                sampleSize = this.computeScale(options, ((WindowManager) ((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getWidth(), ((WindowManager) ((WindowManager) this.mContext.getSystemService("window"))).getDefaultDisplay().getHeight());
                QLog.d(TAG, "sampleSize:" + sampleSize);
            } else {
                if (orientation == 6 || orientation == 8 || orientation == 5 || orientation == 7) {
                    int tmp = widthLimit;
                    widthLimit = heightLimit;
                    heightLimit = tmp;
                }

                sampleSize = this.computeScale(options, widthLimit, heightLimit);
                QLog.d(TAG, "sampleSize:" + sampleSize);
            }

            try {
                options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                options.inSampleSize = sampleSize;
                if (KitStorageUtils.isBuildAndTargetForQ(this.mContext)) {
                    if (!FileUtils.uriStartWithContent(Uri.parse(path))) {
                        bitmap = BitmapFactory.decodeFile(path, options);
                    } else {
                        ParcelFileDescriptor pfd = this.mContext.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
                        bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), (Rect) null, options);
                    }
                } else {
                    bitmap = BitmapFactory.decodeFile(path, options);
                }
            } catch (OutOfMemoryError var14) {
                QLog.e(TAG, "getBitmap" + var14);
                options.inSampleSize <<= 1;
                if (KitStorageUtils.isBuildAndTargetForQ(this.mContext)) {
                    String imageDirPath = KitStorageUtils.getImageSavePath(this.mContext);
                    if (path.startsWith(imageDirPath)) {
                        bitmap = BitmapFactory.decodeFile(path, options);
                    } else {
                        ParcelFileDescriptor pfd = this.mContext.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
                        bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), (Rect) null, options);
                    }
                } else {
                    bitmap = BitmapFactory.decodeFile(path, options);
                }
            }

            Matrix matrix = new Matrix();
            if (bitmap != null) {
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                if (orientation == 6 || orientation == 8 || orientation == 5 || orientation == 7) {
                    int tmp = w;
                    w = h;
                    h = tmp;
                }

                switch (orientation) {
                    case 2:
                        matrix.preScale(-1.0F, 1.0F);
                        break;
                    case 3:
                        matrix.setRotate(180.0F, (float) w / 2.0F, (float) h / 2.0F);
                        break;
                    case 4:
                        matrix.preScale(1.0F, -1.0F);
                        break;
                    case 5:
                        matrix.setRotate(90.0F, (float) w / 2.0F, (float) h / 2.0F);
                        matrix.preScale(1.0F, -1.0F);
                        break;
                    case 6:
                        matrix.setRotate(90.0F, (float) w / 2.0F, (float) h / 2.0F);
                        break;
                    case 7:
                        matrix.setRotate(270.0F, (float) w / 2.0F, (float) h / 2.0F);
                        matrix.preScale(1.0F, -1.0F);
                        break;
                    case 8:
                        matrix.setRotate(270.0F, (float) w / 2.0F, (float) h / 2.0F);
                }

                if (widthLimit != 0 && heightLimit != 0) {
                    float xS = (float) widthLimit / (float) bitmap.getWidth();
                    float yS = (float) heightLimit / (float) bitmap.getHeight();
                    matrix.postScale(Math.min(xS, yS), Math.min(xS, yS));
                } else {
                    QLog.e(TAG, "widthLimit or heightLimit is 0");
                }

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        } catch (IOException var15) {
            QLog.e(TAG, "getBitmap" + var15);
        } catch (IllegalArgumentException var16) {
            QLog.e(TAG, "getBitmap" + var16);
        }

        return bitmap;
    }

    private static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & 1048576) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap) {
            memoryClass = am.getLargeMemoryClass();
        }

        return (int) (1048576L * (long) memoryClass / 8L);
    }

    public interface ILoadImageCallback {
        void onLoadImageCallBack(Bitmap var1, String var2, Object... var3);
    }
}

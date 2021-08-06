package com.qx.imui.plugin.image;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.qx.imlib.QXIMClient;
import com.qx.imlib.qlog.QLog;
import com.qx.imlib.utils.file.FileSizeUtil;
import com.qx.imui.QXConfigManager;
import com.qx.imui.R;
import com.qx.imui.base.BaseActivity;
import com.qx.imui.plugin.AlbumBitmapCacheHelper;
import com.qx.imui.util.FileUtils;
import com.qx.imui.util.KitStorageUtils;
import com.qx.imui.util.PermissionCheckUtil;
import com.qx.imui.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.collection.ArrayMap;
import androidx.core.content.FileProvider;

public class PictureSelectorActivity extends BaseActivity {
    private static final String TAG = PictureSelectorActivity.class.getSimpleName();
    public static final int SIGHT_DEFAULT_DURATION_LIMIT = 300;
    private GridView mGridView;
    private ImageButton mBtnBack;
    private Button mBtnSend;
    private PictureSelectorActivity.PicTypeBtn mPicType;
    private PictureSelectorActivity.PreviewBtn mPreviewBtn;
    private View mCatalogView;
    private ListView mCatalogListView;
    private List<MediaItem> mAllItemList;
    private Map<String, List<MediaItem>> mItemMap;
    private ArrayList<Uri> mAllSelectedItemList;
    private List<String> mCatalogList;
    private String mCurrentCatalog = "";
    private Uri mTakePictureUri;
    private boolean mSendOrigin = false;
    private int perWidth;
    private int perHeight;
    private ExecutorService pool;
    private Handler bgHandler;
    private Handler uiHandler;
    private HandlerThread thread;

    public PictureSelectorActivity() {
    }

    @Override
    public int getLayoutId() {
        return  R.layout.imui_pic_select;
    }

    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(1);
        super.onCreate(savedInstanceState);
        this.thread = new HandlerThread(TAG);
        this.thread.start();
        this.bgHandler = new Handler(this.thread.getLooper());
        this.uiHandler = new Handler(this.getMainLooper());
        this.mGridView = (GridView) this.findViewById(R.id.ps_gridlist);
        this.mBtnBack = (ImageButton) this.findViewById(R.id.ps_back);
        this.mBtnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PictureSelectorActivity.this.finish();
            }
        });
        this.mBtnSend = (Button) this.findViewById(R.id.ps_bsend);
        this.mPicType = this.findViewById(R.id.ps_pic_type);
        this.mPicType.init(this);
        this.mPicType.setEnabled(false);
        this.mPreviewBtn = this.findViewById(R.id.ps_preview);
        this.mPreviewBtn.init(this);
        this.mPreviewBtn.setEnabled(false);
        this.mCatalogView = this.findViewById(R.id.ps_catalog_window);
        this.mCatalogListView = (ListView) this.findViewById(R.id.ps_catalog_listview);
        String[] permissions = new String[]{"android.permission.READ_EXTERNAL_STORAGE"};
        if (!PermissionCheckUtil.checkPermissions(this, permissions)) {
            PermissionCheckUtil.requestPermissions(this, permissions, 100);
        } else {
            this.pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            this.initView();
        }
    }

    public void initView() {
        this.updatePictureItems(() -> {
            if (PictureSelectorActivity.this.uiHandler != null) {
                PictureSelectorActivity.this.uiHandler.post(new Runnable() {
                    public void run() {
                        PictureSelectorActivity.this.initWidget();
                    }
                });
            }

        });
    }

    private void initWidget() {
        this.mGridView.setAdapter(new PictureSelectorActivity.GridViewAdapter());
        this.mGridView.setOnItemClickListener((parent, view, position, id) -> {
            if (position != 0) {
                ArrayList<MediaItem> itemList = new ArrayList();
                if (PictureSelectorActivity.this.mCurrentCatalog.isEmpty()) {
                    itemList.addAll(PictureSelectorActivity.this.mAllItemList);
                    PicItemHolder.itemList = itemList;
                    PicItemHolder.itemSelectedList = null;
                } else {
                    Map<String, List<MediaItem>> itemMap = PictureSelectorActivity.this.mItemMap;
                    if (itemMap == null) {
                        return;
                    }

                    List<MediaItem> currentMediaItems = (List) itemMap.get(PictureSelectorActivity.this.mCurrentCatalog);
                    if (currentMediaItems != null) {
                        itemList.addAll(currentMediaItems);
                        PicItemHolder.itemList = itemList;
                    }

                    ArrayList<MediaItem> itemSelectList = new ArrayList();
                    Iterator var10 = itemMap.keySet().iterator();

                    label46:
                    while (true) {
                        String key;
                        List mediaItems;
                        do {
                            do {
                                if (!var10.hasNext()) {
                                    PicItemHolder.itemSelectedList = itemSelectList;
                                    break label46;
                                }

                                key = (String) var10.next();
                                mediaItems = (List) itemMap.get(key);
                            } while (key.equals(PictureSelectorActivity.this.mCurrentCatalog));
                        } while (mediaItems == null);

                        Iterator var13 = mediaItems.iterator();

                        while (var13.hasNext()) {
                            MediaItem item = (MediaItem) var13.next();
                            if (item.selected) {
                                itemSelectList.add(item);
                            }
                        }
                    }
                }

                //处理点击事件
                MediaItem item = PicItemHolder.itemList.get(position - 1);
                if (item != null && item.isEnableChecked == false) {
                    if (item.mediaType == 1) {
                        //图片文件限制可以选择的最大Size
                        Toast.makeText(PictureSelectorActivity.this, StringUtils.getResourceStr(PictureSelectorActivity.this, R.string.qx_error_image_exceeded, QXConfigManager.getQxFileConfig().getImageMaxSize(FileSizeUtil.SIZETYPE_MB)), Toast.LENGTH_SHORT).show();
                    } else {
                        //视频文件或者其他文件类型，限制可以选择的最大Size
                        Toast.makeText(PictureSelectorActivity.this, StringUtils.getResourceStr(PictureSelectorActivity.this, R.string.qx_error_file_exceeded, 20), Toast.LENGTH_SHORT).show();
                        // Toast.makeText(PictureSelectorActivity.this, StringUtils.getResourceStr(PictureSelectorActivity.this, R.string.qx_error_video_exceeded, QXConfigManager.getQxFileConfig().getVideoMaxDuration(TimeUnit.MINUTES)), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                Intent intent = new Intent(PictureSelectorActivity.this, PicturePreviewActivity.class);
                intent.putExtra("index", position - 1);
                intent.putExtra("sendOrigin", PictureSelectorActivity.this.mSendOrigin);
                PictureSelectorActivity.this.startActivityForResult(intent, 0);
            }
        });
        this.mBtnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                LinkedHashMap<String, Integer> mLinkedHashMap = new LinkedHashMap();
                Iterator var3 = PictureSelectorActivity.this.mItemMap.entrySet().iterator();

                while (var3.hasNext()) {
                    Map.Entry<String, List<MediaItem>> entry = (Map.Entry) var3.next();
                    Iterator var5 = ((List) entry.getValue()).iterator();

                    while (var5.hasNext()) {
                        PictureSelectorActivity.MediaItem item = (PictureSelectorActivity.MediaItem) var5.next();
                        if (item.selected) {
                            mLinkedHashMap.put("file://" + item.uri, item.mediaType);
                        }
                    }
                }

                Gson gson = new Gson();
                String mediaList = gson.toJson(mLinkedHashMap);
                Intent data = new Intent();
                data.putExtra("sendOrigin", PictureSelectorActivity.this.mSendOrigin);
                data.putExtra("android.intent.extra.RETURN_RESULT", mediaList);
                PictureSelectorActivity.this.setResult(-1, data);
                PictureSelectorActivity.this.finish();
            }
        });
        this.mPicType.setEnabled(true);
        this.mPicType.setTextColor(this.getResources().getColor(R.color.rc_picsel_toolbar_send_text_normal));
        this.mPicType.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PictureSelectorActivity.this.mCatalogView.setVisibility(View.VISIBLE);
            }
        });
        this.mPreviewBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PictureSelectorActivity.PicItemHolder.itemList = new ArrayList();
                Iterator var2 = PictureSelectorActivity.this.mItemMap.keySet().iterator();

                PictureSelectorActivity.MediaItem mediaItem;
                while (var2.hasNext()) {
                    String key = (String) var2.next();
                    Iterator var4 = ((List) PictureSelectorActivity.this.mItemMap.get(key)).iterator();

                    while (var4.hasNext()) {
                        mediaItem = (PictureSelectorActivity.MediaItem) var4.next();
                        if (mediaItem.selected) {
                            PictureSelectorActivity.PicItemHolder.itemList.add(mediaItem);
                        }
                    }
                }

                if (PictureSelectorActivity.this.mAllSelectedItemList != null && PictureSelectorActivity.PicItemHolder.itemList != null) {
                    for (int i = 0; i < PictureSelectorActivity.this.mAllSelectedItemList.size(); ++i) {
                        Uri imageUri = (Uri) PictureSelectorActivity.this.mAllSelectedItemList.get(i);

                        for (int j = i + 1; j < PictureSelectorActivity.PicItemHolder.itemList.size(); ++j) {
                            mediaItem = (PictureSelectorActivity.MediaItem) PictureSelectorActivity.PicItemHolder.itemList.get(j);
                            if (mediaItem != null && imageUri.toString().contains(mediaItem.uri)) {
                                PictureSelectorActivity.PicItemHolder.itemList.remove(j);
                                PictureSelectorActivity.PicItemHolder.itemList.add(i, mediaItem);
                            }
                        }
                    }
                }

                PictureSelectorActivity.PicItemHolder.itemSelectedList = null;
                Intent intent = new Intent(PictureSelectorActivity.this, PicturePreviewActivity.class);
                intent.putExtra("sendOrigin", PictureSelectorActivity.this.mSendOrigin);
                PictureSelectorActivity.this.startActivityForResult(intent, 0);
            }
        });
        this.mCatalogView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == 1 && PictureSelectorActivity.this.mCatalogView.getVisibility() == View.VISIBLE) {
                    PictureSelectorActivity.this.mCatalogView.setVisibility(View.GONE);
                }

                return true;
            }
        });
        this.mCatalogListView.setAdapter(new PictureSelectorActivity.CatalogAdapter());
        this.mCatalogListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String catalog;
                if (position == 0) {
                    catalog = "";
                } else {
                    catalog = (String) PictureSelectorActivity.this.mCatalogList.get(position - 1);
                }

                if (catalog.equals(PictureSelectorActivity.this.mCurrentCatalog)) {
                    PictureSelectorActivity.this.mCatalogView.setVisibility(View.GONE);
                } else {
                    PictureSelectorActivity.this.mCurrentCatalog = catalog;
                    TextView textView = (TextView) view.findViewById(R.id.ps_grid_name);
                    PictureSelectorActivity.this.mPicType.setText(textView.getText().toString());
                    PictureSelectorActivity.this.mCatalogView.setVisibility(View.GONE);
                    ((PictureSelectorActivity.CatalogAdapter) PictureSelectorActivity.this.mCatalogListView.getAdapter()).notifyDataSetChanged();
                    ((PictureSelectorActivity.GridViewAdapter) PictureSelectorActivity.this.mGridView.getAdapter()).notifyDataSetChanged();
                }
            }
        });
        this.perWidth = ((WindowManager) ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getWidth() / 3;
        this.perHeight = ((WindowManager) ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getHeight() / 5;
    }

    @TargetApi(23)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= 23 && this.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED) {
                this.initView();
            } else {
                Toast.makeText(this.getApplicationContext(), this.getString(R.string.qx_permission_grant_needed), Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }

        if (resultCode != 0) {
            if (resultCode == 1) {
                this.setResult(-1, data);
                this.finish();
            } else {
                switch (requestCode) {
                    case 0:
                        this.mSendOrigin = data.getBooleanExtra("sendOrigin", false);
                        PictureSelectorActivity.GridViewAdapter gridViewAdapter = (PictureSelectorActivity.GridViewAdapter) this.mGridView.getAdapter();
                        if (gridViewAdapter != null) {
                            gridViewAdapter.notifyDataSetChanged();
                        }

                        PictureSelectorActivity.CatalogAdapter catalogAdapter = (PictureSelectorActivity.CatalogAdapter) this.mCatalogListView.getAdapter();
                        if (catalogAdapter != null) {
                            catalogAdapter.notifyDataSetChanged();
                        }

                        this.updateToolbar();
                        break;
                    case 1:
                        if (this.mTakePictureUri != null) {
                            PictureSelectorActivity.PicItemHolder.itemList = new ArrayList();
                            PictureSelectorActivity.MediaItem item = new PictureSelectorActivity.MediaItem();
                            item.uri = this.mTakePictureUri.getPath();
                            item.mediaType = 1;
                            PictureSelectorActivity.PicItemHolder.itemList.add(item);
                            PictureSelectorActivity.PicItemHolder.itemSelectedList = null;
                            item.uri_sdk29 = this.mTakePictureUri.toString();
                            Intent intent = new Intent(this, PicturePreviewActivity.class);
                            this.startActivityForResult(intent, 0);
                            MediaScannerConnection.scanFile(this.getApplicationContext(), new String[]{this.mTakePictureUri.getPath()}, (String[]) null, new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    PictureSelectorActivity.this.updatePictureItems((PictureSelectorActivity.IExecutedCallback) null);
                                }
                            });
                        }
                }

            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4 && this.mCatalogView != null && this.mCatalogView.getVisibility() == View.VISIBLE) {
            this.mCatalogView.setVisibility(View.GONE);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    protected void requestCamera() {
//        if (IMLibExtensionModuleManager.getInstance().onRequestHardwareResource(HardwareResource.ResourceType.VIDEO)) {
//            Toast.makeText(this, this.getString(R.string.rc_voip_call_video_start_fail), 1).show();
//        } else if (IMLibExtensionModuleManager.getInstance().onRequestHardwareResource(HardwareResource.ResourceType.AUDIO)) {
//            Toast.makeText(this, this.getString(R.string.rc_voip_call_audio_start_fail), 1).show();
//        } else {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resInfoList.size() <= 0) {
            Toast.makeText(this, this.getResources().getString(R.string.qx_voip_cpu_error), Toast.LENGTH_SHORT).show();
        } else {
            String name;
            Uri uri;
            if (KitStorageUtils.isBuildAndTargetForQ(this)) {
                name = String.valueOf(System.currentTimeMillis());
                ContentValues values = new ContentValues();
                values.put("description", "This is an image");
                values.put("_display_name", name);
                values.put("mime_type", "image/jpeg");
                values.put("title", name);
                values.put("relative_path", "Pictures");
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver resolver = this.getContentResolver();
                Uri insertUri = resolver.insert(uri, values);
                this.mTakePictureUri = insertUri;
                intent.putExtra("output", insertUri);
            } else {
                name = System.currentTimeMillis() + ".jpg";
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                if (!path.exists()) {
                    path.mkdirs();
                }

                File file = new File(path, name);
                this.mTakePictureUri = Uri.fromFile(file);

                try {
                    uri = FileProvider.getUriForFile(this, this.getPackageName() + this.getString(R.string.qx_authorities_fileprovider), file);
                } catch (Exception var10) {
                    QLog.e(TAG, "requestCamera" + var10);
                    throw new RuntimeException("Please check IMKit Manifest FileProvider config. Please refer to http://support.rongcloud.cn/kb/NzA1");
                }

                Iterator var12 = resInfoList.iterator();

                while (var12.hasNext()) {
                    ResolveInfo resolveInfo = (ResolveInfo) var12.next();
                    String packageName = resolveInfo.activityInfo.packageName;
                    this.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    this.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                intent.putExtra("output", uri);
            }

            this.startActivityForResult(intent, 1);
        }
//        }
    }

    private void updatePictureItems(final PictureSelectorActivity.IExecutedCallback iExecutedCallback) {
        this.bgHandler.post(new Runnable() {
            public void run() {
                String[] projection = new String[]{"_id", "_data", "date_added", "media_type", "mime_type", "title", "duration", "_size"};
                String selection;
                if (PictureSelectorActivity.this.getResources().getBoolean(R.bool.qx_media_selector_contain_video)) {
                    selection = "media_type=1 OR media_type=3";
                } else {
                    selection = "media_type=1";
                }

                Uri queryUri = MediaStore.Files.getContentUri("external");
                CursorLoader cursorLoader = new CursorLoader(PictureSelectorActivity.this, queryUri, projection, selection, (String[]) null, "date_added DESC");
                Cursor cursor = cursorLoader.loadInBackground();
                PictureSelectorActivity.this.mAllItemList = new ArrayList();
                PictureSelectorActivity.this.mCatalogList = new ArrayList();
                PictureSelectorActivity.this.mAllSelectedItemList = new ArrayList();
                PictureSelectorActivity.PicItemHolder.itemAllSelectedMediaItemList = new ArrayList();
                PictureSelectorActivity.this.mItemMap = new ArrayMap();
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            PictureSelectorActivity.MediaItem item = new PictureSelectorActivity.MediaItem();
                            item.name = cursor.getString(5);
                            item.mediaType = cursor.getInt(3);
                            item.mimeType = cursor.getString(4);
                            item.uri = cursor.getString(1);
                            item.duration = cursor.getInt(6);
                            Uri imageUri = ContentUris.withAppendedId(queryUri, cursor.getLong(0));
                            item.uri_sdk29 = imageUri.toString();
                            long sizeCol = cursor.getLong(7);
                            QLog.e(TAG, "大小：" + sizeCol + ",時長：" + item.duration);
                            //获取文件的本地路径
//                            double fileSize = FileSizeUtil.getFileOrFilesSize(item.uri,FileSizeUtil.SIZETYPE_MB);
                            double fileSize = FileSizeUtil.FormetFileSize(sizeCol, FileSizeUtil.SIZETYPE_MB);
//                            获取时长（min）
//                            long fileDuration = MediaUtil.getMediaFileDuration(item.uri)/60;
                            float fileDuration = item.duration / 60000f;
                            if (item.mediaType == 1) {
//                                QLog.e(TAG, "图片大小："+fileSize+"M");
                                //图片文件限制可以选择的最大Size
                                if (fileSize > QXConfigManager.getQxFileConfig().getImageMaxSize(FileSizeUtil.SIZETYPE_MB)) {
                                    item.isEnableChecked = false;
                                } else {
                                    item.isEnableChecked = true;
                                }
                            } else {
                                QLog.e(TAG, "视频大小：" + fileSize + "M");
                                // 视频文件或者其他文件类型，限制可以选择的最大Size
                                try {
                                    double max = QXConfigManager.getQxFileConfig().getVideoMaxDuration(TimeUnit.MINUTES);
                                    if (fileDuration > max) {
                                        item.isEnableChecked = false;
                                    } else {
                                        //大于20兆不许发送
                                        if (fileSize > 20) {
                                            item.isEnableChecked = false;
                                        } else {
                                            item.isEnableChecked = true;
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (item.uri != null && (item.mediaType != 3 || item.duration != 0) && (item.mediaType != 3 || "video/mp4".equals(item.mimeType))) {
                                File file = new File(item.uri);
                                if (file.exists() && file.length() != 0L) {
                                    PictureSelectorActivity.this.mAllItemList.add(item);
                                    int last = item.uri.lastIndexOf("/");
                                    if (last != -1) {
                                        String catalog;
                                        if (last == 0) {
                                            catalog = "/";
                                        } else {
                                            int secondLast = item.uri.lastIndexOf("/", last - 1);
                                            catalog = item.uri.substring(secondLast + 1, last);
                                        }

                                        if (PictureSelectorActivity.this.mItemMap.containsKey(catalog)) {
                                            ((List) PictureSelectorActivity.this.mItemMap.get(catalog)).add(item);
                                        } else {
                                            ArrayList<PictureSelectorActivity.MediaItem> itemList = new ArrayList();
                                            itemList.add(item);
                                            PictureSelectorActivity.this.mItemMap.put(catalog, itemList);
                                            PictureSelectorActivity.this.mCatalogList.add(catalog);
                                        }
                                    }
                                }
                            }
                        } while (cursor.moveToNext());
                    }

                    cursor.close();
                    if (iExecutedCallback != null) {
                        iExecutedCallback.executed();
                    }
                }

            }
        });
    }


    private int getTotalSelectedNum() {
        int sum = 0;
        Iterator var2 = this.mItemMap.keySet().iterator();

        while (true) {
            List mediaItemList;
            do {
                if (!var2.hasNext()) {
                    return sum;
                }

                String key = (String) var2.next();
                mediaItemList = (List) this.mItemMap.get(key);
            } while (mediaItemList == null);

            List<PictureSelectorActivity.MediaItem> tempList = new ArrayList(mediaItemList);
            Iterator var6 = tempList.iterator();

            while (var6.hasNext()) {
                PictureSelectorActivity.MediaItem item = (PictureSelectorActivity.MediaItem) var6.next();
                if (item.selected) {
                    ++sum;
                }
            }
        }
    }

    private void updateToolbar() {
        int sum = this.getTotalSelectedNum();
        if (sum == 0) {
            this.mBtnSend.setEnabled(false);
            this.mBtnSend.setTextColor(this.getResources().getColor(R.color.rc_picsel_toolbar_send_text_disable));
            this.mBtnSend.setText(R.string.qx_picsel_toolbar_send);
            this.mPreviewBtn.setEnabled(false);
            this.mPreviewBtn.setText(R.string.qx_picsel_toolbar_preview);
        } else if (sum <= 9) {
            this.mBtnSend.setEnabled(true);
            this.mBtnSend.setTextColor(this.getResources().getColor(R.color.rc_picsel_toolbar_send_text_normal));
            this.mBtnSend.setText(String.format(this.getResources().getString(R.string.qx_picsel_toolbar_send_num), sum));
            this.mPreviewBtn.setEnabled(true);
            this.mPreviewBtn.setText(String.format(this.getResources().getString(R.string.qx_picsel_toolbar_preview_num), sum));
        }

    }

    private PictureSelectorActivity.MediaItem getItemAt(int index) {
        int sum = 0;
        Iterator var3 = this.mItemMap.keySet().iterator();

        while (var3.hasNext()) {
            String key = (String) var3.next();

            for (Iterator var5 = ((List) this.mItemMap.get(key)).iterator(); var5.hasNext(); ++sum) {
                PictureSelectorActivity.MediaItem item = (PictureSelectorActivity.MediaItem) var5.next();
                if (sum == index) {
                    return item;
                }
            }
        }

        return null;
    }

    private PictureSelectorActivity.MediaItem getItemAt(String catalog, int index) {
        if (!this.mItemMap.containsKey(catalog)) {
            return null;
        } else {
            int sum = 0;

            for (Iterator var4 = ((List) this.mItemMap.get(catalog)).iterator(); var4.hasNext(); ++sum) {
                PictureSelectorActivity.MediaItem item = (PictureSelectorActivity.MediaItem) var4.next();
                if (sum == index) {
                    return item;
                }
            }

            return null;
        }
    }

    private PictureSelectorActivity.MediaItem findByUri(String uri) {
        Iterator var2 = this.mItemMap.keySet().iterator();

        while (var2.hasNext()) {
            String key = (String) var2.next();
            Iterator var4 = ((List) this.mItemMap.get(key)).iterator();

            while (var4.hasNext()) {
                PictureSelectorActivity.MediaItem item = (PictureSelectorActivity.MediaItem) var4.next();
                if (item.uri.equals(uri)) {
                    return item;
                }
            }
        }

        return null;
    }

    private void setImageViewBackground(String imagePath, ImageView imageView, int position) {
        Bitmap bitmap = AlbumBitmapCacheHelper.getInstance().getBitmap(imagePath, this.perWidth, this.perHeight, new AlbumBitmapCacheHelper.ILoadImageCallback() {
            public void onLoadImageCallBack(Bitmap bitmap, String path1, Object... objects) {
                if (bitmap != null) {
                    BitmapDrawable bd = new BitmapDrawable(PictureSelectorActivity.this.getResources(), bitmap);
                    View v = PictureSelectorActivity.this.mGridView.findViewWithTag(path1);
                    if (v != null) {
                        v.setBackgroundDrawable(bd);
                    }

                }
            }
        }, new Object[]{position});
        if (bitmap != null) {
            BitmapDrawable bd = new BitmapDrawable(this.getResources(), bitmap);
            imageView.setBackgroundDrawable(bd);
        } else {
            imageView.setBackgroundResource(R.drawable.rc_grid_image_default);
        }

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 100:
                if (grantResults[0] == 0) {
                    if (permissions[0].equals("android.permission.READ_EXTERNAL_STORAGE")) {
                        this.initView();
                    } else if (permissions[0].equals("android.permission.CAMERA")) {
                        this.requestCamera();
                    }
                } else if (permissions[0].equals("android.permission.CAMERA")) {
                    Toast.makeText(this.getApplicationContext(), this.getString(R.string.qx_permission_grant_needed), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this.getApplicationContext(), this.getString(R.string.qx_permission_grant_needed), Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    protected void onDestroy() {
        PictureSelectorActivity.PicItemHolder.itemList = null;
        PictureSelectorActivity.PicItemHolder.itemSelectedList = null;
        PictureSelectorActivity.PicItemHolder.itemAllSelectedMediaItemList = null;
        this.shutdownAndAwaitTermination(this.pool);
        this.thread.quit();
        this.bgHandler.removeCallbacks(this.thread);
        this.bgHandler = null;
        this.uiHandler = null;
        super.onDestroy();
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();

        try {
            if (!pool.awaitTermination(60L, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60L, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException var3) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }

    private String formatSize(long length) {
        float size;
        if (length > 1048576L) {
            size = (float) Math.round((float) length / 1048576.0F * 100.0F) / 100.0F;
            return size + "M";
        } else if (length > 1024L) {
            size = (float) Math.round((float) length / 1024.0F * 100.0F) / 100.0F;
            return size + "KB";
        } else {
            return length + "B";
        }
    }

    public static class PicItemHolder {
        public static ArrayList<PictureSelectorActivity.MediaItem> itemList;
        public static ArrayList<PictureSelectorActivity.MediaItem> itemSelectedList;
        public static ArrayList<PictureSelectorActivity.MediaItem> itemAllSelectedMediaItemList;

        PicItemHolder() {
        }
    }

    public static class SelectBox extends AppCompatImageView {
        private boolean mIsChecked;

        public SelectBox(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.setImageResource(R.drawable.rc_select_check_nor);
        }

        public void setChecked(boolean check) {
            this.mIsChecked = check;
            this.setImageResource(this.mIsChecked ? R.drawable.rc_select_check_sel : R.drawable.rc_select_check_nor);
        }

        public boolean getChecked() {
            return this.mIsChecked;
        }
    }

    public static class PreviewBtn extends LinearLayout {
        private TextView mText;

        public PreviewBtn(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void init(Activity root) {
            this.mText = (TextView) root.findViewById(R.id.ps_preview_text);
        }

        public void setText(int id) {
            this.mText.setText(id);
        }

        public void setText(String text) {
            this.mText.setText(text);
        }

        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            int color = enabled ? R.color.rc_picsel_toolbar_send_text_normal : R.color.rc_picsel_toolbar_send_text_disable;
            this.mText.setTextColor(this.getResources().getColor(color));
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (this.isEnabled()) {
                switch (event.getAction()) {
                    case 0:
                        this.mText.setVisibility(View.INVISIBLE);
                        break;
                    case 1:
                        this.mText.setVisibility(View.VISIBLE);
                }
            }

            return super.onTouchEvent(event);
        }
    }

    public static class PicTypeBtn extends LinearLayout {
        TextView mText;

        public PicTypeBtn(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void init(Activity root) {
            this.mText = (TextView) root.findViewById(R.id.ps_type_text);
        }

        public void setText(String text) {
            this.mText.setText(text);
        }

        public void setTextColor(int color) {
            this.mText.setTextColor(color);
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (this.isEnabled()) {
                switch (event.getAction()) {
                    case 0:
                        this.mText.setVisibility(View.INVISIBLE);
                        break;
                    case 1:
                        this.mText.setVisibility(View.VISIBLE);
                }
            }

            return super.onTouchEvent(event);
        }
    }

    public static class MediaItem implements Parcelable {
        public String name;
        public int mediaType;
        public String mimeType;
        public String uri;
        public boolean selected;
        public int duration;
        //是否可选
        public boolean isEnableChecked;
        public String uri_sdk29;
        public static final Creator<PictureSelectorActivity.MediaItem> CREATOR = new Creator<PictureSelectorActivity.MediaItem>() {
            public PictureSelectorActivity.MediaItem createFromParcel(Parcel source) {
                return new PictureSelectorActivity.MediaItem(source);
            }

            public PictureSelectorActivity.MediaItem[] newArray(int size) {
                return new PictureSelectorActivity.MediaItem[size];
            }
        };

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.name);
            dest.writeInt(this.mediaType);
            dest.writeString(this.mimeType);
            dest.writeString(this.uri);
            dest.writeByte((byte) (this.selected ? 1 : 0));
            dest.writeInt(this.duration);
            dest.writeInt((byte) (this.isEnableChecked ? 1 : 0));
            dest.writeString(this.uri_sdk29);
        }

        public MediaItem() {
        }

        protected MediaItem(Parcel in) {
            this.name = in.readString();
            this.mediaType = in.readInt();
            this.mimeType = in.readString();
            this.uri = in.readString();
            this.selected = in.readByte() != 0;
            this.isEnableChecked = in.readByte() != 0;
            this.duration = in.readInt();
            this.uri_sdk29 = in.readString();
        }
    }

    private class CatalogAdapter extends BaseAdapter {
        private LayoutInflater mInflater = PictureSelectorActivity.this.getLayoutInflater();

        public CatalogAdapter() {
        }

        public int getCount() {
            return PictureSelectorActivity.this.mItemMap.size() + 1;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            final PictureSelectorActivity.CatalogAdapter.ViewHolder holder;
            if (convertView == null) {
                view = this.mInflater.inflate(R.layout.imui_pic_select_catalog, parent, false);
                holder = new PictureSelectorActivity.CatalogAdapter.ViewHolder();
                holder.image = (ImageView) view.findViewById(R.id.ps_grid_image);
                holder.name = (TextView) view.findViewById(R.id.ps_grid_name);
                holder.number = (TextView) view.findViewById(R.id.ps_grid_number);
                holder.selected = (ImageView) view.findViewById(R.id.ps_grid_selected);
                view.setTag(holder);
            } else {
                holder = (PictureSelectorActivity.CatalogAdapter.ViewHolder) convertView.getTag();
            }

            String path;
            if (holder.image.getTag() != null) {
                path = (String) holder.image.getTag();
                AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(path);
            }

            path = "";
            int num = 0;
            boolean showSelected = false;
            String name;
            if (position == 0) {
                if (PictureSelectorActivity.this.mItemMap.size() == 0) {
                    holder.image.setImageResource(R.drawable.rc_picsel_empty_pic);
                } else {
                    List<PictureSelectorActivity.MediaItem> mediaItems = (List) PictureSelectorActivity.this.mItemMap.get(PictureSelectorActivity.this.mCatalogList.get(0));
                    if (mediaItems != null && mediaItems.size() > 0) {
                        final PictureSelectorActivity.MediaItem mediaItem = (PictureSelectorActivity.MediaItem) mediaItems.get(0);
                        if (mediaItem.mediaType == 1) {
                            if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                path = mediaItem.uri_sdk29;
                            } else {
                                path = mediaItem.uri;
                            }
                        } else {
                            path = KitStorageUtils.getImageSavePath(PictureSelectorActivity.this) + File.separator + mediaItem.name;
                            if (!(new File(path)).exists()) {
                                (new Thread(new Runnable() {
                                    @RequiresApi(
                                            api = 28
                                    )
                                    public void run() {
                                        Bitmap videoFrame = null;
                                        if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                            try {
                                                MediaMetadataRetriever media = new MediaMetadataRetriever();
                                                media.setDataSource(PictureSelectorActivity.this.getApplicationContext(), Uri.parse(mediaItem.uri_sdk29));
                                                videoFrame = media.getFrameAtTime();
                                            } catch (Exception var3) {
                                                QLog.e(TAG, "video get thumbnail error" + var3);
                                            }
                                        } else {
                                            videoFrame = ThumbnailUtils.createVideoThumbnail(mediaItem.uri, 1);
                                        }

                                        if (videoFrame != null) {
                                            final File captureImageFile = FileUtils.convertBitmap2File(videoFrame, KitStorageUtils.getImageSavePath(PictureSelectorActivity.this), mediaItem.name);
                                            PictureSelectorActivity.this.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    PictureSelectorActivity.this.setImageViewBackground(captureImageFile.getAbsolutePath(), holder.image, position);
                                                }
                                            });
                                        }

                                    }
                                })).start();
                            }
                        }
                    }

                    if (!TextUtils.isEmpty(path)) {
                        AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
                        holder.image.setTag(path);
                        PictureSelectorActivity.this.setImageViewBackground(path, holder.image, position);
                    }
                }

                name = PictureSelectorActivity.this.getResources().getString(R.string.qx_picsel_catalog_allpic);
                holder.number.setVisibility(View.GONE);
                showSelected = PictureSelectorActivity.this.mCurrentCatalog.isEmpty();
            } else {
                final PictureSelectorActivity.MediaItem mediaItemx = (PictureSelectorActivity.MediaItem) ((List) PictureSelectorActivity.this.mItemMap.get(PictureSelectorActivity.this.mCatalogList.get(position - 1))).get(0);
                if (mediaItemx.mediaType == 1) {
                    if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                        path = mediaItemx.uri_sdk29;
                    } else {
                        path = mediaItemx.uri;
                    }
                } else {
                    path = KitStorageUtils.getImageSavePath(PictureSelectorActivity.this) + File.separator + mediaItemx.name;
                    if (!(new File(path)).exists()) {
                        (new Thread(new Runnable() {
                            @RequiresApi(
                                    api = 28
                            )
                            public void run() {
                                Bitmap videoFrame = null;
                                if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                    try {
                                        MediaMetadataRetriever media = new MediaMetadataRetriever();
                                        media.setDataSource(PictureSelectorActivity.this.getApplicationContext(), Uri.parse(mediaItemx.uri_sdk29));
                                        videoFrame = media.getFrameAtTime();
                                    } catch (Exception var3) {
                                        QLog.e(TAG, "video get thumbnail error" + var3);
                                    }
                                } else {
                                    videoFrame = ThumbnailUtils.createVideoThumbnail(mediaItemx.uri, 1);
                                }

                                if (videoFrame != null) {
                                    final File captureImageFile = FileUtils.convertBitmap2File(videoFrame, KitStorageUtils.getImageSavePath(PictureSelectorActivity.this), mediaItemx.name);
                                    PictureSelectorActivity.this.runOnUiThread(new Runnable() {
                                        public void run() {
                                            PictureSelectorActivity.this.setImageViewBackground(captureImageFile.getAbsolutePath(), holder.image, position);
                                        }
                                    });
                                }

                            }
                        })).start();
                    }
                }

                name = (String) PictureSelectorActivity.this.mCatalogList.get(position - 1);
                num = ((List) PictureSelectorActivity.this.mItemMap.get(PictureSelectorActivity.this.mCatalogList.get(position - 1))).size();
                holder.number.setVisibility(View.VISIBLE);
                showSelected = name.equals(PictureSelectorActivity.this.mCurrentCatalog);
                AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
                holder.image.setTag(path);
                PictureSelectorActivity.this.setImageViewBackground(path, holder.image, position);
            }

            holder.name.setText(name);
            holder.number.setText(String.format(PictureSelectorActivity.this.getResources().getString(R.string.qx_picsel_catalog_number), num));
            holder.selected.setVisibility(showSelected ? View.VISIBLE : View.INVISIBLE);
            return view;
        }

        private class ViewHolder {
            ImageView image;
            TextView name;
            TextView number;
            ImageView selected;

            private ViewHolder() {
            }
        }
    }

    private class GridViewAdapter extends BaseAdapter {
        private LayoutInflater mInflater = PictureSelectorActivity.this.getLayoutInflater();

        public GridViewAdapter() {
        }

        public int getCount() {
            int sum = 1;
            if (PictureSelectorActivity.this.mCurrentCatalog.isEmpty()) {
                Iterator var2 = PictureSelectorActivity.this.mItemMap.keySet().iterator();

                while (var2.hasNext()) {
                    String key = (String) var2.next();
                    List<PictureSelectorActivity.MediaItem> mediaItems = (List) PictureSelectorActivity.this.mItemMap.get(key);
                    if (mediaItems != null) {
                        sum += mediaItems.size();
                    }
                }
            } else {
                List<PictureSelectorActivity.MediaItem> mediaItemsx = (List) PictureSelectorActivity.this.mItemMap.get(PictureSelectorActivity.this.mCurrentCatalog);
                if (mediaItemsx != null) {
                    sum += mediaItemsx.size();
                }
            }

            return sum;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return (long) position;
        }

        @TargetApi(23)
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (position == 0) {
                View view = this.mInflater.inflate(R.layout.imui_pic_sel_grid_camera, parent, false);
                ImageButton mask = (ImageButton) view.findViewById(R.id.camera_mask);
                mask.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String[] permissions = new String[]{"android.permission.CAMERA"};
                        if (!PermissionCheckUtil.checkPermissions(PictureSelectorActivity.this, permissions)) {
                            PermissionCheckUtil.requestPermissions(PictureSelectorActivity.this, permissions, 100);
                        } else {
                            PictureSelectorActivity.this.requestCamera();
                        }
                    }
                });
                return view;
            } else {
                final PictureSelectorActivity.MediaItem item;
                if (PictureSelectorActivity.this.mCurrentCatalog.isEmpty()) {
                    item = (PictureSelectorActivity.MediaItem) PictureSelectorActivity.this.mAllItemList.get(position - 1);
                } else {
                    item = PictureSelectorActivity.this.getItemAt(PictureSelectorActivity.this.mCurrentCatalog, position - 1);
                }

                View viewx = convertView;
                final PictureSelectorActivity.GridViewAdapter.ViewHolder holder;
                if (convertView != null && convertView.getTag() != null) {
                    holder = (PictureSelectorActivity.GridViewAdapter.ViewHolder) convertView.getTag();
                } else {
                    viewx = this.mInflater.inflate(R.layout.imui_pic_sel_grid_item, parent, false);
                    holder = new PictureSelectorActivity.GridViewAdapter.ViewHolder();
                    holder.image = (ImageView) viewx.findViewById(R.id.ps_grid_image);
                    holder.mask = viewx.findViewById(R.id.ps_grid_mask);
                    holder.checkBox = (PictureSelectorActivity.SelectBox) viewx.findViewById(R.id.ps_grid_checkbox);
                    holder.videoContainer = viewx.findViewById(R.id.ps_grid_video_container);
                    holder.videoDuration = (TextView) viewx.findViewById(R.id.ps_grid_video_duration);
                    viewx.setTag(holder);
                }

                String thumbImagePath;
                if (holder.image.getTag() != null) {
                    thumbImagePath = (String) holder.image.getTag();
                    AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(thumbImagePath);
                }

                thumbImagePath = "";
                if (item != null) {
                    switch (item.mediaType) {
                        case 1:
                            if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                thumbImagePath = item.uri_sdk29;
                            } else {
                                thumbImagePath = item.uri;
                            }
                            break;
                        case 3:
                            thumbImagePath = KitStorageUtils.getImageSavePath(PictureSelectorActivity.this) + File.separator + item.name;
                            if (!(new File(thumbImagePath)).exists()) {
                                Runnable runnable = new Runnable() {
                                    @RequiresApi(
                                            api = 28
                                    )
                                    public void run() {
                                        Bitmap videoFrame = null;
                                        if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                            try {
                                                MediaMetadataRetriever media = new MediaMetadataRetriever();
                                                media.setDataSource(PictureSelectorActivity.this.getApplicationContext(), Uri.parse(item.uri_sdk29));
                                                videoFrame = media.getFrameAtTime();
                                            } catch (Exception var3) {
                                                QLog.e(TAG, "video get thumbnail error" + var3);
                                            }
                                        } else {
                                            videoFrame = ThumbnailUtils.createVideoThumbnail(item.uri, 1);
                                        }

                                        if (videoFrame != null) {
                                            final File captureImageFile = FileUtils.convertBitmap2File(videoFrame, KitStorageUtils.getImageSavePath(PictureSelectorActivity.this), item.name);
                                            PictureSelectorActivity.this.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    PictureSelectorActivity.this.setImageViewBackground(captureImageFile.getAbsolutePath(), holder.image, position);
                                                }
                                            });
                                        }

                                    }
                                };
                                PictureSelectorActivity.this.pool.execute(runnable);
                            }
                    }

                    AlbumBitmapCacheHelper.getInstance().addPathToShowlist(thumbImagePath);
                    holder.image.setTag(thumbImagePath);
                    PictureSelectorActivity.this.setImageViewBackground(thumbImagePath, holder.image, position);
                    if (item.mediaType == 3) {
                        holder.videoContainer.setVisibility(View.VISIBLE);
                        long minutes = TimeUnit.MILLISECONDS.toMinutes((long) item.duration);
                        long seconds = TimeUnit.MILLISECONDS.toSeconds((long) item.duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) item.duration));
                        holder.videoDuration.setText(String.format(Locale.CHINA, seconds < 10L ? "%d:0%d" : "%d:%d", minutes, seconds));
                    } else {
                        holder.videoContainer.setVisibility(View.GONE);
                    }

                    holder.checkBox.setChecked(item.selected);
                    holder.checkBox.setOnClickListener(new View.OnClickListener() {
                        @SuppressLint("StringFormatInvalid")
                        public void onClick(View v) {
                            int maxDuration;
                            if (item.mediaType == 3) {
                                if (TextUtils.isEmpty(holder.videoDuration.getText())) {
                                    return;
                                }

                                maxDuration = QXIMClient.getInstance().getVideoLimitTime();
                                if (maxDuration < 1) {
                                    maxDuration = 300;
                                }

                                String[] videoTime = holder.videoDuration.getText().toString().split(":");
                                if (Integer.parseInt(videoTime[0]) * 60 + Integer.parseInt(videoTime[1]) > maxDuration) {
                                    (new AlertDialog.Builder(PictureSelectorActivity.this)).setMessage(getResources().getString(R.string.qx_picsel_selected_max_time_span_with_param, new Object[]{maxDuration / 60})).setPositiveButton(R.string.qx_confirm, (android.content.DialogInterface.OnClickListener) null).setCancelable(false).create().show();
                                    return;
                                }
//                                try {
//                                    File file = new File(item.uri);
//                                    double fileSize = FileSizeUtil.getFileOrFilesSize(file, 3);
//                                    double maxFileSize = QXConfigManager.getQxFileConfig().getFileMessageMaxSize((int) fileSize);
//                                    if (fileSize > maxFileSize) {
//                                        Toast.makeText(PictureSelectorActivity.this, StringUtils.getResourceStr(PictureSelectorActivity.this, Integer.parseInt(PictureSelectorActivity.this.getResources().getString(R.string.qx_error_file_exceeded)), maxFileSize), Toast.LENGTH_SHORT).show();
//                                        item.selected = false;
//                                    } else {
//                                        item.selected = true;
//                                    }
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
                            }

                            if (item.uri.endsWith(".gif")) {
                                maxDuration = QXIMClient.getInstance().getGIFLimitSize() * 1024;
                                File file = new File(item.uri);
                                if (file != null && file.exists() && file.length() > (long) maxDuration) {
                                    (new AlertDialog.Builder(PictureSelectorActivity.this)).setMessage(PictureSelectorActivity.this.getResources().getString(R.string.qx_picsel_selected_max_gif_size_span_with_param)).setPositiveButton(R.string.qx_confirm, (android.content.DialogInterface.OnClickListener) null).setCancelable(false).create().show();
                                    return;
                                }
                            }

                            if (!holder.checkBox.getChecked() && PictureSelectorActivity.this.getTotalSelectedNum() == 9) {
                                Toast.makeText(PictureSelectorActivity.this, R.string.qx_picsel_selected_max_pic_count, Toast.LENGTH_SHORT).show();
                            } else {
                                holder.checkBox.setChecked(!holder.checkBox.getChecked());
                                item.selected = holder.checkBox.getChecked();
                                if (item.selected) {
                                    PictureSelectorActivity.this.mAllSelectedItemList.add(Uri.parse("file://" + item.uri));
                                    if (PicItemHolder.itemAllSelectedMediaItemList != null) {
                                        PicItemHolder.itemAllSelectedMediaItemList.add(item);
                                    }

                                    holder.mask.setBackgroundColor(PictureSelectorActivity.this.getResources().getColor(R.color.rc_picsel_grid_mask_pressed));
                                } else {
                                    try {
                                        PictureSelectorActivity.this.mAllSelectedItemList.remove(Uri.parse("file://" + item.uri));
                                    } catch (Exception var4) {
                                        QLog.e(TAG, "GridViewAdapter getView" + var4);
                                    }

                                    if (PicItemHolder.itemAllSelectedMediaItemList != null) {
                                        PicItemHolder.itemAllSelectedMediaItemList.remove(item);
                                    }

                                    holder.mask.setBackgroundDrawable(PictureSelectorActivity.this.getResources().getDrawable(R.drawable.imui_sp_grid_mask));
                                }

                                PictureSelectorActivity.this.updateToolbar();
                            }
                        }
                    });
                    if (item.selected) {
                        holder.mask.setBackgroundColor(PictureSelectorActivity.this.getResources().getColor(R.color.rc_picsel_grid_mask_pressed));
                    } else {
                        holder.mask.setBackgroundDrawable(PictureSelectorActivity.this.getResources().getDrawable(R.drawable.imui_sp_grid_mask));
                    }

                    //过大文件不给选中
                    if (!item.isEnableChecked) {
                        holder.checkBox.setVisibility(View.GONE);
                        holder.mask.setBackgroundColor(PictureSelectorActivity.this.getResources().getColor(R.color.rc_picsel_grid_mask_pressed));
                    } else {
                        holder.checkBox.setVisibility(View.VISIBLE);
                        holder.mask.setBackgroundDrawable(PictureSelectorActivity.this.getResources().getDrawable(R.drawable.imui_sp_grid_mask));
                    }
                }
                return viewx;
            }
        }

        private class ViewHolder {
            ImageView image;
            View mask;
            PictureSelectorActivity.SelectBox checkBox;
            View videoContainer;
            TextView videoDuration;

            private ViewHolder() {
            }
        }
    }

    interface IExecutedCallback {
        void executed();
    }
}


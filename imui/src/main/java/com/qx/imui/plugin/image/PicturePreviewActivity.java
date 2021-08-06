package com.qx.imui.plugin.image;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.qx.im.model.FileInfo;
import com.qx.imlib.QXIMClient;
import com.qx.imlib.qlog.QLog;
import com.qx.imui.R;
import com.qx.imui.base.BaseActivity;
import com.qx.imui.plugin.AlbumBitmapCacheHelper;
import com.qx.imui.util.FileUtils;
import com.qx.imui.util.KitStorageUtils;
import com.qx.imui.view.subscale.ImageSource;
import com.qx.imui.view.subscale.SubsamplingScaleImageView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;


public class PicturePreviewActivity extends BaseActivity {

    @Override
    public int getLayoutId() {
        return R.layout.imui_pic_preview;
    }
    private static final String TAG = "PicturePreviewActivity";
    public static final int RESULT_SEND = 1;
    private TextView mIndexTotal;
    private View mWholeView;
    private View mToolbarTop;
    private View mToolbarBottom;
    private ImageButton mBtnBack;
    private Button mBtnSend;
    private PicturePreviewActivity.CheckButton mUseOrigin;
    private PicturePreviewActivity.CheckButton mSelectBox;
    private com.qx.imui.plugin.image.HackyViewPager mViewPager;
    private ArrayList<PictureSelectorActivity.MediaItem> mItemList;
    private ArrayList<PictureSelectorActivity.MediaItem> mItemSelectedList;
    private int mCurrentIndex;
    private boolean mFullScreen;

    public PicturePreviewActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        this.initView();
        this.mUseOrigin.setChecked(this.getIntent().getBooleanExtra("sendOrigin", false));
        this.mCurrentIndex = this.getIntent().getIntExtra("index", 0);
        if (this.mItemList == null) {
            this.mItemList = PictureSelectorActivity.PicItemHolder.itemList;
            this.mItemSelectedList = PictureSelectorActivity.PicItemHolder.itemSelectedList;
        }

        if (this.mItemList == null) {
            QLog.d(TAG, "Itemlist is null");
        } else {
            this.mIndexTotal.setText(String.format("%d/%d", this.mCurrentIndex + 1, this.mItemList.size()));
            int result;
            if (Build.VERSION.SDK_INT >= 11) {
                this.mWholeView.setSystemUiVisibility(1024);
                result = getSmartBarHeight(this);
                if (result > 0) {
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)this.mToolbarBottom.getLayoutParams();
                    lp.setMargins(0, 0, 0, result);
                    this.mToolbarBottom.setLayoutParams(lp);
                }
            }

            result = 0;
            int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = this.getResources().getDimensionPixelSize(resourceId);
            }

            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(this.mToolbarTop.getLayoutParams());
            lp.setMargins(0, result, 0, 0);
            this.mToolbarTop.setLayoutParams(lp);
            this.mBtnBack.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.putExtra("sendOrigin", PicturePreviewActivity.this.mUseOrigin.getChecked());
                    PicturePreviewActivity.this.setResult(-1, intent);
                    PicturePreviewActivity.this.finish();
                }
            });
            this.mBtnSend.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    LinkedHashMap<String, Integer> mLinkedHashMap = new LinkedHashMap();
                    Iterator var3;
                    PictureSelectorActivity.MediaItem item;
                    String filePath;
                    String fileName;
                    boolean result;
                    if (PicturePreviewActivity.this.mItemSelectedList != null) {
                        var3 = PicturePreviewActivity.this.mItemSelectedList.iterator();

                        while(var3.hasNext()) {
                            item = (PictureSelectorActivity.MediaItem)var3.next();
                            if (item.selected) {
                                if (KitStorageUtils.isBuildAndTargetForQ(PicturePreviewActivity.this.getApplicationContext())) {
                                    fileName = FileUtils.getFileNameWithPath(item.uri);
                                    if (item.mediaType == 1) {
                                        filePath = KitStorageUtils.getImageSavePath(PicturePreviewActivity.this.getApplicationContext()) + File.separator + fileName;
                                    } else if (item.mediaType == 3) {
                                        filePath = KitStorageUtils.getVideoSavePath(PicturePreviewActivity.this.getApplicationContext()) + File.separator + fileName;
                                    } else {
                                        filePath = KitStorageUtils.getFileSavePath(PicturePreviewActivity.this.getApplicationContext()) + File.separator + fileName;
                                    }

                                    result = FileUtils.copyFile(v.getContext(), Uri.parse(item.uri_sdk29), filePath);
                                    if (result) {
                                        mLinkedHashMap.put("file://" + filePath, item.mediaType);
                                    }
                                } else {
                                    mLinkedHashMap.put("file://" + item.uri, item.mediaType);
                                }
                            }
                        }
                    }

                    var3 = PicturePreviewActivity.this.mItemList.iterator();

                    while(var3.hasNext()) {
                        item = (PictureSelectorActivity.MediaItem)var3.next();
                        if (item.selected) {
                            if (KitStorageUtils.isBuildAndTargetForQ(PicturePreviewActivity.this.getApplicationContext())) {
                                fileName = FileUtils.getFileNameWithPath(item.uri);
                                if (item.mediaType == 1) {
                                    filePath = KitStorageUtils.getImageSavePath(PicturePreviewActivity.this.getApplicationContext()) + File.separator + fileName;
                                } else if (item.mediaType == 3) {
                                    filePath = KitStorageUtils.getVideoSavePath(PicturePreviewActivity.this.getApplicationContext()) + File.separator + fileName;
                                } else {
                                    filePath = KitStorageUtils.getFileSavePath(PicturePreviewActivity.this.getApplicationContext()) + File.separator + fileName;
                                }

                                result = FileUtils.copyFile(PicturePreviewActivity.this.getApplicationContext(), Uri.parse(item.uri_sdk29), filePath);
                                if (result) {
                                    mLinkedHashMap.put("file://" + filePath, item.mediaType);
                                }
                            } else {
                                mLinkedHashMap.put("file://" + item.uri, item.mediaType);
                            }
                        }
                    }

                    Gson gson = new Gson();
                    String mediaList = gson.toJson(mLinkedHashMap);
                    Intent data = new Intent();
                    data.putExtra("sendOrigin", PicturePreviewActivity.this.mUseOrigin.getChecked());
                    data.putExtra("android.intent.extra.RETURN_RESULT", mediaList);
                    PicturePreviewActivity.this.setResult(RESULT_SEND, data);
                    PicturePreviewActivity.this.finish();
                }
            });
            this.mUseOrigin.setText(R.string.qx_picprev_origin);
            this.mUseOrigin.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PicturePreviewActivity.this.mUseOrigin.setChecked(!PicturePreviewActivity.this.mUseOrigin.getChecked());
                    if (PicturePreviewActivity.this.mUseOrigin.getChecked() && PicturePreviewActivity.this.getTotalSelectedNum() == 0) {
                        PicturePreviewActivity.this.mSelectBox.setChecked(!PicturePreviewActivity.this.mSelectBox.getChecked());
                        ((PictureSelectorActivity.MediaItem)PicturePreviewActivity.this.mItemList.get(PicturePreviewActivity.this.mCurrentIndex)).selected = PicturePreviewActivity.this.mSelectBox.getChecked();
                        PicturePreviewActivity.this.updateToolbar();
                    }

                }
            });
            this.mSelectBox.setText(R.string.qx_picprev_select);
            this.mSelectBox.setChecked(((PictureSelectorActivity.MediaItem)this.mItemList.get(this.mCurrentIndex)).selected);
            this.mSelectBox.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("StringFormatInvalid")
                public void onClick(View v) {
                    PictureSelectorActivity.MediaItem item = (PictureSelectorActivity.MediaItem)PicturePreviewActivity.this.mItemList.get(PicturePreviewActivity.this.mCurrentIndex);
                    if (item.mediaType == 3) {
                        int maxDuration = QXIMClient.getInstance().getVideoLimitTime();
                        if (maxDuration < 1) {
                            maxDuration = 300;
                        }

                        if (TimeUnit.MILLISECONDS.toSeconds((long)item.duration) > (long)maxDuration) {
                            (new AlertDialog.Builder(PicturePreviewActivity.this)).setMessage
                                    (PicturePreviewActivity.this.getResources().getString(R.string.qx_picsel_selected_max_time_span_with_param,
                                            new Object[]{maxDuration / 60})).setPositiveButton(R.string.qx_confirm,
                                    (android.content.DialogInterface.OnClickListener)null).setCancelable(false).create().show();
                            return;
                        }
                    }

                    if (!PicturePreviewActivity.this.mSelectBox.getChecked() && PicturePreviewActivity.this.getTotalSelectedNum() == 9) {
                        Toast.makeText(PicturePreviewActivity.this, R.string.qx_picsel_selected_max_pic_count, Toast.LENGTH_SHORT).show();
                    } else {
                        PicturePreviewActivity.this.mSelectBox.setChecked(!PicturePreviewActivity.this.mSelectBox.getChecked());
                        ((PictureSelectorActivity.MediaItem)PicturePreviewActivity.this.mItemList.get(PicturePreviewActivity.this.mCurrentIndex)).selected = PicturePreviewActivity.this.mSelectBox.getChecked();
                        PicturePreviewActivity.this.updateToolbar();
                    }
                }
            });
            this.mViewPager.setAdapter(new PicturePreviewActivity.PreviewAdapter());
            this.mViewPager.setCurrentItem(this.mCurrentIndex);
            this.mViewPager.setOffscreenPageLimit(1);
            this.mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                public void onPageSelected(int position) {
                    PicturePreviewActivity.this.mCurrentIndex = position;
                    PicturePreviewActivity.this.mIndexTotal.setText(String.format("%d/%d", position + 1, PicturePreviewActivity.this.mItemList.size()));
                    PicturePreviewActivity.this.mSelectBox.setChecked(((PictureSelectorActivity.MediaItem)PicturePreviewActivity.this.mItemList.get(position)).selected);
                    PictureSelectorActivity.MediaItem mediaItem = (PictureSelectorActivity.MediaItem)PicturePreviewActivity.this.mItemList.get(position);
                    PicturePreviewActivity.this.updateToolbar();
                    if (mediaItem.mediaType == 3) {
                        PicturePreviewActivity.this.mUseOrigin.rootView.setVisibility(View.GONE);
                    } else {
                        PicturePreviewActivity.this.mUseOrigin.rootView.setVisibility(View.VISIBLE);
                    }

                }

                public void onPageScrollStateChanged(int state) {
                }
            });
            this.updateToolbar();
        }
    }

    public void initView() {
        this.mToolbarTop = this.findViewById(R.id.pp_toolbar_top);
        this.mIndexTotal = (TextView)this.findViewById(R.id.pp_index_total);
        this.mBtnBack = (ImageButton)this.findViewById(R.id.pp_back);
        this.mBtnSend = (Button)this.findViewById(R.id.pp_send);
        this.mWholeView = this.findViewById(R.id.pp_whole_layout);
        this.mViewPager = (HackyViewPager)this.findViewById(R.id.pp_viewpager);
        this.mToolbarBottom = this.findViewById(R.id.pp_toolbar_bottom);
        this.mUseOrigin = new PicturePreviewActivity.CheckButton(this.findViewById(R.id.pp_origin_check),R.drawable.rc_origin_check_nor, R.drawable.rc_origin_check_sel);
        this.mSelectBox = new PicturePreviewActivity.CheckButton(this.findViewById(R.id.pp_select_check), R.drawable.rc_select_check_nor, R.drawable.rc_select_check_sel);
    }

    protected void onResume() {
        super.onResume();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            Intent intent = new Intent();
            intent.putExtra("sendOrigin", this.mUseOrigin.getChecked());
            this.setResult(-1, intent);
        }

        return super.onKeyDown(keyCode, event);
    }

    private int getTotalSelectedNum() {
        int sum = 0;

        for(int i = 0; i < this.mItemList.size(); ++i) {
            if (((PictureSelectorActivity.MediaItem)this.mItemList.get(i)).selected) {
                ++sum;
            }
        }

        if (this.mItemSelectedList != null) {
            sum += this.mItemSelectedList.size();
        }

        return sum;
    }

    private String getTotalSelectedSize() {
        float size = 0.0F;

        int i;
        File file;
        for(i = 0; i < this.mItemList.size(); ++i) {
            if (((PictureSelectorActivity.MediaItem)this.mItemList.get(i)).selected) {
                file = new File(((PictureSelectorActivity.MediaItem)this.mItemList.get(i)).uri);
                size += (float)(file.length() / 1024L);
            }
        }

        if (this.mItemSelectedList != null) {
            for(i = 0; i < this.mItemSelectedList.size(); ++i) {
                if (((PictureSelectorActivity.MediaItem)this.mItemSelectedList.get(i)).selected) {
                    file = new File(((PictureSelectorActivity.MediaItem)this.mItemSelectedList.get(i)).uri);
                    size += (float)(file.length() / 1024L);
                }
            }
        }

        String totalSize;
        if (size < 1024.0F) {
            totalSize = String.format("%.0fK", size);
        } else {
            totalSize = String.format("%.1fM", size / 1024.0F);
        }

        return totalSize;
    }

    private String getSelectedSize(int index) {
        float size = 0.0F;
        if (this.mItemList != null && this.mItemList.size() > 0) {
            long maxSize = 0L;
            if (KitStorageUtils.isBuildAndTargetForQ(this)) {
                FileInfo fileInfo = FileUtils.getFileInfoByUri(this, Uri.parse(((PictureSelectorActivity.MediaItem)this.mItemList.get(index)).uri_sdk29));
                if (fileInfo != null) {
                    maxSize = fileInfo.getSize();
                }
            } else {
                maxSize = (new File(((PictureSelectorActivity.MediaItem)this.mItemList.get(index)).uri)).length();
            }
            size = (float)maxSize / 1024.0F;
        }

        String returnSize;
        if (size < 1024.0F) {
            returnSize = String.format("%.0fK", size);
        } else {
            returnSize = String.format("%.1fM", size / 1024.0F);
        }

        return returnSize;
    }

    private void updateToolbar() {
        int selNum = this.getTotalSelectedNum();
        if (this.mItemList.size() == 1 && selNum == 0) {
            this.mBtnSend.setText(R.string.qx_picsel_toolbar_send);
            this.mUseOrigin.setText(R.string.qx_picprev_origin);
            this.mBtnSend.setEnabled(false);
            this.mBtnSend.setTextColor(this.getResources().getColor(R.color.rc_picsel_toolbar_send_text_disable));
        } else {
            if (selNum == 0) {
                this.mBtnSend.setText(R.string.qx_picsel_toolbar_send);
                this.mUseOrigin.setText(R.string.qx_picprev_origin);
                this.mUseOrigin.setChecked(false);
                this.mBtnSend.setEnabled(false);
                this.mBtnSend.setTextColor(this.getResources().getColor(R.color.rc_picsel_toolbar_send_text_disable));
            } else if (selNum <= 9) {
                this.mBtnSend.setEnabled(true);
                this.mBtnSend.setTextColor(this.getResources().getColor(R.color.rc_picsel_toolbar_send_text_normal));
                this.mBtnSend.setText(String.format(this.getResources().getString(R.string.qx_picsel_toolbar_send_num), selNum));
            }

            this.mUseOrigin.setText(String.format(this.getResources().getString(R.string.qx_picprev_origin_size), this.getSelectedSize(this.mCurrentIndex)));
            PictureSelectorActivity.MediaItem mediaItem = (PictureSelectorActivity.MediaItem)this.mItemList.get(this.mCurrentIndex);
            if (mediaItem.mediaType == 3) {
                this.mUseOrigin.rootView.setVisibility(View.GONE);
            } else {
                this.mUseOrigin.rootView.setVisibility(View.VISIBLE);
            }

        }
    }

    @TargetApi(11)
    public static int getSmartBarHeight(Context context) {
        try {
            Class c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("mz_action_button_min_height");
            int height = Integer.parseInt(field.get(obj).toString());
            return context.getResources().getDimensionPixelSize(height);
        } catch (Exception var5) {
            QLog.e(TAG, "getSmartBarHeight"+var5);
            return 0;
        }
    }

    public int readPictureDegree(String path) {
        short degree = 0;

        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt("Orientation", 1);
            switch(orientation) {
                case 3:
                    degree = 180;
                    break;
                case 6:
                    degree = 90;
                    break;
                case 8:
                    degree = 270;
            }
        } catch (IOException var5) {
            var5.printStackTrace();
        }

        return degree;
    }


    private static class CheckButton {
        private View rootView;
        private ImageView image;
        private TextView text;
        private boolean checked = false;
        private int nor_resId;
        private int sel_resId;

        public CheckButton(View root, @DrawableRes int norId, @DrawableRes int selId) {
            this.rootView = root;
            this.image = (ImageView)root.findViewById(R.id.pp_image);
            this.text = (TextView)root.findViewById(R.id.pp_text);
            this.nor_resId = norId;
            this.sel_resId = selId;
            this.image.setImageResource(this.nor_resId);
        }

        public void setChecked(boolean check) {
            this.checked = check;
            this.image.setImageResource(this.checked ? this.sel_resId : this.nor_resId);
        }

        public boolean getChecked() {
            return this.checked;
        }

        public void setText(int resId) {
            this.text.setText(resId);
        }

        public void setText(CharSequence chars) {
            this.text.setText(chars);
        }

        public void setOnClickListener(@Nullable View.OnClickListener l) {
            this.rootView.setOnClickListener(l);
        }
    }

    private class PreviewAdapter extends PagerAdapter {
        private PreviewAdapter() {
        }

        public int getCount() {
            return PicturePreviewActivity.this.mItemList.size();
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public Object instantiateItem(ViewGroup container, int position) {
            final PictureSelectorActivity.MediaItem mediaItem = (PictureSelectorActivity.MediaItem)PicturePreviewActivity.this.mItemList.get(position);
            View view = LayoutInflater.from(container.getContext()).inflate(R.layout.imui_pic_sel_preview, container, false);
            SubsamplingScaleImageView subsamplingScaleImageView = (SubsamplingScaleImageView)view.findViewById(R.id.ps_photoView);
            ImageButton playButton = (ImageButton)view.findViewById(R.id.ps_play_video);
            container.addView(view, -1, -1);
            String imagePath;
            if (mediaItem.mediaType == 3) {
                imagePath = KitStorageUtils.getImageSavePath(PicturePreviewActivity.this) + File.separator + mediaItem.name;
                if (!(new File(imagePath)).exists()) {
                    Bitmap videoFrame = ThumbnailUtils.createVideoThumbnail(mediaItem.uri, 1);
                    if (videoFrame != null) {
                        imagePath = FileUtils.convertBitmap2File(videoFrame, KitStorageUtils.getImageSavePath(PicturePreviewActivity.this), mediaItem.name).toString();
                    } else {
                        imagePath = "";
                    }
                }

                playButton.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint("WrongConstant")
                    public void onClick(View v) {
                        Intent intent = new Intent("android.intent.action.VIEW");
                        if (Build.VERSION.SDK_INT >= 24) {
                            intent.setFlags(335544320);
                            Uri uri = FileProvider.getUriForFile(v.getContext(), v.getContext().getPackageName() + v.getContext().getResources().getString(R.string.qx_authorities_fileprovider), new File(mediaItem.uri));
                            intent.setDataAndType(uri, mediaItem.mimeType);
                            intent.addFlags(1);
                        } else {
                            intent.setDataAndType(Uri.parse("file://" + mediaItem.uri), mediaItem.mimeType);
                        }

                        PicturePreviewActivity.this.startActivity(intent);
                    }
                });
                playButton.setVisibility(View.VISIBLE);
            } else {
                if (KitStorageUtils.isBuildAndTargetForQ(PicturePreviewActivity.this.getApplicationContext())) {
                    imagePath = ((PictureSelectorActivity.MediaItem)PicturePreviewActivity.this.mItemList.get(position)).uri_sdk29;
                } else  {
                    imagePath = ((PictureSelectorActivity.MediaItem)PicturePreviewActivity.this.mItemList.get(position)).uri;
                }

                playButton.setVisibility(View.GONE);
            }

            subsamplingScaleImageView.setImage(ImageSource.uri(imagePath));
            QLog.i(TAG,"readPictureDegree = " + PicturePreviewActivity.this.readPictureDegree(imagePath));
            if (PicturePreviewActivity.this.readPictureDegree(imagePath) == 90) {
                subsamplingScaleImageView.setOrientation(90);
            }

            AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(imagePath);
            AlbumBitmapCacheHelper.getInstance().addPathToShowlist(imagePath);
            return view;
        }

        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }
    }
}
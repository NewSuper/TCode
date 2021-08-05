package com.qx.imui.emotion;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class EmojiDao {
    private static final String TAG = "EmojiDao";
    private String path;
    private static EmojiDao dao;
    private static Context mContext;

    public static EmojiDao getInstance(Context context) {
        if (dao == null) {
            synchronized (EmojiDao.class) {
                if (dao == null) {
                    mContext = context;
                    dao = new EmojiDao();
                }
            }
        }
        return dao;
    }

    private EmojiDao() {
        try {
            path = CopySqliteFileFromRawToDatabases("emoji.db");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<EmojiBean> getEmojiBean() {
        List<EmojiBean> emojiBeans = new ArrayList<>();
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.CREATE_IF_NECESSARY);
            Cursor cursor = db.query("emoji", new String[]{"unicodeInt", "_id"}, null, null, null, null, null);
            while (cursor.moveToNext()) {
                EmojiBean bean = new EmojiBean();
                int unicodeInt = cursor.getInt(0);
                int id = cursor.getInt(1);
                bean.setUnicodeInt(unicodeInt);
                bean.setId(id);
                emojiBeans.add(bean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emojiBeans;
    }

    public static String CopySqliteFileFromRawToDatabases(String sqliteFileName) throws IOException {
        File file = mContext.getDatabasePath(sqliteFileName);
        InputStream inputStream = null;
        OutputStream outputStream = null;
        if (!file.exists()) {
            try {
                file.createNewFile();
                AssetManager assetManager = mContext.getAssets();
                inputStream = assetManager.open(sqliteFileName);
                outputStream = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        return file.getPath();
    }
}

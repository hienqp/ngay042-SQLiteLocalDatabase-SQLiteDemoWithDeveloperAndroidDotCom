package com.hienqp.sqlitedemowithdeveloperandroiddotcom;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FeedReaderDbHelper extends SQLiteOpenHelper {

    // nếu thay đổi cấu trúc table của database, phải tăng DATABASE_VERSION
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedReaderContract.FeedEntry.TABLE_NAME + " (" +
                    FeedReaderContract.FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE + " TEXT," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedReaderContract.FeedEntry.TABLE_NAME;

    public FeedReaderDbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // database này chỉ là bộ nhớ cache cho data trực tuyến, vì vậy cách upgrade của nó là
        // loại bỏ data và bắt đầu lại
        db.execSQL(SQL_DELETE_ENTRIES);

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    }

    // phương thức insert data vào database
    public void insertDataByTitle(String titleValue, String subtitleValue) {
        // lấy đối tượng SQLiteDatabase ở chế độ write
        SQLiteDatabase db = FeedReaderDbHelper.this.getWritableDatabase();

        // tạo 1 cấu trúc map của value, key chính là các column của table
        // sử dụng ContentValues
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, titleValue);
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE, subtitleValue);

        // insert data vào 1 dòng mới trong table, trả về giá trị Primary Key của dòng mới
        long newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
    }

    // phương thức delete data trong database
    public void deleteDataByTitle(String conditionValue) {
        // lấy đối tượng SQLiteDatabase ở chế độ read
        SQLiteDatabase db = FeedReaderDbHelper.this.getReadableDatabase();

        // định nghĩa WHERE điều kiện, 1 phần của câu query
        String selection = FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE + " LIKE ?";

        // chỉ định đối số cho điều kiện
        String[] selectionArgs = { conditionValue };

        // delete data trong database, trả về số dòng đã delete trong database tương ứng với selectionArgs
        int deletedRows = db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, selection, selectionArgs);
    }

    // phương thức update data trong database
    public void updateDataByTitle(String conditionValue, String newValue) {
        // lấy đối tượng SQLiteDatabase ở chế độ write
        SQLiteDatabase db = FeedReaderDbHelper.this.getWritableDatabase();

        // nội dung mới sẽ update ở TITLE
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, newValue);

        // chọn dòng sẽ update, dựa trên TITLE
        String selection = FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE + " LIKE ?";
        String[] selectionArgs = { conditionValue };

        // update data trong database, trả về số dòng đã update trong database tương ứng với selectionArgs
        int updatedRows = db.update(FeedReaderContract.FeedEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    // phương thức duyệt database lấy data với Cursor
    public ArrayList<Long> readDataBytTitle(String conditionValue) {
        // lấy đối tượng SQLiteDatabase ở chế độ read
        SQLiteDatabase db = FeedReaderDbHelper.this.getReadableDatabase();

        // định nghĩa mảng chỉ định các cột trong database sẽ được sử dụng sau truy vấn
        String[] projection = {
                FeedReaderContract.FeedEntry._ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE,
                FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE
        };

        // bộ lọc điều kiện duyệt database WHERE = ? 'giá_trị_cần_lọc'
        String selection = FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE + " = ?";
        String[] selectionArgs = { conditionValue };

        // chỉ định cách data trả về được sort trong con trỏ Cursor kết quả trả về
        // DESC = decrease (giảm dần)
        // ASC = ascending (tăng dần)
        String sortOrder = FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE + " DESC";

        // dùng phương thức query() truy xuất vào database và lấy về 1 con trỏ Cursor
        // với các tham số tương ứng ở trên truyền vào phương thức
        Cursor cursor = db.query(
                FeedReaderContract.FeedEntry.TABLE_NAME,// tên table
                projection,                             // mảng các cột trả về data (null sẽ trả về tất cả)
                selection,                              // chỉ định cột cho mệnh đề WHERE
                selectionArgs,                          // mảng đối số của WHERE
                null,                           // không nhóm các dòng
                null,                            // không lọc bởi các nhóm dòng
                sortOrder                              // phương pháp sort (sắp xếp)
        );

        // sử dụng con trỏ Cursor để lấy data trong database
        // trả về 1 ArrayList<Long> danh sách các dòng thỏa điều kiện chỉ định trong query()
        ArrayList<Long> itemIds = new ArrayList<>();
        while (cursor.moveToNext()) {
            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(BaseColumns._ID)
            );
            itemIds.add(itemId);
        }

        cursor.close();

        return itemIds;
    }
}

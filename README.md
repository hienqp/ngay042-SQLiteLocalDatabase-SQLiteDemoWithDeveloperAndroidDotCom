# 1. SQLite Local Database On Android <a id="1"></a>
________________________________________________________________________________________________________________________
- các API cần thiết để sử dụng database trên Android nằm ở package ``android.database.sqlite``
- mặc dù các API để thao tác với database đều rất mạnh mẽ nhưng:
  - ở compile-time không kiểm tra các truy vấn SQL thô (raw SQL queries) nên quá trình soạn lệnh SQL thường xảy ra lỗi
  - phải sử dụng nhiều đoạn mã soạn sẵn để chuyển đổi giữa truy vấn SQL và đối tượng dữ liệu
- vì 2 lí do trên nên API [Room Persistence Library]("https://developer.android.com/training/data-storage/room") khuyến khích
sử dụng

## 1.1. Định Nghĩa 1 Schema (Lược Đồ) Của Cấu Trúc Table Và Lớp Contract <a id="1.1"></a>
________________________________________________________________________________________________________________________
- khi thao tác với database, ta sẽ thao tác rất nhiều với các thành phần trong table của database.
- vì vậy việc khai báo 1 class quản lý các thành phần này sẽ rất hữu ích, tránh sai sót trong việc soạn câu SQL thô, cũng
như có thể sử dụng chúng từ nhiều class khác nhau mà không xảy ra nhầm lẫn.
- cách tốt nhất để tổ chức 1 contract class là đặt các định nghĩa toàn cục cho toàn bộ database ở cấp root của class
- sau đó tạo mỗi inner class tương ứng mỗi table, liệt kê các column tương ứng cho table đó
- inner class nên implements BaseColumns, inner class này sẽ được inherit 1 thuộc tính Pimary Key được gọi là ``_ID``,
thuộc tính này có thể được sử dụng bởi những class khác khi cần, điều này không bắt buộc nhưng nó giúp database hoạt động
linh hoạt trong khuôn khổ nền tảng Android
- Lớp Contract quản lý các Column trong Table của Database
```java
package com.hienqp.sqlitedemowithdeveloperandroiddotcom;

import android.provider.BaseColumns;

// final: đây là lớp cuối cùng, không có lớp nào có thể kế thừa lớp này
public final class FeedReaderContract {
    // để ngăn việc vô tình khởi tạo Instance của lớp Contract này
    // Constructor thiết lập là private
    private FeedReaderContract() {}

    /* inner class định nghĩa các column của 1 table */
    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "ENTRY";
        public static final String COLUMN_NAME_TITLE = "TITLE";
        public static final String COLUMN_NAME_SUBTITLE = "SUBTITLE";
    }
}
```

## 1.2. Định Nghĩa Class Quản Lý Databse Sử Dụng SQLiteOpenHelper <a id="1.2"></a>
________________________________________________________________________________________________________________________
### 1.2.1. Câu Lệnh SQL CREATE và DELETE table <a id="1.2.1"></a>
________________________________________________________________________________________________________________________
- khi đã xác định được database trông như thế nào, ta nên triển khai cách để tạo và duy trì database và table
- đây là các câu lệnh điển hình để tạo và xóa table
```java
private static final String SQL_CREATE_ENTRIES =
    "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
    FeedEntry._ID + " INTEGER PRIMARY KEY," +
    FeedEntry.COLUMN_NAME_TITLE + " TEXT," +
    FeedEntry.COLUMN_NAME_SUBTITLE + " TEXT)";

private static final String SQL_DELETE_ENTRIES =
    "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;
```

### 1.2.2. Định Nghĩa Class Quản Lý Database <a id="1.2.2"></a>
________________________________________________________________________________________________________________________
- để quản lý database ta định nghĩa 1 class extends SQLiteOpenHelper
- SQLiteOpenHelper chứa các bộ API hữu ích trong việc quản lý database
- việc cần làm là gọi đến 2 phương thức để lấy về 1 đối tượng SQLiteDatabase để thao tác tương ứng tới database
  - ``getWritableDatabase()`` (vừa ghi vừa đọc vào database)
  - ``getReadableDatabase()`` (chỉ đọc nội dung database)
- class quản lý database: FeedReaderDbHelper, class này sẽ sử dụng 2 câu lệnh phía trên trong ``onCreate()`` và ``onUpgrade()``
```java
package com.hienqp.sqlitedemowithdeveloperandroiddotcom;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class FeedReaderDbHelper extends SQLiteOpenHelper {

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
}
```

### 1.2.3. Phương Thức INSERT Data Vào Database Của Class Quản Lý Database <a id="1.2.3"></a>
________________________________________________________________________________________________________________________
- khai báo thêm phương thức insert data vào database bằng cách sử dụng phương thức ``insert()`` của ``SQLiteDatabase``
- phương thức ``insert()`` sẽ trả về 1 giá trị là ``_ID`` của dòng mới được insert vào, nó sẽ trả về ``-1`` nếu xung đột
với dữ liệu tồn tại trước đó trong database
```java
    // phương thức insert data vào database
    public void insertData(String title, String subtitle) {
        // lấy đối tượng SQLiteDatabase ở chế độ write
        SQLiteDatabase db = FeedReaderDbHelper.this.getWritableDatabase();

        // tạo 1 cấu trúc map của value, key chính là các column của table
        // sử dụng ContentValues
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, title);
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE, subtitle);

        // insert data vào 1 dòng mới trong table, trả về giá trị Primary Key của dòng mới
        long newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
    }
```

### 1.2.4. Phương Thức DELETE Data Trong Database Của Class Quản Lý Database <a id="1.2.4"></a>
________________________________________________________________________________________________________________________
- khai báo phương thức delete data trong database bằng cách sử dụng phương thức ``delete()`` của ``SQLiteDatabase``
- phương thức ``delete()`` cần 3 tham số
  - tên table
  - SQL điều kiện WHERE (cột sẽ chọn làm điều kiện lọc)
  - SQL đối số của điều kiện WHERE (giá trị tại cột đã chỉ định - gặp giá trị này sẽ xử lý delete)
- phương thức ``delete()`` sẽ trả về số dòng đã delete tương ứng với điều kiện truyền vào
```java
    // phương thức delete data trong database
    public void deleteData(String condition) {
        // lấy đối tượng SQLiteDatabase ở chế độ read
        SQLiteDatabase db = FeedReaderDbHelper.this.getReadableDatabase();

        // định nghĩa WHERE điều kiện, 1 phần của câu query
        String selection = FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE + " LIKE ?";

        // chỉ định đối số cho điều kiện
        String[] selectionArgs = { condition };

        // delete data trong database, trả về số dòng đã delete trong database tương ứng với selectionArgs
        int deletedRows = db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, selection, selectionArgs);
    }
```

### 1.2.5. Phương Thức UPDATE Data Trong Database Của Class Quản Lý Database <a id="1.2.5"></a>
________________________________________________________________________________________________________________________
- khai báo phương thức update data trong database bằng cách sử dụng phương thức ``update()`` của ``SQLiteDatabase``
- phương thức ``update()`` cần 4 tham số
  - tên table
  - ContentValues (data mới cần update)
  - SQL điều kiện WHERE (cột sẽ chọn làm điều kiện lọc)
  - SQL đối số của điều kiện WHERE (giá trị tại cột đã chỉ định - gặp giá trị này sẽ xử lý update)
- phương thức ``update()`` sẽ trả về số dòng đã được update tương ứng với điều kiện truyền vào
```java
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
```

### 1.2.6. Truy Xuất Data Trong SQLiteDatabase Với Cursor <a id="1.2.6"></a>
________________________________________________________________________________________________________________________
- để truy xuất và đọc data trong SQLiteDatabase ta sử dụng phương thức ``query()``
- phương thức ``query()`` trả về 1 đối tượng con trỏ Cursor, con trỏ Cursor sẽ được dùng để duyệt và đọc data trong SQLiteDatabase
- sử dụng phương thức ``query()`` 7 tham số
  - table name
  - mảng các cột trả về data (để ``null`` sẽ trả về tất cả)
  - SQL mệnh đề điều kiện WHERE
  - SQL mảng đối số giá trị của WHERE
  - ``null`` không group các dòng
  - ``null`` không lọc bởi các nhóm dòng
  - SQL sắp xếp
```java
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
```

## 1.3. Sử Dụng Class Quản Lý Database <a id="1.3"></a>
________________________________________________________________________________________________________________________
- để access vào database, ta cần khởi tạo instance của class quản lý database: FeedReaderDbHelper
- ví dụ ở MainActivity
```java 
FeedReaderDbHelper dbHelper = new FeedReaderDbHelper(getContext());
```
- mỗi lần kết nối đến database rất tốn tài nguyên hệ thống khi gọi ``getWritableDatabase()`` và ``getReadableDatabase()``
- vì vậy cần giữ kết nối đến database mở và đóng 1 lần
- ví dụ ở MainActivity ta chỉ đóng database khi ứng dụng rơi vào ``onDestroy()``
```java
@Override
protected void onDestroy() {
    dbHelper.close();
    super.onDestroy();
}
```
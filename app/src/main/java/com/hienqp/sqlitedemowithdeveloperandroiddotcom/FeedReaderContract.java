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

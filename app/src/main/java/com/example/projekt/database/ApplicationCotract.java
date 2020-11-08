package com.example.projekt.database;

import android.provider.BaseColumns;

public class ApplicationCotract {

    private ApplicationCotract(){}

    public static final class ApllicationEntry implements BaseColumns{
        public static final String TABLE_NAME = "applications";
        public static final String COLUMN_PACKAGE = "package";
        public static final String COLUMN_ICON = "icon";
        public static final String COLUMN_APP_NAME = "appName";
        public static final String COLUMN_APP_INSTALL_DATE = "appInstallDate";
        public static final String COLUMN_APP_LAST_USED_DATE = "appLastUSedDate";
        public static final String COLUMN_APP_IN_FOREGROUND = "appInForeground";
        public static final String COLUMN_APP_RX_RECEIVED = "RXReceived";
        public static final String COLUMN_APP_TX_SEND = "TXSend";
        public static final String COLUMN_SUGGEST_DELETE = "suggestDelete";
        public static final String COLUMN_NEED_DELETE = "needDelete";
        public static final String COLUMN_ENTIRE_SCAN = "entireScan";
    }
    public static final class ApllicationHistroyEntry implements BaseColumns{
        public static final String TABLE_NAME = "applicationsHistory";
        public static final String COLUMN_PACKAGE = "package";
        public static final String COLUMN_APP_NAME = "appName";
        public static final String COLUMN_APP_LAST_USED_DATE = "appLastUSedDate";
        public static final String COLUMN_APP_IN_FOREGROUND = "appInForeground";
        public static final String COLUMN_APP_RX_RECEIVED = "RXReceived";
        public static final String COLUMN_APP_TX_SEND = "TXSend";
        public static final String COLUMN_HISTORY_DATE = "date";
    }

}

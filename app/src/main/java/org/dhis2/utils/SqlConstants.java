package org.dhis2.utils;

public class SqlConstants {

    private SqlConstants() {
        // hide public constructor
    }

    public static final String SELECT = " SELECT ";
    public static final String FROM = " FROM ";
    public static final String WHERE = " WHERE ";
    public static final String JOIN = " JOIN ";
    public static final String ON = " ON ";
    public static final String AND = " AND ";
    public static final String NOT_EQUALS = " != ";
    public static final String EQUAL = " = ";
    public static final String QUESTION_MARK = " ? ";
    public static final String QUOTE = "'";
    public static final String LIMIT_1 = " LIMIT 1 ";
    public static final String TABLE_FIELD_EQUALS = " %s.%s = ";
    public static final String JOIN_TABLE_ON = " JOIN %s ON %s.%s = %s.%s ";
    public static final String SELECT_ALL_FROM_TABLE_WHERE_TABLE_FIELD_EQUALS = "SELECT * FROM %s WHERE %s.%s = ";
    public static final String SELECT_ALL_FROM = "SELECT * FROM ";
    public static final String UID_EQUALS_QUESTION_MARK = "uid = ?";

}

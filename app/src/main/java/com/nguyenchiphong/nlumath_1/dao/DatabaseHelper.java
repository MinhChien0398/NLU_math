package com.nguyenchiphong.nlumath_1.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.nguyenchiphong.nlumath_1.R;
import com.nguyenchiphong.nlumath_1.model.History;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Thesis";
    private static final String TAG = "DatabaseHelper";
    private static final String TABLE_NAME = "History";
    private static final String ID = "ID";
    private static final String NAME = "NAME";
    private static final String CREATED_DATE = "CREATED_DATE";
//    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
//            ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//            NAME + " TEXT, " +
//            CREATED_DATE + " DATE " + ")";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME + " (" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                NAME + " TEXT, " +
                CREATED_DATE + " DATE " + ")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String name, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(NAME, name);
        contentValues.put(CREATED_DATE, date);

        Log.d(TAG, "addData: Adding " + name + " to " + TABLE_NAME);
        Log.d(TAG, "addData: Adding " + date + " to " + TABLE_NAME);
        long result = db.insert(TABLE_NAME, null, contentValues);

//        //if date as inserted incorrectly it will return -1
//        if (result == -1) {
//            return false;
//        } else {
//            return true;
//        }
        return result != -1;
    }

    /**
     * Returns all the data from database
     *
     * @return
     */
    public Cursor getData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + CREATED_DATE + " DESC ";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    /**
     * Returns only the ID that matches the name passed in
     *
     * @param name
     * @return
     */
    public Cursor getItemID(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + ID + " FROM " + TABLE_NAME +
                " WHERE " + NAME + " = '" + name + "'";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public void updateName(String oldName, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME + " SET " + NAME +
                " = '" + newName + "' WHERE " + NAME + " = '" + oldName + "'";
        Log.d(TAG, "updateName: query: " + query);
        Log.d(TAG, "updateName: Setting name to " + newName);
        db.execSQL(query);
    }

    public void deleteByName(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME + " WHERE "
                + NAME + " = '" + name + "'";
        Log.d(TAG, "deleteName: query: " + query);
        db.execSQL(query);
    }

    public ArrayList<History> searchByName(String name) {
        ArrayList<History> histories = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE "
                + NAME + " LIKE '%" + name + "%'";
        Cursor cursor = db.rawQuery(query, null);
        // lặp các hàng và thêm vào list
//        if (cursor.moveToFirst()) {
//            do {
//                Model model = new Model();
//                model.setTitle(cursor.getString(0));
//                model.setDescription(cursor.getString(1));
//                model.setImg(Integer.parseInt(cursor.getString(0)));
//                // thêm vào list
//                models.add(model);
//            } while (cursor.moveToNext());
//        }
        while (cursor.moveToNext()) {
//            model.setImg(R.drawable.hand_icon);
//            model.setTitle(data.getString(1));
//            model.setDescription("This is news feed description");
            histories.add(new History(cursor.getString(1), cursor.getString(2), R.drawable.hand_icon));
        }
        return histories;
        //db.execSQL(query);
    }

    // Xóa tất cả
    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME;
        Log.d(TAG, "Delete all data in table: " + TABLE_NAME);
        db.execSQL(query);
    }
}

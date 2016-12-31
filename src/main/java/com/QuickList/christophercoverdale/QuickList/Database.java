package com.QuickList.christophercoverdale.QuickList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by USER on 12/18/2016.
 */

public class Database extends SQLiteOpenHelper {
    private static final String TABLE  = "TASKS";
    private static final String COL_ID = "ID";
    private static final String COL_LIST_NAME = "LIST";
    private static final String COL_TASK_NAME = "TASK";
    private static final String COL_CHECKED = "CHECKED";
    public String currentList = "DEFAULT";
    private static int databaseVersion = 1;

    public Database(Context context) {
        super(context, "taskLists.db", null, databaseVersion);
    }

    public void changeList() {
         currentList = "DEFAULT";
    }

    public void changeList(String currentList) {
        this.currentList = currentList;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String sql = String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, %s TEXT, %s INTEGER)",
                 TABLE, COL_ID, COL_LIST_NAME, COL_TASK_NAME, COL_CHECKED);

        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " +  TABLE);
        onCreate(db);
    }

    protected void storeTasks(List<Task> tasks) {
        SQLiteDatabase db = getWritableDatabase();

        String sql = String.format("DELETE FROM %s WHERE %s = '%s' AND %s IS NOT NULL", TABLE, COL_LIST_NAME, currentList, COL_TASK_NAME);
        db.execSQL(sql);

        for(Task task : tasks) {
            ContentValues values = new ContentValues();

            values.put(COL_LIST_NAME, currentList);
            values.put(COL_TASK_NAME, task.getTaskName());

            if(task.isDone()) {
                values.put(COL_CHECKED, 1);
            } else {
                values.put(COL_CHECKED, 0);
            }
            db.insert(TABLE, null, values);
        }
        db.close();
    }

    protected ArrayList<Task> loadTasks() {

        ArrayList<Task> tasks = new ArrayList<Task>();

        SQLiteDatabase db = getReadableDatabase();

        String sql = String.format("SELECT %s, %s FROM %s WHERE %s = '%s' AND %s IS NOT NULL ORDER BY %s", COL_TASK_NAME, COL_CHECKED, TABLE, COL_LIST_NAME, currentList, COL_TASK_NAME, COL_ID);

        Cursor cursor = db.rawQuery(sql, null);

        while(cursor.moveToNext()) {
            String taskName = cursor.getString(0);
            int binaryChecked = cursor.getInt(1);

            Task task = new Task(taskName);

            if (binaryChecked ==  1) {
                task.setDone(true);
            } else {
                task.setDone(false);
            }

            tasks.add(task);
        }
        db.close();
        return tasks;
    }

    protected ArrayList<String> loadListSpinners() {
        ArrayList<String> listSpinnerNames = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String sql = String.format("SELECT %s FROM %s WHERE %s IS NULL ORDER BY %s", COL_LIST_NAME, TABLE, COL_TASK_NAME, COL_ID);

        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.getCount() <= 0 ) {
            cursor.close();
        } else {
            while (cursor.moveToNext()) {
                String listName = cursor.getString(0);

                listSpinnerNames.add(listName);
            }
        }
        db.close();
        return listSpinnerNames;
    }

    protected void storeListSpinners(List<String> listSpinnerItems) {
        SQLiteDatabase db = getWritableDatabase();

        String sql = String.format("DELETE FROM %s WHERE %s IS NULL", TABLE, COL_TASK_NAME);
        db.execSQL(sql);

        for(int i = 1; i < listSpinnerItems.size()-1; i++ ) {
            ContentValues values = new ContentValues();

            values.put(COL_LIST_NAME, listSpinnerItems.get(i));
            values.putNull(COL_TASK_NAME);
            values.putNull(COL_CHECKED);

            db.insert(TABLE, null, values);
        }
        db.close();
    }

    protected void updateListName (String newListName ) {
        SQLiteDatabase db = getWritableDatabase();

        String sql = String.format("UPDATE %s SET %s = '%s' WHERE %s = '%s'", TABLE, COL_LIST_NAME, newListName, COL_LIST_NAME, currentList);
        db.execSQL(sql);

        db.close();
    }

}

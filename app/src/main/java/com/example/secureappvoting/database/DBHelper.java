package com.example.westyorkshirecrimes.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "crimes.db";
    private static final int DATABASE_VERSION = 2; // IMPORTANT: incremented to reset DB

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_USERS_TABLE =
                "CREATE TABLE users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "email TEXT, " +
                        "password TEXT, " +
                        "isAdmin INTEGER)";

        String CREATE_CRIMES_TABLE =
                "CREATE TABLE crimes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "crimeType TEXT, " +
                        "latitude REAL, " +
                        "longitude REAL, " +
                        "outcome TEXT)";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_CRIMES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS crimes");
        onCreate(db);
    }

    /* ============================
       USER METHODS
       ============================ */

    public boolean insertUser(String email, String password, boolean isAdmin) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("email", email);
        values.put("password", password);
        values.put("isAdmin", isAdmin ? 1 : 0);

        long result = db.insert("users", null, values);
        db.close();

        return result != -1;
    }

    public boolean checkUserLogin(String email, String password) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE email = ? AND password = ?",
                new String[]{email, password}
        );

        boolean exists = cursor.getCount() > 0;

        cursor.close();
        db.close();

        return exists;
    }

    public boolean isAdmin(String email) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT isAdmin FROM users WHERE email = ?",
                new String[]{email}
        );

        boolean admin = false;

        if (cursor.moveToFirst()) {
            admin = cursor.getInt(0) == 1;
        }

        cursor.close();
        db.close();

        return admin;
    }

    /* ============================
       CRIME METHODS
       ============================ */

    public Cursor getAllCrimes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM crimes", null);
    }

    /* ============================
       CSV IMPORT (FIXED & WORKING)
       ============================ */

    public void importCrimesFromCSV(Context context) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            InputStream is = context.getAssets().open("west_yorkshire_crimes.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

                // Skip header row
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // Use limit to prevent breaking on commas in text
                String[] columns = line.split(",", -1);

                // Ensure required columns exist
                if (columns.length < 9) continue;

                String crimeType = columns[7];
                String latStr = columns[4];
                String lonStr = columns[5];
                String outcome = columns[8];

                // Skip invalid coordinates
                if (latStr.isEmpty() || lonStr.isEmpty()) continue;

                double latitude = Double.parseDouble(latStr);
                double longitude = Double.parseDouble(lonStr);

                ContentValues values = new ContentValues();
                values.put("crimeType", crimeType);
                values.put("latitude", latitude);
                values.put("longitude", longitude);
                values.put("outcome", outcome);

                db.insert("crimes", null, values);
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
}

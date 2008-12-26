/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.data.task;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.AbstractModel;
import com.timsu.astrid.data.enums.Importance;


/** Abstract model of a task. Subclasses implement the getters and setters
 * they are interested in.
 *
 * @author timsu
 *
 */
public abstract class AbstractTaskModel extends AbstractModel {

    /** Version number of this model */
    static final int                   VERSION             = 1;

    public static final int            COMPLETE_PERCENTAGE = 100;

    // field names

    static final String                NAME                = "name";
    static final String                NOTES               = "notes";
    static final String                PROGRESS_PERCENTAGE = "progressPercentage";
    static final String                IMPORTANCE          = "importance";
    static final String                ESTIMATED_SECONDS   = "estimatedSeconds";
    static final String                ELAPSED_SECONDS     = "elapsedSeconds";
    static final String                TIMER_START         = "timerStart";
    static final String                DEFINITE_DUE_DATE   = "definiteDueDate";
    static final String                PREFERRED_DUE_DATE  = "preferredDueDate";
    static final String                HIDDEN_UNTIL        = "hiddenUntil";
    // reserved fields
    static final String                BLOCKING_ON         = "blockingOn";
    static final String                NOTIFICATIONS       = "notifications";
    // end reserved fields
    static final String                CREATION_DATE       = "creationDate";
    static final String                COMPLETION_DATE     = "completionDate";

    /** Default values container */
    private static final ContentValues defaultValues       = new ContentValues();

    static {
        defaultValues.put(NAME, "");
        defaultValues.put(NOTES, "");
        defaultValues.put(PROGRESS_PERCENTAGE, 0);
        defaultValues.put(IMPORTANCE, Importance.DEFAULT.ordinal());
        defaultValues.put(ESTIMATED_SECONDS, 0);
        defaultValues.put(ELAPSED_SECONDS, 0);
        defaultValues.put(TIMER_START, (Long)null);
        defaultValues.put(DEFINITE_DUE_DATE, (Long)null);
        defaultValues.put(PREFERRED_DUE_DATE, (Long)null);
        defaultValues.put(HIDDEN_UNTIL, (Long)null);
        defaultValues.put(BLOCKING_ON, (Long)null);
        defaultValues.put(NOTIFICATIONS, 0);
        defaultValues.put(COMPLETION_DATE, (Long)null);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- database helper

    /** Database Helper manages creating new tables and updating old ones */
    static class TaskModelDatabaseHelper extends SQLiteOpenHelper {
        String tableName;

        TaskModelDatabaseHelper(Context context, String databaseName, String tableName) {
            super(context, databaseName, null, VERSION);

            this.tableName = tableName;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = new StringBuilder().
            append("CREATE TABLE ").append(tableName).append(" (").
                append(AbstractController.KEY_ROWID).append(" integer primary key autoincrement, ").
                append(NAME).append(" text not null,").
                append(NOTES).append(" text not null,").
                append(PROGRESS_PERCENTAGE).append(" integer not null,").
                append(IMPORTANCE).append(" integer not null,").
                append(ESTIMATED_SECONDS).append(" integer,").
                append(ELAPSED_SECONDS).append(" integer,").
                append(TIMER_START).append(" integer,").
                append(DEFINITE_DUE_DATE).append(" integer,").
                append(PREFERRED_DUE_DATE).append(" integer,").
                append(HIDDEN_UNTIL).append(" integer,").
                append(BLOCKING_ON).append(" integer,").
                append(NOTIFICATIONS).append(" integer,").
                append(CREATION_DATE).append(" integer,").
                append(COMPLETION_DATE).append(" integer").
            append(");").toString();
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(getClass().getSimpleName(), "Upgrading database from version " +
                    oldVersion + " to " + newVersion + ".");

            switch(oldVersion) {
            default:
                // we don't know how to handle it... do the unfortunate thing
                Log.e(getClass().getSimpleName(), "Unsupported migration, table dropped!");
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
                onCreate(db);
            }
        }
    }

    // --- utility methods

    /** Gets task color. Requires definiteDueDate and importance */
    protected int getTaskColorResource() {
        if(getDefiniteDueDate() != null && getDefiniteDueDate().getTime() <
                System.currentTimeMillis()) {
            return R.color.task_list_overdue;
        } else {
            return getImportance().getColorResource();
        }
    }

    /** Checks whether task is done. Requires progressPercentage */
    protected boolean isTaskCompleted() {
        return getProgressPercentage() >= COMPLETE_PERCENTAGE;
    }

    /** Stops the timer & increments elapsed time. Requires timerStart and
     * elapsedSeconds */
    protected void stopTimerAndUpdateElapsedTime() {
        long start = getTimerStart().getTime();
        setTimerStart(null);
        long secondsElapsed = (System.currentTimeMillis() - start)/1000;
        setElapsedSeconds((int) (getElapsedSeconds() + secondsElapsed));
    }

    // --- task identifier

    private TaskIdentifier identifier = null;

    public TaskIdentifier getTaskIdentifier() {
        return identifier;
    }

    void setTaskIdentifier(TaskIdentifier identifier) {
        this.identifier = identifier;
    }

    // --- constructor pass-through

    AbstractTaskModel() {
        super();
    }

    /** Read identifier from database */
    AbstractTaskModel(Cursor cursor) {
        super(cursor);

        Integer id = retrieveInteger(AbstractController.KEY_ROWID);
        setTaskIdentifier(new TaskIdentifier(id));
    }

    /** Get identifier from argument */
    AbstractTaskModel(TaskIdentifier identifier, Cursor cursor) {
        super(cursor);

        setTaskIdentifier(identifier);
    }

    // --- getters and setters: expose them as you see fit

    protected String getName() {
        return retrieveString(NAME);
    }

    protected String getNotes() {
        return retrieveString(NOTES);
    }

    protected int getProgressPercentage() {
        return retrieveInteger(PROGRESS_PERCENTAGE);
    }

    protected Importance getImportance() {
        Integer value = retrieveInteger(IMPORTANCE);
        if(value == null)
            return null;
        return Importance.values()[value];
    }

    protected Integer getEstimatedSeconds() {
        return retrieveInteger(ESTIMATED_SECONDS);
    }

    protected Integer getElapsedSeconds() {
        return retrieveInteger(ELAPSED_SECONDS);
    }

    protected Date getTimerStart() {
        return retrieveDate(TIMER_START);
    }

    protected Date getDefiniteDueDate() {
        return retrieveDate(DEFINITE_DUE_DATE);
    }

    protected Date getPreferredDueDate() {
        return retrieveDate(PREFERRED_DUE_DATE);
    }

    protected Date getHiddenUntil() {
        return retrieveDate(HIDDEN_UNTIL);
    }

    protected Date getCreationDate() {
        return retrieveDate(CREATION_DATE);
    }

    protected Date getCompletionDate() {
        return retrieveDate(COMPLETION_DATE);
    }

    protected TaskIdentifier getBlockingOn() {
        Long value = retrieveLong(BLOCKING_ON);
        if(value == null)
            return null;
        return new TaskIdentifier(value);
    }

    // --- setters

    protected void setName(String name) {
        setValues.put(NAME, name);
    }

    protected void setNotes(String notes) {
        setValues.put(NOTES, notes);
    }

    protected void setProgressPercentage(int progressPercentage) {
        setValues.put(PROGRESS_PERCENTAGE, progressPercentage);

        if(getProgressPercentage() != progressPercentage &&
                progressPercentage == COMPLETE_PERCENTAGE)
            setCompletionDate(new Date());
    }

    protected void setImportance(Importance importance) {
        setValues.put(IMPORTANCE, importance.ordinal());
    }

    protected void setEstimatedSeconds(Integer estimatedSeconds) {
        setValues.put(ESTIMATED_SECONDS, estimatedSeconds);
    }

    protected void setElapsedSeconds(int elapsedSeconds) {
        setValues.put(ELAPSED_SECONDS, elapsedSeconds);
    }

    protected void setTimerStart(Date timerStart) {
        putDate(setValues, TIMER_START, timerStart);
    }

    protected void setDefiniteDueDate(Date definiteDueDate) {
        putDate(setValues, DEFINITE_DUE_DATE, definiteDueDate);
    }

    protected void setPreferredDueDate(Date preferredDueDate) {
        putDate(setValues, PREFERRED_DUE_DATE, preferredDueDate);
    }

    protected void setHiddenUntil(Date hiddenUntil) {
        putDate(setValues, HIDDEN_UNTIL, hiddenUntil);
    }

    protected void setBlockingOn(TaskIdentifier blockingOn) {
        if(blockingOn == null)
            setValues.put(BLOCKING_ON, (Integer)null);
        else
            setValues.put(BLOCKING_ON, blockingOn.getId());
    }

    protected void setCreationDate(Date creationDate) {
        putDate(setValues, CREATION_DATE, creationDate);
    }

    protected void setCompletionDate(Date completionDate) {
        putDate(setValues, COMPLETION_DATE, completionDate);
    }

    // --- utility methods

    static void putDate(ContentValues cv, String fieldName, Date date) {
        if(date == null)
            cv.put(fieldName, (Long)null);
        else
            cv.put(fieldName, date.getTime());
    }
}

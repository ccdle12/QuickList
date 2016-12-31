package com.QuickList.christophercoverdale.QuickList;

import java.io.Serializable;

/**
 * Created by USER on 12/15/2016.
 */

public class Task implements Serializable {
    private String mTaskTitle;
    private boolean mTaskDone;

    Task(String taskTitle) {
        mTaskTitle = taskTitle;
    }
    public boolean isDone() {
        return mTaskDone;
    }

    public void setDone(boolean done) {
        mTaskDone = done;
    }

    public String getTaskName() {
        return mTaskTitle;
    }

    public void setTaskName(String task) {
        mTaskTitle = task;
    }
}

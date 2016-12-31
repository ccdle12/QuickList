package com.QuickList.christophercoverdale.QuickList;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {
    private ArrayList<Task> mTaskItems;
    private Task mTask;
    private EditText mUserInputView;
    private TaskAdapter mTaskAdapter;
    private ListView mListViewLayout;
    private ImageView mAddToList;
    private int mItemSelectedCount = 0;
    private Database mDb;
    private ArrayList<String> mSavedListMenu;
    private ArrayAdapter<String> mSpinnerAdapter;
    private boolean mSortByCheckbox = false;
    private boolean mSortByTaskName = false;
    private Spinner mSpinner;
    private Toolbar mToolBar;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mAdView.loadAd(adRequest);

        mTaskItems = new ArrayList<> ();
        mDb = new Database(this);

        mUserInputView = (EditText) findViewById(R.id.user_input_text);
        mListViewLayout = (ListView) findViewById(R.id.listview_layout);

        mAddToList = (ImageView) findViewById(R.id.add_to_list);
        mAddToList.setColorFilter(Color.parseColor("#00796B"));
        mAddToList.setVisibility(View.INVISIBLE);

        mTaskAdapter = new TaskAdapter(mTaskItems);
        mListViewLayout.setAdapter(mTaskAdapter);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.inflateMenu(R.menu.main_menu);

        mSavedListMenu = new ArrayList<>();
        mSavedListMenu.add("QuickList");
        loadSpinnerListNames();
        mSavedListMenu.add("New List");

        mSpinner = (Spinner) findViewById(R.id.spinner);
        mSpinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, mSavedListMenu);
        mSpinner.setAdapter(mSpinnerAdapter);

        //Spinner items listener and creating a new list
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> adapterView, View view, int position, long id) {

                if(position == 0) {
                    showEditListName(false);
                    showDeleteList(false);

                    mTaskItems.clear();
                    mDb.changeList();
                    loadTasks();

                    if(mTaskItems.size() > 0) {
                        checkIfTasksCompleted();
                    } else {
                        showClearCompletedTasks(false);
                    }
                } else if (position == mSavedListMenu.size()- 1) {
                    final AlertDialog.Builder createNewList = new AlertDialog.Builder(MainActivity.this);

                    //Linear layout for the custom dialog
                    LinearLayout layout = new LinearLayout(MainActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(50,100,50,100);
                    layout.setMinimumHeight(100);

                    //Title
                    TextView title = new TextView(MainActivity.this);
                    title.setText("Create New List:  ");
                    title.setTextSize(20);
                    title.setTextColor(Color.BLACK);
                    title.setTypeface(null, Typeface.BOLD);

                    //Edit Text for userinput
                    final EditText listEditText = new EditText(MainActivity.this);

                    //Adding the title and edit text to the layout
                    layout.addView(title);
                    layout.addView(listEditText);

                    //Set the alert dialog view as the layout
                    createNewList.setView(layout);

                    createNewList.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            mSpinner.setSelection(0);
                        }
                    });

                    createNewList.setPositiveButton("OK", new DialogInterface.OnClickListener(){

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String newList = listEditText.getText().toString();
                            mSavedListMenu.add(mSavedListMenu.size()-1, newList);

                            mSpinnerAdapter.notifyDataSetChanged();

                            mDb.changeList(newList);
                            mTaskItems.clear();

                            mDb.storeListSpinners(mSavedListMenu);

                            mSpinner.setSelection(mSpinner.getSelectedItemPosition());
                            showClearCompletedTasks(false);
                            showClearList();
                            showDeleteAndEditListName();
                        }

                    });
                    createNewList.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            adapterView.setSelection(0);
                        }
                    });
                    createNewList.show();
                }
                else {
                    showEditListName(true);
                    showDeleteList(true);

                    mTaskItems.clear();
                    mDb.changeList(adapterView.getSelectedItem().toString());
                    loadTasks();

                    if(mTaskItems.size() > 0) {
                        checkIfTasksCompleted();
                    } else {
                        showClearCompletedTasks(false);
                    }
                }
                mTaskAdapter.notifyDataSetChanged();
                showClearList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //Menu Tool Bar
        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                switch(id) {
                    case R.id.sort_by_checkbox:
                        if(mSortByCheckbox) {
                            Collections.sort(mTaskItems, new Comparator<Task>() {
                                @Override
                                public int compare(Task task, Task t1) {
                                    boolean task1 = task.isDone();
                                    boolean task2 = t1.isDone();

                                    if (task1 && !task2) {
                                        return 1;
                                    } else if (!task1 && task2) {
                                        return -1;
                                    } else {
                                        return task.getTaskName().compareTo(t1.getTaskName());
                                    }
                                }
                            });
                            mSortByCheckbox = false;
                            mTaskAdapter.notifyDataSetChanged();
                            mDb.storeTasks(mTaskItems);
                        } else {
                            Collections.sort(mTaskItems, new Comparator<Task>() {
                                @Override
                                public int compare(Task task, Task t1) {
                                    boolean task1 = task.isDone();
                                    boolean task2 = t1.isDone();

                                    if (task1 && !task2) {
                                        return -1;
                                    } else if (!task1 && task2) {
                                        return 1;
                                    } else {
                                        return task.getTaskName().compareTo(t1.getTaskName());
                                    }
                                }
                            });
                            mSortByCheckbox = true;
                            mTaskAdapter.notifyDataSetChanged();
                            mDb.storeTasks(mTaskItems);
                        }
                        break;

                    case R.id.sort_list:
                        if (mSortByTaskName) {
                            Collections.sort(mTaskItems, new Comparator<Task>() {
                                @Override
                                public int compare(Task task, Task t1) {
                                    return task.getTaskName().compareTo(t1.getTaskName());
                                }
                            });
                            mTaskAdapter.notifyDataSetChanged();
                            mDb.storeTasks(mTaskItems);
                            mSortByTaskName = false;
                        } else {
                            Collections.sort(mTaskItems, new Comparator<Task>() {
                                @Override
                                public int compare(Task task, Task t1) {
                                    return -task.getTaskName().compareTo(t1.getTaskName());
                                }
                            });
                            mTaskAdapter.notifyDataSetChanged();
                            mDb.storeTasks(mTaskItems);
                            mSortByTaskName = true;
                        }
                        break;

                    case R.id.edit_list_name:
                        AlertDialog.Builder editListName = new AlertDialog.Builder(MainActivity.this);

                        //Linear layout for the custom dialog
                        LinearLayout layout = new LinearLayout(MainActivity.this);
                        layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setPadding(50,100,50,100);
                        layout.setMinimumHeight(100);

                        //Title
                        TextView title = new TextView(MainActivity.this);
                        title.setText("Edit List Name:  ");
                        title.setTextSize(20);
                        title.setTextColor(Color.BLACK);
                        title.setTypeface(null, Typeface.BOLD);

                        //Edit Text for userinput
                        final EditText listEditText = new EditText(MainActivity.this);
                        final int listPosition = mSpinner.getSelectedItemPosition();
                        listEditText.setText(mSpinner.getSelectedItem().toString());

                        //Adding the title and edit text to the layout
                        layout.addView(title);
                        layout.addView(listEditText);

                        //Set the alert dialog view as the layout
                        editListName.setView(layout);

                        editListName.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mSavedListMenu.remove(mSpinner.getSelectedItemPosition());
                                mSavedListMenu.add(listPosition, listEditText.getText().toString());
                                mSpinnerAdapter.notifyDataSetChanged();

                                mDb.storeListSpinners(mSavedListMenu);
                                mDb.updateListName(mSpinner.getSelectedItem().toString());
                                mDb.changeList(mSpinner.getSelectedItem().toString());
                            }
                        });

                        editListName.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        editListName.show();
                        break;

                    case R.id.clear_list:
                        AlertDialog.Builder confirmClear = new AlertDialog.Builder(MainActivity.this);
                        confirmClear.setTitle(String.format("Clear all tasks in %s?", mSpinner.getSelectedItem().toString()));
                        confirmClear.setPositiveButton("OK", new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mTaskItems.clear();
                                mTaskAdapter.notifyDataSetChanged();
                                mDb.storeTasks(mTaskItems);

                                Toast.makeText(MainActivity.this, "List cleared", Toast.LENGTH_SHORT).show();
                                showClearList();
                                showClearCompletedTasks(false);
                                showResetCheckboxes(false);
                                showSortByCheckbox(false);
                            }
                        });

                        confirmClear.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        confirmClear.show();
                        break;

                    case R.id.clear_tasks_completed:
                        for (int i = mTaskItems.size() - 1; i >= 0; i--) {
                            if(mTaskItems.get(i).isDone()) {
                                mTaskItems.remove(i);
                            }
                        }
                        mTaskAdapter.notifyDataSetChanged();
                        mDb.storeTasks(mTaskItems);
                        checkIfTasksCompleted();
                        showClearList();
                        Toast.makeText(MainActivity.this, "Cleared completed tasks", Toast.LENGTH_SHORT).show();
                        break;

                    case R.id.delete_list:
                        AlertDialog.Builder confirmDeleteList = new AlertDialog.Builder(MainActivity.this);
                        confirmDeleteList.setTitle(String.format("Confirm delete %s list?", mSpinner.getSelectedItem().toString()));

                        confirmDeleteList.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int listPosition = mSpinner.getSelectedItemPosition();
                                if (listPosition != 0 && listPosition != mSavedListMenu.size()-1) {

                                    mSavedListMenu.remove(listPosition);
                                    mSpinnerAdapter.notifyDataSetChanged();
                                    mTaskItems.clear();
                                    mDb.storeTasks(mTaskItems);
                                    mDb.storeListSpinners(mSavedListMenu);
                                }
                                mSpinner.setSelection(0);
                            }
                        });

                        confirmDeleteList.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });

                        confirmDeleteList.show();
                        break;

                    case R.id.reset_checkboxes:
                        for (Task eachTask : mTaskItems) {
                            eachTask.setDone(false);
                        }
                        Toast.makeText(MainActivity.this, "Reset checkboxes", Toast.LENGTH_SHORT).show();
                        mTaskAdapter.notifyDataSetChanged();
                        mDb.storeTasks(mTaskItems);
                        checkIfTasksCompleted();
                        break;

                }
                return true;
            }
        });

        //Edit Text Listener View
        mUserInputView.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                mAddToList.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                hideAddToListImage();
            }
        });

        //Click on Add to List button
        mAddToList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTask = new Task(mUserInputView.getText().toString());

                if (!mTask.getTaskName().equals("")) {
                    mTaskItems.add(mTask);
                    mUserInputView.setText("");

                    hideAddToListImage();

                    hideSoftKeyboard();
                    mDb.storeTasks(mTaskItems);
                }
                showClearList();
            }
        });

        //Clicking on a row
        mListViewLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

            //Linear layout for the custom dialog
            LinearLayout layout = new LinearLayout(MainActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50,100,50,100);
            layout.setMinimumHeight(100);

            //Title
            TextView title = new TextView(MainActivity.this);
            title.setText("Edit Task Name: ");
            title.setTextSize(20);
            title.setTextColor(Color.BLACK);
            title.setTypeface(null, Typeface.BOLD);

            //Edit Text for userinput
            final EditText editTaskName = new EditText(MainActivity.this);
            final Task task = (Task) adapterView.getAdapter().getItem(position);
            editTaskName.setText(task.getTaskName());

            //Adding the title and edit text to the layout
            layout.addView(title);
            layout.addView(editTaskName);

            //Set the alert dialog view as the layout
            alert.setView(layout);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String newTaskName = editTaskName.getText().toString();
                    if (newTaskName.equals("")){
                        Toast.makeText(MainActivity.this, "Cannot set task to empty", Toast.LENGTH_SHORT).show();
                    } else {
                        task.setTaskName(newTaskName);
                    }
                    mDb.storeTasks(mTaskItems);
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            alert.show();
        }
        });

        //Long click on row
        mListViewLayout.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListViewLayout.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {

                if(checked){
                    mItemSelectedCount++;

                } else {
                    mItemSelectedCount--;
                }

                actionMode.setTitle(mItemSelectedCount + " selected");
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {

                getMenuInflater().inflate(R.menu.menu_task_list_context, menu);

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                int id = menuItem.getItemId();
                if(id == R.id.delete_task) {
                    SparseBooleanArray position = mListViewLayout.getCheckedItemPositions();

                    for(int i = position.size()-1; i >= 0; i--) {
                        if(position.valueAt(i)) {
                            mTaskItems.remove(position.keyAt(i));
                        }

                    }
                }
                actionMode.finish();
                mTaskAdapter.notifyDataSetChanged();
                mDb.storeTasks(mTaskItems);
                showClearList();
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                mItemSelectedCount = 0;
            }
        });
    }

    private class TaskAdapter extends ArrayAdapter<Task> {
        ViewHolder viewHolder;

        TaskAdapter(ArrayList<Task> tasks) {
            super(MainActivity.this, R.layout.task_layout, tasks );
        }

        private class ViewHolder {
            TextView taskName;
            CheckBox checkBox;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.task_layout, null);
                viewHolder = new ViewHolder();

                viewHolder.taskName = (TextView) convertView.findViewById(R.id.task_text_row);
                viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.task_checkbox_row);
                convertView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();

            }

            viewHolder.taskName.setText(mTaskItems.get(position).getTaskName());
            viewHolder.checkBox.setChecked(mTaskItems.get(position).isDone());

            viewHolder.checkBox.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    if(((CheckBox)view).isChecked()){
                        mTaskItems.get(position).setDone(true);
                    } else {
                        mTaskItems.get(position).setDone(false);
                    }
                    checkIfTasksCompleted();
                    mDb.storeTasks(mTaskItems);
                }
            });

            return convertView;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_task_list_context, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.delete_task) {
            AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            mTaskItems.remove(menuInfo.position);
            mTaskAdapter.notifyDataSetChanged();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void hideAddToListImage() {
        if(mUserInputView.getText().toString().equals("")) {
            mAddToList.setVisibility(View.INVISIBLE);
        }
    }

    private void loadTasks() {
        ArrayList<Task> savedTaskList = mDb.loadTasks();
        mTaskAdapter.notifyDataSetChanged();

        for(Task tasks : savedTaskList) {
            mTaskItems.add(tasks);
            if(tasks.isDone()) {
                showClearCompletedTasks(true);
            }
        }
    }

    private void loadSpinnerListNames() {
        ArrayList<String> savedSpinnerListNames = mDb.loadListSpinners();
        for (String listNames : savedSpinnerListNames) {
            mSavedListMenu.add(listNames);
        }
    }

    private void checkIfTasksCompleted() {
        if(mTaskItems.size() <= 0) {
            showClearCompletedTasks(false);
            showResetCheckboxes(false);
            showSortByCheckbox(false);
        } else {
            for (Task task : mTaskItems) {
                if (task.isDone()) {
                    showClearCompletedTasks(true);
                    showResetCheckboxes(true);
                    showSortByCheckbox(true);
                    break;
                } else {
                    showClearCompletedTasks(false);
                    showResetCheckboxes(false);
                    showSortByCheckbox(false);
                }
            }
        }
    }

    private void showEditListName(boolean bool) {
        mToolBar.getMenu().getItem(0).setVisible(bool);
    }

    private void showDeleteList(boolean bool) {
        mToolBar.getMenu().getItem(1).setVisible(bool);
    }

    private void showClearCompletedTasks(boolean bool) {
        mToolBar.getMenu().getItem(2).setVisible(bool);
    }

    private void showSortByCheckbox(boolean bool) {
        mToolBar.getMenu().getItem(4).setVisible(bool);
    }
    private void showResetCheckboxes(boolean bool) {
        mToolBar.getMenu().getItem(6).setVisible(bool);
    }

    private void showClearList() {
        if(mTaskItems.size() > 0) {
            mToolBar.getMenu().getItem(3).setVisible(true);
        } else {
            mToolBar.getMenu().getItem(3).setVisible(false);

        }
    }

    private void showDeleteAndEditListName() {
        showEditListName(true);
        showDeleteList(true);
    }

    //AdMob Banner
    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }
}

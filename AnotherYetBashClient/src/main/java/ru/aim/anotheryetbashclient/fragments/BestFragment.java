package ru.aim.anotheryetbashclient.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.Serializable;
import java.util.Calendar;

import ru.aim.anotheryetbashclient.ActionsAndIntents;
import ru.aim.anotheryetbashclient.QuotesAdapter;
import ru.aim.anotheryetbashclient.R;
import ru.aim.anotheryetbashclient.helper.DbHelper;
import ru.aim.anotheryetbashclient.loaders.BestLoader;
import ru.aim.anotheryetbashclient.loaders.RandomLoader;
import ru.aim.anotheryetbashclient.loaders.SimpleLoaderCallbacks;
import ru.aim.anotheryetbashclient.loaders.SimpleResult;

public class BestFragment extends AbstractFragment implements SimpleLoaderCallbacks<Cursor> {

    DateResult dateResult = new DateResult();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            dateResult = (DateResult) savedInstanceState.getSerializable("dateResult");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(RandomLoader.ID, Bundle.EMPTY, this);
    }

    @Override
    public int getType() {
        return ActionsAndIntents.TYPE_BEST;
    }

    @Override
    public void onManualUpdate() {
        setRefreshing(true);
        getLoaderManager().getLoader(BestLoader.ID).forceLoad();
    }

    @Override
    public Loader<SimpleResult<Cursor>> onCreateLoader(int id, Bundle args) {
        setRefreshing(true);
        return new BestLoader(getActivity(), args);
    }

    @Override
    public void onLoadFinished(Loader<SimpleResult<Cursor>> loader, SimpleResult<Cursor> data) {
        setRefreshing(false);
        if (data.containsError()) {
            showWarning(getFragmentManager(), data.getError().getMessage());
        } else {
            ListAdapter listAdapter;
            if (dateResult.isToday()) {
                listAdapter = new BestAdapter(getActivity(), data.getResult());
            } else {
                listAdapter = new QuotesAdapter(getDbHelper(), getActivity(), data.getResult());
            }
            setListAdapter(listAdapter);
            setMenuItemsVisibility(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<SimpleResult<Cursor>> loader) {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.best, menu);
        MenuItem menuItem = menu.findItem(R.id.action_today);
        if (dateResult.isToday()) {
            menuItem.setTitle(R.string.today);
        } else {
            menuItem.setTitle(String.format("%s, %s",
                    getResources().getStringArray(R.array.months)[dateResult.month - 1],
                    dateResult.year));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_today) {
            DateDialog dateDialog = new DateDialog();
            dateDialog.show(getFragmentManager(), "dateDialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onMessageReceived(Object message) {
        super.onMessageReceived(message);
        if (message instanceof DateResult) {
            setRefreshing(true);
            dateResult = (DateResult) message;
            getActivity().invalidateOptionsMenu();
            Bundle bundle = new Bundle();
            bundle.putSerializable("dateResult", dateResult);
            getLoaderManager().restartLoader(BestLoader.ID, bundle, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("dateResult", dateResult);
    }

    public static class DateResult implements Serializable {
        public int year = -1;
        public int month = -1;

        public boolean isToday() {
            return year == -1 && month == -1;
        }
    }

    /**
     * Date dialog.
     */
    public static class DateDialog extends DialogFragment implements DialogInterface.OnClickListener, NumberPicker.OnValueChangeListener {

        Calendar now = Calendar.getInstance();
        NumberPicker year;
        NumberPicker month;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View view = View.inflate(getActivity(), R.layout.custom_date_picker, null);

            year = (NumberPicker) view.findViewById(R.id.year);
            year.setMaxValue(now.get(Calendar.YEAR));
            year.setMinValue(2004);
            year.setValue(year.getMaxValue());
            year.setOnValueChangedListener(this);
            month = (NumberPicker) view.findViewById(R.id.month);
            month.setMaxValue(12);
            month.setMinValue(1);
            month.setDisplayedValues(getActivity().getResources().getStringArray(R.array.months));
            checkMonthInCurrentYear();
            month.setValue(month.getMaxValue());

            builder.setView(view);
            builder.setPositiveButton(R.string.ok, this);
            builder.setNeutralButton(R.string.today, this);
            builder.setTitle(R.string.choose_period);
            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            DateResult dateResult = new DateResult();
            if (which == AlertDialog.BUTTON_NEUTRAL) {
                // today
                dateResult.year = -1;
                dateResult.month = -1;
            } else if (which == AlertDialog.BUTTON_POSITIVE) {
                // custom date
                dateResult.year = year.getValue();
                dateResult.month = month.getValue();
            }
            sendMessage(getActivity(), BestFragment.class, dateResult);
        }

        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            checkMonthInCurrentYear();
        }

        void checkMonthInCurrentYear() {
            if (year.getValue() == now.get(Calendar.YEAR)) {
                int value = month.getValue();
                month.setMaxValue(now.get(Calendar.MONTH) + 1);
                if (value > month.getMaxValue()) {
                    month.setValue(month.getMaxValue());
                }
            } else {
                month.setMaxValue(12);
            }
        }
    }

    /**
     * Adapter for first page.
     */
    public static class BestAdapter extends BaseAdapter {

        Context context;
        int monthTitlePosition;
        Cursor cursor;

        public BestAdapter(Context context, Cursor cursor) {
            this.context = context;
            this.cursor = cursor;
            monthTitlePosition = 0;
            while (cursor.moveToNext()) {
                int flag = cursor.getInt(cursor.getColumnIndex(DbHelper.QUOTE_FLAG));
                if (flag == 1) {
                    break;
                }
                monthTitlePosition++;
            }
            cursor.moveToFirst();
        }

        @Override
        public int getCount() {
            return cursor.getCount() + 2;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position == 0) {
                TextView textView = (TextView) View.inflate(context, R.layout.best_title_item, null);
                textView.setText(R.string.best_of_day);
                return textView;
            } else if (position == monthTitlePosition) {
                TextView textView = (TextView) View.inflate(context, R.layout.best_title_item, null);
                textView.setText(R.string.best_of_week);
                return textView;
            } else {
                if (position < monthTitlePosition) {
                    position -= 1;
                } else {
                    position -= 2;
                }
                cursor.moveToPosition(position);
                ViewHolder viewHolder;
                if (convertView == null || convertView.getTag() == null) {
                    viewHolder = new ViewHolder();
                    convertView = LayoutInflater.from(context).inflate(R.layout.list_item, null, false);
                    viewHolder.date = (TextView) convertView.findViewById(android.R.id.text1);
                    viewHolder.id = (TextView) convertView.findViewById(android.R.id.text2);
                    viewHolder.text = (TextView) convertView.findViewById(R.id.text);
                    viewHolder.isNew = (TextView) convertView.findViewById(R.id.newQuote);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                String id = cursor.getString(cursor.getColumnIndex(DbHelper.QUOTE_PUBLIC_ID));
                String date = cursor.getString(cursor.getColumnIndex(DbHelper.QUOTE_DATE));
                String text = cursor.getString(cursor.getColumnIndex(DbHelper.QUOTE_TEXT));
                viewHolder.date.setText(date);
                viewHolder.id.setText(id);
                viewHolder.text.setText(Html.fromHtml(text));
                viewHolder.publicId = id;
                viewHolder.innerId = cursor.getLong(cursor.getColumnIndex(DbHelper.QUOTE_ID));
            }
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return position != 0 && position != monthTitlePosition;
        }

        public static class ViewHolder {
            public TextView date;
            public TextView id;
            public TextView text;
            public TextView isNew;
            public String publicId;
            public long innerId;
        }
    }
}
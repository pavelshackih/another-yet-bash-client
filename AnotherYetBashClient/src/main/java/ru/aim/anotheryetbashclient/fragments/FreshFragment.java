package ru.aim.anotheryetbashclient.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListAdapter;

import ru.aim.anotheryetbashclient.ActionsAndIntents;
import ru.aim.anotheryetbashclient.QuotesAdapter;
import ru.aim.anotheryetbashclient.R;
import ru.aim.anotheryetbashclient.loaders.FreshLoader;
import ru.aim.anotheryetbashclient.loaders.FreshResult;
import ru.aim.anotheryetbashclient.loaders.SimpleLoaderCallbacks;
import ru.aim.anotheryetbashclient.loaders.SimpleResult;

import static ru.aim.anotheryetbashclient.ActionsAndIntents.CURRENT_PAGE;

public class FreshFragment extends AbstractFragment implements SimpleLoaderCallbacks<FreshResult> {

    int currentPage;
    int maxPage;

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(FreshLoader.ID, Bundle.EMPTY, this);
    }

    @Override
    public void onManualUpdate() {
        restartLoader(Bundle.EMPTY);
    }

    Bundle buildArguments() {
        Bundle bundle = new Bundle();
        bundle.putInt(CURRENT_PAGE, currentPage);
        return bundle;
    }

    void restartLoader(Bundle arg) {
        setRefreshing(true);
        getLoaderManager().restartLoader(FreshLoader.ID, arg, this);
    }

    @Override
    public int getType() {
        return ActionsAndIntents.TYPE_NEW;
    }

    @Override
    public Loader<SimpleResult<FreshResult>> onCreateLoader(int id, Bundle args) {
        setRefreshing(true);
        return new FreshLoader(getActivity(), args);
    }

    @Override
    public void onLoadFinished(Loader<SimpleResult<FreshResult>> loader, final SimpleResult<FreshResult> data) {
        setRefreshing(false);
        if (data.containsError()) {
            showWarning(getFragmentManager(), data.getError().getMessage());
        } else {
            FreshResult freshResult = data.getResult();
            ListAdapter listAdapter = new QuotesAdapter(getDbHelper(), getActivity(), freshResult.cursor);
            setListAdapter(listAdapter);
            currentPage = freshResult.currentPage;
            if (freshResult.maxPage > maxPage) {
                maxPage = data.getResult().maxPage;
            }
            setMenuItemsVisibility(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<SimpleResult<FreshResult>> loader) {
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.news, menu);
        MenuItem menuItem = menu.findItem(R.id.action_page);
        if (currentPage != 0) {
            menuItem.setTitle(Integer.toString(currentPage));
        }
        checkItemsVisibility(menu);
        if (maxPage != 0 && isItemsVisible()) {
            menu.findItem(R.id.action_back).setVisible(currentPage != maxPage);
            menu.findItem(R.id.action_forward).setVisible(currentPage != 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_forward) {
            currentPage -= 1;
            restartLoader(buildArguments());
            return true;
        }
        if (item.getItemId() == R.id.action_back) {
            currentPage += 1;
            restartLoader(buildArguments());
            return true;
        }
        if (item.getItemId() == R.id.action_page) {
            FreshPickerDialog dialog = new FreshPickerDialog();
            dialog.setArguments(NumberPickerDialog.buildArgs(currentPage, maxPage));
            dialog.show(getFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onMessageReceived(Object message) {
        super.onMessageReceived(message);
        currentPage = (Integer) message;
        restartLoader(buildArguments());
    }

    public static class FreshPickerDialog extends NumberPickerDialog {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            sendMessage(getActivity(), FreshFragment.class, picker.getValue());
        }
    }
}

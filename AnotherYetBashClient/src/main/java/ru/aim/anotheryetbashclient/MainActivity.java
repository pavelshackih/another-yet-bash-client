package ru.aim.anotheryetbashclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import ru.aim.anotheryetbashclient.fragments.AbstractFragment;
import ru.aim.anotheryetbashclient.fragments.FragmentsFactory;
import ru.aim.anotheryetbashclient.helper.Utils;

import static ru.aim.anotheryetbashclient.Package.updateHeader;
import static ru.aim.anotheryetbashclient.SettingsHelper.saveType;

/**
 *
 */
public class MainActivity extends RulezActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    FrameLayout mainFrame;
    ListView mTypesListView;
    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;
    MenuItemsAdapter adapter;
    boolean hideAdditionalMenu;

    int currentTypeId;
    final String fragmentKey = "fragmentKey";
    ListFragment mListFragment;
    boolean mScrollByVolumeEnabled;

    BroadcastReceiver notifyBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(MainActivity.this, intent.getStringExtra(ActionsAndIntents.MESSAGE), Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainFrame = (FrameLayout) findViewById(R.id.main_frame);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                setMenuItemsVisible(true);
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                setMenuItemsVisible(false);
                super.onDrawerOpened(drawerView);
            }
        };
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mTypesListView = (ListView) findViewById(R.id.types);
        adapter = new MenuItemsAdapter(this, R.layout.menu_item);
        mTypesListView.setAdapter(adapter);
        mTypesListView.setOnItemClickListener(this);
        mTypesListView.post(new Runnable() {
            @Override
            public void run() {
                mTypesListView.setItemChecked(currentTypeId, true);
            }
        });

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter1 = new IntentFilter(ActionsAndIntents.NOTIFY);
        localBroadcastManager.registerReceiver(notifyBroadcastReceiver, intentFilter1);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState == null && SettingsHelper.isPreloadedAvailable(this)) {
            currentTypeId = ActionsAndIntents.TYPE_OFFLINE;
        }
        setFragment();

        findViewById(R.id.action_settings).setOnClickListener(this);
    }

    AbstractFragment getCurrentFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentKey);
        if (fragment instanceof AbstractFragment) {
            return (AbstractFragment) fragment;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(notifyBroadcastReceiver);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        Utils.setItemsVisibility(menu, !hideAdditionalMenu);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (currentTypeId != position) {
            currentTypeId = position;
            setFragment();
        }
        mDrawerLayout.closeDrawers();
    }

    void setFragment() {
        updateHeader(this, currentTypeId);
        Fragment fragment = FragmentsFactory.getFragment(currentTypeId);
        if (fragment instanceof ListFragment) {
            mListFragment = (ListFragment) fragment;
        }
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_frame, fragment, fragmentKey)
                .commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveType(this, currentTypeId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SimplifiableIfStatement
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void setMenuItemsVisible(boolean visible) {
        hideAdditionalMenu = !visible;
        if (getCurrentFragment() != null) {
            getCurrentFragment().setMenuItemsVisibility(visible);
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.action_settings) {
            mDrawerLayout.closeDrawers();
            startActivity(new Intent(this, SettingsActivity.class));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mScrollByVolumeEnabled && mListFragment != null) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                mListFragment.getListView().smoothScrollByOffset(1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                mListFragment.getListView().smoothScrollByOffset(-1);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScrollByVolumeEnabled = SettingsHelper.isScrollByVolumeEnabled(this);
    }
}
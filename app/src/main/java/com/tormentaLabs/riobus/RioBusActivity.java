package com.tormentaLabs.riobus;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.tormentaLabs.riobus.favorite.FavoriteActivity_;
import com.tormentaLabs.riobus.map.MapFragment_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_rio_bus)
public class RioBusActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = RioBusActivity_.class.getName();

    private ActionBarDrawerToggle rioBusDrawerToggle;

    @ViewById(R.id.riobusToolbar)
    Toolbar rioBusToolBar;

    @ViewById(R.id.rio_bus_drawer_layout)
    DrawerLayout rioBusDrawerLayout;

    @ViewById(R.id.navigation)
    NavigationView navigationView;

    @AfterViews
    public void afterViews() {
        setupToolBar();
        navigationView.setNavigationItemSelectedListener(this);
        getSupportFragmentManager().beginTransaction()
        .replace(R.id.content_frame, new MapFragment_())
                .commit();
    }

    private void setupToolBar() {
        rioBusDrawerToggle = new ActionBarDrawerToggle(this, rioBusDrawerLayout, rioBusToolBar, R.string.drawer_open, R.string.drawer_close);
        rioBusDrawerLayout.setDrawerListener(rioBusDrawerToggle);

        setSupportActionBar(rioBusToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        rioBusDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        rioBusDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (rioBusDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        rioBusDrawerLayout.closeDrawers();
        switch (menuItem.getItemId()) {
            case R.id.action_favorite:
                openActivity(new FavoriteActivity_());
                break;
            default:
                break;
        }
        return true;
    }

    private void openActivity(AppCompatActivity activity) {
        Intent i = new Intent(this, activity.getClass());
        startActivity(i);
    }
}
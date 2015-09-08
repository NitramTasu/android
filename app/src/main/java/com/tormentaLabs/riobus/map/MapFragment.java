package com.tormentaLabs.riobus.map;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.tormentaLabs.riobus.R;
import com.tormentaLabs.riobus.RioBusActivity_;
import com.tormentaLabs.riobus.bus.BusMapConponent;
import com.tormentaLabs.riobus.map.listener.MapComponentListener;
import com.tormentaLabs.riobus.map.utils.MapPrefs_;
import com.tormentaLabs.riobus.map.utils.MapUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.EditorAction;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;

/**
 * @author limazix
 * @since 2.0
 * Created on 01/09/15
 */
@EFragment(R.layout.fragment_google_map)
public class MapFragment extends Fragment implements MapComponentListener {

    private static final String TAG = MapFragment_.class.getName();
    private GoogleMap map;
    private SupportMapFragment mapFragment;

    @SystemService
    InputMethodManager inputMethodManager;

    @Pref
    MapPrefs_ mapPrefs;

    @Bean
    BusMapConponent busMapComponent;

    @AfterViews
    public void afterViews() {
        mapFragment = getMapFragment();
        map = mapFragment.getMap();
        if(map.getUiSettings() != null) {
            map.getUiSettings().setMapToolbarEnabled(mapPrefs.isMapToolbarEnable().get());
            map.getUiSettings().setCompassEnabled(mapPrefs.isMapCompasEnable().get());
            map.setTrafficEnabled(mapPrefs.isMapTrafficEnable().get());
        }
        map.setMyLocationEnabled(mapPrefs.isMapMyLocationEnable().get());
    }

    /**
     * Listener of input search for keybord event action <br>
     *     It is called every time the user press enter button.
     * @param textView
     * @param actionId
     * @param event
     */
    @EditorAction(R.id.search)
    public void onPerformSearch(TextView textView, int actionId, KeyEvent event) {
        String keyword = textView.getText().toString();
        if(event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && MapUtils.isValidString(keyword)) {
            inputMethodManager.hideSoftInputFromWindow(textView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            busMapComponent.setLine(keyword);
            busMapComponent.setListener(this);
            busMapComponent.setMap(map);
            busMapComponent.buildComponent();
        }
    };

    @Click(R.id.sidemenu_toggle)
    public void sidemenuToggle() {
        ((RioBusActivity_)getActivity()).sidemenuToggle();
    }

    /**
     * Used to access the map fragment which is a child fragment called map_fragment
     * @return SupportMapFragment
     */
    private SupportMapFragment getMapFragment() {
        return (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
    }

    @Override
    public void onComponentMapReady() {
        Log.d(TAG, "Map Ready");
    }

    @Override
    public void onComponentMapError(Exception error) {
        Log.e(TAG, error.getMessage());
    }
}
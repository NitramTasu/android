package com.tormentaLabs.riobus;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;
import com.tormentaLabs.riobus.adapter.BusInfoWindowAdapter;
import com.tormentaLabs.riobus.asyncTasks.BusSearchTask;
import com.tormentaLabs.riobus.common.BusDataReceptor;
import com.tormentaLabs.riobus.common.NetworkUtil;
import com.tormentaLabs.riobus.common.Util;
import com.tormentaLabs.riobus.model.Bus;
import com.tormentaLabs.riobus.model.BusData;
import com.tormentaLabs.riobus.model.Itinerary;
import com.tormentaLabs.riobus.model.MapMarker;
import com.tormentaLabs.riobus.model.Spot;
import com.tormentaLabs.riobus.service.HttpService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, BusDataReceptor,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private AutoCompleteTextView search;
    private ImageButton info;


    ArrayAdapter arrayAdapter;
    String[] lines;

    Location currentLocation;

    MapMarker mapMarker;
    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;

    private Subscription _subcription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapa);



        buildGoogleApiClient();
        setupSearch();
        setupInfo();
        setupMap();
        getSupportActionBar().hide();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void setupInfo() {
        info = (ImageButton) findViewById(R.id.button_about);

        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.about_dialog);
                dialog.setTitle(getString(R.string.about_title));
                TextView tv = (TextView) dialog.findViewById(R.id.content);
                tv.setText(Html.fromHtml(getString(R.string.about_text)));
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                dialog.show();
            }
        });
    }

    private void setupSearch() {

        search = (AutoCompleteTextView) findViewById(R.id.search);


        //Quando o usuario digita enter, ele faz a requisição procurando a posição daquela linha
        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                String searchContent = search.getText().toString();
                Activity activity = MainActivity.this;

                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER) &&
                        Util.isValidString(searchContent)) {

                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(search.getWindowToken(), 0);

                    if (NetworkUtil.checkInternetConnection(activity)) {

                        new BusSearchTask(MainActivity.this).execute(searchContent);

                    } else {
                        Toast.makeText(activity, getString(R.string.error_connection_internet), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });

        defineSearchObservable();



    }

    private void defineSearchObservable() {
        final HttpService serviceApi;

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(EnvironmentConfig.URL_ENDPOINT)
                .setConverter(new GsonConverter(gson))
                .build();


        serviceApi = restAdapter.create(HttpService.class);

        Action1 subscriberOk = new Action1<Observable<List<Bus>>>() {
            @Override
            public void call(Observable<List<Bus>> observableBuses) {
                Log.i("Rx", "Entrou ");

                observableBuses.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action1<List<Bus>>() {
                                @Override
                                public void call(List<Bus> buses) {
                                    lines = new String[buses.size()];
                                    int cont = 0;
                                    for (Bus bus:buses ) {
                                        //Log.i("Teste","teste "+bus.getLine());

                                        lines[cont] = bus.getLine();

                                        cont++;
                                    }

                                    arrayAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1,lines );

                                    search.setAdapter(arrayAdapter);
                                }
                            });
            }
        };

        Action1 subscriberError = new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                // Handle Error
                throwable.printStackTrace();
            }
        };

        RxTextView.textChangeEvents(search)
                .debounce(400, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .filter(new Func1<TextViewTextChangeEvent, Boolean>() {
                    @Override
                    public Boolean call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        return textViewTextChangeEvent.view().getText().length() > 2;
                    }
                })
                .map(new Func1<TextViewTextChangeEvent, Observable<List<Bus>>>() {
                    @Override
                    public Observable<List<Bus>> call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        return  serviceApi.getPageObservable(textViewTextChangeEvent.view().getText().toString());
                    }
                })
                .subscribe(subscriberOk,subscriberError);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        map = mapFragment.getMap();
        if(map.getUiSettings() != null) {
            map.getUiSettings().setMapToolbarEnabled(false);
            map.getUiSettings().setCompassEnabled(false);
            map.setTrafficEnabled(true);
        }
        map.setMyLocationEnabled(false);

        mapMarker = new MapMarker(map);
    }

    public void updateUserLocation() {
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (currentLocation == null)
            return;
        LatLng position = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 13));
        map.animateCamera(CameraUpdateFactory.zoomTo(16), 2000, null);
        mapMarker.markUserPosition(this, position);

    }

    protected void drawItineraryPolyline(Itinerary itinerary) {
        List<Spot> spots = itinerary.getSpots();
        if (spots != null && !spots.isEmpty()) {

            List<Spot> spotsGoing = new ArrayList<Spot>();
            List<Spot> spotsReturning = new ArrayList<Spot>();
            PolylineOptions line = new PolylineOptions();
            PolylineOptions lineReturning = new PolylineOptions();
            for(int i=0; i<spots.size(); i++) {
                Spot spot = spots.get(i);
                if(spot.getReturning().equalsIgnoreCase("true")){
                    spotsReturning.add(spot);
                    lineReturning.add(new LatLng(spot.getLatitude(),spot.getLongitude()));
                }
                else{
                    spotsGoing.add(spot);
                    line.add(new LatLng(spot.getLatitude(),spot.getLongitude()));
                }
            }

            Random rnd = new Random();
            float[] hsv = new float[3];
            map.setInfoWindowAdapter(new BusInfoWindowAdapter(this));
            MapMarker marker = new MapMarker(map);

            int r = rnd.nextInt(256); int g = rnd.nextInt(256); int b = rnd.nextInt(256);
            int color = Color.argb(255, r, g, b);
            lineReturning.color(color);
            lineReturning.width(6);
            lineReturning.geodesic(true);
            map.addPolyline(lineReturning);

            if(!spotsReturning.isEmpty()){
                Color.RGBToHSV(r, g, b, hsv);
                marker.addMarkers(spotsReturning.get(0), hsv[0], itinerary);
                marker.addMarkers(spotsReturning.get(spotsReturning.size() - 1), hsv[0], itinerary);
            }

            r *= 0.75; g *= 0.75; b *= 0.75;
            color = Color.argb(255, r, g, b);
            line.color(color);
            line.width(6);
            line.geodesic(true);
            map.addPolyline(line);

            Color.RGBToHSV(r, g, b, hsv);
            marker.addMarkers(spotsGoing.get(0), hsv[0], itinerary);
            marker.addMarkers(spotsGoing.get(spotsGoing.size() - 1), hsv[0], itinerary);
        }
    }

    @Override
    public void retrieveBusData(BusData busData) {
        List<Bus> buses = busData.getBuses();
        if (buses == null) {
            Toast.makeText(this, getString(R.string.error_connection_server), Toast.LENGTH_SHORT).show();
            return;
        }
        if (buses.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_bus_404), Toast.LENGTH_SHORT).show();
            return;
        }

        map.clear();

        List<Itinerary> itineraries = busData.getItineraries();
        if(itineraries != null && !itineraries.isEmpty()) {
            for(Itinerary itinerary: itineraries)
                drawItineraryPolyline(itinerary);
        }

        map.setInfoWindowAdapter(new BusInfoWindowAdapter(this));
        MapMarker marker = new MapMarker(map);
        marker.addMarkers(buses);
        LatLngBounds.Builder builder = marker.getBoundsBuilder();

        if (currentLocation != null) {
            LatLng userPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mapMarker.markUserPosition(this, userPosition);
            builder.include(userPosition);
        }

        LatLngBounds bounds = builder.build();

        int padding = 100; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        map.moveCamera(cu);
        map.animateCamera(cu);

    }

    @Override
    public void onMapReady(GoogleMap map) {
    }


    @Override
    public void onConnected(Bundle bundle) {
        updateUserLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
}

package com.tormentaLabs.riobus.model;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.tormentaLabs.riobus.R;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MapMarker {

    private GoogleMap map;
    private LatLngBounds.Builder builder;
    Marker userMarker;

    public MapMarker(GoogleMap map){
        this.map = map;
        builder = new LatLngBounds.Builder();
    }

    public void addMarkers(List<Bus> buses) {
        for (Bus bus : buses) {
            map.addMarker(getMarker(bus));
            builder.include(new LatLng(bus.getLatitude(), bus.getLongitude()));
        }
    }
    private MarkerOptions getMarker(Bus bus) {
        MarkerOptions options = new MarkerOptions();
        LatLng position = new LatLng(bus.getLatitude(), bus.getLongitude());
        options.position(position);
        options.anchor(0.0f, 1.0f);
        options.icon(getIcon(bus.getTimestamp()));
        options.snippet(new Gson().toJson(bus));
        return options;
    }


    public void addMarkers(Spot spot, float hueColor, Itinerary itinerary) {
        map.addMarker(getMarker(spot, hueColor, itinerary));
        builder.include(new LatLng(spot.getLatitude(), spot.getLongitude()));
    }
    private MarkerOptions getMarker(Spot spot, float hueColor, Itinerary itinerary) {
        MarkerOptions options = new MarkerOptions();
        LatLng position = new LatLng(spot.getLatitude(), spot.getLongitude());
        options.position(position);
        options.anchor(0.0f, 1.0f);
        options.icon(BitmapDescriptorFactory.defaultMarker(hueColor));
        options.snippet(new Gson().toJson(itinerary)+">"+spot.getReturning());
        return options;
    }


    public void markUserPosition(Context context, LatLng posicao) {

        if (userMarker != null) {
            userMarker.remove();
        }

        userMarker = map.addMarker(new MarkerOptions()
                .position(posicao)
                .title(context.getString(R.string.marker_user))
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.man_maps)));
    }

    private BitmapDescriptor getIcon(Date data) {
        DateTime current = new DateTime(Calendar.getInstance());
        DateTime last = new DateTime(data);
        int diff =  Minutes.minutesBetween(last, current).getMinutes();

        BitmapDescriptor bitmap;

        if(diff >= 5 && diff < 10 ) {
            bitmap = BitmapDescriptorFactory
                    .fromResource(R.drawable.bus_yellow);
        }  else if(diff >= 10 ) {
           bitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.bus_red);
        } else {
            bitmap = BitmapDescriptorFactory
                    .fromResource(R.drawable.bus_green);
        }
        return bitmap;
    }

    public LatLngBounds.Builder getBoundsBuilder(){
        return builder;
    }

}

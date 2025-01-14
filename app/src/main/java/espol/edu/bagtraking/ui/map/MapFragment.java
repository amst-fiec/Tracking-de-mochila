package espol.edu.bagtraking.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedList;


import espol.edu.bagtraking.Activity.perfil_usuario_1;
import espol.edu.bagtraking.Modelo.Variables;
import espol.edu.bagtraking.R;

import static android.content.Context.LOCATION_SERVICE;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {
    private static final int TAG_CODE_PERMISSION_LOCATION = 200;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private  DatabaseReference ref;

    private MapViewModel mapViewModel;
    private static final String TAG = MapFragment.class.getSimpleName();
    private GoogleMap mMap;

    private LinkedList<LatLng> posiciones;

    private HashMap<String, Marker> mMarkers;
    public static double DISTANCIA = 0.0d;
    public static boolean Ready = false;
    private Location location;
    private ChildEventListener child;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mapViewModel =
                ViewModelProviders.of(this).get(MapViewModel.class);
        View root = inflater.inflate(R.layout.fragment_map, container, false);
        mMap = null;
        ((perfil_usuario_1) getActivity()).getSupportActionBar().setTitle("Home Mapa");
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map1);
        if (mapFragment == null) {
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            mapFragment = SupportMapFragment.newInstance();
            ft.replace(R.id.map1, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        DISTANCIA = 0.0d;
        mMarkers = new HashMap<>();
        posiciones = new LinkedList<>();
        this.mAuth = FirebaseAuth.getInstance();
        this.currentUser = mAuth.getCurrentUser();
        ActivityCompat.requestPermissions(getActivity(), new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION },
                TAG_CODE_PERMISSION_LOCATION);

        this.ref = FirebaseDatabase.getInstance().getReference().child("datos_gps").child(Variables.ID_MALETA);
        this.ref.removeEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



        StartThread();
        HiloNotificacions();

        return root;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Authenticate with Firebase when the Google map is loaded
        mMap = googleMap;

        mMap.setMaxZoomPreference(16);
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }else {

            Toast.makeText(getActivity().getApplicationContext(), "No inicia Mi posicion", Toast.LENGTH_LONG).show();
        }



        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        LocationManager locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);

        try {
            Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (myLocation != null) {
                Log.d("TAG", "Not null");
                this.location = myLocation;
            }
            else {
                Log.d("TAG", "NULL");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
            }
        }
        catch (SecurityException se) {
            Log.d("TAG", "SE CAUGHT");
            se.printStackTrace();
        }
        subscribeToUpdates();

        //Toast.makeText(getActivity(),"La distancia de tu maleta ahora es de .."+String.valueOf(DISTANCIA), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        Ready = true;

        super.onStart();
    }

    @Override
    public void onResume() {

        if(child!=null){
            ref.removeEventListener(child);
        }

        super.onResume();
    }

    @Override
    public void onDestroy() {


        super.onDestroy();
    }

    private void subscribeToUpdates() {
       child = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                setMarker(dataSnapshot);



            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                setMarker(dataSnapshot);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d(TAG, "Failed to read value.", error.toException());

            }
        };
       this.ref.addChildEventListener(child);
    }

    private void setMarker(DataSnapshot dataSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        String key = dataSnapshot.getKey();
        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshot.getValue();
        double lat = Double.parseDouble(value.get("Latitud").toString());
        double lng = Double.parseDouble(value.get("Longitud").toString());
        Date date = new Date();
        DateFormat hourdateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");


        if (lat != 0 && lng != 0) {
            LatLng location = new LatLng(lat, lng);
            posiciones.add(location);
            if (!mMarkers.containsKey(key)) {
                mMarkers.put(key, mMap.addMarker(new MarkerOptions().title(hourdateFormat.format(date)).position(location)));
            } else {
                mMarkers.get(key).setPosition(location);
            }
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : mMarkers.values()) {
                builder.include(marker.getPosition());
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 350));
        }

    }

    public void StartThread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        while (posiciones.isEmpty()) {
                        }
                        LocationManager locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);

                        try {
                            Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                            if (myLocation != null) {
                                Log.d("TAG", "Not null");
                                location = myLocation;
                            }
                            else {
                                Log.d("TAG", "NULL");
                                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) getContext());
                            }
                        }
                        catch (SecurityException se) {
                            Log.d("TAG", "SE CAUGHT");
                            se.printStackTrace();
                        }

                        DISTANCIA = CalcularDistancia(posiciones.getLast().latitude, posiciones.getLast().longitude
                                ,location.getLatitude(), location.getLongitude());
                        //toast.show();
                        //Toast.makeText(contex,"La distancia de tu maleta ahora es de .."+String.valueOf(DISTANCIA), Toast.LENGTH_SHORT).show();


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        thread.start();
    }

    private double CalcularDistancia(double lat1, double long1, double lat2, double long2) {
        double constante = Math.PI / 180;
        lat2 = lat2 * constante;
        long2 = long2 * constante;
        lat1 = lat1 * constante;
        long1 = long1 * constante;
        double R = 6371; //km

        double deltalat = lat2 - lat1;
        double deltalong = long2 - long1;
        double a = Math.pow(Math.sin(deltalat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltalong / 2), 2);
        double c = 2 * R * Math.atan(Math.sqrt(a)) * 1000;
        return c;
    }

    public void HiloNotificacions() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {

                    try {
                        Thread.sleep(10000);
                        while (posiciones.isEmpty() && DISTANCIA != 0) {
                        }
                        if (DISTANCIA >= Variables.MAXIMO_DISTANCIA && Variables.RECIBIR_NOTIFICACION) {

                            enviarDatos();


                        }


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        thread.start();
    }

    public void enviarDatos() {
        String url = "https://things.ubidots.com/api/v1.6/devices/MyCellTraking/?token=BBFF-ibSk9rqslckzhIWiPyu2YYN18FG5Iw";

        try {
            RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
            final org.json.JSONObject jsonBody = new org.json.JSONObject();
            final org.json.JSONObject jsonBody1 = new org.json.JSONObject();
            jsonBody1.put("value", DISTANCIA);
            jsonBody.put("Distancia", jsonBody1);




            JsonObjectRequest request = new JsonObjectRequest
                    (Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            // TODO Auto-generated method stub
                            // hazle un print al object o lo que gustes
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub

                        }
                    });


            requestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onMyLocationButtonClick() {

        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {


    }
}
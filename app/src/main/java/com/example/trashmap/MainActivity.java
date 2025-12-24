package com.example.trashmap;

import android.Manifest.permission;
import android.annotation.SuppressLint;

import com.example.trashmap.AddMarkerByUser.AddNewMarker;
import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.DBClasses.Markers;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.Adapters.GarbageAdapter;
import com.example.trashmap.Encyclopedia.EncyclopediaActivity;
import com.example.trashmap.Helpers.JavaMailAPI;
import com.example.trashmap.Helpers.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;


public class MainActivity extends AppCompatActivity implements
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraIdleListener {


    // Карта
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean permissionDenied = false;
    private GoogleMap map;
    private View locationButton;
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    //private ClusterManager<MyItem> clusterManager;

    // БД
    private DatabaseReference myDataBase;
    private FirebaseAuth myAuth;

    //БД MARKERS:
    private final String MARKER_KEY = Constant.MARKER_KEY;
    List<Markers> markersList;

    //БД TYPES:
    private final String GARBAGE_KEY = Constant.GARBAGE_KEY;
    List<GarbageType> garbageList;
    private GarbageAdapter garbageAdapter;

    // Элементы формы
    ImageButton buttonZoomIn;
    ImageButton buttonZoomOut;
    ImageButton buttonFindMe;
    ImageButton btnAdd;
    Button btnLoad;
    Button btnLoadGarbage;
    BottomNavigationView bottomNavigationView;
    RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // БД
        myDataBase = FirebaseDatabase.getInstance().getReference(MARKER_KEY);

        // Получаем маркеры
        getDataFromDB();
        //getMarkers();

        // Получаем типы отходов
        getGarbageTypes();

        // Карта
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().
                findFragmentById(R.id.mainMap);
        supportMapFragment.getMapAsync(this);


        btnAdd = findViewById(R.id.mainAdd);
        btnLoad = findViewById(R.id.mainLoad);
        btnLoadGarbage = findViewById(R.id.mainLoadGarbageTypes);
        buttonFindMe = findViewById(R.id.mainFindMe);

        bottomNavigationView = findViewById(R.id.mainBottomNavigationMenu);
        bottomNavigationView.setSelectedItemId(R.id.menu_map);



        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            switch (item.getItemId()){
                case R.id.menu_encyclopedia:
                    intent = new Intent(getApplicationContext(), EncyclopediaActivity.class);
                    intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                case R.id.menu_map:
                    return true;
                case R.id.menu_ai_recognition:
                    intent = new Intent(getApplicationContext(), com.example.trashmap.AI.WasteRecognitionActivity.class);
                    intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                case R.id.menu_profile:
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
            }
            return false;
        });

        recyclerView = findViewById(R.id.mainRecyclerView);

        GarbageAdapter.OnGarbageTypeClickListener onGarbageTypeClickListener = new GarbageAdapter.OnGarbageTypeClickListener() {
            @Override
            public void onGarbageClick(GarbageType garbageType, int position) {
                map.clear();
                if(garbageType.idType == 1000){
                    preAddMarkers(markersList);
                } else{
                    List<Markers> markList = new ArrayList<>();
                    for (Markers marker : markersList){
                        if(marker.typeGarbage == garbageType.idType){
                            markList.add(marker);
                        }
                    }
                    preAddMarkers(markList);
                }

            }
        };

        garbageAdapter = new GarbageAdapter(garbageList, onGarbageTypeClickListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setOrientation(linearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        // Отключаем бесконечную прокрутку
        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        recyclerView.setAdapter(garbageAdapter);
        garbageAdapter.notifyDataSetChanged();


        myAuth = FirebaseAuth.getInstance();

        //preAddMarkers(markersList);


        //Увеличить карту
        buttonZoomIn = findViewById(R.id.mainPlus);
        buttonZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                map.animateCamera(CameraUpdateFactory.zoomIn());
                //checkMail(Utils.Email, Utils.Password);
            }

        });

        //Уменьшить карту
        buttonZoomOut = findViewById(R.id.mainMinus);
        buttonZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                map.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        buttonFindMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(map != null)
                {
                    if(locationButton != null)
                        locationButton.callOnClick();

                }
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                intent = new Intent(MainActivity.this, AddNewMarker.class);
                startActivity(intent);
            }
        });

        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                preAddMarkers(markersList);
            }
        });

        btnLoadGarbage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setGarbageTypes();
            }
        });
    }

    /**
     * Добавляем маркеры на карту
     */
    private void preAddMarkers(List<Markers> markList){
        for (Markers marker : markList){
            if (marker.typeGarbage == 0){
                addMarkers(marker, R.drawable.types_icon_household);
            }
            else if (marker.typeGarbage == 1){
                addMarkers(marker, R.drawable.types_icon_plastic);
            }
            else if (marker.typeGarbage == 2){
                addMarkers(marker, R.drawable.types_icon_glass);
            }
            else if (marker.typeGarbage == 3){
                addMarkers(marker, R.drawable.types_icon_metal);
            }
            else if (marker.typeGarbage == 4){
                addMarkers(marker, R.drawable.types_icon_battery);
            }
            else if (marker.typeGarbage == 5){
                addMarkers(marker, R.drawable.types_icon_paper);
            }
            else if (marker.typeGarbage == 6){
                addMarkers(marker, R.drawable.types_icon_wood);
            }
            else if (marker.typeGarbage == 7){
                addMarkers(marker, R.drawable.types_icon_technology);
            }
            else if (marker.typeGarbage == 8){
                addMarkers(marker, R.drawable.types_icon_neft);
            }
            else if (marker.typeGarbage == 9){
                addMarkers(marker, R.drawable.types_icon_oil);
            }
            else if (marker.typeGarbage == 10){
                addMarkers(marker, R.drawable.types_icon_clothes);
            }
        }
    }


    private void addMarkers(Markers marker, int draw){
        BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(draw);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 80, 80, false);
        map.addMarker(new MarkerOptions()
                .position(new LatLng(Double.parseDouble(marker.lat), Double.parseDouble(marker.lng)))
                .title(marker.idMarker).icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
    }


    //TODO: Автоматизировать.
    /**
     * Функция для создания/обновления типов отходов в БД
     * Обновляет типы отходов в Firebase на основе массива GarbageCategory из strings.xml
     */
    private void setGarbageTypes(){

        //TODO: Доработать добавление картинок (сейчас приходится делать руками в БД)
        myDataBase = FirebaseDatabase.getInstance().getReference(GARBAGE_KEY);

        List<GarbageType> garbageTypes = new ArrayList<>();
        int counter = 0;

        String[] types;

        types=getResources().getStringArray(R.array.GarbageCategory);

        for (String type : types){
            garbageTypes.add(new GarbageType(counter, type, Constant.GARBAGE_IMG_ALL));
            counter++;
        }
        
        // Записываем все типы в БД
        for (GarbageType gT : garbageTypes) {
            myDataBase.child(String.valueOf(gT.idType)).setValue(gT);
        }
        
        // Показываем уведомление об успешном обновлении
        Toast.makeText(MainActivity.this, 
                "Типы отходов обновлены! Загружено типов: " + garbageTypes.size(), 
                Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser cUser = myAuth.getCurrentUser();

        if(cUser == null) {
            finish();
            Toast.makeText(MainActivity.this, "Сначала войдите в приложение", Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);
        map.setOnMyLocationClickListener(this);
        map.setOnMarkerClickListener(this);

        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);
        //map.setMyLocationEnabled(true);
        enableMyLocation();

        View mapView = supportMapFragment.getView();
        // Get the button view
        locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

        if(locationButton != null)
            locationButton.setVisibility(View.GONE);
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            client = LocationServices.getFusedLocationProviderClient(this);
            Task<Location> task = client.getLastLocation();
            getMyLocation(task);
            return;
        }

        PermissionUtils.requestLocationPermissions(this, LOCATION_PERMISSION_REQUEST_CODE, true);
    }

    public void getMyLocation(Task<Location> task){
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 20));
        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 20));
        //Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION) || PermissionUtils
                .isPermissionGranted(permissions, grantResults,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
            enableMyLocation();
        } else {
            permissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            permissionDenied = false;
        }
    }

    @Override
    public void onCameraIdle() {

        if (map.getCameraPosition().zoom == map.getMinZoomLevel()){
            //при минимальном зуме, делаем неактивной кнопку минус
            buttonZoomOut.setEnabled(false);
            buttonZoomIn.setEnabled(true);
        }else if (map.getCameraPosition().zoom == map.getMaxZoomLevel()){
            //при максимальном зуме, делаем неактивной кнопку плюс
            buttonZoomOut.setEnabled(true);
            buttonZoomIn.setEnabled(false);
        }else {
            //во всех остальных случаях обе кнопки активны
            buttonZoomOut.setEnabled(true);
            buttonZoomIn.setEnabled(true);
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    private void getDataFromDB() {
        markersList = new ArrayList<Markers>();
        myDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Markers value = ds.getValue(Markers.class);
                    assert value != null;
                    markersList.add(value);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Нажатие по маркеру
     * @param marker - маркер
     * @return
     */
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        marker.hideInfoWindow();
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(
                MainActivity.this, R.style.BottomSheetDialogTheme
        );
        View bottomSheetView = LayoutInflater.from(getApplicationContext()).inflate(
                R.layout.info_marker_bottom_sheet,
                (LinearLayout) findViewById(R.id.bottomSheetContainer)
        );

        // Инициализируем
        TextView imbs_title = bottomSheetView.findViewById(R.id.imbs_name);
        TextView imbs_type = bottomSheetView.findViewById(R.id.imbs_type);
        TextView imbs_date = bottomSheetView.findViewById(R.id.imbs_date);
        TextView imbs_address = bottomSheetView.findViewById(R.id.imbs_address);
        TextView imbs_latlng = bottomSheetView.findViewById(R.id.imbs_latlng);
        ImageView imbs_img = bottomSheetView.findViewById(R.id.imbs_img);
        Button imbs_error = bottomSheetView.findViewById(R.id.imbs_error);
        ImageButton imbs_nav = bottomSheetView.findViewById(R.id.imbs_navigation);

        // Получаем маркеры
        Markers markerPicked = new Markers();
        for (Markers markerItem : markersList){
            if(marker.getTitle().equals(markerItem.idMarker)){
                markerPicked = markerItem;
                break;
            }
        }

        // TEMP переменные
        String typeGT = "";
        String latLng = String.format("%.4f", Float.valueOf(markerPicked.lat)) + "; " + String.format("%.4f", Float.valueOf(markerPicked.lng));

        // Получаем типы мусора
        for (GarbageType gT : garbageList){
            if(markerPicked.typeGarbage == gT.idType)
                typeGT = gT.nameType;
        }

        // Отрисовываем
        imbs_address.setText(markerPicked.address);
        imbs_date.setText(markerPicked.dateAdd);
        imbs_title.setText(markerPicked.name);
        imbs_type.setText(typeGT);
        imbs_latlng.setText(latLng);
        Picasso.get().load(markerPicked.imgUri).into(imbs_img);

        // Кнопка "Сообщить об ошибке"
        imbs_error.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendErrorMark(marker);
            }
        });

        imbs_nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationRoute(marker);
            }
        });

        imbs_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage(imbs_img);
            }
        });



        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
        return false;
    }

    /**
     * Строим маршрут, переход на Google Maps
     * @param marker
     */
    private void navigationRoute(Marker marker){
        String latitude = String.valueOf(marker.getPosition().latitude);
        String longitude = String.valueOf(marker.getPosition().longitude);
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try{
            if (mapIntent.resolveActivity(Objects.requireNonNull(MainActivity.this).getPackageManager()) != null) {
                startActivity(mapIntent);
            }
        }catch (NullPointerException e){
            Toast.makeText(MainActivity.this, "Не могу открыть приложение Google Maps", Toast.LENGTH_SHORT).show();
        }
    }





    private void openImage(ImageView imageView){
        // AlertDialog
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.alert_dialog_open_image, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        AlertDialog alertDialog = builder.create();

        ImageView imgView = dialoglayout.findViewById(R.id.adoi_img);
        imgView.setImageDrawable(imageView.getDrawable());

        alertDialog.setView(dialoglayout);
        alertDialog.show();
    }

    /**
     * Отправляем сообщение на почту
     * @param marker
     */
    private void sendErrorMark(Marker marker){
        // БД
        FirebaseUser cUser = myAuth.getCurrentUser();

        // AlertDialog
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.alert_dialog_error_message, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        AlertDialog alertDialog = builder.create();

        // Объекты AlertDialog
        Button btnSend = dialoglayout.findViewById(R.id.adem_send);
        Button btnCancel = dialoglayout.findViewById(R.id.adem_cancel);
        EditText errorDesc = dialoglayout.findViewById(R.id.adem_desc);
        Spinner errorType = dialoglayout.findViewById(R.id.adem_type);

        // Кнопка "Отправить"
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String subject = "Сообщение об ошибке! Имя: " + marker.getTitle();
                String description = "Пользователь " + cUser.getEmail().toString() + " сообщает об " +
                        "ошибке.\n\nТочка: " + marker.getTitle() + "\nКоординаты: " +
                        marker.getPosition().latitude + "; " + marker.getPosition().longitude +
                        "\n\nТип ошибки: " + errorType.getSelectedItem().toString() +
                        "\nКомментарий пользователя: " + errorDesc.getText().toString();
                JavaMailAPI javaMailAPI = new JavaMailAPI(MainActivity.this, Utils.EMAIL, subject, description);

                javaMailAPI.execute();
                alertDialog.cancel();
                //Toast.makeText(MainActivity.this, "Сообщить об ошибке!", Toast.LENGTH_SHORT).show();
            }
        });

        // Кнопка "Отмена"
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.cancel();
            }
        });



        alertDialog.setView(dialoglayout);
        alertDialog.show();
    }

    /**
     * Переходим на фрагмент
      */

    void SelectFragment(Fragment newFragment){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.mainFragment, newFragment);
        ft.commit();
    }

    /**
     * Получаем типы отходов с OpenWindow activity
     */
    private void getGarbageTypes(){
        //garbageList.clear();
        Intent i = getIntent();

        garbageList = (List<GarbageType>) i.getSerializableExtra(Constant.GARBAGE_KEY);
    }

    /*private void getMarkers(){
        //garbageList.clear();
        Intent i = getIntent();

        markersList = (List<Markers>) i.getSerializableExtra(Constant.MARKER_KEY);
    }*/



     /*private void setUpClusterer() {
        // Position the map.
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.503186, -0.126446), 10));

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        clusterManager = new ClusterManager<MyItem>(MainActivity.this, map);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);

        // Add cluster items (markers) to the cluster manager.
        addItems();
    }

    private void addItems() {

        // Set some lat/lng coordinates to start with.
        double lat = 51.5145160;
        double lng = -0.1270060;

        // Add ten cluster items in close proximity, for purposes of this example.
        for (int i = 0; i < 10; i++) {
            double offset = i / 60d;
            lat = lat + offset;
            lng = lng + offset;
            MyItem offsetItem = new MyItem(lat, lng, "Title " + i, "Snippet " + i);
            clusterManager.addItem(offsetItem);
        }
    }*/

}
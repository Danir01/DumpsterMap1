package com.example.trashmap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trashmap.AddMarkerByUser.AddNewMarker;
import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.DBClasses.Markers;
import com.example.trashmap.DBClasses.Messages;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.Helpers.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.SearchTerm;

public class InfoMessageActivity extends AppCompatActivity implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnCameraIdleListener{

    // МОЙ КЛАСС
    Messages messages;
    // JAVAMAIL класс
    Message msg;

    List<GarbageType> garbageList;

    // 0 - ошибки, 1 - добавить
    int msgType;

    Markers marker;

    private GoogleMap map;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean permissionDenied = false;
    FusedLocationProviderClient client;
    private View locationButton;
    SupportMapFragment supportMapFragment;

    private StorageReference myStorageReference;
    private DatabaseReference myDataBase;
    private final String MARKER_KEY = Constant.MARKER_KEY;

    TextView text;
    TextView subject;
    ImageView img;
    Button accept;
    Button decline;
    Button cancel;

    String addID;
    String addName;
    String addLat;
    String addLng;
    String addAddress;
    String addType;
    String addDate;
    String addImg;
    Uri uploadUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_message);

        getData();
        init();

        if (msgType == 1){
            parserAdd();
            accept.setText("Добавить");
            Picasso.get().load(addImg).into(img);
        } else if (msgType == 0){
            accept.setText("Удалить");
            Picasso.get().load(marker.imgUri).into(img);
        }

        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().
                findFragmentById(R.id.aim_map);
        supportMapFragment.getMapAsync(this);

        text.setText(messages.getContent().toString());
        subject.setText(messages.getSubject());


        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage(img);
            }
        });

        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (msgType == 0){
                    //TODO: Существует баг. При создании письма, а затем при отправке письма в данной форме точка УДАЛЯЕТСЯ,
                    //TODO: но письмо не перенаправляется в другую папку.
                    deleteMarkerFromDB(marker);
                    searchEmail("imap.gmail.com", "993", Utils.EMAIL, Utils.PASSWORD, messages.getSubject(), "Ошибки", "Ошибки.Принято");
                    Toast.makeText(InfoMessageActivity.this, "Точка удалена", Toast.LENGTH_SHORT).show();
                    gotoProfile();
                }
                else if (msgType == 1){
                    uploadImage();
                    searchEmail("imap.gmail.com", "993", Utils.EMAIL, Utils.PASSWORD, messages.getSubject(), "Добавление", "Доб.Принято");
                    gotoProfile();
                }
            }
        });

        decline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (msgType == 0){
                    searchEmail("imap.gmail.com", "993", Utils.EMAIL, Utils.PASSWORD, messages.getSubject(), "Ошибки", "Ошибки.Отказано");
                    gotoProfile();
                }
                else if (msgType == 1){
                    searchEmail("imap.gmail.com", "993", Utils.EMAIL, Utils.PASSWORD, messages.getSubject(), "Добавление", "Доб.Отказано");
                    gotoProfile();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


    }

    /**
     * Переход в профиль
     */
    private void gotoProfile(){
        Intent intent;
        intent = new Intent(getApplicationContext(), ProfileActivity.class);
        intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }


    private void deleteMarkerFromDB(Markers markerDelete){
        Toast.makeText(InfoMessageActivity.this, "Операция обрабатывается, подождите", Toast.LENGTH_SHORT).show();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Markers").child(markerDelete.idMarker);

        Task<Void> mTask = ref.removeValue();
        mTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(InfoMessageActivity.this, "Точка удалена", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(InfoMessageActivity.this, "Ошибка: Точка НЕ удалена", Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * Ищем письма в почте
     * @param host
     * @param port
     * @param userName
     * @param password
     * @param keyword
     * @param inboxFolder
     * @param toFolder
     */
    private void searchEmail(String host, String port, String userName,
                             String password, final String keyword, String inboxFolder, String toFolder){
        Properties properties = new Properties();

        // server setting
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", port);

        // SSL setting
        properties.setProperty("mail.imap.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.imap.socketFactory.fallback", "false");
        properties.setProperty("mail.imap.socketFactory.port",
                String.valueOf(port));

        Session session = Session.getDefaultInstance(properties);

        try {
            // connects to the message store
            Store store = session.getStore("imap");
            store.connect(userName, password);
            // opens the inbox folder
            Folder folderInbox = store.getFolder(inboxFolder);
            Folder outputFolder = store.getFolder(toFolder);
            folderInbox.open(Folder.READ_WRITE);


            // creates a search criterion
            SearchTerm searchCondition = new SearchTerm() {
                @Override
                public boolean match(Message message) {
                    try {
                        if (message.getSubject().contains(keyword)) {
                            return true;
                        }
                    } catch (MessagingException ex) {
                        ex.printStackTrace();
                    }
                    return false;
                }
            };

            // performs search through the folder
            Message[] foundMessages = folderInbox.search(searchCondition);

            for (Message foundMessage : foundMessages) {
                msg = foundMessage;
            }

            moveMessage(msg, outputFolder);

            // disconnect
            folderInbox.close(false);

            store.close();
        } catch (NoSuchProviderException ex) {
            System.out.println("No provider.");
            ex.printStackTrace();
        } catch (MessagingException ex) {
            System.out.println("Could not connect to the message store.");
            ex.printStackTrace();
        }
    }

    /**
     * Перемещаем письма из 1 папки в другую
     * @param m
     * @param to
     * @throws MessagingException
     */
    public void moveMessage(Message m, Folder to) throws MessagingException
    {
        m.getFolder().copyMessages(new Message[] {m}, to);
        m.setFlag(Flags.Flag.DELETED, true);
        m.getFolder().expunge();
    }

    /**
     * Открываем картинку
     * @param imageView
     */
    private void openImage(ImageView imageView){
        // AlertDialog
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.alert_dialog_open_image, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(InfoMessageActivity.this);

        AlertDialog alertDialog = builder.create();

        ImageView imgView = dialoglayout.findViewById(R.id.adoi_img);
        imgView.setImageDrawable(imageView.getDrawable());

        alertDialog.setView(dialoglayout);
        alertDialog.show();
    }

    /**
     * Добавляем точку
     */
    private void preAddMarkers(LatLng latLng, long type){

        if (type == 0){
            addMarkers(latLng, R.drawable.types_icon_household);
        }
        else if (type == 1){
            addMarkers(latLng, R.drawable.types_icon_plastic);
        }
        else if (type == 2){
            addMarkers(latLng, R.drawable.types_icon_glass);
        }
        else if (type == 3){
            addMarkers(latLng, R.drawable.types_icon_metal);
        }
        else if (type == 4){
            addMarkers(latLng, R.drawable.types_icon_battery);
        }
        else if (type == 5){
            addMarkers(latLng, R.drawable.types_icon_paper);
        }
        else if (type == 6){
            addMarkers(latLng, R.drawable.types_icon_wood);
        }
        else if (type == 7){
            addMarkers(latLng, R.drawable.types_icon_technology);
        }
        else if (type == 8){
            addMarkers(latLng, R.drawable.types_icon_neft);
        }
        else if (type == 9){
            addMarkers(latLng, R.drawable.types_icon_oil);
        }
        else if (type == 10){
            addMarkers(latLng, R.drawable.types_icon_clothes);
        }
    }

    /**
     * Добавляем точку
     * @param draw
     */
    private void addMarkers(LatLng latLng, int draw){
        BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(draw);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 60, 60, false);
        map.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    /**
     * Парсим письмо типа "Добавление"
     */
    private void parserAdd(){
        // ID
        addID = messages.getContent().toString().substring(messages.getContent().toString().indexOf("ID: ") + 4, messages.getContent().toString().indexOf("Name: ")-2);
        // Name
        addName = messages.getContent().toString().substring(messages.getContent().toString().indexOf("Name: ") + 6, messages.getContent().toString().indexOf("Lat: ")-2);
        // Lat
        addLat = messages.getContent().toString().substring(messages.getContent().toString().indexOf("Lat: ") + 5, messages.getContent().toString().indexOf("Lng: ")-2);
        // Lng
        addLng = messages.getContent().toString().substring(messages.getContent().toString().indexOf("Lng: ") + 5, messages.getContent().toString().indexOf("Address: ")-2);
        // Address
        addAddress = messages.getContent().toString().substring(messages.getContent().toString().indexOf("Address: ") + 9, messages.getContent().toString().indexOf("Type: ")-2);
        // Type
        addType = messages.getContent().toString().substring(messages.getContent().toString().indexOf("Type: ") + 6, messages.getContent().toString().indexOf("Date: ")-2);
        // Date
        addDate = messages.getContent().toString().substring(messages.getContent().toString().indexOf("Date: ") + 6, messages.getContent().toString().indexOf("ImgUri: ")-2);
        // Img
        addImg = messages.getContent().toString().substring(messages.getContent().toString().indexOf("ImgUri: ") + 8);
    }

    private void init(){
        text = findViewById(R.id.aim_text);
        subject = findViewById(R.id.aim_subject);
        img = findViewById(R.id.aim_img);
        accept = findViewById(R.id.aim_accept);
        decline = findViewById(R.id.aim_decline);
        cancel = findViewById(R.id.aim_cancel);
        myDataBase = FirebaseDatabase.getInstance().getReference(MARKER_KEY);
    }

    private void getData(){
        Intent i = getIntent();
        messages = (Messages) i.getSerializableExtra(Constant.MESSAGE_ACTIVITY);
        garbageList = (List<GarbageType>) i.getSerializableExtra(Constant.GARBAGE_KEY);
        msgType = i.getIntExtra(Constant.MESSAGE_TYPE,0);
        marker = (Markers) i.getSerializableExtra(Constant.FRAGMENT_INFO_MARKER);
    }

    /**
     * Загрузка фотографии в Firebase Storage
     */
    private void uploadImage(){
        myStorageReference = FirebaseStorage.getInstance().getReference("ImageDB");
        Toast.makeText(InfoMessageActivity.this, "Операция обрабатывается, подождите", Toast.LENGTH_SHORT).show();

        Bitmap bitmap = ((BitmapDrawable)img.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteArray = baos.toByteArray();
        final StorageReference childStorageReference = myStorageReference.child(System.currentTimeMillis() + "marker");
        UploadTask up = childStorageReference.putBytes(byteArray);
        Task<Uri> task = up.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                return childStorageReference.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                uploadUri = task.getResult();
                saveMarker();
            }
        });
    }

    /**
     * Сохраняем точку
     */
    private void saveMarker(){
        // Создаем объект класса
        Markers markerNew = new Markers(addID, addName, Long.parseLong(addType), addLat, addLng, addAddress, addDate, uploadUri.toString());

        addMarkerInDB(addID, addName, addLat, addLng, markerNew);
    }

    private void addMarkerInDB(String idMarker, String nameMarker, String lat, String lng, Markers markers){
        if (!TextUtils.isEmpty(nameMarker) && !TextUtils.isEmpty(lat) && !TextUtils.isEmpty(lng)){
            if (idMarker!=null){
                myDataBase.child(idMarker).setValue(markers);
                Toast.makeText(InfoMessageActivity.this, "Точка добавлена", Toast.LENGTH_SHORT).show();
            }
        } else{
            Toast.makeText(InfoMessageActivity.this, "Ошибка, уточните введенные данные", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCameraIdle() {

    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);
        map.setOnMyLocationClickListener(this);
        //map.setMyLocationEnabled(true);
        enableMyLocation();

        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);

        View mapView = supportMapFragment.getView();
        // Get the button view
        locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

        if(locationButton != null)
            locationButton.setVisibility(View.GONE);

        if (msgType==1){
            LatLng latLng = new LatLng(Double.parseDouble(addLat), Double.parseDouble(addLng));
            preAddMarkers(latLng, Long.parseLong(addType));
        }else if (msgType==0){
            LatLng latLng = new LatLng(Double.parseDouble(marker.lat), Double.parseDouble(marker.lng));
            preAddMarkers(latLng, marker.typeGarbage);
        }
    }

    /**
     * Включаем локацию пользователя
     */
    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            /*client = LocationServices.getFusedLocationProviderClient(this);
            Task<Location> task = client.getLastLocation();
            getMyLocation(task);*/
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
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                        }
                    });
                }
            }
        });
    }

    /**
     * Даём разрешение
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
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

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }
}
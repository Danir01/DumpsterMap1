package com.example.trashmap.AddMarkerByUser;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.DBClasses.Markers;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.Helpers.JavaMailAPI;
import com.example.trashmap.Helpers.Utils;
import com.example.trashmap.MainActivity;
import com.example.trashmap.PermissionUtils;
import com.example.trashmap.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class AddNewMarker extends AppCompatActivity implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnCameraIdleListener{

    private GoogleMap map;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean permissionDenied = false;
    FusedLocationProviderClient client;
    private View locationButton;
    SupportMapFragment supportMapFragment;

    private FirebaseAuth myAuth;
    private FirebaseUser user;

    //MARKERS:
    private StorageReference myStorageReference;
    private DatabaseReference myDataBase;
    private final String MARKER_KEY = Constant.MARKER_KEY;

    //TYPES:
    private final String GARBAGE_KEY = Constant.GARBAGE_KEY;
    List<GarbageType> garbageTypes;
    long typeLong;

    //Pictures:
    static final int REQUEST_IMAGE_CAPTURE = 102;

    Button save;
    Button close;
    ImageButton plus;
    ImageButton minus;
    ImageButton me;

    TextView coordinates;
    TextView name;
    Spinner type;
    ImageView img;
    Uri uploadUri;

    String latitude;
    String longitude;
    String address;



    public interface MyCallback {
        void onCallback(long value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_marker);

        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().
                findFragmentById(R.id.mark_map);
        supportMapFragment.getMapAsync(this);

        // Инициализация
        init();

        // Ставим фотку в форме
        ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        img.setImageURI(uri);
                    }
                });

        // Открываем проводник при нажатии на фотку
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAlertDialog(mGetContent);
            }
        });

        // Выпадающий список типов мусора
        ArrayAdapter<?> adapter = null;
        adapter = ArrayAdapter.createFromResource(this, R.array.GarbageCategory,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        type.setAdapter(adapter);

        // Обработка нажатия кнопки "Сохранить"
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(map != null)
                {
                    if(locationButton != null)
                        locationButton.callOnClick();
                }
            }
        });
    }

    /**
     * Инициализация формы
     */
    private void init(){
        // Объекты на форме
        close = findViewById(R.id.mark_cancel);
        save = findViewById(R.id.mark_save);
        coordinates = findViewById(R.id.mark_coordinates);
        name = findViewById(R.id.mark_name);
        type = findViewById(R.id.mark_type);
        img = findViewById(R.id.mark_photo_garbage);
        plus = findViewById(R.id.mark_plus);
        minus = findViewById(R.id.mark_minus);
        me = findViewById(R.id.mark_me);

        myAuth = FirebaseAuth.getInstance();

        user = myAuth.getCurrentUser();

        if(user.getEmail().toString().equals("qurst13@gmail.com")){
            save.setText("Сохранить");
        }
        else {
            save.setText("Отправить");
        }

        // БД
        // Это для прямой загрузки в БД
        //myStorageReference = FirebaseStorage.getInstance().getReference("ImageDB");
        // Это для модерации
        //myStorageReference = FirebaseStorage.getInstance().getReference("Moderation");
    }


    private void openAlertDialog(ActivityResultLauncher<String> mGetContent){
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.alert_dialog_camera_folders, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(AddNewMarker.this);

        AlertDialog alertDialog = builder.create();

        LinearLayout camera = dialoglayout.findViewById(R.id.adcf_camera);
        LinearLayout folder = dialoglayout.findViewById(R.id.adcf_folder);

        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGetContent.launch("image/*");
                alertDialog.cancel();
            }
        });

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(AddNewMarker.this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(AddNewMarker.this,
                            new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
                }

                dispatchTakePictureIntent();

                alertDialog.cancel();
            }
        });

        alertDialog.setView(dialoglayout);
        alertDialog.show();
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "GM_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }



    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE_CAPTURE){
            try{
                galleryAddPic();
                setPic();
            }catch (Exception ex){
                }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = img.getWidth();
        int targetH = img.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.max(1, Math.min(photoW/targetW, photoH/targetH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        img.setImageBitmap(bitmap);

    }

    /**
     * Сохраняем точку
     */
    private void saveMarker(){

        getGarbageType(new MyCallback() {
            @Override
            public void onCallback(long value) {
                // Получаем текущую дату
                Calendar calendar = Calendar.getInstance();

                String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
                String month = (getMonth(calendar));
                String year = (String.valueOf(calendar.get(Calendar.YEAR)));
                typeLong = value;

                myDataBase = FirebaseDatabase.getInstance().getReference(MARKER_KEY);

                // Определяем параметры
                String idMarker=myDataBase.push().getKey();
                String nameMarker = name.getText().toString();
                String lat = latitude;
                String lng = longitude;
                String addr = address;
                String dateAdd = day+"."+month+"."+year;

                // Создаем объект класса
                Markers markers = new Markers(idMarker, nameMarker, typeLong, lat, lng, addr, dateAdd, uploadUri.toString());


                // Добавляем элемент в БД
                if(user.getEmail().toString().equals("qurst13@gmail.com")){
                    addMarkerInDB(idMarker, nameMarker, lat, lng, markers);
                }
                else {
                    sendMarkerToEmail(idMarker, nameMarker, lat, lng, markers);
                }
            }
        });
    }

    private void addMarkerInDB(String idMarker, String nameMarker, String lat, String lng, Markers markers){
        if (!TextUtils.isEmpty(nameMarker) && !TextUtils.isEmpty(lat) && !TextUtils.isEmpty(lng)){
            if (idMarker!=null){
                myDataBase.child(idMarker).setValue(markers);
                Toast.makeText(AddNewMarker.this, "Точка добавлена", Toast.LENGTH_SHORT).show();
            }
        } else{
            Toast.makeText(AddNewMarker.this, "Ошибка, уточните введенные данные", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMarkerToEmail(String idMarker, String nameMarker, String lat, String lng, Markers marker){

        if (!TextUtils.isEmpty(nameMarker) && !TextUtils.isEmpty(lat) && !TextUtils.isEmpty(lng)){
            if (idMarker!=null){
                String subject = "Добавление! Имя: " + marker.name;
                String description = "Пользователь " + user.getEmail().toString() + " просит добавить " +
                        "новую точку." + "\n\nID: " + marker.idMarker + "\nName: " + marker.name + "\n" +
                        "Lat: "+ marker.lat + "\nLng: " + marker.lng + "\nAddress: " + marker.address + "\n" +
                        "Type: " +marker.typeGarbage + "\nDate: " + marker.dateAdd + "\nImgUri: " + marker.imgUri;
                JavaMailAPI javaMailAPI = new JavaMailAPI(AddNewMarker.this, Utils.EMAIL, subject, description);

                javaMailAPI.execute();

            }
        } else{
            Toast.makeText(AddNewMarker.this, "Ошибка, уточните введенные данные", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Получаем тип отходов из справочника
     * @return ID типа отходов
     */
    private void getGarbageType(MyCallback myCallback){
        myDataBase = FirebaseDatabase.getInstance().getReference(GARBAGE_KEY);
        garbageTypes = new ArrayList<GarbageType>();
        myDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    GarbageType value = ds.getValue(GarbageType.class);

                    assert value != null;
                    garbageTypes.add(value);

                }
                for (GarbageType gT : garbageTypes){
                    if(gT.nameType.equals(type.getSelectedItem().toString())){
                        myCallback.onCallback(gT.idType);
                        //AddNewMarker.typeLong = gT.idType;
                        //Toast.makeText(AddNewMarker.this, String.valueOf(typeLong), Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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
    }

    @Override
    public void onCameraIdle() {
        if (map.getCameraPosition().zoom == map.getMinZoomLevel()){
            //при минимальном зуме, делаем неактивной кнопку минус
            minus.setEnabled(false);
            plus.setEnabled(true);
        }else if (map.getCameraPosition().zoom == map.getMaxZoomLevel()){
            //при максимальном зуме, делаем неактивной кнопку плюс
            minus.setEnabled(true);
            plus.setEnabled(false);
        }else {
            //во всех остальных случаях обе кнопки активны
            minus.setEnabled(true);
            plus.setEnabled(true);
        }
    }

    /**
     * Загрузка фотографии в Firebase Storage
     */
    private void uploadImage(){

        Toast.makeText(AddNewMarker.this, "Операция обрабатывается, подождите", Toast.LENGTH_SHORT).show();
        if(user.getEmail().toString().equals("qurst13@gmail.com")){
            myStorageReference = FirebaseStorage.getInstance().getReference("ImageDB");
        }
        else {
            myStorageReference = FirebaseStorage.getInstance().getReference("Moderation");
        }

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
     * Запускаем мини-карту
     * @param googleMap
     */
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

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng point) {
                map.clear();
                map.addMarker(new MarkerOptions().position(point));

                Locale ruLocale = new Locale("ru");

                Geocoder geocoder = new Geocoder(AddNewMarker.this, ruLocale);
                try {
                    List<Address> addressList = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                    address = addressList.get(0).getAddressLine(0);
                    String stringMarker = "Координаты: " + address;
                    coordinates.setText(stringMarker);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //String stringMarker = "Координаты: " + point.latitude + ", " + point.longitude;
                latitude = String.valueOf(point.latitude);
                longitude = String.valueOf(point.longitude);
                //coordinates.setText(stringMarker);
            }
        });
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



    /**
     * Получаем месяц
     * @param calendar
     * @return месяц
     */
    private String getMonth(Calendar calendar){
        String tempReturn = "";
        int month = Integer.parseInt(String.valueOf(calendar.get(Calendar.MONTH)));
        if(month < 9){
            month++;
            tempReturn="0" + String.valueOf(month);
        } else if (month > 8 && month < 12) {
            tempReturn=String.valueOf(month++);
        }
        return tempReturn;
    }
}
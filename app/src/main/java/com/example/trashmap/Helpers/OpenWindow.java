package com.example.trashmap.Helpers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Toast;

import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.DBClasses.Markers;
import com.example.trashmap.MainActivity;
import com.example.trashmap.R;
import com.example.trashmap.Authorization.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OpenWindow extends AppCompatActivity {

    private FirebaseAuth myAuth;
    private DatabaseReference myDataBase;

    //MARKERS:
    private final String MARKER_KEY = Constant.MARKER_KEY;
    ArrayList<Markers> markersList = new ArrayList<>();

    //GARBAGE:
    private final String GARBAGE_KEY = Constant.GARBAGE_KEY;
    ArrayList<GarbageType> garbageList = new ArrayList<>();

    public interface MyCallback {
        void onCallback(ArrayList<GarbageType> value);
    }

    public interface MarkersCallback {
        void onCallback(ArrayList<Markers> markersValue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_window);
        init();
    }

    private void getTypesFromDB(MyCallback myCallback){
        myDataBase = FirebaseDatabase.getInstance().getReference(GARBAGE_KEY);
        garbageList = new ArrayList<GarbageType>();
        // тип "ВСЕ"
        garbageList.add(new GarbageType(1000, "Все", Constant.GARBAGE_IMG_ALL));

        myDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    GarbageType value = ds.getValue(GarbageType.class);

                    assert value != null;
                    garbageList.add(value);

                }
                myCallback.onCallback(garbageList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void getMarkersFromDB(MarkersCallback markersCallback) {
        myDataBase = FirebaseDatabase.getInstance().getReference(MARKER_KEY);
        markersList = new ArrayList<Markers>();
        myDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Markers value = ds.getValue(Markers.class);

                    assert value != null;
                    markersList.add(value);
                }
                markersCallback.onCallback(markersList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser cUser = myAuth.getCurrentUser();

        getTypesFromDB(new MyCallback() {
            @Override
            public void onCallback(ArrayList<GarbageType> value) {
                Intent intent;
                if(cUser != null){
                    intent = new Intent(OpenWindow.this, MainActivity.class);
                    intent.putExtra(Constant.GARBAGE_KEY, (Serializable) value);
                }
                else{
                    intent = new Intent(OpenWindow.this, LoginActivity.class);
                }
                startActivity(intent);
                finish();
            }
        });
    }

    private void init(){
        myAuth = FirebaseAuth.getInstance();
        //myDataBase = FirebaseDatabase.getInstance().getReference(MARKER_KEY);
    }

}



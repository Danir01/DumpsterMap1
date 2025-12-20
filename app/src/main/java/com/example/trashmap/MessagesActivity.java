package com.example.trashmap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.trashmap.Adapters.MessagesAdapter;
import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.DBClasses.Markers;
import com.example.trashmap.DBClasses.Messages;
import com.example.trashmap.Encyclopedia.EncyclopediaActivity;
import com.example.trashmap.Helpers.Constant;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    ArrayList<Messages> msgs;
    List<GarbageType> garbageList;
    RecyclerView recyclerView;
    private MessagesAdapter messagesAdapter;
    int msgType;

    private DatabaseReference myDataBase;
    private final String MARKER_KEY = Constant.MARKER_KEY;
    Markers marker;

    interface MessageCallback {
        void onCallback(Markers markerInterface);
    }

    /**
     * Делаем активти для выписывания всех сообщений через recyclerView. По нажатию по письму, октрываем письмо полностью
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        getGarbageTypes();
        getMessages();

        BottomNavigationView bottomNavigationView = findViewById(R.id.messagesBottomNavigationMenu);
        bottomNavigationView.setSelectedItemId(R.id.menu_profile);

        recyclerView = findViewById(R.id.messages_recycler_view);

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
                    intent = new Intent(getApplicationContext(), MainActivity.class);
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

        MessagesAdapter.OnMessagesClickListener onMessagesClickListener = new MessagesAdapter.OnMessagesClickListener() {
            @Override
            public void onGarbageClick(Messages message, int position) {
                marker = new Markers();
                if (msgType == 0){
                    goToErrorMessage(message);
                }else if (msgType == 1){
                    goToAddMessage(message);
                }
            }
        };

        messagesAdapter = new MessagesAdapter(msgs, onMessagesClickListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messagesAdapter);
        messagesAdapter.notifyDataSetChanged();

        //for (Messages msg : msgs) {
        //    Toast.makeText(MessagesActivity.this, msg.getSubject(), Toast.LENGTH_SHORT).show();
        //}
        //Toast.makeText(MessagesActivity.this, String.valueOf(msgType), Toast.LENGTH_SHORT).show();
    }

    private void goToAddMessage(Messages message){
        Intent intent;
        intent = new Intent(getApplicationContext(), InfoMessageActivity.class);
        intent.putExtra(Constant.MESSAGE_ACTIVITY, (Serializable) message);
        intent.putExtra(Constant.MESSAGE_TYPE, (Serializable) msgType);
        intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
        intent.putExtra(Constant.FRAGMENT_INFO_MARKER, (Serializable) marker);
        startActivity(intent);
    }

    //TODO: Нельзя запустить письмо после того, как посмотрел уже одно.
    private void goToErrorMessage(Messages message){
        getMarkerFromDB(new MessageCallback() {
            @Override
            public void onCallback(Markers markerInterface) {
                Intent intent;
                intent = new Intent(getApplicationContext(), InfoMessageActivity.class);
                intent.putExtra(Constant.MESSAGE_ACTIVITY, (Serializable) message);
                intent.putExtra(Constant.MESSAGE_TYPE, (Serializable) msgType);
                intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
                intent.putExtra(Constant.FRAGMENT_INFO_MARKER, (Serializable) markerInterface);
                startActivity(intent);
            }
        }, message);
    }

    private void getMarkerFromDB(MessageCallback messageCallback, Messages message){
        String idMarker = message.getSubject().substring(message.getSubject().indexOf("Имя: ") + 5);
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        myDataBase = FirebaseDatabase.getInstance().getReference(MARKER_KEY);
        myDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot ds : dataSnapshot.getChildren()){
                        marker = ds.getValue(Markers.class);
                        if (marker.idMarker.equals(idMarker)){
                            messageCallback.onCallback(marker);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getMessages(){
        //garbageList.clear();
        Intent i = getIntent();
        //Bundle bundle = getIntent().getExtras();
       // msgs = (ArrayList<Message>)bundle.getSerializable(Constant.MESSAGE_ACTIVITY);

        msgs = (ArrayList<Messages>) i.getSerializableExtra(Constant.MESSAGE_ACTIVITY);
        msgType = i.getIntExtra(Constant.MESSAGE_TYPE,0);

    }

    private void getGarbageTypes(){
        //garbageList.clear();
        Intent i = getIntent();

        garbageList = (List<GarbageType>) i.getSerializableExtra(Constant.GARBAGE_KEY);
    }
}
package com.example.trashmap.Encyclopedia;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.Adapters.EncyclopediaAdapter;
import com.example.trashmap.MainActivity;
import com.example.trashmap.ProfileActivity;
import com.example.trashmap.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EncyclopediaActivity extends AppCompatActivity {

    List<GarbageType> garbageList;
    List<GarbageType> itemList;
    RecyclerView recyclerView;
    private EncyclopediaAdapter encyclopediaAdapter;

    //TODO: Не работает((
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encyclopedia);

        getGarbageTypes();

        BottomNavigationView bottomNavigationView = findViewById(R.id.encyclopediaBottomNavigationMenu);
        bottomNavigationView.setSelectedItemId(R.id.menu_encyclopedia);

        recyclerView = findViewById(R.id.encyclopedia_recycler_view);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            switch (item.getItemId()){
                case R.id.menu_encyclopedia:
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

        EncyclopediaAdapter.OnGarbageTypeClickListener onGarbageTypeClickListener = new EncyclopediaAdapter.OnGarbageTypeClickListener() {
            @Override
            public void onGarbageClick(GarbageType garbageType, int position) {
                Intent intent;
                intent = new Intent(getApplicationContext(), InfoAboutItem.class);
                intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
                intent.putExtra(Constant.GARBAGE_TYPE, garbageType.idType);
                startActivity(intent);
                //Toast.makeText(EncyclopediaActivity.this, String.valueOf(position), Toast.LENGTH_SHORT).show();
            }
        };

        encyclopediaAdapter = new EncyclopediaAdapter(itemList, onGarbageTypeClickListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(encyclopediaAdapter);
        encyclopediaAdapter.notifyDataSetChanged();
    }


    private void getGarbageTypes(){
        //garbageList.clear();
        Intent i = getIntent();

        garbageList = (List<GarbageType>) i.getSerializableExtra(Constant.GARBAGE_KEY);

        itemList = new ArrayList<GarbageType>(garbageList);

        // Это костыль. Удаление типа "ВСЕ"
        itemList.remove(0);
    }
}
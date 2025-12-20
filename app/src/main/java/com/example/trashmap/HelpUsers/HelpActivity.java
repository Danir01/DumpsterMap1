package com.example.trashmap.HelpUsers;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.trashmap.Adapters.HelpAdapter;
import com.example.trashmap.Adapters.MessagesAdapter;
import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.DBClasses.HelpItem;
import com.example.trashmap.DBClasses.Markers;
import com.example.trashmap.DBClasses.Messages;
import com.example.trashmap.Encyclopedia.EncyclopediaActivity;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.MainActivity;
import com.example.trashmap.ProfileActivity;
import com.example.trashmap.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity {

    List<GarbageType> garbageList;
    RecyclerView recyclerView;
    private HelpAdapter helpAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        getGarbageTypes();

        BottomNavigationView bottomNavigationView = findViewById(R.id.helpBottomNavigationMenu);
        bottomNavigationView.setSelectedItemId(R.id.menu_profile);

        recyclerView = findViewById(R.id.help_recycler_view);

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

        HelpAdapter.OnHelpersClickListener onHelpersClickListener = new HelpAdapter.OnHelpersClickListener() {
            @Override
            public void onHelperClick(HelpItem helpItem, int position) {
                Intent intent;
                intent = new Intent(getApplicationContext(), HelpElementActivity.class);
                intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
                intent.putExtra(Constant.STRING_HELP, helpItem.getName());
                startActivity(intent);
            }
        };

        ArrayList<HelpItem> helpItems = new ArrayList<>();

        String[] elements;

        elements=getResources().getStringArray(R.array.HelpElements);

        for(String element : elements){
            helpItems.add(new HelpItem(element));
        }

        helpAdapter = new HelpAdapter(helpItems, onHelpersClickListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(helpAdapter);
        helpAdapter.notifyDataSetChanged();
    }

    private void getGarbageTypes(){
        //garbageList.clear();
        Intent i = getIntent();

        garbageList = (List<GarbageType>) i.getSerializableExtra(Constant.GARBAGE_KEY);
    }
}
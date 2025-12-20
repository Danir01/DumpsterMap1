package com.example.trashmap.HelpUsers;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.Encyclopedia.EncyclopediaActivity;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.MainActivity;
import com.example.trashmap.ProfileActivity;
import com.example.trashmap.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.Serializable;
import java.util.List;

public class HelpElementActivity extends AppCompatActivity {

    List<GarbageType> garbageList;
    String nameHelpElement;

    TextView text;
    TextView title;
    ImageView img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_element);

        getGarbageTypes();
        init();
        setInfo();

        BottomNavigationView bottomNavigationView = findViewById(R.id.helpElementBottomNavigationMenu);
        bottomNavigationView.setSelectedItemId(R.id.menu_profile);

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

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage(img);
            }
        });
    }

    private void setInfo(){
        if(nameHelpElement.equals("Работа с картой")){
            setText(getString(R.string.text_map), R.drawable.help_map);
        }
        else if (nameHelpElement.equals("Добавление новой точки")){
            setText(getString(R.string.text_add_point), R.drawable.help_add_point);;
        }
        else if (nameHelpElement.equals("Энциклопедия")){
            setText(getString(R.string.text_encyclopedia), R.drawable.help_encyclopedia);
        }
        else if (nameHelpElement.equals("Подробная информация о точке")){
            setText(getString(R.string.text_info_point), R.drawable.help_main_info_point);
        }
        else if (nameHelpElement.equals("Меню")){
            setText(getString(R.string.text_menu), R.drawable.help_menu);
        }
    }

    private void setText(String stringText, int image){
        title.setText(nameHelpElement);
        text.setText(stringText);
        img.setImageResource(image);
    }

    private void openImage(ImageView imageView){
        // AlertDialog
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.alert_dialog_open_image, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(HelpElementActivity.this);

        AlertDialog alertDialog = builder.create();

        ImageView imgView = dialoglayout.findViewById(R.id.adoi_img);
        imgView.setImageDrawable(imageView.getDrawable());

        alertDialog.setView(dialoglayout);
        alertDialog.show();
    }

    private void init(){
        text = findViewById(R.id.ahe_text);
        title = findViewById(R.id.ahe_title);
        img = findViewById(R.id.ahe_img);
    }

    private void getGarbageTypes(){
        //garbageList.clear();
        Intent i = getIntent();

        nameHelpElement = i.getStringExtra(Constant.STRING_HELP);
        garbageList = (List<GarbageType>) i.getSerializableExtra(Constant.GARBAGE_KEY);
    }
}
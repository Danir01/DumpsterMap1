package com.example.trashmap.Encyclopedia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trashmap.Authorization.LoginActivity;
import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.DBClasses.SliderItem;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.Adapters.SliderAdapter;
import com.example.trashmap.MainActivity;
import com.example.trashmap.ProfileActivity;
import com.example.trashmap.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InfoAboutItem extends AppCompatActivity {

    List<GarbageType> garbageList;
    ArrayList<SliderItem> sliderItems;
    private long garbageType;
    private DatabaseReference myDataBase;
    private final String SLIDER_ITEM_KEY = Constant.SLIDER_ITEM_KEY;
    private final String INFO_GARBAGE_TYPE = Constant.INFO_GARBAGE_TYPE;
    ViewPager2 viewPager2;
    TextView namePage;
    Button imgNumber;
    TextView mainTextPage;
    String textPage;
    int cont;

    public interface MyCallback {
        void onCallback(ArrayList<SliderItem> value);
    }

    public interface StringCallback{
        void onCallback(String value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_about_item);

        // Берём значения с прошлой активити
        getGarbageTypes();

        //createSliderItem();

        // БД
        myDataBase = FirebaseDatabase.getInstance().getReference(SLIDER_ITEM_KEY);

        viewPager2 = findViewById(R.id.iai_view_pager2);
        namePage = findViewById(R.id.iai_name_page);
        mainTextPage = findViewById(R.id.iai_text);
        imgNumber = findViewById(R.id.iai_number_img);

        BottomNavigationView bottomNavigationView = findViewById(R.id.infoBottomNavigationMenu);
        bottomNavigationView.setSelectedItemId(R.id.menu_encyclopedia);

        setSliderItems();

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

    }



    /**
     * Для просмотра картинок, слайдер + текст из БД
     */
    private void setSliderItems(){
        getSliderItemsFromDb(new MyCallback() {
            @Override
            public void onCallback(ArrayList<SliderItem> value) {
                viewPager2.setAdapter(new SliderAdapter(value, viewPager2));

                viewPager2.setClipToPadding(false);
                viewPager2.setClipChildren(false);
                viewPager2.setOffscreenPageLimit(3);
                // Отключаем бесконечную прокрутку
                if (viewPager2.getChildAt(0) != null) {
                    viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
                }
                // Отключаем прокрутку за пределы списка
                viewPager2.setUserInputEnabled(true);

                CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
                compositePageTransformer.addTransformer(new MarginPageTransformer(40));
                compositePageTransformer.addTransformer(new ViewPager2.PageTransformer() {
                    @Override
                    public void transformPage(@NonNull View page, float position) {
                        float r = 1 - Math.abs(position);
                        page.setScaleY(0.85f + r * 0.15f);
                        page.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                openImage(value.get(cont).getImgUri());
                            }
                        });
                    }
                });

                viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        // Ограничиваем позицию в пределах списка
                        if (position >= 0 && position < value.size()) {
                            cont = position;
                            imgNumber.setText(String.valueOf(cont + 1));
                        } else {
                            // Если позиция выходит за пределы, возвращаемся к началу
                            if (position < 0) {
                                viewPager2.setCurrentItem(0, false);
                            } else if (position >= value.size()) {
                                viewPager2.setCurrentItem(value.size() - 1, false);
                            }
                        }
                    }
                    
                    @Override
                    public void onPageScrollStateChanged(int state) {
                        super.onPageScrollStateChanged(state);
                        // Предотвращаем прокрутку за пределы
                        if (state == ViewPager2.SCROLL_STATE_IDLE) {
                            int currentItem = viewPager2.getCurrentItem();
                            if (currentItem < 0) {
                                viewPager2.setCurrentItem(0, false);
                            } else if (currentItem >= value.size()) {
                                viewPager2.setCurrentItem(value.size() - 1, false);
                            }
                        }
                    }
                });


                /*viewPager2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(InfoAboutItem.this, String.valueOf(viewPager2.getCurrentItem()), Toast.LENGTH_SHORT).show();
                    }
                });*/

                viewPager2.setPageTransformer(compositePageTransformer);
                // Имя страницы
                setName();
            }
        });
        getTextFromDb(new StringCallback() {
            @Override
            public void onCallback(String value) {
                String mainText = value.replace("\\n", "\n").replace("\\t", "\t");
                mainTextPage.setText(mainText);
            }
        });
    }

    private void openImage(String imageUri){

        // AlertDialog
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.alert_dialog_open_image, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(InfoAboutItem.this);

        AlertDialog alertDialog = builder.create();

        ImageView imgView = dialoglayout.findViewById(R.id.adoi_img);
        Picasso.get().load(imageUri).into(imgView);

        alertDialog.setView(dialoglayout);
        alertDialog.show();
    }

    /**
     * Берём фотки из БД
     * @param myCallback
     */
    private void getSliderItemsFromDb(MyCallback myCallback){
        sliderItems = new ArrayList<SliderItem>();
        myDataBase = FirebaseDatabase.getInstance().getReference(SLIDER_ITEM_KEY);
        myDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    SliderItem sliderItem = ds.getValue(SliderItem.class);

                    assert sliderItem != null;
                    if(sliderItem.getIdType() == garbageType){
                        sliderItems.add(sliderItem);
                    }
                }
                myCallback.onCallback(sliderItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Берём текст из БД
     * @param stringCallback
     */
    private void getTextFromDb(StringCallback stringCallback){
        textPage = "";
        myDataBase = FirebaseDatabase.getInstance().getReference(INFO_GARBAGE_TYPE);
        myDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //TODO: Подумать над оптимизацией
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    if(ds.getKey().equals(String.valueOf(garbageType))){
                        for (DataSnapshot dd : ds.getChildren()){
                            if (dd.getKey().equals("Text")){
                                textPage = dd.getValue(String.class);
                            }
                        }
                        break;
                    }
                }
                stringCallback.onCallback(textPage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Определяем имя страницы
     */
    private void setName(){
        for (GarbageType gT : garbageList){
            if(gT.idType == garbageType){
                namePage.setText(gT.nameType);
                break;
            }
        }
    }

    //TODO: Это надо автоматизировать, т.е. сейчас это не очень удобно. Нужно каждый раз компилить
    //TODO: прогу, включая данную функцию, и руками в БД всё делать. Но как автоматизировать - пока хз
    /**
     * Функция создания таблицы в БД, просто как пример
     */
    private void createSliderItem(){
        myDataBase = FirebaseDatabase.getInstance().getReference(SLIDER_ITEM_KEY);
        String idSlider=myDataBase.push().getKey();

        // Делаем пластик как пример
        SliderItem sliderItem = new SliderItem(1,"null");

        myDataBase.child(idSlider).setValue(sliderItem);
    }

    private void getGarbageTypes(){
        //garbageList.clear();
        Intent i = getIntent();

        garbageList = (List<GarbageType>) i.getSerializableExtra(Constant.GARBAGE_KEY);
        garbageType = i.getLongExtra(Constant.GARBAGE_TYPE, 0);
    }
}
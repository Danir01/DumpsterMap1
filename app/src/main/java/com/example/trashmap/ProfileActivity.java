package com.example.trashmap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trashmap.Adapters.HelpAdapter;
import com.example.trashmap.Authorization.LoginActivity;
import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.DBClasses.Markers;
import com.example.trashmap.DBClasses.Messages;
import com.example.trashmap.HelpUsers.HelpActivity;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.Encyclopedia.EncyclopediaActivity;
import com.example.trashmap.Helpers.OpenWindow;
import com.example.trashmap.Helpers.Utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth myAuth;
    private FirebaseUser user;
    List<GarbageType> garbageList;
    ArrayList<Messages> messageList = new ArrayList<>();

    public interface ProfileCallback{
        void onCallback(ArrayList<Messages> value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        BottomNavigationView bottomNavigationView = findViewById(R.id.profileBottomNavigationMenu);
        bottomNavigationView.setSelectedItemId(R.id.menu_profile);

        getGarbageTypes();

        myAuth = FirebaseAuth.getInstance();

        user = myAuth.getCurrentUser();

        TextView profileName = findViewById(R.id.profile_name);

        Button profileExit = findViewById(R.id.profile_exit);
        Button profileChange = findViewById(R.id.profile_change_pass);
        Button profileAddMarkers = findViewById(R.id.profile_admin_add_markers);
        Button profileErrors = findViewById(R.id.profile_admin_errors);
        Button profileHelp = findViewById(R.id.profile_help);

        if(user.getEmail().toString().equals("qurst13@gmail.com")){
            profileAddMarkers.setVisibility(View.VISIBLE);
            profileAddMarkers.setEnabled(true);
            profileErrors.setVisibility(View.VISIBLE);
            profileErrors.setEnabled(true);
            profileName.setText("Администратор: " + user.getEmail());
        }
        else {
            profileAddMarkers.setVisibility(View.GONE);
            profileAddMarkers.setEnabled(false);
            profileErrors.setVisibility(View.GONE);
            profileErrors.setEnabled(false);
            profileName.setText("Вы вошли как: " + user.getEmail());
        }

        profileErrors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkMail(Utils.EMAIL, Utils.PASSWORD, "Ошибки", new ProfileCallback() {
                    @Override
                    public void onCallback(ArrayList<Messages> value) {
                        Intent intent;
                        intent = new Intent(ProfileActivity.this, MessagesActivity.class);
                        intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
                        intent.putExtra(Constant.MESSAGE_ACTIVITY, (Serializable) value);
                        intent.putExtra(Constant.MESSAGE_TYPE, 0); // 0 - ошибки, 1 - добавления
                        startActivity(intent)   ;
                    }
                });
            }
        });

        profileAddMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkMail(Utils.EMAIL, Utils.PASSWORD, "Добавление", new ProfileCallback() {
                    @Override
                    public void onCallback(ArrayList<Messages> value) {
                        Intent intent;
                        intent = new Intent(ProfileActivity.this, MessagesActivity.class);
                        intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
                        intent.putExtra(Constant.MESSAGE_ACTIVITY, (Serializable) value);
                        intent.putExtra(Constant.MESSAGE_TYPE, 1); // 0 - ошибки, 1 - добавления
                        startActivity(intent)   ;
                    }
                });
            }
        });

        profileExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent;
                intent = new Intent(ProfileActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        profileHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                intent = new Intent(ProfileActivity.this, HelpActivity.class);
                intent.putExtra(Constant.GARBAGE_KEY, (Serializable) garbageList);
                startActivity(intent);
            }
        });

        profileChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePassword();
            }
        });

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
                    return true;
            }
            return false;
        });
    }

    private void changePassword(){
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.alert_dialog_change_password, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);

        AlertDialog alertDialog = builder.create();

        // Объекты AlertDialog
        Button btnSend = dialoglayout.findViewById(R.id.adcp_send);
        Button btnCancel = dialoglayout.findViewById(R.id.adcp_cancel);
        EditText oldPass = dialoglayout.findViewById(R.id.adcp_old_email);
        EditText newPass = dialoglayout.findViewById(R.id.adcp_new_email);

        // Кнопка "Отмена"
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.cancel();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(oldPass.getText().toString())){
                    Toast.makeText(ProfileActivity.this, "Поле старого пароля пусто", Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (newPass.getText().toString().equals(oldPass.toString().toString())){
                    Toast.makeText(ProfileActivity.this, "Пароли не должны совпадать", Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (newPass.length() < 5){
                    Toast.makeText(ProfileActivity.this, "Пароль должен быть больше 5 символов", Toast.LENGTH_SHORT).show();
                    return;
                }
                alertDialog.dismiss();
                updatePassword(oldPass.getText().toString(), newPass.getText().toString());
            }
        });

        alertDialog.setView(dialoglayout);
        alertDialog.show();
    }


    private void updatePassword(String oldPass, String newPass){
        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);
        user.reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        user.updatePassword(newPass)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(ProfileActivity.this, "Пароль обновлён", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(ProfileActivity.this, "Ошибка обновления пароля", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileActivity.this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
                        return;
                    }
                });
    }


    /**
     * Берём пиьсма из почты
     * @param username
     * @param password
     */
    public void checkMail(String username, String password, String folder, ProfileCallback profileCallback) {
        messageList = new ArrayList<>();
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect("imap.gmail.com", username, password);
            Folder inbox = store.getFolder(folder);
            inbox.open(Folder.READ_ONLY);
            Message[] msgs = inbox.getMessages();

            for (Message msg : msgs) {
                try {
                    Object content = msg.getContent();
                    if (content instanceof String)
                    {
                        String body = (String)content;
                        Messages messages = new Messages(msg.getSubject(), msg.getSentDate(), body);
                        messageList.add(messages);
                    }
                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
            profileCallback.onCallback(messageList);
            // close folder and store (normally in a finally block)
            inbox.close(false);
            store.close();

        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }

    private void getGarbageTypes(){
        //garbageList.clear();
        Intent i = getIntent();

        garbageList = (List<GarbageType>) i.getSerializableExtra(Constant.GARBAGE_KEY);
    }
}
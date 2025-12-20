package com.example.trashmap.Authorization;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.trashmap.DBClasses.Users;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.Helpers.OpenWindow;
import com.example.trashmap.MainActivity;
import com.example.trashmap.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText regEmail, regPass;
    private Button regSignUp;
    private Button regNext;
    private FirebaseAuth myAuth;

    private DatabaseReference myDataBase;
    private final String USER_KEY = Constant.USER_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        init();


        regSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                regSignUpOnClick();
            }
        });

        regNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                regGoNext();
            }
        });
    }

    private void regGoNext(){
        Intent intent;
        intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void regSignUpOnClick(){
        if(!TextUtils.isEmpty(regEmail.getText().toString()) &&
                !TextUtils.isEmpty(regPass.getText().toString())) {
            myAuth.createUserWithEmailAndPassword(regEmail.getText().toString(), regPass.getText().toString())
                    .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            FirebaseUser user = myAuth.getCurrentUser();
                            if (task.isSuccessful()) {
                                sendLetterOnEmail();

                                assert user != null;
                                if(user.isEmailVerified()){
                                    //setNewUserDB();
                                    Intent intent;
                                    intent = new Intent(RegisterActivity.this, OpenWindow.class);
                                    startActivity(intent);
                                }
                                else{
                                    Toast.makeText(RegisterActivity.this, "Проверьте вашу почту для подтверждения почты", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(RegisterActivity.this, "Не удалось зарегистрироваться", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
        else{
            Toast.makeText(RegisterActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
        }
    }

    /*private void setNewUserDB(){
        myDataBase = FirebaseDatabase.getInstance().getReference(USER_KEY);
        Users user = new Users(regEmail.getText().toString(), regName.getText().toString(), regPass.getText().toString());
        myDataBase.child(regEmail.getText().toString()).setValue(user);
    }*/

    private void sendLetterOnEmail(){
        FirebaseUser user = myAuth.getCurrentUser();

        assert user != null;
        user.sendEmailVerification().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(RegisterActivity.this, "Проверьте вашу почту для подтверждения почты", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(RegisterActivity.this, "Ошибка отправки письма на почту", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    // Проверяем, вошёл ли уже пользователь
    @Override
    protected void onStart() {
        super.onStart();
    }

    private void init(){
        regEmail = findViewById(R.id.registerEmail);
        regPass = findViewById(R.id.registerPass);
        regSignUp = findViewById(R.id.registerSignUp);
        regNext = findViewById(R.id.registerGoNext);
        myAuth = FirebaseAuth.getInstance();
    }
}
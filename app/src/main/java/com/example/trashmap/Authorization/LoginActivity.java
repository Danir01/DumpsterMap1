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

import com.example.trashmap.Helpers.OpenWindow;
import com.example.trashmap.MainActivity;
import com.example.trashmap.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText logEmail, logPass;
    Button signInBtn, signUpBtn, signForgot;
    private FirebaseAuth myAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        init();

        signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUp();
            }
        });

        signForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forgotPassword();
            }
        });
    }

    private void forgotPassword(){
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    private void signUp(){
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        finish();
    }

    private void signIn(){
        if(!TextUtils.isEmpty(logEmail.getText().toString()) &&
                !TextUtils.isEmpty(logPass.getText().toString())) {
            myAuth.signInWithEmailAndPassword(logEmail.getText().toString(), logPass.getText().toString()).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Intent intent;
                        intent = new Intent(LoginActivity.this, OpenWindow.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Войти не удалось", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }
        else {
            Toast.makeText(LoginActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
        }
    }

    // Проверяем, вошёл ли уже пользователь
    @Override
    protected void onStart() {
        super.onStart();
    }

    private void init(){
        logEmail = findViewById(R.id.loginEmail);
        logPass = findViewById(R.id.loginPass);
        signInBtn = (Button) findViewById(R.id.loginSignIn);
        signUpBtn = (Button) findViewById(R.id.loginSignUp);
        signForgot = (Button) findViewById(R.id.loginForgottenPassword);
        myAuth = FirebaseAuth.getInstance();
    }
}
package com.example.trashmap.DBClasses;

public class Users {
    private String email;
    private String password;

    public Users (String Email, String Password){
        this.email = Email;
        this.password = Password;
    }

    public Users(){

    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}

package com.example.trashmap.DBClasses;

import java.io.Serializable;
import java.util.Date;

public class Messages implements Serializable {
    private String subject;
    private Date date;
    private Object content;

    public Messages(String Subject, Date Date, Object Content){
        this.content = Content;
        this.date = Date;
        this.subject = Subject;
    }

    public Messages(){

    }

    public Date getDate() {
        return date;
    }

    public Object getContent() {
        return content;
    }

    public String getSubject() {
        return subject;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}

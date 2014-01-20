package com.jfo.app.chat.db;

import com.lidroid.xutils.db.annotation.Table;


// 建议加上注解， 混淆后表名不受影响
//@Table(name = "parent", execAfterTableCreated = "CREATE UNIQUE INDEX index_name ON parent(name,email)")
@Table(name = "message")
public class DBMessage extends EntityBase {
    public String address;
    private int thread_id;
    private String body;
    private long date;
    private String subject;
    private int read;
    private int type;
    private int status;
    private String protocol;
    private int media_type;
    private String e_d1;
    private String e_d2;
    private String e_d3;
    private String e_d4;
    private String e_d5;
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public int getThread_id() {
        return thread_id;
    }
    public void setThread_id(int thread_id) {
        this.thread_id = thread_id;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public long getDate() {
        return date;
    }
    public void setDate(long date) {
        this.date = date;
    }
    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
    public int getRead() {
        return read;
    }
    public void setRead(int read) {
        this.read = read;
    }
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public String getProtocol() {
        return protocol;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public int getMedia_type() {
        return media_type;
    }
    public void setMedia_type(int media_type) {
        this.media_type = media_type;
    }
    public String getE_d1() {
        return e_d1;
    }
    public void setE_d1(String e_d1) {
        this.e_d1 = e_d1;
    }
    public String getE_d2() {
        return e_d2;
    }
    public void setE_d2(String e_d2) {
        this.e_d2 = e_d2;
    }
    public String getE_d3() {
        return e_d3;
    }
    public void setE_d3(String e_d3) {
        this.e_d3 = e_d3;
    }
    public String getE_d4() {
        return e_d4;
    }
    public void setE_d4(String e_d4) {
        this.e_d4 = e_d4;
    }
    public String getE_d5() {
        return e_d5;
    }
    public void setE_d5(String e_d5) {
        this.e_d5 = e_d5;
    }
    @Override
    public String toString() {
        return "DBMessage [address=" + address + ", thread_id=" + thread_id
                + ", body=" + body + ", date=" + date + ", subject=" + subject
                + ", read=" + read + ", type=" + type + ", status=" + status
                + ", protocol=" + protocol + ", media_type=" + media_type
                + ", e_d1=" + e_d1 + ", e_d2=" + e_d2 + ", e_d3=" + e_d3
                + ", e_d4=" + e_d4 + ", e_d5=" + e_d5 + "]";
    }
}

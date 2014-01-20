package com.jfo.app.chat.db;

import com.lidroid.xutils.db.annotation.Table;


// 建议加上注解， 混淆后表名不受影响
//@Table(name = "parent", execAfterTableCreated = "CREATE UNIQUE INDEX index_name ON parent(name,email)")
@Table(name = "attachments")
public class DBAttachment extends EntityBase {
    public String name;
    private String desc;
    private String url;
    private long size;
    private long create_time;
    private long modify_time;
    private String md5;
    private int message_id;
    private String local_path;

//    @Finder(valueColumn = "id", targetColumn = "parentId")
//    public FinderLazyLoader<Child> children; // 关联对象多时建议使用这种方式，延迟加载效率较高。
    //@Finder(valueColumn = "id",targetColumn = "parentId")
    //public Child children;
    //@Finder(valueColumn = "id", targetColumn = "parentId")
    //private List<Child> children;



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCreate_time() {
        return create_time;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public long getModify_time() {
        return modify_time;
    }

    public void setModify_time(long modify_time) {
        this.modify_time = modify_time;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public int getMessage_id() {
        return message_id;
    }

    public void setMessage_id(int message_id) {
        this.message_id = message_id;
    }

    public String getLocal_path() {
        return local_path;
    }

    public void setLocal_path(String local_path) {
        this.local_path = local_path;
    }

    @Override
    public String toString() {
        return "DBAttachment [name=" + name + ", desc=" + desc + ", url=" + url
                + ", size=" + size + ", create_time=" + create_time
                + ", modify_time=" + modify_time + ", md5=" + md5
                + ", message_id=" + message_id + ", local_path=" + local_path
                + "]";
    }

}

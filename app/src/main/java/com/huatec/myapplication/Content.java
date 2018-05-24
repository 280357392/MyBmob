package com.huatec.myapplication;

import cn.bmob.v3.BmobObject;


/**
 * 对应表名
 */
public class Content extends BmobObject {

    private String num;
    private String name;

    public Content(){

    }

    public Content(String name, String num) {
        this.num = num;
        this.name = name;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

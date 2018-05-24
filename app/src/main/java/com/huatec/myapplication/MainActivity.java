package com.huatec.myapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobDate;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.QueryListener;
import cn.bmob.v3.listener.SaveListener;

import static cn.bmob.v3.b.From.e;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化Bmob,Application ID
        Bmob.initialize(this, "53e6e971111903913c0851c5c555e9fc");

        findViewById(R.id.send_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //运行时权限
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_CONTACTS}, 1);
                } else {
                    readContacts();//获取手机中联系人与号码，并提交到后台服务器
                }
            }
        });


        findViewById(R.id.get_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getData();//根据时间从后台服务器获取数据，并添加到联系人中
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                //运行时权限
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readContacts();
                } else {
                    Toast.makeText(this, "因权限不足无法使用，请重试并授权", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * 获取手机中联系人与号码，并提交到后台服务器
     */
    private void readContacts() {
        Cursor mMCursor = null;
        try {
            mMCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);
            if (mMCursor != null) {
                while (mMCursor.moveToNext()) {
                    //联系人
                    String displayName = mMCursor.getString(mMCursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    //电话号码（去除中间多余空格）
                    String number = mMCursor.getString(mMCursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER)).replace(" ", "");

                    //提交到服务器
                    Content content = new Content();
                    content.setName(displayName);
                    content.setNum(number);
                    content.save();
                }
                Toast.makeText(MainActivity.this,
                        "同步成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this,
                        "暂无联系人", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,
                    "同步失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            if (mMCursor != null) {
                mMCursor.close();
            }
        }
    }

    /**
     * 根据时间从后台服务器获取数据，并添加到联系人中
     */
    private void getData() {
        BmobQuery<Content> query = new BmobQuery<>();
        List<BmobQuery<Content>> and = new ArrayList<>();

        //大于00：00：00
        BmobQuery<Content> q1 = new BmobQuery<>();
        String start = "2015-01-01 13:47:42";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = sdf.parse(start);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        q1.addWhereGreaterThanOrEqualTo("createdAt", new BmobDate(date));
        and.add(q1);

        query.and(and);
        query.findObjects(new FindListener<Content>() {
            @Override
            public void done(List<Content> list, BmobException e) {
                if (list!=null){
                    for (int i = 0; i < list.size(); i++) {
                        //将远程数据添加到联系人中
                        testAddContacts(list.get(i).getName(),list.get(i).getNum());
                    }
                    Toast.makeText(MainActivity.this,
                            "同步成功", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this,
                            "同步失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 将数据添加到联系人中
     */
    private void testAddContacts(String name, String num) {
        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        ContentResolver resolver = this.getContentResolver();
        ContentValues values = new ContentValues();
        long contactId = ContentUris.parseId(resolver.insert(uri, values));
        /* 往 data 中添加数据（要根据前面获取的id号） */
        // 添加姓名
        uri = Uri.parse("content://com.android.contacts/data");
        values.put("raw_contact_id", contactId);
        values.put("mimetype", "vnd.android.cursor.item/name");
        values.put("data2", name);
        resolver.insert(uri, values);
        // 添加电话
        values.clear();
        values.put("raw_contact_id", contactId);
        values.put("mimetype", "vnd.android.cursor.item/phone_v2");
        values.put("data2", "2");
        values.put("data1", num);
        resolver.insert(uri, values);
    }

}

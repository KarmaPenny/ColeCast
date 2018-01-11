package com.colecast.colecast;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.Map;

public class Pair extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);

        SharedPreferences hosts = getSharedPreferences("com.colecast.colecast.hosts", Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> entry : hosts.getAll().entrySet())
        {
            
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }

    }
}

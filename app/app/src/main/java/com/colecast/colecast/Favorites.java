package com.colecast.colecast;
import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView;
import android.content.Intent;
import android.net.Uri;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONObject;
import org.json.JSONArray;

class FavoritesAdapter extends BaseAdapter {
    private LayoutInflater inflater;

    public FavoritesAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return Favorites.GetFavoritesCount();
    }

    @Override
    public Object getItem(int index) {
        return Favorites.GetFavorite(index);
    }

    @Override
    public long getItemId(int index) {
        return index;
    }

    @Override
    public View getView(final int index, View convertView, ViewGroup parent) {
        // get the view associated with this listview item
        View view = inflater.inflate(R.layout.item_favorites, parent, false);

        // set the symbol text
        ((TextView) view.findViewById(R.id.name)).setText(Favorites.GetFavoriteName(index));

        // set the icon of the listview item
        File file = new File(Favorites.GetFavoriteIcon(index));
        if (file.exists()) {
            Uri uri = Uri.fromFile(file);
            ((ImageView) view.findViewById(R.id.icon)).setImageURI(uri);
        } else {
            ((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.favoriteicon);
        }

        return view;
    }
}

public class Favorites extends Activity {
    public static JSONObject saveData = new JSONObject();
    static Context context;
    static int selected;
    FavoritesAdapter favoritesAdapter;

    public static int GetFavoritesCount() {
        try {
            JSONArray favorites = saveData.getJSONArray("favorites");
            return favorites.length();
        } catch (Exception e) {}
        return 0;
    }

    public static JSONObject GetFavorite(int index) {
        try {
            JSONArray favorites = saveData.getJSONArray("favorites");
            return favorites.getJSONObject(index);
        } catch (Exception e) {}
        return null;
    }

    public static String GetFavoriteName(int index) {
        try {
            JSONObject favorite = GetFavorite(index);
            if (favorite != null) {
                return favorite.getString("name");
            }
        } catch (Exception e) {}
        return "ERROR";
    }

    public static String GetFavoriteIcon(int index) {
        try {
            JSONObject favorite = GetFavorite(index);
            if (favorite != null) {
                return favorite.getString("icon");
            }
        } catch (Exception e) {}
        return "ERROR";
    }

    public static String GetFavoriteURL(int index) {
        try {
            JSONObject favorite = GetFavorite(index);
            if (favorite != null) {
                return favorite.getString("url");
            }
        } catch (Exception e) {}
        return "https://google.com/teapot";
    }

    public static void SetFavorite(String name, String icon, String url) {
        try {
            JSONArray favorites = new JSONArray();
            if (saveData.has("favorites")) {
                favorites = saveData.getJSONArray("favorites");
            }
            JSONObject favorite = new JSONObject();
            favorite.put("name", name);
            favorite.put("icon", icon);
            favorite.put("url", url);
            if (selected < 0) {
                favorites.put(favorite);
            } else {
                favorites.put(selected, favorite);
            }
            saveData.put("favorites", favorites);
        } catch (Exception e) {}
    }

    public static void DeleteFavorite() {
        if (selected >= 0) {
            try {
                JSONArray favorites = saveData.getJSONArray("favorites");
                favorites.remove(selected);
                saveData.put("favorites", favorites);
            } catch (Exception e) {}
        }
    }

    void Load() {
        try {
            // load data from file
            InputStream inputStream = openFileInput("favorites.json");
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                saveData = new JSONObject(bufferedReader.readLine());
                inputStream.close();
            }
        }
        catch (Exception e) {}
    }

    public void AddNewFavorite(View v) {
        selected = -1;
        startActivity(new Intent(this, AddFavorite.class));
    }

    public void Back(View v) {
        // close the add favorite activity
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_favorites);

        Load();

        GridView favoritesList = (GridView) findViewById(R.id.favorites);
        favoritesAdapter = new FavoritesAdapter(this);

        favoritesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                Intent intent = new Intent();
                intent.setData(Uri.parse(GetFavoriteURL(position)));
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        favoritesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapter, View v, int position, long id) {
                selected = position;
                startActivity(new Intent(context, AddFavorite.class));
                return true;
            }
        });

        favoritesList.setAdapter(favoritesAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        favoritesAdapter.notifyDataSetChanged();
    }
}

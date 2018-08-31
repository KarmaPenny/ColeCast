package com.colecast.colecast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;

public class AddFavorite extends Activity {
    public void DeleteFavorite(View v) {
        // delete the favorite
        Favorites.DeleteFavorite();

        // dave favorites to file
        Save();

        // close the add favorite activity
        finish();
    }

    public void SelectIcon(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    public void SaveFavorite(View v) {
        // get favorite name
        EditText nameInput = (EditText) findViewById(R.id.name);
        String name = nameInput.getText().toString();

        // get favorite url
        EditText urlInput = (EditText) findViewById(R.id.url);
        String url = urlInput.getText().toString();

        // get favorite icon url
        ImageView iconInput = (ImageView) findViewById(R.id.icon);
        String iconPath = getApplicationInfo().dataDir + "/" + name + ".png";
        try {
            Bitmap icon = ((BitmapDrawable)iconInput.getDrawable()).getBitmap();
            FileOutputStream out = new FileOutputStream(iconPath);
            icon.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (Exception e) {
            Log.e("Saving Icon", e.getMessage());
        }

        // add/update favorite
        Favorites.SetFavorite(name, iconPath, url);

        // save favorites to file
        Save();

        // close the add favorite activity
        finish();
    }

    public void Back(View v) {
        // close the add favorite activity
        finish();
    }

    void Save() {
        try {
            // save data to file
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("favorites.json", Context.MODE_PRIVATE));
            outputStreamWriter.write(Favorites.saveData.toString());
            outputStreamWriter.close();
        }
        catch (IOException e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_favorite);

        if (Favorites.selected >= 0) {
            // set favorite name
            EditText nameInput = (EditText) findViewById(R.id.name);
            nameInput.setText(Favorites.GetFavoriteName(Favorites.selected));

            // set favorite url
            EditText urlInput = (EditText) findViewById(R.id.url);
            urlInput.setText(Favorites.GetFavoriteURL(Favorites.selected));

            // set favorite icon
            File file = new File(Favorites.GetFavoriteIcon(Favorites.selected));
            if (file.exists()) {
                Uri uri = Uri.fromFile(file);
                ((ImageView) findViewById(R.id.icon)).setImageURI(uri);
            } else {
                ((ImageView) findViewById(R.id.icon)).setImageResource(R.drawable.favoriteicon);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == 1) {
            Uri uri = data.getData();
            ImageView iconInput = (ImageView) findViewById(R.id.icon);
            iconInput.setImageURI(uri);
        }
    }
}

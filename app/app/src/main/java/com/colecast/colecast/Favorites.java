package com.colecast.colecast;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

import java.security.MessageDigest;

public class Favorites extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        final GridView listview = (GridView) findViewById(R.id.listview);
        Favorite[] values = new Favorite[]{
                new Favorite("Netflix", "https://www.netflix.com"),
                new Favorite("Music", "https://play.google.com/music/listen#/all"),
                new Favorite("ESPN", "http://espn.go.com/watchespn"),
                new Favorite("Spotify", "C:\\Users\\Cole\\AppData\\Roaming\\Spotify\\Spotify.exe")
        };

        final FavortiesArrayAdapter adapter = new FavortiesArrayAdapter(this, values);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final Favorite item = (Favorite) parent.getItemAtPosition(position);
                Intent data = new Intent();
                data.setData(Uri.parse(item.url));
                setResult(RESULT_OK, data);
                finish();
            }

        });
    }

    public class Favorite {
        public String name = "";
        public String url = "";
        public String icon = "";

        public Favorite(String name, String url) {
            this.name = name;
            this.url = url;
            try {
                byte[] bytesOfMessage = url.getBytes("UTF-8");
                MessageDigest md = MessageDigest.getInstance("MD5");
                this.icon = bytesToHex(md.digest(bytesOfMessage)) + ".ico";
                DownloadFavIcon(url, icon);
            }
            catch (Exception e) {}
        }
    }

    public void DownloadFavIcon(String url, String hash) {
        new AsyncTask<String, Void, Void>() {
            protected Void doInBackground(String... params) {
                try {
                    URL website = new URL(params[0]);
                    URLConnection connection = website.openConnection();
                    connection.setRequestProperty("User-Agent","curl/7.9.8 (i686-pc-linux-gnu) libcurl 7.9.8 (OpenSSL 0.9.6b) (ipv6 enabled)");
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) response.append(inputLine);
                    in.close();
                    String text = response.toString().toLowerCase();
                    int start_index = text.indexOf("<link rel=\"shortcut icon\" href=") + 32;
                    URL ico_url = new URL(website.getProtocol() + "://" + website.getHost() + "/favicon.ico");
                    if (start_index > 31) {
                        int end_index = text.indexOf("\"", start_index);
                        if (end_index >= 0) {
                            ico_url = new URL(text.substring(start_index, end_index));
                        }
                    }
                    InputStream ico_in = new BufferedInputStream(ico_url.openStream());
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int n = 0;
                    while (-1 != (n = ico_in.read(buf))) out.write(buf, 0, n);
                    out.close();
                    in.close();
                    byte[] icon = out.toByteArray();
                    FileOutputStream outputStream = openFileOutput(params[1], Context.MODE_PRIVATE);
                    outputStream.write(icon);
                    outputStream.close();
                }
                catch (Exception e) {Log.e("ERROR", e.getMessage());}
                return null;
            }
        }.execute(url, hash);
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public class FavortiesArrayAdapter extends ArrayAdapter<Favorite> {
        private final Context context;
        private final Favorite[] values;

        public FavortiesArrayAdapter(Context context, Favorite[] values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.item_favorites, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.text);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            textView.setText(values[position].name);
            File file = new File(context.getFilesDir(), values[position].icon);
            if(file.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                Bitmap myBitmap = BitmapFactory.decodeFile(Uri.fromFile(file).getPath());
                imageView.setImageBitmap(myBitmap);
            }
            else imageView.setImageResource(R.drawable.blank);
            return rowView;
        }
    }
}

package com.colecast.colecast;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class DevicesAdapter extends BaseAdapter {
    private LayoutInflater inflater;

    public DevicesAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return Pair.GetDevicesCount();
    }

    @Override
    public Object getItem(int index) {
        return Pair.GetDevice(index);
    }

    @Override
    public long getItemId(int index) {
        return index;
    }

    @Override
    public View getView(final int index, View convertView, ViewGroup parent) {
        // get the view associated with this listview item
        View view = inflater.inflate(R.layout.item_device, parent, false);

        // set the text values
        ((TextView) view.findViewById(R.id.name)).setText(Pair.GetDeviceName(index));
        ((TextView) view.findViewById(R.id.mac)).setText("MAC: " + Pair.GetDeviceMAC(index));
        ((TextView) view.findViewById(R.id.ip)).setText("IPv4: " + Pair.GetDeviceIP(index));

        return view;
    }
}

public class Pair extends Activity {
    static DevicesAdapter devicesAdapter;
    public static JSONObject devicesJSON = new JSONObject();
    private String broadcastIp = "255.255.255.255";
    private int port = 3128;
    DatagramSocket socket;
    Thread UDPBroadcastThread;
    Boolean listening;
    Context context;

    public static int GetDevicesCount() {
        return devicesJSON.length();
    }

    public static JSONObject GetDevice(int index) {
        try {
            JSONArray devices = devicesJSON.names();
            return devicesJSON.getJSONObject(devices.getString(index));
        } catch (Exception e) {}
        return null;
    }

    public static String GetDeviceName(int index) {
        try {
            JSONObject device = GetDevice(index);
            return device.getString("name");
        } catch (Exception e) {}
        return "ERROR";
    }

    public static String GetDeviceMAC(int index) {
        try {
            JSONObject device = GetDevice(index);
            return device.getString("mac");
        } catch (Exception e) {}
        return "ERROR";
    }

    public static String GetDeviceIP(int index) {
        try {
            JSONObject device = GetDevice(index);
            return device.getString("ip");
        } catch (Exception e) {}
        return "ERROR";
    }

    public static void AddDevice(String name, String ip, String mac) {
        try {
            JSONObject device = new JSONObject();
            device.put("name", name);
            device.put("ip", ip);
            device.put("mac", mac);
            devicesJSON.put(name, device);
            devicesAdapter.notifyDataSetChanged();
        } catch (Exception e) {}
    }

    private void ReceiveDeviceInfo() throws Exception {
        byte[] recvBuf = new byte[2048];
        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket(port, InetAddress.getByName(broadcastIp));
            socket.setBroadcast(true);
        }
        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
        socket.receive(packet);

        final String ip = packet.getAddress().getHostAddress();
        String message = new String(packet.getData()).trim();
        Log.d("DEVICE INFO", message);
        String parts[] = message.split("\\|");
        final String mac = parts[0];
        final String name = parts[1];

        Handler mainHandler = new Handler(context.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                AddDevice(name, ip, mac);
            }
        };
        mainHandler.post(myRunnable);

        socket.close();
    }

    void startListenForUDPBroadcast() {
        listening = true;
        UDPBroadcastThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (listening) {
                        ReceiveDeviceInfo();
                    }
                } catch (Exception e) {
                    Log.i("UDP", "no longer listening for UDP broadcasts cause of error " + e.getMessage());
                }
            }
        });
        UDPBroadcastThread.start();
    }

    void stopListen() {
        listening = false;
        socket.close();
    }

    public void Back(View v) {
        // close the add favorite activity
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);

        context = this;

        devicesJSON = new JSONObject();

        ListView deviceList = (ListView) findViewById(R.id.devices);
        devicesAdapter = new DevicesAdapter(this);

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                // set device
                MainActivity.device = GetDevice(position);

                // save device to file
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("device.json", Context.MODE_PRIVATE));
                    outputStreamWriter.write(MainActivity.device.toString());
                    outputStreamWriter.close();
                }
                catch (IOException e) {}

                // close activity
                finish();
            }
        });

        deviceList.setAdapter(devicesAdapter);
    }

    @Override
    public void onResume() {
        startListenForUDPBroadcast();
        super.onResume();
    }

    @Override
    public void onPause() {
        stopListen();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        stopListen();
        super.onDestroy();
    }
}

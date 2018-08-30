package com.colecast.colecast;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends Activity {
    //region PROPERTIES
    public static JSONObject device = new JSONObject();
    public static InetAddress DeviceIP() {
        try {
            return InetAddress.getByName(device.getString("ip"));
        } catch (Exception e) {}
        return null;
    }
    public static byte[] DeviceMAC() {
        try {
            String mac = device.getString("mac").replace(":", "").toLowerCase();
            int len = mac.length();
            byte[] data = new byte[len/2];
            for(int i = 0; i < len; i+=2){
                data[i/2] = (byte) ((Character.digit(mac.charAt(i), 16) << 4) + Character.digit(mac.charAt(i+1), 16));
            }
            return data;
        } catch (Exception e) {}
        return new byte[] { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
    }
    public static String DeviceName() {
        try {
            return device.getString("name");
        } catch (Exception e) {}
        return "Not Connected";
    }

    //IP address of the the htpc
    private byte[] broadcastIp = { (byte)255, (byte)255, (byte)255, (byte)255 };

    //PORT that the htpc is listening on
    private int port = 3129;

    //State of the left mouse button
    private boolean isLeftMouseButtonDown = false;

    //intent requests types
    private static final int OPEN_FAVORITE = 1;
    private static final int SELECT_HOST = 2;

    //OPCODES interpreted by htpc
    private enum OPCODE {
        //POWER CODES
        Ping((byte)0x00),
        Sleep((byte)0x01),
        Wake((byte)0x02),
        WakeOnLan(new byte[] {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}),

        //MOUSE CODES
        MoveCursor((byte)0x10),
        Scroll((byte)0x11),
        LeftMouseButtonDown((byte)0x12),
        LeftMouseButtonUp((byte)0x13),
        RightMouseButtonDown((byte)0x14),
        RightMouseButtonUp((byte)0x15),

        //TV CODES
        VolumeUp((byte)0x20),
        VolumeDown((byte)0x21),
        Mute((byte)0x22),
        CycleInput((byte)0x23),

        //ADVANCED CODES
        OpenUrl((byte)0x30),

        //keyboard controls
        FullScreen((byte)0x33),
        CloseTab((byte)0x34),
        PreviousTrack((byte)0x35),
        PlayPause((byte)0x36),
        NextTrack((byte)0x37);


        public byte[] value;
        OPCODE(byte value) { this.value = new byte[] { value }; }
        OPCODE(byte[] value) { this.value = value; }
    }
    //endregion

    //region ACTIVITY EVENTS
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load device from file
        try {
            InputStream inputStream = openFileInput("device.json");
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                device = new JSONObject(bufferedReader.readLine());
                inputStream.close();
            }
        }
        catch (Exception e) {
            try {
                device = new JSONObject();
                device.put("name", "Cole's Desktop");
                device.put("ip", "192.168.1.106");
                device.put("mac", "00:1b:21:24:29:80");
            } catch (Exception e2) {}
        }

        setContentView(R.layout.activity_main);
        ((LinearLayout)findViewById(R.id.trackPad)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return MouseEvent(event);
            }
        });

        findViewById(R.id.VolumeDown).setOnTouchListener(new RepeatListener(750, 150, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VolumeDown();
            }
        }));

        findViewById(R.id.VolumeUp).setOnTouchListener(new RepeatListener(750, 150, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VolumeUp();
            }
        }));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                OpenUrl(intent.getStringExtra(Intent.EXTRA_TEXT));
                intent.removeExtra(Intent.EXTRA_TEXT);
            }
        }

        ((TextView)findViewById(R.id.title)).setText(DeviceName());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == OPEN_FAVORITE) {
                OpenUrl(data.getData().toString());
            }
        }
    }
    //endregion

    //region ACTIVITIES
    public void OpenFavorites(View view) {
        startActivityForResult(new Intent(this, Favorites.class), OPEN_FAVORITE);
    }

    public void OpenPair(View view) {
        startActivityForResult(new Intent(this, Pair.class), SELECT_HOST);
    }
    //endregion

    //region ADVANCED CONTROLS
    private void OpenUrl(String url) {
        new AsyncTask<String, Void, Void>() {
            protected Void doInBackground(String... params) {
                Wake();
                sendCommand(OPCODE.OpenUrl, params);
                return null;
            }
        }.execute(url);
    }
    //endregion

    //region KEYBOARD CONTROLS
    public void FullScreen(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.FullScreen);
                return null;
            }
        }.execute();
    }

    public void CloseTab(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.CloseTab);
                return null;
            }
        }.execute();
    }

    public void PreviousTrack(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.PreviousTrack);
                return null;
            }
        }.execute();
    }

    public void PlayPause(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.PlayPause);
                return null;
            }
        }.execute();
    }

    public void NextTrack(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.NextTrack);
                return null;
            }
        }.execute();
    }
    //endregion

    //region TV CONTROLS
    public void VolumeUp() {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.VolumeUp);
                return null;
            }
        }.execute();
    }

    public void VolumeDown() {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.VolumeDown);
                return null;
            }
        }.execute();
    }

    public void Mute(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.Mute);
                return null;
            }
        }.execute();
    }
    //endregion

    //region POWER CONTROLS
    public void TogglePower(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                if (On()) Sleep();
                else Wake();
                return null;
            }
        }.execute();
    }

    private boolean On() {
        sendCommand(OPCODE.Ping, DeviceMAC());
        DatagramPacket packet = getPacket();
        if (packet == null) { return false; }
        return true;
    }

    private void Wake() {
        byte[] mac = DeviceMAC();
        long end = System.currentTimeMillis();
        end += 60*1000;
        byte[] wol = new byte[96];
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 6; j++) {
                wol[i * 6 + j] = mac[j];
            }
        }
        while (!On()) {
            broadcastCommand(OPCODE.WakeOnLan, wol, 9);
            if (System.currentTimeMillis() > end) break;
        }
    }

    private void Sleep() {
        sendCommand(OPCODE.Sleep);
    }
    //endregion

    //region MOUSE CONTROLS
    public boolean MouseEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getEventTime() - event.getDownTime() < 100) {
                    new AsyncTask<Void, Void, Void>() {
                        protected Void doInBackground(Void... params) {
                            RightMouseButtonDown();
                            RightMouseButtonUp();
                            return null;
                        }
                    }.execute();
                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
                new AsyncTask<MotionEvent, Void, Void>() {
                    protected Void doInBackground(MotionEvent... events) {
                        if (events[0].getEventTime() - events[0].getDownTime() < 100) LeftMouseButtonDown();
                        if (isLeftMouseButtonDown) LeftMouseButtonUp();
                        return null;
                    }
                }.execute(event);
                break;
            case MotionEvent.ACTION_MOVE:
                new AsyncTask<MotionEvent, Void, Void>() {
                    protected Void doInBackground(MotionEvent... events) {
                        try {
                            int x = Math.round(events[0].getX() - events[0].getHistoricalX(0));
                            int y = Math.round(events[0].getY() - events[0].getHistoricalY(0));
                            if (events[0].getPointerCount() > 1) {
                                int x2 = Math.round(events[0].getX(1) - events[0].getHistoricalX(1, 0));
                                int y2 = Math.round(events[0].getY(1) - events[0].getHistoricalY(1, 0));
                                if (Math.hypot(x, y) < 2 && Math.hypot(x2, y2) > 3) {
                                    if (!isLeftMouseButtonDown) LeftMouseButtonDown();
                                    MoveCursor(x2, y2);
                                } else Scroll(y);
                            } else MoveCursor(x, y);
                        }
                        catch (Exception e) { }
                        return null;
                    }
                }.execute(event);
                break;
        }
        return true;
    }

    private void MoveCursor(Integer x, Integer y) {
        Log.i("Mouse", "MoveCursor");
        sendCommand(OPCODE.MoveCursor, x, y);
    }

    private void Scroll(Integer z) {
        Log.i("Mouse", "Scroll " + z);
        sendCommand(OPCODE.Scroll, z * 5);
    }

    private void LeftMouseButtonDown() {
        Log.i("Mouse", "LeftButtonDown");
        isLeftMouseButtonDown = true;
        sendCommand(OPCODE.LeftMouseButtonDown);
    }

    private void LeftMouseButtonUp() {
        Log.i("Mouse", "LeftButtonUp");
        isLeftMouseButtonDown = false;
        sendCommand(OPCODE.LeftMouseButtonUp);
    }

    private void RightMouseButtonDown() {
        Log.i("Mouse", "RightButtonDown");
        sendCommand(OPCODE.RightMouseButtonDown);
    }

    private void RightMouseButtonUp() {
        Log.i("Mouse", "RightButtonUp");
        sendCommand(OPCODE.RightMouseButtonUp);
    }
    //endregion

    //region SOCKET IO
    private void sendCommand(OPCODE opcode) {
        sendPacket(opcode.value, port);
    }

    private void sendCommand(OPCODE opcode, String... args) {
        String full_args = "";
        for (String arg : args) full_args += arg;
        ByteBuffer buffer = ByteBuffer.allocate(full_args.length() + opcode.value.length);
        buffer.put(opcode.value);
        buffer.put(full_args.getBytes());
        sendPacket(buffer.array(), port);
    }

    private void sendCommand(OPCODE opcode, int... args) {
        ByteBuffer buffer = ByteBuffer.allocate(args.length * 4 + opcode.value.length);
        buffer.put(opcode.value);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int arg : args) buffer.putInt(arg);
        sendPacket(buffer.array(), port);
    }

    private void sendCommand(OPCODE opcode, byte[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(args.length + opcode.value.length);
        buffer.put(opcode.value);
        buffer.put(args);
        sendPacket(buffer.array(), port);
    }

    private void sendCommand(OPCODE opcode, byte[] args, int port) {
        ByteBuffer buffer = ByteBuffer.allocate(args.length + opcode.value.length);
        buffer.put(opcode.value);
        buffer.put(args);
        sendPacket(buffer.array(), port);
    }

    private void broadcastCommand(OPCODE opcode, byte[] args, int port) {
        ByteBuffer buffer = ByteBuffer.allocate(args.length + opcode.value.length);
        buffer.put(opcode.value);
        buffer.put(args);
        broadcastPacket(buffer.array(), port);
    }

    private void sendPacket(byte[] data, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.send(new DatagramPacket(data, data.length, DeviceIP(), port));
            socket.close();
        } catch (Exception e) { Log.e("ERROR", e.toString()); }
    }

    private void broadcastPacket(byte[] data, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.send(new DatagramPacket(data, data.length, InetAddress.getByAddress(broadcastIp), port));
            socket.close();
        } catch (Exception e) { Log.e("ERROR", e.toString()); }
    }

    private DatagramPacket getPacket() {
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            DatagramSocket socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.setSoTimeout(200);
            socket.bind(new InetSocketAddress(port));
            socket.receive(packet);
            socket.close();
            return packet;
        }
        catch (SocketTimeoutException e) {} //we expect this to fire so ignore it
        catch (Exception e) { Log.e("ERROR", e.toString()); }
        return null;
    }
    //endregion

}

package com.colecast.colecast;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    //IP address of the the htpc
    private byte[] ip = { (byte)192, (byte)168, (byte)1, (byte)106 };

    //MAC address of the htpc
    private byte[] mac = { 0x30, (byte)0x85, (byte)0xa9, (byte)0x8e, 0x7d, 0x51 };

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
        Paste((byte)0x31),
        FullScreen((byte)0x32),
        ExitFullScreen((byte)0x33),
        CloseTab((byte)0x34),
        PreviousTrack((byte)0x35),
        PlayPause((byte)0x36),
        NextTrack((byte)0x37);


        public byte[] value;
        OPCODE(byte value) { this.value = new byte[] { value }; }
        OPCODE(byte[] value) { this.value = value; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoadHost();

        setContentView(R.layout.activity_main);
        ((LinearLayout)findViewById(R.id.trackPad)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return MouseEvent(event);
            }
        });
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
    }

    @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == OPEN_FAVORITE) {
                OpenUrl(data.getData().toString());
            }
            else if (requestCode == SELECT_HOST) {
                mac = data.getData().toString().getBytes();
            }
        }
    }

    //ADVANCED CONTROLS
    private void LoadHost() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String host = sharedPref.getString("CurrentHost", "none");
        if (host != "none") {
            SharedPreferences hosts = getSharedPreferences("com.colecast.colecast.hosts", Context.MODE_PRIVATE);
            mac = hosts.getString(host, null).getBytes();
        }
    }

    public void OpenSettings(View view) {}

    public void OpenFavorites(View view) {
        startActivityForResult(new Intent(this, Favorites.class), OPEN_FAVORITE);
    }

    private void OpenUrl(String url) {
        new AsyncTask<String, Void, Void>() {
            protected Void doInBackground(String... params) {
                Wake();
                sendCommand(OPCODE.OpenUrl, params);
                return null;
            }
        }.execute(url);
    }

    public void Paste(String text) {
        new AsyncTask<String, Void, Void>() {
            protected Void doInBackground(String... params) {
                sendCommand(OPCODE.Paste, params);
                return null;
            }
        }.execute(text);
    }

    public void FullScreen(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.FullScreen);
                return null;
            }
        }.execute();
    }

    public void ExitFullScreen(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.ExitFullScreen);
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

    //TV CONTROLS
    public void VolumeUp(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.VolumeUp);
                return null;
            }
        }.execute();
    }

    public void VolumeDown(View view) {
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

    public void CycleInput(View view) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                sendCommand(OPCODE.CycleInput);
                return null;
            }
        }.execute();
    }

    //POWER CONTROLS
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
        Log.d("TRACE", "ON");
        sendCommand(OPCODE.Ping, mac);
        Log.d("TRACE", "PING SENT");
        DatagramPacket packet = getPacket();
        Log.d("TRACE", "PONG RETURNED");
        if (packet == null) { return false; }
        //ip = packet.getData();
        return true;
    }

    private void Wake() {
        long end = System.currentTimeMillis();
        end += 60*1000;
        byte[] wol = new byte[96];
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 6; j++) {
                wol[i * 6 + j] = mac[j];
            }
        }
        while (!On()) {
            sendCommand(OPCODE.WakeOnLan, wol, 9);
            if (System.currentTimeMillis() > end) break;
        }
        sendCommand(OPCODE.Wake);
    }

    private void Sleep() {
        sendCommand(OPCODE.Sleep);
    }

    //MOUSE CONTROLS
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

    //SOCKET IO
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

    private void sendPacket(byte[] data, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.send(new DatagramPacket(data, data.length, InetAddress.getByAddress(ip), port));
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

}

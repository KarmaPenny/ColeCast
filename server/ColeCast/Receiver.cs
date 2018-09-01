using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Net;
using System.Text;
using System.Threading;
using System.Net.Sockets;
using System.IO.Ports;
using WindowsInput;
using Microsoft.Win32;
using System.Net.NetworkInformation;

namespace ColeCast
{
    public class Receiver
    {
        private Thread listener;
        private Thread broadcaster;
        private volatile bool done = false;
        private InputSimulator virtualInput = new InputSimulator();
        private Dictionary<byte, Action<IPEndPoint, byte[]>> operations = new Dictionary<byte, Action<IPEndPoint, byte[]>>();

        public Receiver()
        {
            //POWER CONTROLS
            operations.Add(0x00, Ping);
            operations.Add(0x01, Sleep);

            //MOUSE CONTROLS
            operations.Add(0x10, MoveCursor);
            operations.Add(0x11, Scroll);
            operations.Add(0x12, LeftMouseButtonDown);
            operations.Add(0x13, LeftMouseButtonUp);
            operations.Add(0x14, RightMouseButtonDown);
            operations.Add(0x15, RightMouseButtonUp);

            //TV CONTROLS
            operations.Add(0x20, VolumeUp);
            operations.Add(0x21, VolumeDown);
            operations.Add(0x22, Mute);

            //ADVANCED CONTROLS
            operations.Add(0x30, OpenUrl);

            //KEYBOARD CONTROLS
            operations.Add(0x33, FullScreen);
            operations.Add(0x34, CloseWindow);
            operations.Add(0x35, PrevKey);
            operations.Add(0x36, PlayKey);
            operations.Add(0x37, NextKey);

            // detect power state changes
            SystemEvents.PowerModeChanged += OnPowerChange;
            SystemEvents.SessionEnding += OnShutDown;
        }

        #region ADVANCED CONTROLS

        private void OpenUrl(IPEndPoint sender, byte[] parameters)
        {
            if (parameters == null) return;
            string url = Encoding.ASCII.GetString(parameters, 1, parameters.Length - 1);
            System.Diagnostics.Process.Start(url);
        }

        #endregion

        #region TV CONTROLS

        private void TVOn()
        {
            if (Config.GetBool("exlink_enabled"))
            {
                ExLink(Config.GetBytes("exlink_power_on"));
            }
        }

        private void TVOff()
        {
            if (Config.GetBool("exlink_enabled"))
            {
                ExLink(Config.GetBytes("exlink_power_off"));
            }
        }

        private void Mute(IPEndPoint sender, byte[] parameters)
        {
            if (Config.GetBool("exlink_enabled"))
            {
                ExLink(Config.GetBytes("exlink_mute"));
            } else
            {
                MuteKey();
            }
        }
        
        private void VolumeDown(IPEndPoint sender, byte[] parmeters)
        {
            if (Config.GetBool("exlink_enabled"))
            {
                ExLink(Config.GetBytes("exlink_volume_down"));
            }
            else
            {
                VolumeDownKey();
            }
        }

        private void VolumeUp(IPEndPoint sender, byte[] parmeters)
        {
            if (Config.GetBool("exlink_enabled"))
            {
                ExLink(Config.GetBytes("exlink_volume_up"));
            }
            else
            {
                VolumeUpKey();
            }
        }

        #endregion

        #region POWER CONTROLS

        private void Ping(IPEndPoint sender, byte[] parameters)
        {
            UdpClient response = new UdpClient(sender.Address.ToString(), Config.GetInt("port"));
            response.Send(new byte[] { 0x01 }, 1);
        }

        private void Sleep(IPEndPoint sender, byte[] parameters)
        {
            Application.SetSuspendState(PowerState.Suspend, true, true);
        }

        private void OnPowerChange(object s, PowerModeChangedEventArgs e)
        {
            if (e.Mode == PowerModes.Resume)
            {
                TVOn();
            }
            else if (e.Mode == PowerModes.Suspend)
            {
                TVOff();
            }
        }

        private void OnShutDown(object s, SessionEndingEventArgs e)
        {
            if (e.Reason == SessionEndReasons.SystemShutdown)
            {
                TVOff();
            }
        }

        #endregion

        #region MOUSE CONTROLS

        private void MoveCursor(IPEndPoint sender, byte[] parameters)
        {
            int x = BitConverter.ToInt32(parameters, 1);
            int y = BitConverter.ToInt32(parameters, 5);
            VirtualMouse.Move(x, y);
        }

        private void LeftMouseButtonDown(IPEndPoint sender, byte[] parameters)
        {
            VirtualMouse.LeftButtonDown();
        }

        private void LeftMouseButtonUp(IPEndPoint sender, byte[] parameters)
        {
            VirtualMouse.LeftButtonUp();
        }

        private void RightMouseButtonDown(IPEndPoint sender, byte[] parameters)
        {
            VirtualMouse.RightButtonDown();
        }

        private void RightMouseButtonUp(IPEndPoint sender, byte[] parameters)
        {
            VirtualMouse.RightButtonUp();
        }

        private void Scroll(IPEndPoint sender, byte[] parameters)
        {
            int z = BitConverter.ToInt32(parameters, 1);
            VirtualMouse.Scroll(z);
        }

        #endregion

        #region KEYBOARD CONTROLS
        private void MuteKey()
        {
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.VOLUME_MUTE);
        }
        
        private void VolumeDownKey()
        {
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.VOLUME_DOWN);
        }

        private void VolumeUpKey()
        {
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.VOLUME_UP);
        }

        private void PrevKey(IPEndPoint sender, byte[] parameters)
        {
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.MEDIA_PREV_TRACK);
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.LEFT);
        }

        private void NextKey(IPEndPoint sender, byte[] parameters)
        {
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.MEDIA_NEXT_TRACK);
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.RIGHT);
        }

        private void PlayKey(IPEndPoint sender, byte[] parameters)
        {
            //virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.MEDIA_PLAY_PAUSE);
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.SPACE);
        }

        private void FullScreen(IPEndPoint sender, byte[] parameters)
        {
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.VK_F);
        }

        private void CloseWindow(IPEndPoint sender, byte[] parameters)
        {
            // press alt + f4
            virtualInput.Keyboard.KeyDown(WindowsInput.Native.VirtualKeyCode.LMENU);
            virtualInput.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.F4);
            virtualInput.Keyboard.KeyUp(WindowsInput.Native.VirtualKeyCode.LMENU);
        }

        #endregion

        #region IO

        private void ExLink(byte[] code)
        {
            string portName = Config.GetString("portName");
            int baudRate = Config.GetInt("baudRate");
            Parity parity = (Parity)Config.GetInt("parity");
            int dataBits = Config.GetInt("dataBits");
            StopBits stopBits = (StopBits)Config.GetInt("stopBits");
            SerialPort exLinkPort = new SerialPort(portName, baudRate, parity, dataBits, stopBits);
            exLinkPort.Open();
            exLinkPort.Write(code, 0, code.Length);
            exLinkPort.Close();
        }

        private void Listen()
        {
            UdpClient request = new UdpClient(Config.GetInt("port"));
            IPEndPoint sender = new IPEndPoint(IPAddress.Any, Config.GetInt("port"));
            while (!done)
            {
                try
                {
                    byte[] command = request.Receive(ref sender);
                    if (command.Length == 0) continue;
                    operations[command[0]](sender, command);
                }
                catch (Exception e) { }
            }
        }

        private PhysicalAddress GetMAC()
        {
            foreach (NetworkInterface nic in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (nic.NetworkInterfaceType != NetworkInterfaceType.Ethernet)
                    continue;
                if (nic.OperationalStatus != OperationalStatus.Up)
                    continue;

                return nic.GetPhysicalAddress();
            }
            return null;
        }

        private void BroadCast()
        {
            while (!done)
            {
                try
                {
                    UdpClient client = new UdpClient();
                    IPEndPoint endpoint = new IPEndPoint(IPAddress.Broadcast, 3128);
                    string mac = GetMAC().ToString();
                    for (int i = 2; i <= 14; i += 3)
                    {
                        mac = mac.Insert(i, ":");
                    }
                    byte[] bytes = Encoding.ASCII.GetBytes(mac + "|" + Environment.MachineName);
                    client.Send(bytes, bytes.Length, endpoint);
                    client.Close();
                }
                catch (Exception e) { }
                Thread.Sleep(1000);
            }
        }

        public void Start()
        {
            TVOn();
            done = false;
            listener = new Thread(Listen);
            listener.Start();
            broadcaster = new Thread(BroadCast);
            broadcaster.Start();
        }

        public void Stop()
        {
            done = true;
        }

        #endregion
    }
}

﻿using CERTENROLLLib;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Windows.Forms;
using System.IO;
using System.Linq;
using System.Net;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.IO.Ports;

namespace ColeCast
{
    public class Receiver
    {
        private int port;
        private volatile bool done = false;
        private Dictionary<byte, Action<IPEndPoint, byte[]>> operations = new Dictionary<byte, Action<IPEndPoint, byte[]>>();

        public Receiver(int port)
        {
            this.port = port;
            //POWER CONTROLS
            operations.Add(0x00, Ping);
            operations.Add(0x01, Sleep);
            operations.Add(0x02, Wake);

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
            operations.Add(0x23, CycleInput);

            //ADVANCED CONTROLS
            operations.Add(0x30, OpenUrl);
            operations.Add(0x31, Paste);
            operations.Add(0x32, Escape);
        }

        #region ADVANCED CONTROLS

        private void Escape(IPEndPoint sender, byte[] parmeters)
        {
            
        }

        private void Paste(IPEndPoint sender, byte[] parmeters)
        {
            
        }

        private void OpenUrl(IPEndPoint sender, byte[] parameters)
        {
            if (parameters == null) return;
            string url = Encoding.ASCII.GetString(parameters, 1, parameters.Length - 1);
            System.Diagnostics.Process.Start(url);
        }

        #endregion

        #region TV CONTROLS

        private int currentInput = 1;

        private void Mute(IPEndPoint sender, byte[] parameters)
        {
            ExLink(new byte[] { 0x08, 0x22, 0x02, 0x00, 0x00, 0x00, 0xd4 });
        }

        private void CycleInput(IPEndPoint sender, byte[] parmeters)
        {
            if (currentInput == 1)
            {
                ExLink(new byte[] { 0x08, 0x22, 0x0a, 0x00, 0x05, 0x01, 0xc6 });
                currentInput = 2;
            }
            else
            {
                ExLink(new byte[] { 0x08, 0x22, 0x0a, 0x00, 0x05, 0x00, 0xc7 });
                currentInput = 1;
            }
        }

        private void VolumeDown(IPEndPoint sender, byte[] parmeters)
        {
            ExLink(new byte[] { 0x08, 0x22, 0x01, 0x00, 0x02, 0x00, 0xd3 });
        }
        
        private void VolumeUp(IPEndPoint sender, byte[] parmeters)
        {
            ExLink(new byte[] { 0x08, 0x22, 0x01, 0x00, 0x01, 0x00, 0xd4 });
        }

        #endregion

        #region POWER CONTROLS

        private void Ping(IPEndPoint sender, byte[] parameters)
        {
            UdpClient response = new UdpClient(sender.Address.ToString(), port);
            response.Send(new byte[] { 0x01 }, 1);
        }

        private void Sleep(IPEndPoint sender, byte[] parameters)
        {
            ExLink(new byte[] { 0x08, 0x22, 0x00, 0x00, 0x00, 0x01, 0xd5 });
            Application.SetSuspendState(PowerState.Suspend, true, true);
        }

        private void Wake(IPEndPoint sender, byte[] parmeters)
        {
            ExLink(new byte[] { 0x08, 0x22, 0x00, 0x00, 0x00, 0x02, 0xd4 });
            Thread.Sleep(100);
            currentInput = 2;
            CycleInput(sender, parmeters);
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

        #region IO

        private void ExLink(byte[] code)
        {
            SerialPort exLinkPort = new SerialPort("COM4", 9600, Parity.None, 8, StopBits.One);
            exLinkPort.Open();
            exLinkPort.Write(code, 0, code.Length);
            exLinkPort.Close();
        }

        public void Start()
        {
            UdpClient request = new UdpClient(port);
            IPEndPoint sender = new IPEndPoint(IPAddress.Any, port);
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

        #endregion
    }
}

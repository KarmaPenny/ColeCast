using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

namespace ColeCast
{
    public static class VirtualMouse
    {
        public static void Move(int dx, int dy)
        {
            SendInput(new MouseInput(MouseEvent.MOVE, dx, dy, 0));
        }

        public static void LeftButtonDown()
        {
            SendInput(new MouseInput(MouseEvent.LEFTDOWN, 0, 0, 0));
        }

        public static void LeftButtonUp()
        {
            SendInput(new MouseInput(MouseEvent.LEFTUP, 0, 0, 0));
        }

        public static void RightButtonDown()
        {
            SendInput(new MouseInput(MouseEvent.RIGHTDOWN, 0, 0, 0));
        }

        public static void RightButtonUp()
        {
            SendInput(new MouseInput(MouseEvent.RIGHTUP, 0, 0, 0));
        }

        public static void Scroll(int z)
        {
            SendInput(new MouseInput(MouseEvent.WHEEL, 0, 0, -1 * z));
        }

        public static void HScroll(int z)
        {
            SendInput(new MouseInput(MouseEvent.HWHEEL, 0, 0, z));
        }

        #region NATIVE METHODS

        private static int SendInput(MouseInput input)
        {
            return SendInputs(new MouseInput[1] { input });
        }

        private static int SendInputs(MouseInput[] inputs)
        {
            return SendInput(inputs.Length, inputs, Marshal.SizeOf(typeof(MouseInput)));
        }

        [DllImport("user32.dll", SetLastError = true)]
        private static extern int SendInput(int count, MouseInput[] input, int size);

        [StructLayout(LayoutKind.Sequential, Pack = 1)]
        private struct MouseInput
        {
            int type;
            int x;
            int y;
            int z;
            MouseEvent action;
            int time;
            IntPtr extra;
            public MouseInput(MouseEvent action, int x, int y, int z)
            {
                type = 0;
                this.x = x;
                this.y = y;
                this.z = z;
                this.action = action;
                time = 0;
                extra = IntPtr.Zero;
            }
        }

        [Flags()]
        private enum MouseEvent : int
        {
            MOVE = 0x1,
            LEFTDOWN = 0x2,
            LEFTUP = 0x4,
            RIGHTDOWN = 0x8,
            RIGHTUP = 0x10,
            WHEEL = 0x800,
            HWHEEL = 0x1000
        }

        #endregion
    }
}

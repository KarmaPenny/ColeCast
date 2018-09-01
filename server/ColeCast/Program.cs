using System;
using System.Diagnostics;

namespace ColeCast
{
    class Program
    {
        static void Main(string[] args)
        {
            Process.GetCurrentProcess().PriorityClass = ProcessPriorityClass.High;
            new Receiver().Start();
        }
    }
}

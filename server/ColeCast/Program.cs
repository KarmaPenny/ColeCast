using System;
using System.Collections.Generic;
using System.Linq;
using System.ServiceProcess;
using System.Text;
using System.Threading.Tasks;
using System.IO;

namespace ColeCast
{
    class Program
    {
        static void Main(string[] args)
        {
            try
            {
                new Receiver().Start();
            }
            catch (Exception e)
            {
                File.WriteAllText("c:\\error.txt", e.Message);
            }
        }
    }
}

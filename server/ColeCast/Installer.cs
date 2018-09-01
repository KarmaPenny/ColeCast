using System.Collections;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;

namespace ColeCast
{
    [RunInstaller(true)]
    public partial class Installer : System.Configuration.Install.Installer
    {
        public Installer()
        {
            InitializeComponent();
        }

        public override void Commit(IDictionary savedState)
        {
            base.Commit(savedState);

            // add firewall exception
            string assemblyDir = Path.GetDirectoryName(Context.Parameters["AssemblyPath"]);
            string exePath = Path.Combine(assemblyDir, "ColeCast.exe");
            CreateFirewallException(exePath, "ColeCast");

            // start the process
            Process.Start(exePath);
        }

        public override void Uninstall(IDictionary savedState)
        {
            // remove firewall exception
            string assemblyDir = Path.GetDirectoryName(Context.Parameters["targetdir"]);
            string exePath = Path.Combine(assemblyDir, "ColeCast.exe");
            RemoveFirewallException(exePath);

            // kill the currently active process
            foreach (Process process in Process.GetProcessesByName("ColeCast"))
            {
                if (process.Id != Process.GetCurrentProcess().Id)
                {
                    process.Kill();
                }
            }

            base.Uninstall(savedState);
        }
        
        private static void CreateFirewallException(string exePath, string displayName)
        {
            ProcessStartInfo info = new ProcessStartInfo();
            info.FileName = "netsh";
            info.WindowStyle = ProcessWindowStyle.Hidden;
            info.Arguments = string.Format("firewall add allowedprogram program=\"{0}\" name=\"{1}\" profile=\"ALL\"", exePath, displayName);
            Process proc = Process.Start(info);
            proc.WaitForExit();
        }

        private static void RemoveFirewallException(string exePath)
        {
            ProcessStartInfo info = new ProcessStartInfo();
            info.FileName = "netsh";
            info.WindowStyle = ProcessWindowStyle.Hidden;
            info.Arguments = string.Format("firewall delete rule allowedprogram program=\"{0}\" profile=\"ALL\"", exePath);
            Process proc = Process.Start(info);
            proc.WaitForExit();
        }
    }
}

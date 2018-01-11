using System.Configuration;

namespace ColeCast
{
    public static class Config
    {
        public static int port
        {
            get
            {
                return int.Parse(ConfigurationManager.AppSettings["port"]);
            }
        }

        public static string com
        {
            get
            {
                return ConfigurationManager.AppSettings["com"];
            }
        }
    }
}

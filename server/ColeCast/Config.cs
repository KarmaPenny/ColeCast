using System.Configuration;
using System.Text;
using System.Text.RegularExpressions;

namespace ColeCast
{
    public static class Config
    {
        public static string GetString(string key)
        {
            return ConfigurationManager.AppSettings[key];
        }

        public static int GetInt(string key)
        {
            return int.Parse(GetString(key));
        }

        public static bool GetBool(string key)
        {
            return GetString(key).ToLower() == "true";
        }

        public static byte[] GetBytes(string key)
        {
            string hex = GetString(key).ToLower();
            hex = hex.Replace("\\x", "");
            hex = hex.Replace(" ", "");
            hex = hex.Replace("\t", "");
            return StringToBytes(hex);
        }

        public static byte[] StringToBytes(string hex)
        {
            byte[] arr = new byte[hex.Length >> 1];
            for (int i = 0; i < hex.Length >> 1; ++i)
            {
                arr[i] = (byte)((GetHexVal(hex[i << 1]) << 4) + (GetHexVal(hex[(i << 1) + 1])));
            }
            return arr;
        }

        public static int GetHexVal(char hex)
        {
            int val = (int)hex;
            return val - (val < 58 ? 48 : 87);
        }
    }
}

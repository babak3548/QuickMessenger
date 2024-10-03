using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Reflection;
using System.Security.Cryptography;


namespace AnarSoft.Utility.Utilities
{
    public static class ExtentionMethods
    {
        /// <summary>
        /// Ported from mehrang
        /// </summary>
        /// <param name="obj"></param>
        /// <returns></returns>
        public static string ToPersianString(this string obj)
        {
            if (string.IsNullOrEmpty(obj))
                return string.Empty;
            return obj.ToString();
            //    .Replace((char)1603/*ك*/, (char)1705/*ک*/)
            //                     .Replace((char)1610/*ي*/, (char)1740/*ی*/)
            //                 .Replace('0', '٠')
            //.Replace('1', '١')
            //.Replace('2', '٢')
            //.Replace('3', '٣')
            //.Replace('4', '٤')
            //.Replace('5', '٥')
            //.Replace('6', '٦')
            //.Replace('7', '٧')
            //.Replace('8', '٨')
            //.Replace('9', '٩');
        }

        /// <summary>
        /// Ported from mehrang project
        /// </summary>
        /// <param name="obj"></param>
        /// <returns></returns>
        public static string SavingToPersianString(this string obj)
        {
            if (string.IsNullOrEmpty(obj))
                return string.Empty;
            return obj.ToString().Replace((char)1603/*ك*/, (char)1705/*ک*/)
                                 .Replace((char)1610/*ي*/, (char)1740/*ی*/)
                             .Replace('0', '٠')
            .Replace('1', '١')
            .Replace('2', '٢')
            .Replace('3', '٣')
            .Replace('4', '٤')
            .Replace('5', '٥')
            .Replace('6', '٦')
            .Replace('7', '٧')
            .Replace('8', '٨')
            .Replace('9', '٩');
        }


        /// <summary>
        /// Ported from mehrang project
        /// </summary>
        /// <param name="obj"></param>
        /// <returns></returns>
        public static string ToPersianStringForNumbers(this string obj)
        {
            if (string.IsNullOrEmpty(obj))
                return string.Empty;
            return obj.ToString()
                //.Replace((char)1603/*ك*/, (char)1705/*ک*/)
                //                 .Replace((char)1610/*ي*/, (char)1740/*ی*/)
                             .Replace('0', '٠')
            .Replace('1', '١')
            .Replace('2', '٢')
            .Replace('3', '٣')
            .Replace('4', '٤')
            .Replace('5', '٥')
            .Replace('6', '٦')
            .Replace('7', '٧')
            .Replace('8', '٨')
            .Replace('9', '٩');
        }

        /// <summary>
        /// Ported from mehrang project
        /// </summary>
        /// <param name="obj"></param>
        /// <returns></returns>
        public static string ToPersianStringForEditor(this string obj)
        {
            if (string.IsNullOrEmpty(obj))
                return string.Empty;
            return obj.ToString();
            //.Replace((char)1603/*ك*/, (char)1705/*ک*/)
            //                 .Replace((char)1610/*ي*/, (char)1740/*ی*/);
            //                 .Replace('0', '٠')
            //.Replace('1', '١')
            //.Replace('2', '٢')
            //.Replace('3', '٣')
            //.Replace('4', '٤')
            //.Replace('5', '٥')
            //.Replace('6', '٦')
            //.Replace('7', '٧')
            //.Replace('8', '٨')
            //.Replace('9', '٩');
        }

        /// <summary>
        /// Ported from mehrang project
        /// </summary>
        /// <param name="obj"></param>
        /// <returns></returns>
        public static string SavingToPersianStringForEditor(this string obj)
        {
            if (string.IsNullOrEmpty(obj))
                return string.Empty;
            return obj.ToString().Replace((char)1603/*ك*/, (char)1705/*ک*/)
                                 .Replace((char)1610/*ي*/, (char)1740/*ی*/);
            //                 .Replace('0', '٠')
            //.Replace('1', '١')
            //.Replace('2', '٢')
            //.Replace('3', '٣')
            //.Replace('4', '٤')
            //.Replace('5', '٥')
            //.Replace('6', '٦')
            //.Replace('7', '٧')
            //.Replace('8', '٨')
            //.Replace('9', '٩');
        }


        public static bool ToBoolean(this object o, bool defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.BooleanValue;
        }
        public static byte ToByte(this object o, byte defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.ByteValue;
        }
        public static int ToInteger(this object o, int defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.IntegerValue;
        }
        public static int? ToIntegerDefaultNull(this object o)
        {
            ObjectManager objectManager = new ObjectManager(o, 0);
            if (objectManager.IntegerValue == 0)
                return null;
            else
            return objectManager.IntegerValue;
        }
        public static int? ToNullableInteger(this object o)
        {
            if (o == null) return null;
            ObjectManager objectManager = new ObjectManager(o, 0);
            return objectManager.IntegerValue;
        }
        public static long ToLong(this object o, long defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.LongValue;
        }
        public static Single ToSingle(this object o, Single defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.SingleValue;
        }
        public static float ToFloat(this object o, float defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.FloatValue;
        }
        public static double ToDouble(this object o, double defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.DoubleValue;
        }
        public static decimal ToDecimal(this object o, double defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.DecimalValue;
        }
        public static DateTime ToDateTime(this object o, DateTime defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.DateTimeValue;
        }
        public static Guid ToGuid(this object o, Guid defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.GuidValue;
        }


        public static string ToString(this object o, string defaultValue)
        {
            ObjectManager objectManager = new ObjectManager(o, defaultValue);
            return objectManager.StringValue;
        }

        public static short likeness(this string Value, string campearValue)
        {
            var str = string.Concat(Value, campearValue);
            short ContainCounter = 0;
          //  short ValueWordCount = 0;
            short campearWordCount = 0;
            //var ValueList= Value.Split(' ');
            //var campearList =campearValue.Split(' ');
            foreach (var ValItem in Value.Split(' '))
            {
                campearWordCount = 0;
                foreach (var CmpItem in campearValue.Split(' '))
                {
                    campearWordCount++;
                    if (ValItem.Contains(CmpItem) | CmpItem.Contains(ValItem)) ContainCounter++;
                    if (campearWordCount >= 6) break;
                }
                
            }
            return (short)(((float)ContainCounter /  campearWordCount) * 100);
            // return true;   
        }
        public static string DeliveryCoded(int length)
        {
            string result;
            Random rand = new Random();

           result= rand.Next(1, 9).ToString();
           for (int i = 0; i < length-1; i++)
           {
               result += rand.Next(0, 9).ToString();
           }
           return result; 

        }
      /*  public static string DeliveryCodedUnPerfect(this string s)
        {
            string result="";
            string AChar="";
            int Sum=0;
            int randValue=0;
          Random rand = new Random();
          randValue = rand.Next(-9, 9);
          Sum += randValue * s.Substring(0, 1).ToByte(0);
          result += "(" + randValue.ToString() + "*?)";
            for (int i = 1; i < s.Length; i++)
            {
                // AChar= s.Substring(i, i);
                 randValue= rand.Next(-9,9);
                 Sum+=randValue * s.Substring(i, 1).ToByte(0);
                 result += "+("+randValue.ToString()+"*?)";
            }
            return result+"="+Sum.ToString();
        }
        
        public static string DeliveryCodedUnPerfect(this string s)
        {

            var random = new Random();
            while(s.Count(ch => ch =='*') < 10)
            {
               int rand= random.Next(15);
               s=s.Remove(rand,1);
               s=s.Insert(rand , "*");

            }
        
            return s;
        }*/

        public static int StringDateToInteger(this string d)
        {
            d = d.Replace("/", "");
            return Convert.ToInt32(d);
        }
        public static string decimalToDigMony(this decimal d)
        {
            string r = decimaToDigPrivate(d);

            return r + " ریال";
        }
        public static string decimalToDigMonyWithOutRial(this decimal? d)
        {
            if (!d.HasValue) d = 0;
            string r = decimaToDigPrivate((decimal)d);

            return r ;
        }
        public static string decimalToDigMonyWithOutRial(this decimal d)
        {

            return decimaToDigPrivate((decimal)d);

           
        }
        private static string decimaToDigPrivate(decimal d)
        {
            string r = d.ToString();
            int lenR = r.Length;
            for (int i = lenR - 3; i > 0; i = i - 3)
            {
                r = r.Insert(i, ",");
            }
            return r;
        }
        public static string decimalToDigMony(this decimal? d)
        {
            if (!d.HasValue) d = 0;
            return decimalToDigMony((decimal)d);
        }
        //
        public static string BusinessNameTrue(this string s)
        {

            return s.Trim().ToLower().Replace(" ", "-"); 
        }
        public static string MD5Hash(this string text)
        {
            MD5 md5 = new MD5CryptoServiceProvider();

            //compute hash from the bytes of text
            md5.ComputeHash(ASCIIEncoding.ASCII.GetBytes(text));

            //get hash result after compute it
            byte[] result = md5.Hash;

            StringBuilder strBuilder = new StringBuilder();
            for (int i = 0; i < result.Length; i++)
            {
                //change it into 2 hexadecimal digits
                //for each byte
                strBuilder.Append(result[i].ToString("x2"));
            }

            return (strBuilder.ToString().Length > 20 ? strBuilder.ToString().Substring(0, 20) : strBuilder.ToString());
        }
        public static string GetLenghStr(this string text,int num)
        {
            if (text == null) return "";
            return (text.Length > num ? text.Substring(0, num) : text );
        }
        public static string dateTimeToUtcInMilisecond(this DateTime? dt)
        {
            if (dt == null) dt = DateTime.Now;

            DateTime baseTime = new DateTime(1970, 1, 1, 0, 0, 0);
            string ms = ((DateTime)dt - baseTime).TotalMilliseconds.ToString().Substring(0, 13);
            return ms;
        }

    }
}

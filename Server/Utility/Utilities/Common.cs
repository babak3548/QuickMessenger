using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace AnarSoft.Utility.Utilities
{
   public  class Common
    {


       public static string utcInMilisecond()
       {
           DateTime baseTime = new DateTime(1970, 1, 1, 0, 0, 0);
           string ms = (DateTime.Now - baseTime).TotalMilliseconds.ToString().Substring(0, 13);
           return ms;
       }

        /// <summary>
        /// بر حسب ولیو عددی یک اینوم مقدار استرینق لاتین آن را بر می گردداند
        /// </summary>
        /// <typeparam name="T">نوع اینوم</typeparam>
        /// <param name="value"></param>
        /// <returns></returns>
        public static string EnumShortValueToStringValue<T>(short value)
        {
            var item = Enum.GetValues(typeof(T)).Cast<T>().FirstOrDefault(i => Convert.ToInt16(i) == value);
            return item.ToString();
        }
        /// <summary>
        /// بر حسب سرینق یک اینوم مقدار ولیو آن را بر می گردداند
        /// </summary>
        /// <typeparam name="T">نوع اینوم</typeparam>
        /// <param name="value"></param>
        /// <returns></returns>
        public static byte EnumStringValueToIntValue<T>(string value)
        {
            var item = Enum.GetValues(typeof(T)).Cast<T>().FirstOrDefault(i => i.ToString() == value);
            return Convert.ToByte(item);
        }
        /// <summary>
        /// بر حسب اسرینق یک اینوم مقدار انم آن را بر می گردداند
        /// </summary>
        /// <typeparam name="T">نوع اینوم</typeparam>
        /// <param name="value"></param>
        /// <returns></returns>
        public static T EnumStringValueToEnumValue<T>(string value)
        {
            var item = Enum.GetValues(typeof(T)).Cast<T>().FirstOrDefault(i => i.ToString() == value);
            return item;
        }
        /// مقدار انم را به صورت لاتین در ایتم لیست بر می گرداند
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="selectedValue"></param>
        /// <returns></returns>
        private static List<string> EnumToList<T>(string selectedValue = "")
        {
            List<string> LSelectListItem = new List<string>();

            foreach (var item in Enum.GetValues(typeof(T)).Cast<T>())
            {
                LSelectListItem.Add(item.ToString());
            }
            return LSelectListItem;
        }

        public static System.DateTime PersionToGergorian(List<int> perstionDate)
        {
            System.Globalization.PersianCalendar pg = new System.Globalization.PersianCalendar();
            System.DateTime dat = new System.DateTime(1900, 1, 1);
            List<int> Start = GergorianToPersion(dat);
            dat = pg.AddYears(dat, perstionDate[0] - Start[0]);
            dat = pg.AddMonths(dat, perstionDate[1] - Start[1]);
            dat = pg.AddDays(dat, perstionDate[2] - Start[2]);
            try
            {
                dat = pg.AddHours(dat, perstionDate[3] - Start[3]);
                dat = pg.AddMinutes(dat, perstionDate[4] - Start[4]);
                dat = pg.AddSeconds(dat, perstionDate[5] - Start[5]);
            }
            catch
            {
                dat = pg.AddHours(dat, 12 - Start[3]);
                dat = pg.AddMinutes(dat, 0 - Start[4]);
                dat = pg.AddSeconds(dat, 0 - Start[5]);
            }

            return dat;
        }
        public static List<int> GergorianToPersion(System.DateTime date)
        {
            System.Globalization.PersianCalendar pg = new System.Globalization.PersianCalendar();

            List<int> list = new List<int>();
            list.Add(pg.GetYear(date));
            list.Add(pg.GetMonth(date));
            list.Add(pg.GetDayOfMonth(date));
            list.Add(pg.GetHour(date));
            list.Add(pg.GetMinute(date));
            list.Add(pg.GetSecond(date));
            return list;

        }
        public static string GergorianToPersionString(System.DateTime date)
        {
            var r = GergorianToPersion(date);
            return r[0].ToString("0000") + "/" + r[1].ToString("00") + "/" + r[2].ToString("00");
        }

        public static string GergorianToPersionStringRtl(System.DateTime date)
        {
            var r = GergorianToPersion(date);
            return r[2].ToString("00") + "/" + r[1].ToString("00") + "/" + r[0].ToString("0000");
        }
    }
}

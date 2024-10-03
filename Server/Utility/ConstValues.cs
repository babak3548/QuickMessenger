using AnarSoft.Utility.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Web.Configuration;

namespace Utility
{
    public class ConstValues
    {
        public const string CaptchaImgPath = @"E:\workArea\ShoppingCenters\UILayer\UILayer\Images\captchaImg.gif";//"Images\\captchaImg";

        public static string HostName
        {
            get
            {
                if (WebConfigurationManager.ConnectionStrings["HostName"].ConnectionString == "" | WebConfigurationManager.ConnectionStrings["HostName"].ConnectionString == null)
                    throw new Exception("نام هاست خالی می باشد");
                return WebConfigurationManager.ConnectionStrings["HostName"].ConnectionString;

            }
        }

        public static string DefaultMail
        {
            get
            {
                if (WebConfigurationManager.ConnectionStrings["DefaultMail"].ConnectionString == "" | WebConfigurationManager.ConnectionStrings["DefaultMail"].ConnectionString == null)
                    throw new Exception("نام ایمیل پیش فرض خالی می باشد");
                return WebConfigurationManager.ConnectionStrings["DefaultMail"].ConnectionString;

            }
        }

        public static string DefaultMailYahoo
        {
            get
            {
                if (WebConfigurationManager.ConnectionStrings["DefaultMailYahoo"].ConnectionString == "" | WebConfigurationManager.ConnectionStrings["DefaultMailYahoo"].ConnectionString == null)
                    throw new Exception("نام ایمیل پیش فرض خالی می باشد");
                return WebConfigurationManager.ConnectionStrings["DefaultMailYahoo"].ConnectionString;

            }
        }
        /*
        public static int Port
        {
            get
            {
                if (WebConfigurationManager.ConnectionStrings["Port"].ConnectionString == "" | WebConfigurationManager.ConnectionStrings["Port"].ConnectionString == null)
                    throw new Exception("کد پورت خالی می باشد");
                return WebConfigurationManager.ConnectionStrings["Port"].ConnectionString.ToInteger(0);

            }
        }
        public static bool enableSsl
        {
            get
            {
                if (WebConfigurationManager.ConnectionStrings["enableSsl"].ConnectionString == "" | WebConfigurationManager.ConnectionStrings["enableSsl"].ConnectionString == null)
                    return false;
                    //throw new Exception("کد پورت خالی می باشد");
                    return WebConfigurationManager.ConnectionStrings["enableSsl"].ConnectionString.ToBoolean(false);

            }
        }*/
        public static string UserName
        {
            get
            {
                if (WebConfigurationManager.ConnectionStrings["UserName"] == null)
                    return "";
                else
                    return WebConfigurationManager.ConnectionStrings["UserName"].ConnectionString;

            }
        }

        public static string Password
        {
            get
            {
                if (WebConfigurationManager.ConnectionStrings["Password"] == null)
                    return "";
                else
                    return WebConfigurationManager.ConnectionStrings["Password"].ConnectionString;

            }
        }
    }
}

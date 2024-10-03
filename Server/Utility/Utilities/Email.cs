using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Mail;
using Utility;
using System.Net;
using System.IO;

using System.Globalization;
using System.Text.RegularExpressions;

namespace AnarSoft.Utility.Utilities
{
    public class Email
    {

        MailMessage mail;
        SmtpClient SmtpServer;
        string DefaultMyMail;
        string PathLogFile = "";
        /// <summary>
        /// از نام هاست پیش فرض انار سافت استفاده می کند
        /// </summary>
        public Email()
        {
          
           // mail = new MailMessage();
         //   SmtpServer = new SmtpClient(ConstValues.HostName, ConstValues.Port);//2003-2f83422c8f

            //if (ConstValues.UserName != "")
            //SmtpServer.Credentials = new NetworkCredential(ConstValues.UserName, ConstValues.Password);

            DefaultMyMail = ConstValues.DefaultMail;//"shobefroosh@info.ir";

        }


        public void SendEmailTest(string toEmail, string Subject, string Body)
        {
            MailMessage mail = new MailMessage();
            mail.Subject = Subject;
            mail.Body = Body;

            mail.From = new System.Net.Mail.MailAddress(DefaultMyMail,"شعبه فروش");

            mail.IsBodyHtml =true;
            mail.SubjectEncoding = System.Text.Encoding.UTF8;
            mail.BodyEncoding = System.Text.Encoding.UTF8;
            mail.To.Add(toEmail);

            //emailListTxtToListCc();
            //if (ListEmailCc.Count > 0)
            //{
            //    foreach (var item in ListEmailCc)
            //    {

            //        mail.CC.Add(item);
            //    }
            //}

           // emailListTxtToListBcc();
            //if (ListEmailBcc.Count > 0)
            //{
            //    foreach (var item in ListEmailBcc)
            //    {
            //        mail.Bcc.Add(item);
            //    }
            //}
            SmtpClient smtpClient = new SmtpClient(ConstValues.HostName);
            smtpClient.UseDefaultCredentials = false;
            smtpClient.EnableSsl = false;

            smtpClient.Port = 25;
            smtpClient.Send(mail);
            mail.Dispose();
        }
        /* یا هو کانستراکتور*/
        /*
        <add name="HostName" connectionString="smtp.mail.yahoo.com" providerName="System.Data.SqlClient" />
		<add name="DefaultMail" connectionString="shobefroosh@yahoo.com" providerName="System.Data.SqlClient" />
		<add name="Port" connectionString="587" providerName="System.Data.SqlClient" />
         */
        /*	<!--<smtp deliveryMethod="Network" from="shobefroosh@yahoo.com">
    <network defaultCredentials="false" host="smtp.mail.yahoo.com" password="B@b@k3548" port="587" userName="shobefroosh@yahoo.com" />
    </smtp>-->
        /// <summary>
        /// برای استفاده از میل سرور 2003 هم استفاده می گردد 
        /// برای استفاده در یاهو کاربر دارد
        /// </summary>
        public Email(string pathLogFile)
        {
            PathLogFile = pathLogFile;
            mail = new MailMessage();
            SmtpServer = new SmtpClient(ConstValues.HostName, ConstValues.Port);//2003-2f83422c8f
            //if (ConstValues.UserName != "")
            //SmtpServer.Credentials = new NetworkCredential(ConstValues.UserName, ConstValues.Password);

            DefaultMyMail = ConstValues.DefaultMail;//"shobefroosh@info.ir";

        }*/

///جی میل کانفیق
        /*  <add name="enableSsl" connectionString="true" providerName="System.Data.SqlClient" />
  <add name="DefaultMail" connectionString="shobefroosh@gmail.com" providerName="System.Data.SqlClient" />
  <add name="UserName" connectionString="shobefroosh@gmail.com" providerName="System.Data.SqlClient" />
  <add name="Password" connectionString="B@b@k3548" providerName="System.Data.SqlClient" />
  <add name="Port" connectionString="587" providerName="System.Data.SqlClient" />*/
         /*  <smtp deliveryMethod="Network" from="shobefroosh@gmail.com">
        <network defaultCredentials="false" enableSsl="true"  host="smtp.gmail.com" password="B@b@k3548" port="587" userName="shobefroosh@gmail.com" />
      </smtp>
        /// <summary>
        /// برای استفاده از جی میل طراحی شده است
        ///<add name="HostName" connectionString="smtp.gmail.com" providerName="System.Data.SqlClient" />
        /// </summary>
        /// <param name="pathLogFile"></param>
        /// <param name="isGmail"></param>
        public Email(string pathLogFile,bool isGmail)
        {
            PathLogFile = pathLogFile;
            mail = new MailMessage();
            SmtpServer = new SmtpClient(ConstValues.HostName, ConstValues.Port);//2003-2f83422c8f
            SmtpServer.EnableSsl = ConstValues.enableSsl;//*&
            SmtpServer.Credentials = new NetworkCredential(ConstValues.UserName, ConstValues.Password);
            //if (ConstValues.UserName != "")
            //SmtpServer.Credentials = new NetworkCredential(ConstValues.UserName, ConstValues.Password);

            DefaultMyMail = ConstValues.DefaultMail;//"shobefroosh@info.ir";

        }
*/
/*
        /// <summary>
        ///این سازنده براساس نوع ایمیل هاست ارسال کننده را انتخاب و کانفیفق می نمایید 
        ///وبرای تک ارسالی ها طراحی شده است
        ///<add name="HostName" connectionString="smtp.gmail.com" providerName="System.Data.SqlClient" />
        /// </summary>
        /// <param name="pathLogFile"></param>
        public Email(string pathLogFile, string ToEmailAddress)
        {
            if(IsYahoo(ToEmailAddress))
            PathLogFile = pathLogFile;
            mail = new MailMessage();
            SmtpServer = new SmtpClient(ConstValues.HostName, ConstValues.Port);//2003-2f83422c8f
            SmtpServer.EnableSsl = ConstValues.enableSsl;//*&
            SmtpServer.Credentials = new NetworkCredential(ConstValues.UserName, ConstValues.Password);

            DefaultMyMail = ConstValues.DefaultMail;

        }*/
        /// <summary>
        /// نام هاست بالید پاس داده شود
        /// </summary>
        /// <param name="SmtpHostnameCom"></param>
        public Email(object SmtpHostnameCom)
        {
            mail = new MailMessage();
            SmtpServer = new SmtpClient(SmtpHostnameCom.ToString());
        }

        /// <summary>
        /// ارسال ایمیل تکی
        /// </summary>
        /// <param name="FromEmailAddress"></param>
        /// <param name="ToEmailAddress"></param>
        /// <param name="Subject"></param>
        /// <param name="Body"></param>
        public void SendAEmail(string ToEmailAddress, string Subject, string Body)
        {
            try
            {
               Body= Body.Replace("\r\n", "<br />");
                mail.To.Clear();
                mail.To.Add(ToEmailAddress);
                if (IsYahoo(ToEmailAddress))
                { SendEmail(ConstValues.DefaultMailYahoo, Subject, Body); }
                else
                { SendEmail(ConstValues.DefaultMail, Subject, Body); }
            }
            catch (Exception ex)
            {

            }

        }

        /// <summary>
        /// ارسال ایمیل گروهی
        /// </summary>
        /// <param name="FromEmailAddress"></param>
        /// <param name="ListToEmailAddress"></param>
        /// <param name="Subject"></param>
        /// <param name="Body"></param>
        public void SendGroupEmail(List<string> ListToEmailAddress, string Subject, string Body)
        {
            try
            {
                mail.To.Clear();
                foreach (var ToEmailAddress in ListToEmailAddress)
                {
                    mail.Bcc.Add(ToEmailAddress);
                }

                SendEmail(DefaultMyMail, Subject, Body);
            }
            catch (Exception ex)
            {

                if (PathLogFile != null && PathLogFile != "")
                {
                    File.AppendAllText(PathLogFile, "\n ************************Email Send Exception************" + DateTime.Now.ToString() + "************ \n"
                    + ex.ToString(), Encoding.UTF8);
                }
            }

        }

        /// <summary>
        /// متد ارسال
        /// </summary>
        /// <param name="FromEmailAddress"></param>
        /// <param name="Subject"></param>
        /// <param name="Body"></param>
        private void SendEmail(string FromEmailAddress, string Subject, string Body)
        {
            var emailAdress = new MailAddress(FromEmailAddress);

            mail.From = emailAdress;
            mail.Sender = emailAdress;
            mail.Subject = Subject;
            mail.Body = Body;
            mail.BodyEncoding = Encoding.UTF8;
            mail.IsBodyHtml = true;
            SmtpServer.Send(mail);
        }

        //public static bool IsValid(string emailaddress)//&*
        //{
        //    try
        //    {
        //        MailAddress m = new MailAddress(emailaddress);
        //        return true;
        //    }
        //    catch (Exception ex)
        //    {
        //        return false;
        //    }
        //}


         bool invalid = false;

        public   bool IsValidEmail(string strIn)
        {
            invalid = false;
            if (String.IsNullOrEmpty(strIn))
                return false;

            // Use IdnMapping class to convert Unicode domain names. 
            try
            {
                strIn = Regex.Replace(strIn, @"(@)(.+)$", this.DomainMapper,
                                      RegexOptions.None);
            }
            catch (Exception)
            {
                return false;
            }

            if (invalid)
                return false;

            // Return true if strIn is in valid e-mail format. 
            try
            {
                return Regex.IsMatch(strIn,
                      @"^(?("")(""[^""]+?""@)|(([0-9a-z]((\.(?!\.))|[-!#\$%&'\*\+/=\?\^`\{\}\|~\w])*)(?<=[0-9a-z])@))" +
                      @"(?(\[)(\[(\d{1,3}\.){3}\d{1,3}\])|(([0-9a-z][-\w]*[0-9a-z]*\.)+[a-z0-9]{2,24}))$",
                      RegexOptions.IgnoreCase);
            }
            catch (Exception)
            {
                return false;
            }
        }

        private  string DomainMapper(Match match)
        {
            // IdnMapping class with default property values.
            IdnMapping idn = new IdnMapping();

            string domainName = match.Groups[2].Value;
            try
            {
                domainName = idn.GetAscii(domainName);
            }
            catch (ArgumentException)
            {
                invalid = true;
            }
            return match.Groups[1].Value + domainName;
        }

        private bool IsYahoo(string emailAdressTrimed)
        {
            string[] hostNameArr = emailAdressTrimed.Split('@');
            string hostName = "";
            string[] hostNameArr1;
            if (hostNameArr.Count() >= 2)
            {
                hostNameArr1 = hostNameArr[1].Split('.');
                hostName = hostNameArr1[0];
            }

            if (hostName.ToLower() == "yahoo") return true;
            else return false;

        }
    }
}

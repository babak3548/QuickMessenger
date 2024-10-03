/****************************************************************
 * This work is original work authored by Craig Baird, released *
 * under the Code Project Open Licence (CPOL) 1.02;             *
 * http://www.codeproject.com/info/cpol10.aspx                  *
 * This work is provided as is, no guarentees are made as to    *
 * suitability of this work for any specific purpose, use it at *
 * your own risk.                                               *
 * This product is not intended for use in any form except      *
 * learning. The author recommends only using small sections of *
 * code from this project when integrating the attacked         *
 * TcpServer project into your own project.                     *
 * This product is not intended for use for any comercial       *
 * purposes, however it may be used for such purposes.          *
 ****************************************************************/

using System;
using System.Drawing;
using System.IO;
using System.Net.Sockets;
using System.Text;
using System.Windows.Forms;
using Toopeto.JsonPacket;
using AnarSoft.Utility.Utilities;
using tcpServer;
using System.Linq;
using System.Collections.Generic;
using System.Threading;
namespace Toopeto
{
    public partial class Manager : Form
    {
        public delegate void invokeDelegate();
        private static Object thisLock = new Object();
        public Manager()
        {
            InitializeComponent();
        }

        private void btnChangePort_Click(object sender, EventArgs e)
        {
            try
            {
                openTcpPort();
            }
            catch (FormatException)
            {
                MessageBox.Show("Port must be an integer", "Invalid Port", MessageBoxButtons.OK, MessageBoxIcon.Error, MessageBoxDefaultButton.Button1);
            }
            catch (OverflowException)
            {
                MessageBox.Show("Port is too large", "Invalid Port", MessageBoxButtons.OK, MessageBoxIcon.Error, MessageBoxDefaultButton.Button1);
            }
        }

        private void openTcpPort()
        {
            tcpServer1.Close();
            tcpServer1.setEncoding(new UTF8Encoding(true, true), true);//???
            tcpServer1.Port = Convert.ToInt32(txtPort.Text);
            txtPort.Text = tcpServer1.Port.ToString();
            tcpServer1.Open();

            displayTcpServerStatus();
        }

        private void displayTcpServerStatus()
        {
            if (tcpServer1.IsOpen)
            {
                lblStatus.Text = "PORT OPEN";
                lblStatus.BackColor = Color.Lime;
            }
            else
            {
                lblStatus.Text = "PORT NOT OPEN";
                lblStatus.BackColor = Color.Red;
            }
        }

        private void btnSend_Click(object sender, EventArgs e)
        {
            send();
        }

        private void send()
        {
            string data = "";

            foreach (string line in txtText.Lines)
            {
                data = data + line.Replace("\r", "").Replace("\n", "") + "\r\n";
            }
            data = data.Substring(0, data.Length - 2);

            tcpServer1.Send(data);

            logData(true, data);
        }

        public void logData(bool sent, string text)
        {
            txtLog.Text += "\r\n" + DateTime.Now.ToString("yyyy/MM/dd HH:mm:GetOfflineMessageUser tt") + (sent ? " SENT:\r\n" : " RECEIVED:\r\n");
            txtLog.Text += text;
            txtLog.Text += "\r\n";
            if (txtLog.Lines.Length > 500)
            {
                string[] temp = new string[500];
                Array.Copy(txtLog.Lines, txtLog.Lines.Length - 500, temp, 0, 500);
                txtLog.Lines = temp;
            }
            txtLog.SelectionStart = txtLog.Text.Length;
            txtLog.ScrollToCaret();
        }

        private void btnClose_Click(object sender, EventArgs e)
        {
            Close();
        }

        private void frmMain_FormClosed(object sender, FormClosedEventArgs e)
        {
            tcpServer1.Close();
        }

        private void frmMain_Load(object sender, EventArgs e)
        {
            btnChangePort_Click(null, null);

            timer1.Enabled = true;
        }

        //خواندن استریم از سوکت در این قسمت انجام می گیره
        protected byte[] readStream(TcpClient client)
        {
            NetworkStream stream = client.GetStream();
            if (stream.DataAvailable)
            {
                byte[] data = new byte[client.Available];

                int bytesRead = 0;
                try
                {
                    bytesRead = stream.Read(data, 0, data.Length);
                }
                catch (IOException)
                {
                }

                if (bytesRead < data.Length)
                {
                    byte[] lastData = data;
                    data = new byte[bytesRead];
                    Array.ConstrainedCopy(lastData, 0, data, 0, bytesRead);
                }
                return data;
            }
            return null;
        }


        private void timer1_Tick(object sender, EventArgs e)
        {
            displayTcpServerStatus();
            lblConnected.Text = tcpServer1.Connections.Count.ToString();
        }

        private void txtIdleTime_TextChanged(object sender, EventArgs e)
        {
            try
            {
                int time = Convert.ToInt32(txtIdleTime.Text);
                tcpServer1.IdleTime = time;
            }
            catch (FormatException) { }
            catch (OverflowException) { }
        }

        private void txtMaxThreads_TextChanged(object sender, EventArgs e)
        {
            try
            {
                int threads = Convert.ToInt32(txtMaxThreads.Text);
                tcpServer1.MaxCallbackThreads = threads;
            }
            catch (FormatException) { }
            catch (OverflowException) { }
        }

        private void txtAttempts_TextChanged(object sender, EventArgs e)
        {
            try
            {
                int attempts = Convert.ToInt32(txtAttempts.Text);
                tcpServer1.MaxSendAttempts = attempts;
            }
            catch (FormatException) { }
            catch (OverflowException) { }
        }

        private void frmMain_FormClosing(object sender, FormClosingEventArgs e)
        {
            timer1.Enabled = false;
        }

        private void txtValidateInterval_TextChanged(object sender, EventArgs e)
        {
            try
            {
                int interval = Convert.ToInt32(txtValidateInterval.Text);
                tcpServer1.VerifyConnectionInterval = interval;
            }
            catch (FormatException) { }
            catch (OverflowException) { }
        }

        string a = " { 'k2': {  'mk1': 'mv1',   'mk2': [  'lv1', 'lv2' ]  }  }\n";
        private void tcpServer1_OnConnect(tcpServer.TcpServerConnection connection)
        {
            MessagePacket messagePacket = new MessagePacket();
            messagePacket.to = messagePacket.from;
            messagePacket.from = Setting.fromServerToopeto;
   
            messagePacket.type = MessageTypeEnum.ack.ToString();
            messagePacket.data = "you connect";

            connection.sendData(messagePacket.getString());
            Console.WriteLine("کانکت کاربر");
            //do satre payin bayad hazf gadand
            invokeDelegate setText = () => lblConnected.Text = tcpServer1.Connections.Count.ToString();

            Invoke(setText);
        }

        private static MessagePacket AckResponse(Json json)
        {
            MessagePacket messagePacket = new MessagePacket();
            messagePacket.id = json.id;
            messagePacket.to = json.from;
            messagePacket.from = Setting.fromServerToopeto;
            messagePacket.type = MessageTypeEnum.ack.ToString();
            messagePacket.data = "you connect";
            return messagePacket;
        }

        private void tcpServer1_OnDataAvailable(tcpServer.TcpServerConnection connection)
        {
            byte[] data = readStream(connection.Socket);

            if (data != null)
            {
                // connection.sendData(a);
                string dataStr = Encoding.UTF8.GetString(data); //Encoding.ASCII.GetString(data);
                string[] packets = dataStr.Split('\n');
                foreach (var packet in packets)
                {
                    if (!string.IsNullOrWhiteSpace(packet))
                    {
                        ReceiverMessage(packet, connection);
                    }
                }

                /*
                                invokeDelegate del = () =>
                                {
                                    logData(false, dataStr);
              
                                };
                                Invoke(del);
                                */
                data = null;
            }
        }
        public void ReceiverMessageForTest(string jsonString, tcpServer.TcpServerConnection connection)
        {

        }
        /// <summary>
        ///  این متد اصلی تشخیص نوع عملیات بر اساس نوع پیام ارسال شده می باشد
        /// </summary>
        /// <param name="jsonString"></param>
        /// <param name="connection"></param>
        private void ReceiverMessage(string jsonString, tcpServer.TcpServerConnection connection)
        {

            try
            {
                // Contact c = new Contact();

                Json json;
                json = Json.GetJson(jsonString);
                //گرفتن لاگ در سیستم
                loging(json);
                //اگر کاربر لاگین نکرده بود 
                if (string.IsNullOrWhiteSpace(connection.User_Name)  && json.type != UserTypeEnum.registerUser.ToString())
                {
                   bool res= PresenceUser(json, connection);
                   if (!res)
                   {
                       //در صورتی که ریجیستر موفق نبود خارج شو
                       return;     
                   }
                }
              if (json.packetName == PacketNameEnum.user.ToString())
                {
                   /* if (json.type == UserTypeEnum.presence.ToString())
                    {
                        //کاربر اعلام حضور و لاگین می کند
                        PresenceUser(json, connection);
                    }
                    else */
                        if (json.type == UserTypeEnum.registerUser.ToString())
                    {
                        //درخواست ایجاد اکانت از کاربر
                        RegisterUser(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.changePassword.ToString())
                    {
                        //کاربر در خواست تغییر پسورد کرده است
                        ChangePassword(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.deleteUser.ToString())
                    {
                        //کاربر در خواست حذف کرده است
                        DeleteUser(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.createGroup.ToString())
                    {
                        //ایجاد یک گروه 
                        createGroup(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.addToGroup.ToString())
                    {
                        //کاربر درخواست اضافه شدن به گروه را می کند
                        addToGroup(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.acceptToGroup.ToString())
                    {
                        // ادمین گروه کاربر را به گروه می پزیرد
                        acceptToGroup(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.getUser.ToString())
                    {
                        //کاربر درخواست اضافه شدن به گروه را می کند
                        //و در این مرحله بدون اجازه ادمین گروه ، مستقیما به گروه اضافه می گردد
                        getUser(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.enterToChatRoom.ToString())
                    {
                        enterToChatRoom(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.exitFromChatRoom.ToString())
                    {
                        exitFromChatRoom(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.addToMyGroup.ToString())
                    {
                        addToMyGroup(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.getAvatar.ToString())
                    {
                        getAvatarContact(jsonString, connection);
                    }
                    else if (json.type == UserTypeEnum.likeCounterUser.ToString())
                    {
                        getCountLikeUser(jsonString, connection);
                    }
                }//end user if
                else if (json.packetName == PacketNameEnum.message.ToString())
                {
                    if (json.type == MessageTypeEnum.ping.ToString())
                    {
                        //میسج پینگ به سرور و پاسخ اک سرور به کلاینت
                        connection.sendData(AckResponse(json).getString());
                    }
                    else if (json.groupType == GroupType.TYPE_MUC.ToString() || json.groupType == GroupType.TYPE_SINGLE.ToString() &&
                        (json.type == MessageTypeEnum.text.ToString() || json.type == MessageTypeEnum.image.ToString()
                        || json.type == MessageTypeEnum.video.ToString() || json.type == MessageTypeEnum.file.ToString()))
                    {
                        //درخواست ارسال پیام
                        sendMessageToGroup(jsonString, connection);
                    }
                    else if (json.groupType == GroupType.CHAT_ROOM.ToString() &&
        (json.type == MessageTypeEnum.text.ToString() || json.type == MessageTypeEnum.image.ToString()
        || json.type == MessageTypeEnum.video.ToString() || json.type == MessageTypeEnum.file.ToString()))
                    {
                        //درخواست ارسال پیام
                        sendMessageToChatRoom(jsonString);
                    }
                    else if (json.groupType == GroupType.TIME_LINE.ToString() &&
(json.type == MessageTypeEnum.text.ToString() || json.type == MessageTypeEnum.image.ToString()
|| json.type == MessageTypeEnum.video.ToString() || json.type == MessageTypeEnum.file.ToString()))
                    {
                        MessageService messageService = new MessageService();
                        messageService.AddOfflineJsonString(jsonString, json, Setting.isTimeLine);
                    }

                    else if (json.type == MessageTypeEnum.profileImage.ToString())
                    {
                        ////پینگ از طریق دیگر کاربران
                        saveProfileImage(jsonString, connection);//

                    }
                    if (json.type == MessageTypeEnum.like.ToString())
                    {
                        addLike(jsonString);
                    }

                }//end message if
                else if (json.packetName == PacketNameEnum.listTransfer.ToString())
                {
                    if (json.type == ListTransferEnum.getContact.ToString())
                    {
                        //لیست کانتکت ها را درخواست می کند
                        sendListContact(jsonString, connection);
                    }
                    else if (json.type == ListTransferEnum.getChatRooms.ToString())
                    {
                        //لیست رومهای را درخواست می کند
                        sendChatRooms(jsonString, connection);//
                    }
                    else if (json.type == ListTransferEnum.getChatRoomsChild.ToString())
                    {
                        //لیست رومهای را درخواست می کند
                        sendChatRoomsChild(jsonString, connection);//
                    }
                    else if (json.type == ListTransferEnum.requestTimeLine.ToString())
                    {
                        sendTimeLine(jsonString, connection);
                    }
                    else if (json.type == ListTransferEnum.getNearbyContacts.ToString())
                    {
                        sendNearbyContacts(jsonString, connection);
                    }
                    else if (json.type == ListTransferEnum.getGroup.ToString())
                    {
                        sendListGroupsUser(jsonString, connection);
                    }
                    else if (json.type == ListTransferEnum.getUsersChatRoom.ToString())
                    {
                        sendUsersChatRoom(jsonString, connection);
                    }
                    else if (json.type == ListTransferEnum.getUsersGroup.ToString())
                    {
                        sendUsersGroup(jsonString, connection);
                    }
                }
                else
                {
                    // خطا خطا پکیج کنترل نشده ای  دریافت شده است
                    throw new System.NotImplementedException();
                }

            }
            catch (Exception e)
            {
                try
                {
                    lock (thisLock)
                    {
                        System.IO.File.AppendAllText("ReceiverMessageException" + LoggingScenario.getDate() + ".txt", DateTime.Now + " " + e.Message
                        + " jsonPacket : " + jsonString + " \r\n");
                    }
                }
                catch (Exception){}
            }

        }

        private void getCountLikeUser(string jsonString, TcpServerConnection connection)
        {
            UserPacket userPacket = UserPacket.GetUserPacket(jsonString);
            UserService userService = new UserService();
            userPacket.description = userService.countLikeUser(userPacket.userName).ToString();
            userPacket.to = userPacket.from;
            userPacket.from = Setting.fromServerToopeto;
            connection.sendData(userPacket.getString());
        }

        private static void addLike(string jsonString)
        {
            MessageService messageService = new MessageService();
            MessagePacket messagePacket = MessagePacket.GetMessagePacket(jsonString);
            //az field date be sorat gharardai be onvan meghdar like estefade shode ast
            messageService.addLike(messagePacket.id, messagePacket.from, messagePacket.date);
        }

        private void getAvatarContact(string jsonString, TcpServerConnection connection)
        {
            UserPacket userPacket = UserPacket.GetUserPacket(jsonString);
            UserService userService = new UserService();
            userPacket.avatar = userService.getAvatarContact(userPacket.userName);
            userPacket.to = userPacket.from;
            userPacket.from = Setting.fromServerToopeto;
            connection.sendData(userPacket.getString());
        }

        private void saveProfileImage(string jsonString, TcpServerConnection connection)
        {
            MessagePacket messagePacket = MessagePacket.GetMessagePacket(jsonString);
            UserService userService = new UserService();
            string result = userService.updateProfileImage(messagePacket.from, messagePacket.data);
            messagePacket.to = messagePacket.from;
            messagePacket.from = Setting.fromServerToopeto;
            messagePacket.type = result;
            connection.sendData(messagePacket.getString());
        }
        /// <summary>
        /// ادمین گروه یک کاربر را به گروه خود اضافه می کند
        /// </summary>
        /// <param name="jsonString"></param>
        /// <param name="connection"></param>
        private void addToMyGroup(string jsonString, TcpServerConnection connection)
        {
            GroupService groupService = new GroupService();
            JsonPacket.UserPacket acceptGroup = JsonPacket.UserPacket.GetUserPacket(jsonString);
            string from = acceptGroup.from;
            //اضافه کردن به گروه
            UserTypeEnum result = groupService.AddContact(acceptGroup.userName, acceptGroup.groupName, LevelAccessToGroup.AcceptFriend);
            acceptGroup.type = result.ToString();
            acceptGroup.from = Setting.fromServerToopeto;
            acceptGroup.to = from;
            //ارسال نتیجه به ادمین اضافه کننده
            connection.sendData(acceptGroup.getString());

            UserPacket userPacketAcceptFreind = new UserPacket();
            userPacketAcceptFreind.id = Guid.NewGuid().ToString();
            userPacketAcceptFreind.type = UserTypeEnum.addToMyGroup.ToString();
            userPacketAcceptFreind.userName = acceptGroup.userName;
            userPacketAcceptFreind.to = acceptGroup.userName;
            userPacketAcceptFreind.from = from;
            userPacketAcceptFreind.date = Common.utcInMilisecond();
            userPacketAcceptFreind.groupName = acceptGroup.groupName;
            userPacketAcceptFreind.groupType = acceptGroup.groupType;
            //خبر دادن به کاربر مقصد
            sendData(userPacketAcceptFreind.to, userPacketAcceptFreind.getString(), connection, userPacketAcceptFreind);
        }

        private void sendNearbyContacts(string jsonString, TcpServerConnection connection)
        {
            ListTransferPacket listTransferPacket = ListTransferPacket.GetListTransferPacket(jsonString);
            UserService userService = new UserService();

            ListTransferPacket listTransferPacketReply = ListTransferPacket.GetListTransferPacket(jsonString);
            listTransferPacketReply.id = listTransferPacket.id;
            listTransferPacketReply.from = Setting.fromServerToopeto;
            listTransferPacketReply.to = listTransferPacket.from;
            listTransferPacketReply.date = Common.utcInMilisecond();
            listTransferPacketReply.type = Toopeto.JsonPacket.ListTransferEnum.getNearbyContacts.ToString();
            List<string> contactList = new List<string>();//getLatitude() + "," + location.getLongitude

            IEnumerable<Contact> timeLinesTotal = userService.getNearbyContacts(listTransferPacket.description
                , listTransferPacket.from, "");
            IEnumerable<Contact> sendingTimeLines = timeLinesTotal.Skip(listTransferPacket.currentNumber).Take(Setting.pageSize);

            foreach (Contact contact in sendingTimeLines)
            {
                if (listTransferPacket.from != contact.userName)
                {
                    UserPacket userPacket = new UserPacket();
                    userPacket.avatar = contact.avatarUrl;
                    userPacket.userName = contact.userName;
                    contactList.Add(userPacket.getString());
                }
            }
            listTransferPacketReply.count = timeLinesTotal.Count();
            listTransferPacketReply.currentNumber = listTransferPacket.currentNumber + contactList.Count;
            listTransferPacketReply.list = new List<string>();

            listTransferPacketReply.list = contactList;
            connection.sendData(listTransferPacketReply.getString());
        }

        private void sendTimeLine(string jsonString, TcpServerConnection connection)
        {
            ListTransferPacket listTransferPacket = ListTransferPacket.GetListTransferPacket(jsonString);
            MessageService messageService = new MessageService();
            ListTransferPacket listTransferPacketReply = ListTransferPacket.GetListTransferPacket(jsonString);
            string from=listTransferPacketReply.from ;
            string likeValue="";
            listTransferPacketReply.id = listTransferPacket.id;
            listTransferPacketReply.from = Setting.fromServerToopeto;
            listTransferPacketReply.to = from;
            listTransferPacketReply.date = Common.utcInMilisecond();
            listTransferPacketReply.type = Toopeto.JsonPacket.ListTransferEnum.requestTimeLine.ToString();
            List<string> messageList = new List<string>();
            IEnumerable<Message> timeLinesTotal = messageService.GetOfflineMessageGroup(listTransferPacket.groupName);
            IEnumerable<Message> sendingTimeLines = timeLinesTotal.Skip(listTransferPacket.currentNumber).Take(Setting.pageSize);
            foreach (Message message in sendingTimeLines)
            {
                MessagePacket mp = MessagePacket.GetMessagePacket(message.text);
                mp.description = messageService.CountLikeMessage(message.id, from, ref likeValue).ToString();//  message.likeCounter.ToInteger(0).ToString();
                mp.date = likeValue;//کاربر درخواست دهنده لایک کرده یا نه
                messageList.Add(mp.getString());
            }
            listTransferPacketReply.count = timeLinesTotal.Count();
            listTransferPacketReply.currentNumber = listTransferPacket.currentNumber + messageList.Count;
            listTransferPacketReply.list = new List<string>();
            listTransferPacketReply.list = messageList;
            connection.sendData(listTransferPacketReply.getString());
        }

        private void exitFromChatRoom(string jsonString, TcpServerConnection connection)
        {
            connection.chatroomName = "";
            UserPacket userPacket = UserPacket.GetUserPacket(jsonString);
            
            userPacket.type = UserTypeEnum.result.ToString();
            string from = userPacket.from;
            userPacket.to = userPacket.from;
            userPacket.from = Setting.fromServerToopeto;
            connection.sendData(userPacket.getString());

            MessagePacket messagePacket = new MessagePacket();
            messagePacket.from = from;
            messagePacket.to = Setting.fromServerToopeto;
            messagePacket.groupName = userPacket.groupName;
            messagePacket.groupType = userPacket.groupType;
            messagePacket.type = MessageTypeEnum.text.ToString();
            messagePacket.data = "از چت روم خارج شد";
            sendMessageToChatRoom(messagePacket);
        }

        private void sendMessageToChatRoom(string jsonString)
        {
            Toopeto.JsonPacket.MessagePacket messagePacket = Toopeto.JsonPacket.MessagePacket.GetMessagePacket(jsonString);

            sendMessageToChatRoom(messagePacket);

        }

        private void sendMessageToChatRoom(Toopeto.JsonPacket.MessagePacket messagePacket)
        {
            messagePacket.date = Common.utcInMilisecond();

            //ارسال پیام به اعضای چت روم
            foreach (TcpServerConnection con in tcpServer1.Connections.Where(c => c.chatroomName == messagePacket.groupName))
            {
                con.sendData(messagePacket.getString());
            }
        }

        private void sendUsersChatRoom(string jsonString ,TcpServerConnection connection)
        {
            Toopeto.JsonPacket.ListTransferPacket listTransferPacket = Toopeto.JsonPacket.ListTransferPacket.GetListTransferPacket(jsonString);
            string userName = listTransferPacket.from;
            listTransferPacket.to = userName;
            listTransferPacket.from = Setting.fromServerToopeto;
            List<string> listUsers = new List<string>();
            //ارسال پیام به اعضای چت روم
            foreach (TcpServerConnection con in tcpServer1.Connections.Where(c => c.chatroomName == listTransferPacket.groupName && 
                c.User_Name!=userName))
            {
                listUsers.Add(con.User_Name);

            }
            listTransferPacket.list = listUsers;
            connection.sendData(listTransferPacket.getString());
        }
        private void sendUsersGroup(string jsonString, TcpServerConnection connection)
        {
            UserService userService=new UserService();
            Toopeto.JsonPacket.ListTransferPacket listTransferPacket = Toopeto.JsonPacket.ListTransferPacket.GetListTransferPacket(jsonString);
            string userName = listTransferPacket.from;
            listTransferPacket.to = userName;
            listTransferPacket.from = Setting.fromServerToopeto;
            List<string> listUsers = new List<string>();
            //ارسال پیام به اعضای چت روم
            foreach (var group_contact in userService.getContactsGroup(listTransferPacket.groupName))
            {
                if (userName != group_contact.Fk_Contact)//خود درخواست کننده رو ارسال نمی کنه
                {
                    listUsers.Add(group_contact.Fk_Contact); 
                }
                
            }
            listTransferPacket.list = listUsers;
            connection.sendData(listTransferPacket.getString());
        }
        private void enterToChatRoom(string jsonString, TcpServerConnection connection)
        {
            UserPacket userPacket = UserPacket.GetUserPacket(jsonString);
            int counterUserChatRoom = 0;
            //تعداد کاربران آنلاین گروه را میشمارد
            counterUserChatRoom = tcpServer1.Connections.Where(c => c.chatroomName == userPacket.groupName).Count();

            string from = userPacket.from;
            userPacket.to = from;
            userPacket.from = Setting.fromServerToopeto;
            //اگر ظرفیت خالی داشت کاربر وارد می شود وگر نه ، وارد نمی شود
            if (counterUserChatRoom < Setting.maxUserInChatroom)
            {
                connection.chatroomName = userPacket.groupName;
                userPacket.type = UserTypeEnum.result.ToString();
                //ارسال نتیجه وصل شدن به کاربر
                connection.sendData(userPacket.getString());
                //Thread.Sleep(50);
                //sleep
                MessagePacket messagePacket = new MessagePacket();
                messagePacket.from = from;
                messagePacket.to = Setting.fromServerToopeto;
                messagePacket.groupName = userPacket.groupName;
                messagePacket.groupType = userPacket.groupType;
                messagePacket.type = MessageTypeEnum.text.ToString();
                messagePacket.data =  " به چت روم وارد شد";

                sendMessageToChatRoom(messagePacket.getString());
            }
            else
            {
                userPacket.type = UserTypeEnum.error.ToString();
                connection.sendData(userPacket.getString());
            }


        }

        private void deleteIdelConnections()
        {

            throw new NotImplementedException();
        }
        /// <summary>
        /// آیا این کاربر وجود دارد
        /// </summary>
        /// <param name="jsonString"></param>
        /// <param name="connection"></param>
        private void getUser(string jsonString, TcpServerConnection connection)
        {

            UserService userService = new UserService();
            JsonPacket.UserPacket userPacket = JsonPacket.UserPacket.GetUserPacket(jsonString);

            Contact contact = userService.getUserByUserName(userPacket.userName);
            if (contact != null)
            {
                userPacket.type = UserTypeEnum.result.ToString();
                userPacket.avatar = contact.avatarUrl;
            }
            else
            {
                userPacket.type = UserTypeEnum.error.ToString();
            }
            userPacket.to = userPacket.from;
            userPacket.from = Setting.fromServerToopeto;
            connection.sendData(userPacket.getString());

        }
        /// <summary>
        /// یک کاربر درخواست وصل شدن به گروه را دارد و در این مرحله بدون اجازه ادمین گروه به گروه وصل می شود
        /// </summary>
        /// <param name="jsonString"></param>
        /// <param name="connection"></param>
        private void addToGroup(string jsonString, TcpServerConnection connection)
        {
            GroupService groupService = new GroupService();
            JsonPacket.UserPacket addGroup = JsonPacket.UserPacket.GetUserPacket(jsonString);
            //درخواست کاربر مبنی بر اضافه شدن به گروه پذیرفته می شود
            UserTypeEnum result = groupService.AddContact(addGroup.from, addGroup.groupName, LevelAccessToGroup.requestFriend);

            addGroup.type = result.ToString();
            addGroup.to = addGroup.from;
            addGroup.from = Setting.fromServerToopeto;
            connection.sendData(addGroup.getString());
        }

        /// <summary>
        /// آدمین گروه درخواست اضافه شدن کاربر به گروه را می پذیرد
        /// </summary>
        /// <param name="jsonString"></param>
        /// <param name="connection"></param>
        private void acceptToGroup(string jsonString, TcpServerConnection connection)
        {
            GroupService groupService = new GroupService();
            JsonPacket.UserPacket acceptGroup = JsonPacket.UserPacket.GetUserPacket(jsonString);
            UserTypeEnum result = groupService.AcceptContactInGroup(acceptGroup.from, acceptGroup.to, acceptGroup.groupName);
            acceptGroup.type = result.ToString();
            acceptGroup.from = Setting.fromServerToopeto;
            sendData(acceptGroup.to, acceptGroup.getString(), connection, acceptGroup);
            //connection.sendData(acceptGroup.getString());
        }

        private void sendData(string contactTo, string jsonPacket, TcpServerConnection connection, Json json)
        {
            TcpServerConnection tcpServerConnection = getConnectionUser(contactTo);
          //  Json json = Json.GetJson(jsonPacket);
            if (tcpServerConnection != null)
            {
                tcpServerConnection.sendData(jsonPacket);
                notiPartnearOnOrOff(contactTo, connection, json, UserTypeEnum.userOn);
            }
            else
            {//کاربر مورد نظر در دسترس نیست
                MessageService messageService = new MessageService();
                messageService.AddOfflineJsonString(jsonPacket, json, contactTo);
                //این قسمت زمان اعمال در کلاینت فعال گردد
                //این قسمت به فرستنده خبر می دهد که کاربر هدف شما آنلاین نیست
                notiPartnearOnOrOff(contactTo, connection, json, UserTypeEnum.userOff);
            }
        }

        private void notiPartnearOnOrOff(string contactTo, TcpServerConnection connection, Json json, UserTypeEnum resultType)
        {
            UserPacket up = new UserPacket();
            up.id = json.id;
            up.to = json.from;
            up.from = Setting.fromServerToopeto;
            up.type = resultType.ToString();
            up.userName = contactTo;
            connection.sendData(up.getString());
        }

        private TcpServerConnection getConnectionUser(string contactTo)
        {
            TcpServerConnection tcpServerConnection = tcpServer1.Connections.FirstOrDefault(c => c.User_Name == contactTo);
            return tcpServerConnection;
        }

        private void createGroup(string jsonString, TcpServerConnection connection)
        {
            GroupService groupService = new GroupService();
            JsonPacket.UserPacket creteGroupPacket = JsonPacket.UserPacket.GetUserPacket(jsonString);
            Group group = null;
            UserTypeEnum result = groupService.CreateGroup(creteGroupPacket.groupName, creteGroupPacket.description
                , Common.EnumStringValueToEnumValue<GroupType>(creteGroupPacket.groupType), creteGroupPacket.from, out  group);

            creteGroupPacket.type = result.ToString();
            creteGroupPacket.to = creteGroupPacket.from;
            creteGroupPacket.from = Setting.fromServerToopeto;
            creteGroupPacket.date = group.date.dateTimeToUtcInMilisecond();
            connection.sendData(creteGroupPacket.getString());
        }

        private void DeleteUser(string jsonString, TcpServerConnection connection)
        {
            JsonPacket.UserPacket deletUser = JsonPacket.UserPacket.GetUserPacket(jsonString);
            UserService userService = new UserService();

            if (userService.deleteUser(deletUser))
            {
                deletUser.type = UserTypeEnum.result.ToString();
            }
            else
            {
                deletUser.type = UserTypeEnum.error.ToString();
            }
            deletUser.to = deletUser.from;
            deletUser.from = Setting.fromServerToopeto;
            connection.sendData(deletUser.getString());
        }

        private void sendChatRooms(string jsonString, TcpServerConnection connection)
        {
            ListTransferPacket listTransferPacket = ListTransferPacket.GetListTransferPacket(jsonString);
            GroupService groupService = new GroupService();

            ListTransferPacket listTransferPacketReply = ListTransferPacket.GetListTransferPacket(jsonString);
            listTransferPacketReply.id = listTransferPacket.id;
            listTransferPacketReply.from = Setting.fromServerToopeto;
            listTransferPacketReply.to = listTransferPacket.from;
            listTransferPacketReply.date = Common.utcInMilisecond();
            listTransferPacketReply.type = Toopeto.JsonPacket.ListTransferEnum.getGroup.ToString();//???
            List<string> groupList = new List<string>();
            IEnumerable<Group> onLineGroupsTotal = groupService.GetChatRooms(listTransferPacket.groupName);
           // IEnumerable<Group> onLineGroups = onLineGroupsTotal.Skip(listTransferPacket.currentNumber).Take(Setting.pageSize);
            foreach (Group group in onLineGroupsTotal)
            {
                groupList.Add(group.groupName);
            }
            listTransferPacketReply.count = onLineGroupsTotal.Count();
            listTransferPacketReply.currentNumber = listTransferPacket.currentNumber + groupList.Count;

            listTransferPacketReply.list = groupList;
            connection.sendData(listTransferPacketReply.getString());
        }
        private void sendChatRoomsChild(string jsonString, TcpServerConnection connection)
        {
            ListTransferPacket listTransferPacket = ListTransferPacket.GetListTransferPacket(jsonString);
            GroupService groupService = new GroupService();

            ListTransferPacket listTransferPacketReply = ListTransferPacket.GetListTransferPacket(jsonString);
            listTransferPacketReply.id = listTransferPacket.id;
            listTransferPacketReply.from = Setting.fromServerToopeto;
            listTransferPacketReply.to = listTransferPacket.from;
            listTransferPacketReply.date = Common.utcInMilisecond();
            listTransferPacketReply.type = Toopeto.JsonPacket.ListTransferEnum.getChatRoomsChild.ToString();
            listTransferPacketReply.groupName = listTransferPacket.groupName;
            List<string> groupList = new List<string>();
            IEnumerable<Group> onLineGroupsChild = groupService.GetChatRoomsChild(listTransferPacket.groupName);
            foreach (Group group in onLineGroupsChild)
            {
                groupList.Add(group.groupName);
            }
            listTransferPacketReply.count = onLineGroupsChild.Count();
            listTransferPacketReply.currentNumber = 0;

            listTransferPacketReply.list = groupList;
            connection.sendData(listTransferPacketReply.getString());
        }
        //لیست دوستان کاربر را به اوارسال می کند
        private void sendListContact(string jsonString, TcpServerConnection connection)
        {
            // MessagePacket messagePacket = MessagePacket.GetMessagePacket(jsonString);

            ListTransferPacket listTransferPacketReply = ListTransferPacket.GetListTransferPacket(jsonString);
            string userNameReq = listTransferPacketReply.from;
            listTransferPacketReply.from = Setting.fromServerToopeto;
            listTransferPacketReply.to = userNameReq;
            List<string> friends = new List<string>();
            UserPacket userPacket;
            UserService userService = new UserService();
            foreach (Contact friend in userService.getFriends(userNameReq))
            {
                userPacket = new UserPacket();
                userPacket.from = Setting.fromServerToopeto;
                userPacket.to = userNameReq;
                userPacket.userName = friend.userName;
                userPacket.avatar = friend.avatarUrl;
                friends.Add(userPacket.getString());
            }
            listTransferPacketReply.list = friends;
            connection.sendData(listTransferPacketReply.getString());
        }
        //لیست گروه ها کاربر را به او ارسال می کند
        private void sendListGroupsUser(string jsonString, TcpServerConnection connection)
        {
            ListTransferPacket listTransferPacketReply = ListTransferPacket.GetListTransferPacket(jsonString);
            String userNameReq = listTransferPacketReply.from;
            listTransferPacketReply.from = Setting.fromServerToopeto;
            listTransferPacketReply.to = listTransferPacketReply.from;
            List<string> groups = new List<string>();
            UserPacket userPacket;
            GroupService groupService = new GroupService();
            foreach (Group group in groupService.GetGroupsContact(userNameReq))
            {
                userPacket = new UserPacket();
                userPacket.from = Setting.fromServerToopeto;
                userPacket.to = userNameReq;
                userPacket.userName = group.fk_ContactCretor;
                userPacket.groupName = group.groupName;
                userPacket.groupType = Common.EnumShortValueToStringValue<GroupType>((byte)group.type);
                groups.Add(userPacket.getString());
            }
            listTransferPacketReply.list = groups;
            connection.sendData(listTransferPacketReply.getString());
        }
        //دریافت پینق
        // private void Ping(string jsonString, TcpServerConnection connection)
        //  {

        //throw new NotImplementedException();
        //   }



        private void RegisterUser(string jsonString, TcpServerConnection connection)
        {
            JsonPacket.UserPacket registerUser = JsonPacket.UserPacket.GetUserPacket(jsonString);
            UserService userService = new UserService();
            UserTypeEnum result = userService.RegisterUser(registerUser);
            if (result == UserTypeEnum.registerDone)
            {
                connection.User_Name = registerUser.userName;
                registerUser.to = registerUser.userName;
            }
            else
            {
                registerUser.to = registerUser.from;
            }
            registerUser.type = result.ToString();
            registerUser.from = Setting.fromServerToopeto;
            connection.sendData(registerUser.getString());

        }

        private void ChangePassword(string jsonString, TcpServerConnection connection)
        {
            JsonPacket.UserPacket registerUser = JsonPacket.UserPacket.GetUserPacket(jsonString);
            UserService userService = new UserService();
            if (userService.ChangePassword(registerUser))
            {
                registerUser.type = UserTypeEnum.result.ToString();
            }
            else
            {
                registerUser.type = UserTypeEnum.error.ToString();
            }
            registerUser.to = registerUser.from;
            registerUser.from = Setting.fromServerToopeto;
            connection.sendData(registerUser.getString());
        }
        /// <summary>
        /// ارسال پیام به یک کاربرو در صورتی که کاربر آنلاین نبود پیام ذخیره می گردد
        /// </summary>
        /// <param name="messageJson"></param>
        /// <param name="jsonString"></param>
        //private void sendMessageToUser(Toopeto.JsonPacket.MessagePacket messageJson, string jsonString)
        //{
        //    MessageService messageService = new MessageService();
        //    TcpServerConnection tcpServerConnection = tcpServer1.Connections.FirstOrDefault(c => c.User_Name == messageJson.to);
        //    if (tcpServerConnection != null)
        //    {
        //        tcpServerConnection.sendData(jsonString);
        //    }
        //    else
        //    {
        //        messageService.AddOfflineJsonString(jsonString, MessageTypeEnum.text, messageJson);
        //    }

        //}

        /// <summary>
        /// یک پیام را به کاربران یک گروه ارسال می کند ودر صوتی که هر یک از کاربر ها آنلاین نبود پیام آن را ذخیره می کند
        /// </summary>
        /// <param name="messagePacket"></param>
        /// <param name="jsonString"></param>
        private void sendMessageToGroup(string jsonString, TcpServerConnection connection)
        {
            Toopeto.JsonPacket.MessagePacket messagePacket = Toopeto.JsonPacket.MessagePacket.GetMessagePacket(jsonString);
            MessageService messageService = new MessageService();
            GroupService groupService = new GroupService();
            messagePacket.date = Common.utcInMilisecond();

            //اگر گیرنده شخص بود چک می کند که حتما گروه مشترک بین دو کاربر وجود داشته باشد وگرنه این گروه را ایجاد می کند
            if (!string.IsNullOrWhiteSpace(messagePacket.to))
            {
                // اگر گروهی بین دو کاربر و جود نداشت یک گروه ایجاد می کنه
                messagePacket.groupName = groupService.CreateRelationContat(messagePacket.from, messagePacket.to);
            }
            //ارسال پیام به اعضای گروه
            foreach (Contact contact in groupService.GetGroupContacts(messagePacket.groupName, messagePacket.from))
            {
                sendData(contact.userName, messagePacket.getString(), connection, messagePacket);
                
            }
        }
        /// <summary>
        /// اول حضور کاربر را تیک می زند دوم حضور کاربر را به دوستان اعلام می کند
        /// سوم لیست میسج های آفلاین کاربر را به ترتیب به او ارسال می کند
        /// و پیام های بر گردانده شده را خوانده شده می کند
        /// </summary>
        private bool PresenceUser(Json jsonPacket, TcpServerConnection connection)
        {
            UserService userService = new UserService();
            UserPacket userPacketPeresence = new UserPacket();
            CopyJsonToUserPacket(jsonPacket, userPacketPeresence);
            //لاگین کاربر را چک می کند
            if (userService.Login(jsonPacket.from,jsonPacket.password))
            {
                addUserToConnection(connection, jsonPacket.from);

                peresenceResult(connection, userPacketPeresence, UserTypeEnum.presence);

                //اعلان حضور به دوستان
            /*    Toopeto.JsonPacket.UserPacket userJsonNotyfiction = new JsonPacket.UserPacket();
                foreach (Contact user in userService.getFriends(userPacket.from))
                {
                    userJsonNotyfiction.id = System.Guid.NewGuid().ToString();
                    userJsonNotyfiction.from = userPacket.from;
                    userJsonNotyfiction.to = user.userName;
                    userJsonNotyfiction.type = UserTypeEnum.presence.ToString();
                    userJsonNotyfiction.groupName = user.userName.getRelationGroupName(userPacket.from);

                    sendData(userJsonNotyfiction.to, userJsonNotyfiction.getString(), connection);
                }*/
                //میسج های آفلاین کاربر را به او ارسال می کند
                foreach (Message message in userService.GetOfflineMessageUser(jsonPacket.from))
                {
                    connection.sendData(message.text);
                    message.Status = (byte)JsonPacket.MessageStatusEnum.sent;
                }
                userService.SaveDbContect();
                return true;
            }
            else
            {
                peresenceResult(connection, userPacketPeresence, UserTypeEnum.error);
                return false;
            }

        }

        private void deleteOtherConnection(string userName, TcpServerConnection connection)
        {
            //دیگر کانکشن های این یوزر را پاک می کند
            foreach (TcpServerConnection oldConnection in tcpServer1.Connections.Where(c => c.User_Name == userName && c != connection))
            {
                tcpServer1.Connections.Remove(oldConnection);
            }
            //تمام کانکشن های که هفت ثانیه از ایجاد آنها گذشته و تا حالا ریجستر نکرده اند را حذف می کند 
               tcpServer1.Connections.RemoveAll(c=>string.IsNullOrWhiteSpace(c.User_Name) &&
                   c.LastVerifyTime.AddMilliseconds(7000) < DateTime.UtcNow);  
        }

        private void addUserToConnection(TcpServerConnection connection, string userName)
        {
            //اگر کانکشن جاری کاربر خالی بود و از قبل کانکشن داشت آن را پاک می کند  
            deleteOtherConnection(userName, connection);
            connection.User_Name = userName;
        }
        //نتیجه لاگین را به کاربر هدف می رساند
        private static void peresenceResult(TcpServerConnection connection, UserPacket userPacket, UserTypeEnum result)
        {
          
            userPacket.to = userPacket.from;
            userPacket.from = Setting.fromServerToopeto;
            userPacket.type = result.ToString();
            connection.sendData(userPacket.getString());
        }

      /*  private static void requestLogin(TcpServerConnection connection, Json jsonPacket)
        {
            UserPacket userPacket = new UserPacket();
            userPacket.to = jsonPacket.from;
            userPacket.from = Setting.fromServerToopeto;
            userPacket.type = UserTypeEnum.presence.ToString();
            connection.sendData(userPacket.getString());
        }*/

        private void tcpServer1_OnError(TcpServer server, Exception e)
        {
            throw new System.NotImplementedException();
        }

        private void button1_Click(object sender, EventArgs e)
        {
            UserService userService = new UserService();
            var x = userService.getFriends("babak").ToList();
            var z = x.Count;
        }

        private void CopyJsonToUserPacket(Json src, UserPacket des)
        {
            des.userName = src.from;
            des.type = src.type;
            des.to = src.to;
            des.Signature ="";
            des.password = src.password;
            des.packetName = src.packetName;
            des.id = src.id;
            des.groupType = src.groupType;
            des.groupName = src.groupName;
            des.from = src.from;
            des.description = src.description;
            des.date = src.date;
            des.avatar = "";
        }

        private void ActiveLogging_CheckedChanged(object sender, EventArgs e)
        {
            if (cbActiveLogging.Checked) { activeLogging = true; }
            else { activeLogging = false; }
        }
        private static bool activeLogging = false;
        private static bool activeLoggingUser = false;

        public static void loging(Json json)
        {
            if (activeLogging)
            {
                LoggingScenario.loging(json);
            }
        }

        private void cbActiveLoggingUser_CheckedChanged(object sender, EventArgs e)
        {
            if (cbActiveLoggingUser.Checked) { activeLoggingUser = true; }
            else { activeLoggingUser = false; }
        }
        public static void logingWithUser(Json json)
        {
            if (activeLogging)
            {
                // LoggingScenario.logingWithUser(json);
            }
        }
    }
}

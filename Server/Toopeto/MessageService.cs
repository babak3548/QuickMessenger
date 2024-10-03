using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Toopeto.JsonPacket;
using AnarSoft.Utility.Utilities;

namespace Toopeto
{
    public class MessageService : Service
    {
        public Message message = new Message();

/*
        /// <summary>
        /// پیام یک کاربر آفلاین را ذخیره می کند
        /// </summary>
        /// <param name="messageJson"></param>
        public void AddOfflineMessage(JsonPacket.MessagePacket messageJson)
        {
            Message = null;
            if (messageJson.type == MessageTypeEnum.text.ToString())
            {
                Message = DbContext.Messages.FirstOrDefault(m => m.PacketId == messageJson.id);
                if (this.Message == null)
                {
                    Message.text = messageJson.data;
                    Message.type = MessageTypeEnum.text.ToByte(0);

                    Message.PacketId = messageJson.id;
                    DbContext.Messages.Add(Message);
                    DbContext.SaveChanges();
                }

                User_Message user_Message = new User_Message();
                user_Message.fk_User = DbContext.Users.FirstOrDefault(e => e.userName == messageJson.to).id;
                user_Message.fk_message = Message.id;
                user_Message.status = MessageStatusEnum.noRead.ToByte(0);
                DbContext.SaveChanges();
            }
            else if (messageJson.type == MessageTypeEnum.image.ToString())
            {
                throw new System.NotImplementedException();
            }
        }
        */

        // فرق نمی کند مسیج از چه نوعی باشد چون مسیج های فایلی آدرس فایل میرزاید و خود فایل قبلا آپلود می گردد
        public bool AddOfflineJsonString( String messagePacket, Json json,string receiver)
        {
            try
            {
                message = null;
               
                if (!String.IsNullOrWhiteSpace(json.groupName))
                {
                    message = new Message();
                    message.id = json.id;
                    message.text = messagePacket;
                    //در این فیلد نوع پکیت ذخیره می گردد برعکس نامش
                    message.type = Common.EnumStringValueToIntValue<PacketNameEnum>(json.packetName);
                    message.fkGroup = json.groupName;
                    message.fk_Contact_sender = json.from;
                    message.fk_Contact_receiver = receiver;
                    message.date = DateTime.Now;
                    message.Status = (byte)MessageStatusEnum.noSent;
                    DbContext.Messages.Add(message);
                    return DbContext.SaveChanges() > 0;
                }
                else return false;
            }
            catch (Exception)
            {
                return false;
            }

        }

        /// <summary>
        /// حد اکثر صد پیام خوانده نشده کاربر را برمی گرداند 
        /// </summary>
        public IEnumerable<Message> GetOfflineMessageGroup(string fkGroup)
        {
            IEnumerable<Message> messageList =
                DbContext.Messages.Where(m => m.fkGroup == fkGroup);
            return messageList;
        }


        internal void addLike(string fk_message,string fromContact, string likeValue)
        {
            bool isLike=(likeValue != "0"?true:false);
            LikeCommand lc = DbContext.LikeCommands.FirstOrDefault(l => l.fk_message == fk_message && l.fk_userName == fromContact);
          if (lc == null) {
              lc = new LikeCommand();
              lc.fk_message = fk_message;
              lc.fk_userName = fromContact;
              lc.isLike = isLike;
              DbContext.LikeCommands.Add(lc);
          }
          DbContext.SaveChanges();  
        }

        internal int CountLikeMessage(string fk_message, string fromContact,ref string likeValue)
        {
            bool isLike = (likeValue != "0" ? true : false);
            int countLike = DbContext.LikeCommands.Count(l => l.fk_message == fk_message);
            LikeCommand lc = DbContext.LikeCommands.FirstOrDefault(l => l.fk_message == fk_message && l.fk_userName == fromContact);
            likeValue=(lc==null?"0":"1");
            return countLike;

        }

    }
}

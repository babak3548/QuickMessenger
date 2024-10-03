using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Toopeto.JsonPacket;
using AnarSoft.Utility.Utilities;

using System.Data.Entity.Spatial;
using System.Data.Entity;

namespace Toopeto
{
    public class UserService : EntityService
    {
        private Contact contact
        {
            get;
            set;
        }

        private Group_Contact room_User
        {
            get;
            set;
        }

        public List<Group_Contact> room_Users
        {
            get;
            set;
        }

        public List<Contact> users
        {
            get;
            set;
        }

        public GroupService roomService
        {
            get;
            set;
        }

        public MessageService messageService
        {
            get;
            set;
        }

        public UserService()
        {
            this.contact = new Contact();
            this.room_User = new Group_Contact();
            this.messageService = new MessageService();
            this.roomService = new GroupService();
        }
        public void AddFriend()
        {
            throw new System.NotImplementedException();
        }

        public void DeleteFriend()
        {
            throw new System.NotImplementedException();
        }

        public void ChangePassword()
        {
            throw new System.NotImplementedException();
        }

        public void SaveMessageInOffline()
        {
            throw new System.NotImplementedException();
        }
        internal Contact getUserByUserName(string userName)
        {
            return DbContext.Contacts.FirstOrDefault(c => c.userName == userName);

        }

        //باید حذف گردد
        internal void SetPresence(JsonPacket.UserPacket userJson)
        {
            Contact user = DbContext.Contacts.FirstOrDefault(u => u.userName == userJson.from);
            user.Presence = UserTypeEnum.presence.ToByte(0);
            DbContext.SaveChanges();
        }


        /// <summary>
        /// حد اکثر صد پیام خوانده نشده کاربر را برمی گرداند 
        /// </summary>
        public IEnumerable<Message> GetOfflineMessageUser(string contactName)
        {
            IEnumerable<Message> messageList =
                //  DbContext.Messages.Where(m => m.Group.Group_Contact.Any(gc => gc.Fk_Contact == contactName)    && m.Status == (byte)MessageStatusEnum.noSent).Take(100);
           DbContext.Messages.Where(m => m.fk_Contact_receiver == contactName && m.Status == (byte)MessageStatusEnum.noSent).Take(100);
            return messageList;
        }

        internal List<Contact> getFriends(string userName)
        {
            var group_contactList = DbContext.Group_Contact.Where(gc => gc.Fk_Contact == userName
                && (gc.levelAccess == (byte)LevelAccessToGroup.AcceptFriend || gc.levelAccess == (byte)LevelAccessToGroup.AdminGroup)
                && gc.Group.type == (byte)GroupType.TYPE_SINGLE);

            List<Contact> contactFriends = new List<Contact>();
            foreach (var gc in group_contactList)
            {
                Group_Contact g_c = DbContext.Group_Contact.FirstOrDefault(c => c.fkGroup == gc.fkGroup && c.Fk_Contact != userName);
                if(g_c != null)
                contactFriends.Add(g_c.Contact);
            }

            return contactFriends;
        }

        internal UserTypeEnum RegisterUser(UserPacket registerUser)
        {
            try
            {
                if (DbContext.Contacts.Any(u => u.userName == registerUser.userName))
                {
                    return UserTypeEnum.registerConflict;
                }
                else
                {
                    contact.userName = registerUser.userName;
                    contact.password = registerUser.password;
                    //contact.numberPhone = registerUser.Signature;
                    contact.Presence = UserTypeEnum.presence.ToByte(0);
                    contact.date = DateTime.Now;
                    DbContext.Contacts.Add(contact);
                    if (DbContext.SaveChanges() > 0)
                    {
                        return UserTypeEnum.registerDone;
                    }
                    else
                    { return UserTypeEnum.registerFailed; }
                }
            }
            catch (Exception)
            {

                return UserTypeEnum.registerFailed;
            }

        }
        public void setNearbyTime(string userName, DbGeography userLoction)
        {
            Contact contact = DbContext.Contacts.FirstOrDefault(c => c.userName == userName);
            contact.nearbySearch = DateTime.Now;
            contact.position = userLoction;
            DbContext.SaveChanges();

        }
        internal bool ChangePassword(JsonPacket.UserPacket registerUser)
        {
            try
            {
                contact = DbContext.Contacts.FirstOrDefault(u => u.userName == registerUser.from);
                if (contact.password == registerUser.password)
                {
                    contact.password = registerUser.description;
                    contact.Presence = UserTypeEnum.presence.ToByte(0);
                }

                return DbContext.SaveChanges() > 0;
            }
            catch (Exception)
            {

                return false;
            }

        }

        internal bool Login(string userName,string password)
        {
            if (DbContext.Contacts.FirstOrDefault(u => u.userName == userName && u.password == password) != null)
            {

                return true;
            }
            else
            {
                return false;
            }
        }

        internal bool deleteUser(UserPacket deletUser)
        {

            contact = DbContext.Contacts.FirstOrDefault(u => u.userName == deletUser.from);
            if (contact != null && contact.password == deletUser.password)
            {
                DbContext.Contacts.Remove(contact);
                return DbContext.SaveChanges() > 0;
            }
            else
            { return false; }
        }

        internal IEnumerable<Contact> getNearbyContacts(string latitudeLongitude, string requestUserName, string sex)
        {
            DbGeography userLoction = ExtentionMethods.CreatePoint(latitudeLongitude);
            setNearbyTime(requestUserName, userLoction);

            //var eCoord = new GeoCoordinate(eLatitude, eLongitude);    DbFunctions.DiffHours(c.nearbySearch, DateTime.Now) < 24)
            TimeSpan bazeh = new TimeSpan(23, 59, 0);
            var nearbyContacts = DbContext.Contacts.Where(c => c.position.Distance(userLoction) < 20000 &&
              DbFunctions.DiffHours(c.nearbySearch, DateTime.Now) < 24).OrderBy(c => c.nearbySearch);
            return nearbyContacts;
        }

        internal IEnumerable<Group_Contact> getContactsGroup(string groupName)
        {

            var group_Contacts = DbContext.Group_Contact.Where(gc => gc.fkGroup == groupName);
            return group_Contacts;
        }
        internal string updateProfileImage(string contactName, string avatarUrl)
        {
            DbContext.Contacts.FirstOrDefault(c => c.userName == contactName).avatarUrl = avatarUrl;
            if (DbContext.SaveChanges() > 0) return MessageTypeEnum.result.ToString();
            else return MessageTypeEnum.error.ToString();
        }

        internal string getAvatarContact(string contactName)
        {
            Contact contact = DbContext.Contacts.FirstOrDefault(c => c.userName == contactName);

            return contact != null ? contact.avatarUrl : "";
        }

        internal int countLikeUser(string fromContact)
        {
            return DbContext.LikeCommands.Count(l => l.fk_userName == fromContact);
        }
    }
}

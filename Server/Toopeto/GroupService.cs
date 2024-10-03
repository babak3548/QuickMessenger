using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Toopeto.JsonPacket;
using AnarSoft.Utility.Utilities;
namespace Toopeto
{
    public class GroupService : EntityService
    {

        public Group group = new Group();

        /// <summary>
        ///  اگر یک کاربر به کاربر دیگر برای اولین بار پیام فرستاده باشد 
        ///  ارتباط دو کاربر را با ایجاد یک گروه، در سیستم ثبت می کند 
        ///  اگر از قبل این ارتباط ثبت شده بود ثبت تکرار نمی شود
        /// </summary>
        /// <param name="fromContact"></param>
        /// <param name="toContact"></param>
        /// <returns></returns>
        public String CreateRelationContat(string fromContact, string toContact)
        {
            try
            {
                string groupName=fromContact.getRelationGroupName( toContact);
                //کاربر دریافت کننده به عنوان ادمین گروه در نظر گرفته می شود چون 
                //این کاربر بتوانند کاربر درخواست کننده را محدود کند
                CreateGroup(groupName, "", GroupType.TYPE_SINGLE,toContact,out  group);

                AddContact(fromContact, groupName, LevelAccessToGroup.AcceptFriend);
                AddContact(toContact, groupName, LevelAccessToGroup.AdminGroup);
                return groupName;
            }
            catch (Exception)
            {
                return "";
            }


        }
        /// <summary>
        /// یک یوزر را به گروه اضافه می کند
        /// </summary>
        public UserTypeEnum AddContact(string contactName, string groupName)
        {
            return AddContact(contactName, groupName, LevelAccessToGroup.requestFriend);
        }

        public UserTypeEnum AddContact(string contactName, string groupName, LevelAccessToGroup leveAccessToGroup)
        {
            try
            {
                Group_Contact group_contactExist = DbContext.Group_Contact.FirstOrDefault(gc => gc.Fk_Contact == contactName
                    && gc.fkGroup == groupName);
                if (group_contactExist != null) return UserTypeEnum.registerConflict;
                else
                {
                    Group_Contact group_contact = new Group_Contact();
                    group_contact.Fk_Contact = contactName;
                    group_contact.fkGroup = groupName;
                    group_contact.levelAccess = (byte)leveAccessToGroup;
                    DbContext.Group_Contact.Add(group_contact);
                    if (DbContext.SaveChanges() > 0)
                        return UserTypeEnum.result;
                    else return UserTypeEnum.error;
                }
            }
            catch (Exception e)
            {
                return UserTypeEnum.error;
            }
        }

        /// <summary>
        /// لیست کاربرهای یک گروه را ، منهای کاربر دریافت شده را برمی گرداند
        /// </summary>
        /// <param name="groupName"></param>
        /// <param name="contactSender"></param>
        /// <returns></returns>
        internal IEnumerable<Contact> GetGroupContacts(string groupName, string contactSender)
        {
            return DbContext.Contacts.Where(u => u.Group_Contact.Any(gr => gr.fkGroup == groupName
                && (gr.levelAccess==(byte)LevelAccessToGroup.AdminGroup 
                || gr.levelAccess==(byte)LevelAccessToGroup.AcceptFriend )) && u.userName != contactSender);
        }

        /// <summary>
        /// لیست گروه های مورد جستجو را بر می گرداند
        /// </summary>
        /// <param name="searchValue"></param>
        /// <returns></returns>
        /// 
        internal IEnumerable<Group> GetChatRooms(string searchValue)
        {
            if (!string.IsNullOrWhiteSpace(searchValue))
            {
                return DbContext.Groups.Where(g => g.groupName.Contains(searchValue) && g.type == (byte)GroupType.CHAT_ROOM
                    && g.fk_parentName == null);
            }
            else
            {
                return DbContext.Groups.Where(g => g.type == (byte)GroupType.CHAT_ROOM && g.fk_parentName == null);
            }
        }
        /// <summary>
        /// لیست گروه های مورد جستجو را بر می گرداند
        /// </summary>
        /// <param name="searchValue"></param>
        /// <returns></returns>
        /// 
        internal IEnumerable<Group> GetChatRoomsChild(string parentName)
        {
            return DbContext.Groups.Where(g => g.fk_parentName==parentName && g.type == (byte)GroupType.CHAT_ROOM);
        }
        //,string ContactCreator
        internal UserTypeEnum CreateGroup(string groupName, string subject, GroupType groupType,string contactCreator,out Group group)
        {
            group = null;
            try
            {
                group = DbContext.Groups.FirstOrDefault(g => g.groupName == groupName);
                if (group != null) return UserTypeEnum.createGroupConflict;
                else
                {
                    group = new Group();
                    group.groupName = groupName;
                    group.subject = subject;
                    group.type = (byte)groupType;
                    group.date = DateTime.Now;
                    group.fk_ContactCretor = contactCreator;
                    DbContext.Groups.Add(group);
                    if (DbContext.SaveChanges() > 0) {
                        // اضافه کردن آدمین به گروه  
                        AddContact(contactCreator, groupName, LevelAccessToGroup.AdminGroup);
                        DbContext.SaveChanges();
                        return UserTypeEnum.result; 
                    }
                    else { return UserTypeEnum.error; }
                }
            }
            catch (Exception)
            {
                
                return UserTypeEnum.error;
            }

            
        }
        /// <summary>
        /// ادمین درخواست کاربر را برای اضافه شدن به گرده می پزیرد
        /// </summary>
        /// <param name="contactAdmin"></param>
        /// <param name="contactReqest"></param>
        /// <param name="groupName"></param>
        /// <returns></returns>
        internal UserTypeEnum AcceptContactInGroup(string contactAdmin, string contactReqest,string groupName)
        {
            try
            {
                group = DbContext.Groups.FirstOrDefault(g => g.groupName == groupName);
                if (group.fk_ContactCretor == contactAdmin)
                {
                    Group_Contact group_contact = group.Group_Contact.FirstOrDefault(gc => gc.fkGroup == groupName
                        && gc.Fk_Contact == contactReqest);
                    group_contact.levelAccess = (byte)LevelAccessToGroup.AcceptFriend;
                    return DbContext.SaveChanges() > 0 ? UserTypeEnum.acceptYouToGroup : UserTypeEnum.noAcceptYouToGroup;
                }
                else
                    return UserTypeEnum.noAcceptYouToGroup;
            }
            catch (Exception)
            {
                return UserTypeEnum.noAcceptYouToGroup;
            }

        }

        internal IEnumerable<Group> GetGroupsContact(String contactName)
        {
          return  DbContext.Groups.Where(g => g.Group_Contact.Any(gc => gc.Fk_Contact == contactName));
        }

 
    }
}

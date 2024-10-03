using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Toopeto.JsonPacket
{

    public enum PacketNameEnum
    {
        message = 1, user = 2, listTransfer = 3
    }
    public enum MessageTypeEnum
    {
        //query=8این تایپ می تواند به یک پکت نام تبدیل گردد
        //ping -- akt  //پاسخ پینگ، اکت می باشد
        text = 1, image = 2, file = 3, video = 4, ack = 5, result = 6, error = 7,
        ping = 8, query = 9, end = 10, notifySubject = 11, read = 12, repeatBind = 13,
        profileImage = 14, like=15
    }

    public enum UserTypeEnum
    {
        presence = 1, registerUser = 2, changePassword = 3, result = 4, error = 5, registerFailed = 6,
        registerDone = 7, registerConflict = 8, deleteUser = 9, Leave = 10, createGroup = 11,
        addToGroup = 12, acceptToGroup = 13, acceptYouToGroup = 14, noAcceptYouToGroup = 15,
        userOff = 16, getUser = 17, createGroupConflict = 18, enterToChatRoom = 19, exitFromChatRoom = 20,
        addToMyGroup = 21, getAvatar = 22, likeCounterUser = 23, noPresence = 24,
        userOn=25,
    }
    public enum ListTransferEnum
    {
        getContact = 1, getGroup = 2, getChatRooms = 3, requestTimeLine = 4, getNearbyContacts = 5,
        getChatRoomsChild = 6
            , getUsersChatRoom = 7,
        getUsersGroup=8
    }

   public enum GroupType//
   {
       // سنکرون با طرف کلاینت البته در انتیتی گروه
        TYPE_SINGLE = 1,
        TYPE_MUC = 2,
        TIME_LINE=3,
        CHAT_ROOM=4

   }
   public enum LevelAccessToGroup
   {
       AdminGroup = 1,
       requestFriend = 2,
       AcceptFriend = 3,
       blockContact = 4,
   }

    public enum MessageStatusEnum
    {
        noRead = 1, Read = 2,noSent = 3,sent=4

    }

    public static class Setting
    {
        public static String fromServerToopeto = "fromServerToopeto";//TIME_LINE
        public static String isTimeLine = "isTimeLine";//
        public static int pageSize = 10;
        public static int maxUserInChatroom = 100;
        public static string defaultNumberLine = "+1";
    }
}




	
	







		
	
	
	
	
	

	
	
	
	
	
	

	
	
	
	

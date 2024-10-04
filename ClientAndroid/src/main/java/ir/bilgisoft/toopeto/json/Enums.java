package ir.bilgisoft.toopeto.json;

/**
 * Created by mehrang on 07/03/2015.
 */
public  class Enums {
    public static enum PacketNameEnum
    {
       message,user,listTransfer
    }
    public static  enum MessageTypeEnum
    {
        text, image , file , video , ack , result, error ,ping,query,end,notifySubject,read,repeatBind;
//end :endsestion
    }

    public static  enum UserTypeEnum
    {
        presence , registerUser , changePassword , result , error,registerFailed
        , registerDone,registerConflict ,deleteUser,Leave;
    }

    public static enum ListTransferEnum
    {
        getContact,getRooms,
    }

    public static  enum ReceiverEnum
    {
        user, room
    }
    public static class Setting
    {
        public static String fromServerToopeto = "fromServerToopeto";
    }
    //for server
    /*
    public static  enum MessageStatusEnum {
        noRead, Read

    }
    public static  enum PresenceEnum
    {
        noPresence,presence
    }*/




}

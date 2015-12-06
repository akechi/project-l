package akechi.projectl;

/**
 * Created by coffee on 06/08/2015.
 */
public final class Event
{
    public static final class AccountChange
    {
        public static final String ACTION= AccountChange.class.getCanonicalName();
        public static final String KEY_ACCOUNT= "account";

        private AccountChange()
        {
            throw new AssertionError();
        }
    }

    public static final class RoomChange
    {
        public static final String ACTION= RoomChange.class.getCanonicalName();
        public static final String KEY_ROOM_ID= "roomId";
        public static final String KEY_OLD_ROOM_ID= "oldRoomId";

        private RoomChange()
        {
            throw new AssertionError();
        }
    }

    public static final class PreferenceChange
    {
        public static final String ACTION= PreferenceChange.class.getCanonicalName();

        private PreferenceChange()
        {
            throw new AssertionError();
        }
    }

    public static final class Reload
    {
        public static final String ACTION= Reload.class.getCanonicalName();

        private Reload()
        {
            throw new AssertionError();
        }
    }

    public static final class OnNotificationTapped
    {
        public static final String ACTION= OnNotificationTapped.class.getCanonicalName();
        public static final String KEY_ACCOUNT_NAME= "accountName";
        public static final String KEY_ROOM_ID= "roomId";
        public static final String KEY_MESSAGE_ID= "messageId";

        private OnNotificationTapped()
        {
            throw new AssertionError();
        }
    }

    public static final class FindMessage
    {
        public static final String ACTION= FindMessage.class.getCanonicalName();
        public static final String KEY_MESSAGE_ID= "messageId";

        private FindMessage()
        {
            throw new AssertionError();
        }
    }

    private Event()
    {
        throw new AssertionError();
    }
}

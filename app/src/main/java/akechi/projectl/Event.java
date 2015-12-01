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

    private Event()
    {
        throw new AssertionError();
    }
}

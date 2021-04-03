package com.madao.simplebeat;

import android.os.Message;

public final class Messages {

    public final static int MsgTickTime = 1;
    public final static int MsgUpdateTimer = 2;

    public static Message TickTime(int delta, int ticks) {
        Message msg = Message.obtain();
        msg.what = MsgTickTime;
        msg.arg1 = delta;
        msg.arg2 = ticks;
        return msg;
    }

    public static Message UpdateTimer() {
        Message msg = Message.obtain();
        msg.what = MsgUpdateTimer;
        return msg;
    }
}

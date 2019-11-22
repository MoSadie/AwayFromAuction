package io.github.mosadie.awayfromauction;

public class TimerThread extends Thread {
    private final AwayFromAuction awa;

    public TimerThread(AwayFromAuction mod) {
        this.awa = mod;
    }

    @Override
    public void run() {
        while(!this.isInterrupted()) {
            try {
                Thread.sleep(Config.GENERAL_REFRESH_DELAY.get() * 1000);
                awa.sync();
            } catch (InterruptedException e) {
                // Do nothing, it's fine.
            }
        }
    }
}
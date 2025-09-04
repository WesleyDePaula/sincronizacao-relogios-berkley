package br.furb.sisdis;

public class Relogio {
    private long startTime;

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

}
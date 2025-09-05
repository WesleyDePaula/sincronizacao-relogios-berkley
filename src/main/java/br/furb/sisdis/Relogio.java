package br.furb.sisdis;

import lombok.Getter;
import lombok.Setter;

public class Relogio {
    private long startTime;

    @Getter
    @Setter
    private long correction = 0;

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return (System.currentTimeMillis() - startTime) + correction;
    }

}
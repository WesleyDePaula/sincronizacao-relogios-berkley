package br.furb.sisdis;

public record Evento(Events event, String parameter) {
    public String getRequest() {
        return event.request + parameter;
    }
}

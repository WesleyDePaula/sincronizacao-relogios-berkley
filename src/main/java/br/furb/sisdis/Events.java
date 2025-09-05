package br.furb.sisdis;

public enum Events {

	GET_TIME("GET_TIME", "REQUEST:GET_TIME:"),
	SEND_TIME("SEND_TIME", "REQUEST:SEND_TIME:"),
    AJUSTA_TEMPO("AJUSTA_TEMPO", "REQUEST:AJUSTA_TEMPO:" );

	public String value;
	
	public String request;
	
	Events(String value, String request) {
		this.value = value;
		this.request = request;
	}
	
}

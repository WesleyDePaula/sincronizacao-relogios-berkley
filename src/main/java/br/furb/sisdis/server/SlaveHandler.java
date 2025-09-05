package br.furb.sisdis.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import br.furb.sisdis.Evento;
import br.furb.sisdis.Events;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlaveHandler implements Runnable {

	private final Socket socket;
	private final BufferedReader in;
	private final PrintWriter out;

	private final BlockingQueue<Evento> requestQueue = new LinkedBlockingQueue<>();
	private volatile boolean running = true;

    @Getter
    private Long ultimaDiferencaCalculada = null;

	public SlaveHandler(Socket socket) throws IOException {
		this.socket = socket;
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Para "receber/ler" requisições
																						// do slave
		this.out = new PrintWriter(socket.getOutputStream(), true); // Para "enviar" requisições ao slave
	}

	@Override
	public void run() {
		log.info("# Handler iniciado com sucesso para {}", getSlaveInfo());

		try {
			while (running && !socket.isClosed()) {
				// Busca a próxima requisição enfileirada, caso não tenha, bloqueia a thread até
				// haver uma
				Evento requisicao = requestQueue.take();

				if (Events.GET_TIME == requisicao.event()) {

					// "Envia" a requisição ao slave
					out.println(requisicao.getRequest());
					out.flush();
					log.info("# Enviado requisição {}", Events.GET_TIME.value);

					try {
						socket.setSoTimeout(5_000);
						String resposta = in.readLine();

						if (resposta.startsWith(Events.SEND_TIME.request)) {
							String payload = resposta.substring(Events.SEND_TIME.request.length());
							log.info("## Recebido de {} -> {}", getSlaveInfo(), payload);

                            this.ultimaDiferencaCalculada = Long.parseLong(payload);

							out.flush();
						}

					} catch (SocketException e) {
						log.error("## Timeout ao aguardar resposta de {}", getSlaveInfo());
						MasterApp.removeHandler(this);
					}

				}
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			cleanup();
		}
	}

	public void getTime(long tempoAtualServidor) {
        Evento evento = new Evento(Events.GET_TIME, String.valueOf(tempoAtualServidor));
		requestQueue.add(evento);
	}

	private void cleanup() {
		running = false;
		try {
			socket.close();
		} catch (IOException ignored) {
		}
		MasterApp.removeHandler(this);
		log.warn("Handler finalizado para {}", getSlaveInfo());
	}

	public String getSlaveInfo() {
		return socket.getRemoteSocketAddress().toString();
	}

    public void ajustaTempo(long diff) {
        var requisicao = new Evento(Events.AJUSTA_TEMPO, String.valueOf(diff));

        out.println(requisicao.getRequest());
        out.flush();
    }
}

;

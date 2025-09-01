package br.furb.sisdis.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import br.furb.sisdis.Events;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientHandler implements Runnable {

	private Socket socket;
	private final BufferedReader in;
	private final PrintWriter out;

	private final BlockingQueue<Events> requestQueue = new LinkedBlockingQueue<>();
	private volatile boolean running = true;

	public ClientHandler(Socket socket) throws IOException {
		this.socket = socket;
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Para "receber/ler" requisições
																						// do client
		this.out = new PrintWriter(socket.getOutputStream(), true); // Para "enviar" requisições ao client
	}

	@Override
	public void run() {
		log.info("# Handler iniciado com sucesso para {}", getClientInfo());

		try {
			while (running && !socket.isClosed()) {
				// Busca a próxima requisição enfileirada, caso não tenha, bloqueia a thread até
				// haver uma
				Events requisicao = requestQueue.take();

				if (Events.GET_TIME == requisicao) {

					// "Envia" a requisição ao client
					out.println(Events.GET_TIME.request);
					out.flush();
					log.info("# Enviado requisição {}", Events.GET_TIME.value);

					try {
						socket.setSoTimeout(5_000);
						String resposta = in.readLine();

						if (resposta.startsWith(Events.SEND_TIME.request)) {
							String payload = resposta.substring("RESPONSE:".length());
							log.info("## Recebido de {} -> {}", getClientInfo(), payload);

							// TODO: EXECUTAR ALGORITMO DE BECKLER E TRATAR HORÁRIO

							// Envia resultado para o client
							out.println("OK"); // TODO: A principio, será um OK, deverá enviar o ajuste de horário ao
												// client
							out.flush();
							log.info("OK enviado para {}", getClientInfo());
						}

					} catch (SocketException e) {
						log.error("## Timeout ao aguardar resposta de {}", getClientInfo());
						ServerApp.removeHandler(this);
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

	public void getTime() {
		requestQueue.add(Events.GET_TIME);
	}

	private void cleanup() {
		running = false;
		try {
			socket.close();
		} catch (IOException ignored) {
		}
		ServerApp.removeHandler(this);
		log.warn("Handler finalizado para {}", getClientInfo());
	}

	public String getClientInfo() {
		return socket.getRemoteSocketAddress().toString();
	}

}

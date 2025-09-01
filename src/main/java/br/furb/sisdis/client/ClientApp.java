package br.furb.sisdis.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;

import br.furb.sisdis.Events;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientApp {

	private static final String SERVER_HOST = "localhost";
	private static final int SERVER_PORT = 5000;

	public static void main(String[] args) {
		int tentativasConexao = 0;

		while (tentativasConexao < 5) {
			try (Socket socket = new Socket()) {

				socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 5000);
				log.info("# Client conectando em {}:{}", SERVER_HOST, SERVER_PORT);

				
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				
				String line;
				while ((line = in.readLine()) != null) {

					if (line.equals(Events.GET_TIME.request)) {
						log.info("# Recebido requisição {}", Events.GET_TIME.value);

						String now = LocalDateTime.now().toString(); // Gera um horário LOCAL

						// Envia a resposta seguindo o protocolo
						String response = Events.SEND_TIME.request + now;
						out.println(response);
						out.flush();
						log.info(response);
						continue;
					}

					if ("OK".equals(line)) {
						log.info("# OK recebido (ack)");
						continue;
					}

					// Pode haver outros tipos de mensagens no futuro — apenas logamos
					log.warn("# Requisição desconhecida: {}", line);
				}

			} catch (IOException e) {
				log.error("Erro ao se conectar ao servidor: ", e);
				tentativasConexao++;
			}
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
}

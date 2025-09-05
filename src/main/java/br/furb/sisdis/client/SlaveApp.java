package br.furb.sisdis.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;

import br.furb.sisdis.Events;
import br.furb.sisdis.Relogio;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlaveApp {

	private static final String SERVER_HOST = "localhost";
	private static final int SERVER_PORT = 5000;
    private static final Relogio relogio = new Relogio();

	public static void main(String[] args) {
		int tentativasConexao = 0;

		while (tentativasConexao < 5) {
            relogio.start();
			try (Socket socket = new Socket()) {

				socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 5000);
				log.info("# Client conectando em {}:{}", SERVER_HOST, SERVER_PORT);

				
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				
				String line;
				while ((line = in.readLine()) != null) {

					if (line.startsWith(Events.GET_TIME.request)) {
						log.info("# Recebido requisição {}", Events.GET_TIME.value);

                        var horarioMaster = line.substring(Events.GET_TIME.request.length());

						long differenceTime = relogio.getElapsedTime() - Long.parseLong(horarioMaster);

						// Envia a resposta seguindo o protocolo
						String response = Events.SEND_TIME.request + differenceTime;
						out.println(response);
						out.flush();
						log.info(response);
						continue;
					}

					if (line.startsWith(Events.AJUSTA_TEMPO.request)) {
                        var ajusteTempo = line.substring(Events.AJUSTA_TEMPO.request.length());
                        log.info("# Recebido requisição {} -> Ajuste do tempo {}", Events.GET_TIME.value, ajusteTempo);

                        relogio.setCorrection(Long.parseLong(ajusteTempo));

                        log.info("# Novo tempo definido -> {}", relogio.getElapsedTime());
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

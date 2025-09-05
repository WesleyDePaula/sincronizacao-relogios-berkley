package br.furb.sisdis.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

import br.furb.sisdis.Events;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlaveApp {

	private static final String SERVER_HOST = "localhost";
	private static final int SERVER_PORT = 5000;
    private static final Random RANDOM = new Random();
    private static long time = RANDOM.nextLong(0, 120);

	public static void main(String[] args) {
		int tentativasConexao = 0;

		while (tentativasConexao < 5) {
			try (Socket socket = new Socket()) {

				socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 5000);
				log.info("# Slave conectando em {}:{}", SERVER_HOST, SERVER_PORT);
                log.info("# Horário slave: {}", time);

				
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				
				String line;
				while ((line = in.readLine()) != null) {

					if (line.startsWith(Events.GET_TIME.request)) {
						log.info("# Recebido requisição {}", Events.GET_TIME.value);

                        var horarioMaster = line.substring(Events.GET_TIME.request.length());


						long differenceTime = time - Long.parseLong(horarioMaster);

                        log.info("## Diferença de tempo: {}",  differenceTime);

						// Envia a resposta seguindo o protocolo
						String response = Events.SEND_TIME.request + differenceTime;
						out.println(response);
						out.flush();
						log.info(response);
						continue;
					}

					if (line.startsWith(Events.AJUSTA_TEMPO.request)) {
                        var ajusteTempo = line.substring(Events.AJUSTA_TEMPO.request.length());
                        log.info("# Recebido requisição {} -> Ajuste do tempo {}", Events.AJUSTA_TEMPO.value, ajusteTempo);

                        time += Long.parseLong(ajusteTempo);

                        log.info("# Novo tempo definido -> {}", time);
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

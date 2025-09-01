package br.furb.sisdis.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerApp {

    private static final int PORT = 5000;
    private static final ExecutorService POOL = Executors.newCachedThreadPool();
    
    static final CopyOnWriteArrayList<ClientHandler> handlers = new CopyOnWriteArrayList<>();
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor();
	
    
    public static void main(String[] args) {
    	criaRotinaGetTime();
    	
    	try (ServerSocket serverSocket = new ServerSocket(PORT)) {
    		log.info("# Servidor iniciado na porta {}", PORT);
    		
    		while (true) {
    			Socket clientSocket = serverSocket.accept();
    			ClientHandler handler = new ClientHandler(clientSocket);
    			handlers.add(handler);
    			POOL.execute(handler);
    			
    			log.info("## Conexão aceita de {}", clientSocket.getRemoteSocketAddress());
    		}
    		
    	} catch (IOException e) {
			log.error("Erro no servidor", e);
		} finally {
			shutdown();
		}
    	
	}


    /**
     * Rotina para realizar requisições do horário aos clients a cada 60s, com delay de 10 segundos assim que for criada
     * TODO: Verificar se deve haver uma rotina cronometrada para esta requisição (acredito eu que sim)
     */
	private static void criaRotinaGetTime() {
		SCHED.scheduleAtFixedRate(() -> {
            if (handlers.isEmpty()) {
                log.warn("# Nenhum client conectado");
                return;
            }
            
            log.info("# Enviando REQUEST:GET_TIME para " + handlers.size() + " clients...");
            handlers.forEach(ClientHandler::getTime);
        }, 10, 60, TimeUnit.SECONDS);
	}
    
	/**
	 *  Método público chamado pelos handlers ao finalizarem para se removerem da lista
	 * @param handler a ser removido
	 */
    public static void removeHandler(ClientHandler handler) {
        handlers.remove(handler);
        log.info("Handler removido: {}", handler.getClientInfo());
        log.info("Clients conectados: {}", handlers.size());
    }

    private static void shutdown() {
        log.warn("Servidor encerrando...");
        SCHED.shutdownNow();
        POOL.shutdownNow();
    }
	
}

package br.furb.sisdis.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import br.furb.sisdis.Relogio;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterApp {

    private static final int PORT = 5000;
    private static final ExecutorService POOL = Executors.newCachedThreadPool();

    static final CopyOnWriteArrayList<SlaveHandler> handlers = new CopyOnWriteArrayList<>();
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor();

    private static final BlockingQueue<SlaveHandler> chamadasRestantes = new LinkedBlockingQueue<>();

    private static final Relogio relogio = new Relogio();

    public static void main(String[] args) {
        criaRotinaGetTime();
        relogio.start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log.info("# Servidor iniciado na porta {}", PORT);

            while (true) {
                Socket slaveSocket = serverSocket.accept();
                SlaveHandler handler = new SlaveHandler(slaveSocket);
                handlers.add(handler);
                POOL.execute(handler);

                log.info("## Conexão aceita de {}", slaveSocket.getRemoteSocketAddress());
            }

        } catch (IOException e) {
            log.error("Erro no servidor", e);
        } finally {
            shutdown();
        }

    }

    private static void verifyTime() {
        if (chamadasRestantes.stream().allMatch(slave -> slave.getUltimaDiferencaCalculada() != null)) {
            log.info("calculando media");
            var mediaTempo = chamadasRestantes.stream().map(SlaveHandler::getUltimaDiferencaCalculada)
                    .reduce(Long::sum).get() / chamadasRestantes.size();

            while (!chamadasRestantes.isEmpty()) {
                var slave = chamadasRestantes.poll();
                var diff = (slave.getUltimaDiferencaCalculada() / -1) + mediaTempo;
                slave.ajustaTempo(diff);
            }
            //TODO: Ajusta o proprio tempo do master
        }
    }


    /**
     * Rotina para realizar requisições do horário aos slaves a cada 60s, com delay de 10 segundos assim que for criada
     */
    private static void criaRotinaGetTime() {
        SCHED.scheduleAtFixedRate(() -> {
            if (handlers.isEmpty()) {
                log.warn("# Nenhum slave conectado");
                return;
            }

            log.info("# Enviando REQUEST:GET_TIME para " + handlers.size() + " slaves...");
            var tempoAtualServidor = relogio.getElapsedTime();
            handlers.forEach(slave -> {
                slave.getTime(tempoAtualServidor);
                chamadasRestantes.add(slave);
            });

        }, 10, 60, TimeUnit.SECONDS);

        SCHED.scheduleAtFixedRate(MasterApp::verifyTime,10, 1, TimeUnit.SECONDS);

    }

    /**
     * Método público chamado pelos handlers ao finalizarem para se removerem da lista
     *
     * @param handler a ser removido
     */
    public static void removeHandler(SlaveHandler handler) {
        handlers.remove(handler);
        log.info("Handler removido: {}", handler.getSlaveInfo());
        log.info("slaves conectados: {}", handlers.size());
    }

    private static void shutdown() {
        log.warn("Servidor encerrando...");
        SCHED.shutdownNow();
        POOL.shutdownNow();
    }

}

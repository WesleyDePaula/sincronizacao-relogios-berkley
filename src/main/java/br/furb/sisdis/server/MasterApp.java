package br.furb.sisdis.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterApp {

    private static final int PORT = 5000;
    private static final ExecutorService POOL = Executors.newCachedThreadPool();

    static final CopyOnWriteArrayList<SlaveHandler> handlers = new CopyOnWriteArrayList<>();
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor();

    private static final BlockingQueue<SlaveHandler> chamadasRestantes = new LinkedBlockingQueue<>();

    private static final Random RANDOM = new Random();
    private static long time = RANDOM.nextLong(0, 240);

    public static void main(String[] args) {
        criaRotinaGetTime();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log.info("# Servidor iniciado na porta {}", PORT);
            log.info("## Horário do master: {}", time);

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
        if (!chamadasRestantes.isEmpty() && chamadasRestantes.stream().allMatch(slave -> slave.getUltimaDiferencaCalculada() != null)) {
            var soma = chamadasRestantes.stream().map(SlaveHandler::getUltimaDiferencaCalculada)
                    .reduce(Long::sum).get() ;

            log.info("## Soma das diferenças: {}", soma);

            var mediaTempo = soma / (chamadasRestantes.size() + 1);
            log.info("## Média do tempo: {}", mediaTempo);

            while (!chamadasRestantes.isEmpty()) {

                var slave = chamadasRestantes.poll();
                var diff = (slave.getUltimaDiferencaCalculada() * -1) + mediaTempo;
                slave.ajustaTempo(diff);
                log.info("## Slave: {}, diferença: {}", slave.getSlaveInfo(), mediaTempo);
            }
            time += mediaTempo;
            log.info("## Novo horário do master: {}", time);
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
            var tempoAtualServidor = time;
            handlers.forEach(slave -> {
                slave.getTime(tempoAtualServidor);
                chamadasRestantes.add(slave);
            });

        }, 10, 20, TimeUnit.SECONDS);

        SCHED.scheduleAtFixedRate(MasterApp::verifyTime,1, 1, TimeUnit.SECONDS);

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

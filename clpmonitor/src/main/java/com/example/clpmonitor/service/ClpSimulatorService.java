package com.example.clpmonitor.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.clpmonitor.model.ClpData;
import com.example.clpmonitor.plc.PlcConnector;

import jakarta.annotation.PostConstruct;

// Define que esta classe é um componente de serviço do Spring (fica disponível para injeção com @Autowired).
// Contém a lógica de negócio: neste caso, a simulação de dados dos CLPs e envio via SSE.
@Service
public class ClpSimulatorService {

    public byte[] indxColorBlk = new byte[28];
    public PlcConnector plcStock;
    public PlcConnector plcExpedition;

    // emitters – Lista de clientes conectados via SSE
    // Guarda todos os clientes que estão conectados e escutando eventos via SSE.
    // CopyOnWriteArrayList é usada para permitir acesso concorrente com
    // segurança (vários threads atualizando a lista).
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // executor – Agendamento das tarefas de simulações
    // Cria uma pool de threads agendadas (com 2 threads).
    // É usada para executar tarefas repetidamente com um intervalo fixo (ex: a cada 1 segundo).
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    // @PostConstruct – Inicialização automática
    @PostConstruct
    // Esse método é chamado automaticamente após a construção do bean.
    // Define os dois agendamentos de envio de dados simulados:
    public void startSimulation() {
        //executor.scheduleAtFixedRate(this::sendClp1Update, 0, 3800, TimeUnit.MILLISECONDS);
        //executor.scheduleAtFixedRate(this::sendClp2to4Updates, 0, 3, TimeUnit.SECONDS);
        //executor.scheduleAtFixedRate(this::sendClp4Ocupacao, 0, 2, TimeUnit.SECONDS); // Novo
    }

    // subscribe() – Adiciona cliente à lista de ouvintes SSE
    // Esse método é chamado quando o frontend conecta-se à URL /clp-data-stream.
    public SseEmitter subscribe() {
        // Cria um novo SseEmitter com timeout infinito (0L).
        SseEmitter emitter = new SseEmitter(0L);

        // Adiciona esse emitter à lista emitters.
        emitters.add(emitter);

        // Remove o cliente se ele desconectar ou der timeout.
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    // sendClp1Update() – Gera 28 bytes (valores de 0 a 3) para o CLP 1
    private void sendClp1Update() {
        plcStock = new PlcConnector("10.74.241.10", 102);
        try {
            plcStock.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            indxColorBlk = plcStock.readBlock(9, 68, 28);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Gera uma lista de 28 inteiros entre 0 e 3.
        List<Integer> byteArray = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            int color = (int) indxColorBlk[i];
            byteArray.add(color); // 0 a 3
        }

        // Cria um ClpData com id = 1 e envia com o evento "clp1-data".
        ClpData clp1 = new ClpData(1, byteArray);
        sendToEmitters("clp1-data", clp1);
    }

    // sendClp2to4Updates() – Gera valores inteiros simples
    // Simula os valores para os CLPs 2, 3 e 4 com números aleatórios de 0 a 99.
    private void sendClp2to4Updates() {
        Random rand = new Random();

        sendToEmitters("clp2-data", new ClpData(2, rand.nextInt(100)));
        sendToEmitters("clp3-data", new ClpData(3, rand.nextInt(100)));
        sendToEmitters("clp4-data", new ClpData(4, rand.nextInt(100)));
    }

    // sendToEmitters() – Envia um evento SSE para todos os clientes
    private void sendToEmitters(String eventName, ClpData clpData) {
        // Percorre todos os SseEmitters conectados.
        for (SseEmitter emitter : emitters) {
            try {
                // Envia um evento com:
                //      eventName → nome do evento no frontend (ex: clp1-data, clp2-data, etc).
                //      data(clpData) → dados a serem enviados (convertidos para JSON automaticamente).
                emitter.send(SseEmitter.event().name(eventName).data(clpData));
            } catch (IOException e) {
                // Se algum cliente tiver erro de conexão, ele é removido da lista.
                emitters.remove(emitter);
            }
        }
    }

    // Envia um SSE para demonstrar o que esta ocupado
    private void sendClp4Ocupacao() {
        plcExpedition = new PlcConnector("10.74.241.40", 102);
        List<Integer> ocupacao = new ArrayList<>();

        try {
            plcExpedition.connect();

            // Lê 12 valores inteiros (DB9, offsets 6 a 28, incrementando de 2 em 2)
            for (int i = 6, j = 0; i <= 28; i += 2, j++) {
                int value = plcExpedition.readInt(9, i);
                ocupacao.add(value);
            }
        } catch (Exception e) {
            System.err.println("Falha na leitura do CLP4 (Expedição): " + e.getMessage());
            for (int i = 0; i < 12; i++) {
                ocupacao.add(0);
            }
        } finally {
            try {
                if (plcExpedition != null) {
                    plcExpedition.disconnect();
                }
            } catch (Exception e) {
                System.err.println("Erro ao desconectar do CLP4: " + e.getMessage());
            }
        }

        sendToEmitters("clp4-ocupacao", new ClpData(4, ocupacao));
    }

    // Botão de atualização
    public void triggerManualUpdate() {
        sendClp1Update();
        sendClp2to4Updates();
        sendClp4Ocupacao();
    }

}

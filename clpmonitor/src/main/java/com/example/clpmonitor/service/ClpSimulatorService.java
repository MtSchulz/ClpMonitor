package com.example.clpmonitor.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.clpmonitor.model.Block;
import com.example.clpmonitor.model.ClpData;
import com.example.clpmonitor.plc.PlcConnector;
import jakarta.annotation.PostConstruct;

import com.example.clpmonitor.repository.BlockRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ClpSimulatorService {

    private byte[] indxColorBlk = new byte[28];
    private PlcConnector plcStock;
    private PlcConnector plcExpedition;

    @Autowired
    private BlockRepository blockRepository;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    @PostConstruct
    public void startSimulation() {
        executor.scheduleAtFixedRate(this::sendClp1Update, 0, 3800, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::sendClp2to4Updates, 0, 3, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::sendClp4Ocupacao, 0, 2, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    private void sendClp1Update() {
        try {
            List<Integer> byteArray = new ArrayList<>();
            List<Block> blocks = blockRepository.findByStorageIdOrderByPositionAsc(1);
            
            for (Block block : blocks) {
                byteArray.add(block.getColor());
            }
            
            while (byteArray.size() < 28) {
                byteArray.add(0);
            }
            
            ClpData clp1 = new ClpData(1, byteArray);
            sendToEmitters("clp1-data", clp1);
        } catch (Exception e) {
            System.err.println("Erro ao atualizar CLP1: " + e.getMessage());
        }
    }

    private void sendClp2to4Updates() {
        Random rand = new Random();
        sendToEmitters("clp2-data", new ClpData(2, rand.nextInt(100)));
        sendToEmitters("clp3-data", new ClpData(3, rand.nextInt(100)));
        sendToEmitters("clp4-data", new ClpData(4, rand.nextInt(100)));
    }

    private void sendClp4Ocupacao() {
        try {
            List<Integer> ocupacao = new ArrayList<>();
            List<Block> blocks = blockRepository.findByStorageIdOrderByPositionAsc(1);
            
            for (Block block : blocks) {
                ocupacao.add(block.getProductionOrder() != null ? 1 : 0);
            }
            
            while (ocupacao.size() < 12) {
                ocupacao.add(0);
            }
            
            sendToEmitters("clp4-ocupacao", new ClpData(4, ocupacao));
        } catch (Exception e) {
            System.err.println("Erro ao atualizar ocupação: " + e.getMessage());
        }
    }

    private void sendToEmitters(String eventName, ClpData clpData) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(clpData));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });
        emitters.removeAll(deadEmitters);
    }

    public void triggerManualUpdate() {
        sendClp1Update();
        sendClp2to4Updates();
        sendClp4Ocupacao();
    }
}


/* Versão anterior de conexão ao CLP

package com.example.clpmonitor.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.clpmonitor.model.ClpData;
import com.example.clpmonitor.plc.PlcConnector;
import jakarta.annotation.PostConstruct;

@Service
public class ClpSimulatorService {

    public byte[] indxColorBlk = new byte[28];
    public PlcConnector plcStock;
    public PlcConnector plcExpedition;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    @PostConstruct
    public void startSimulation() {
        executor.scheduleAtFixedRate(this::sendClp1Update, 0, 3800, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::sendClp2to4Updates, 0, 3, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::sendClp4Ocupacao, 0, 2, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    // Versão original que lê do CLP físico
    private void sendClp1Update() {
        plcStock = new PlcConnector("10.74.241.10", 102); // IP do CLP de estoque
        try {
            plcStock.connect();
            indxColorBlk = plcStock.readBlock(9, 68, 28); // DB9, offset 68, 28 bytes
            
            List<Integer> byteArray = new ArrayList<>();
            for (int i = 0; i < 28; i++) {
                int color = (int) indxColorBlk[i];
                byteArray.add(color); // 0 a 3
            }
            
            ClpData clp1 = new ClpData(1, byteArray);
            sendToEmitters("clp1-data", clp1);
            
        } catch (Exception e) {
            System.err.println("Falha na leitura do CLP1 (Estoque): " + e.getMessage());
        } finally {
            try {
                if (plcStock != null) {
                    plcStock.disconnect();
                }
            } catch (Exception e) {
                System.err.println("Erro ao desconectar do CLP1: " + e.getMessage());
            }
        }
    }

    private void sendClp2to4Updates() {
        Random rand = new Random();
        sendToEmitters("clp2-data", new ClpData(2, rand.nextInt(100)));
        sendToEmitters("clp3-data", new ClpData(3, rand.nextInt(100)));
        sendToEmitters("clp4-data", new ClpData(4, rand.nextInt(100)));
    }

    // Versão original que lê do CLP físico
    private void sendClp4Ocupacao() {
        plcExpedition = new PlcConnector("10.74.241.40", 102); // IP do CLP de expedição
        List<Integer> ocupacao = new ArrayList<>();

        try {
            plcExpedition.connect();
            
            // Lê 12 valores inteiros (DB9, offsets 6 a 28, incrementando de 2 em 2)
            for (int i = 6, j = 0; i <= 28; i += 2, j++) {
                int value = plcExpedition.readInt(9, i);
                ocupacao.add(value);
            }
            
            sendToEmitters("clp4-ocupacao", new ClpData(4, ocupacao));
            
        } catch (Exception e) {
            System.err.println("Falha na leitura do CLP4 (Expedição): " + e.getMessage());
            // Preenche com valores padrão em caso de erro
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
    }

    private void sendToEmitters(String eventName, ClpData clpData) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(clpData));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });
        emitters.removeAll(deadEmitters);
    }

    public void triggerManualUpdate() {
        sendClp1Update();
        sendClp2to4Updates();
        sendClp4Ocupacao();
    }
}
 */
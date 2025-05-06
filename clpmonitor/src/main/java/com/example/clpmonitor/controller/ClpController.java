package com.example.clpmonitor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.clpmonitor.model.Block;
import com.example.clpmonitor.model.Order;
import com.example.clpmonitor.model.Storage;
import com.example.clpmonitor.model.Tag;
import com.example.clpmonitor.model.TagWriteRequest;
import com.example.clpmonitor.plc.PlcConnector;
import com.example.clpmonitor.repository.BlockRepository;
import com.example.clpmonitor.repository.OrderRepository;
import com.example.clpmonitor.repository.StorageRepository;
import com.example.clpmonitor.service.ClpSimulatorService;

import jakarta.transaction.Transactional;

@Controller
public class ClpController {

    @Autowired
    private ClpSimulatorService simulatorService;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private StorageRepository storageRepository;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("tag", new TagWriteRequest());
        return "index";
    }

    @GetMapping("/clp-data-stream")
    public SseEmitter streamClpData() {
        return simulatorService.subscribe();
    }

    @GetMapping("/write-tag")
    public String showWriteForm(Model model) {
        model.addAttribute("tag", new Tag());
        return "clp-write-fragment";
    }

    @Transactional
    @PostMapping("/write-tag")
    public String writeTag(@ModelAttribute Tag tag, Model model) {
        try {
            // Debug: Mostra os dados recebidos
            System.out.println("[DEBUG] Dados do formulário:");
            System.out.println("IP: " + tag.getIp());
            System.out.println("Porta: " + tag.getPort());
            System.out.println("DB: " + tag.getDb());
            System.out.println("Tipo: " + tag.getType());
            System.out.println("Offset: " + tag.getOffset());
            System.out.println("BitNumber: " + tag.getBitNumber());
            System.out.println("Size: " + tag.getSize());
            System.out.println("Valor: " + tag.getValue());

            // Conecta ao CLP
            PlcConnector plc = new PlcConnector(tag.getIp().trim(), tag.getPort());
            plc.connect();

            boolean success = false;
            String operationDetails = "";

            // Executa a operação conforme o tipo
            switch (tag.getType().toUpperCase()) {
                case "STRING":
                    success = plc.writeString(tag.getDb(), tag.getOffset(), tag.getSize(), tag.getValue().trim());
                    operationDetails = String.format("DB%d.%d (STRING) = '%s'", tag.getDb(), tag.getOffset(), tag.getValue());
                    break;

                case "BLOCK":
                    byte[] bytes = PlcConnector.hexStringToByteArray(tag.getValue().trim());
                    success = plc.writeBlock(tag.getDb(), tag.getOffset(), tag.getSize(), bytes);
                    operationDetails = String.format("DB%d.%d (BLOCK) = %s", tag.getDb(), tag.getOffset(), java.util.Arrays.toString(bytes));
                    break;

                case "FLOAT":
                    success = plc.writeFloat(tag.getDb(), tag.getOffset(), Float.parseFloat(tag.getValue().trim()));
                    operationDetails = String.format("DB%d.%d (FLOAT) = %.2f", tag.getDb(), tag.getOffset(), Float.parseFloat(tag.getValue()));
                    break;

                case "INTEGER":
                    success = plc.writeInt(tag.getDb(), tag.getOffset(), Integer.parseInt(tag.getValue().trim()));
                    operationDetails = String.format("DB%d.%d (INT) = %d", tag.getDb(), tag.getOffset(), Integer.parseInt(tag.getValue()));

                    // Atualiza ordem de produção se for no DB correto
                    if (tag.getIp().equals("10.74.241.40") && tag.getDb() == 9) {
                        updateProductionOrder(tag.getOffset(), tag.getValue().trim());
                    }
                    break;

                case "BYTE":
                    byte byteValue = Byte.parseByte(tag.getValue().trim());
                    success = plc.writeByte(tag.getDb(), tag.getOffset(), byteValue);
                    operationDetails = String.format("DB%d.%d (BYTE) = %d", tag.getDb(), tag.getOffset(), byteValue);

                    // Atualiza cor do bloco no banco de dados
                    if (tag.getIp().equals("10.74.241.10")) {
                        updateBlockInDatabase(tag.getDb(), tag.getOffset(), byteValue);
                    }
                    break;

                case "BIT":
                    if (tag.getBitNumber() == null) {
                        throw new IllegalArgumentException("Bit Number é obrigatório para tipo BIT");
                    }
                    boolean bitValue = Boolean.parseBoolean(tag.getValue().trim());
                    success = plc.writeBit(tag.getDb(), tag.getOffset(), tag.getBitNumber(), bitValue);
                    operationDetails = String.format("DB%d.%d.%d = %b", tag.getDb(), tag.getOffset(), tag.getBitNumber(), bitValue);
                    break;

                default:
                    throw new IllegalArgumentException("Tipo não suportado: " + tag.getType());
            }

            plc.disconnect();

            if (success) {
                model.addAttribute("mensagem", "Escrita no CLP e banco de dados realizada com sucesso!");

                // Atualização imediata da matriz (para CLP1)
                if (tag.getIp().equals("10.74.241.10") && tag.getDb() == 9 && tag.getType().equalsIgnoreCase("BYTE")) {
                    simulatorService.triggerManualUpdate();
                }
            } else {
                model.addAttribute("erro", "Erro de escrita no CLP!");
            }
        } catch (Exception ex) {
            model.addAttribute("erro", "Erro: " + ex.getMessage());
            System.err.println("[ERROR] Erro ao escrever tag: " + ex.getMessage());
            ex.printStackTrace();
        }

        return "clp-write-fragment";
    }

    private void updateBlockInDatabase(int dbNumber, int position, int color) {
        // Obtém o storage correspondente (ajuste conforme sua aplicação)
        Storage storage = storageRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Storage não encontrado"));

        // Verifica se a posição é válida
        if (position < 0 || position >= storage.getCapacity()) {
            throw new IllegalArgumentException("Posição inválida: " + position);
        }

        // Busca ou cria o bloco
        Block block = blockRepository.findByStorageAndPosition(storage, position)
                .orElseGet(() -> {
                    Block newBlock = new Block();
                    newBlock.setStorage(storage);
                    newBlock.setPosition(position);
                    return newBlock;
                });

        // Atualiza a cor
        block.setColor(color);
        blockRepository.save(block);
    }

    private void updateProductionOrder(int offset, String orderValue) {
        // Obtém o bloco correspondente ao offset
        Storage storage = storageRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Storage não encontrado"));

        Block block = blockRepository.findByStorageAndPosition(storage, offset)
                .orElseThrow(() -> new RuntimeException("Bloco não encontrado para offset: " + offset));

        // Cria ou atualiza a ordem de produção
        if (!orderValue.isEmpty() && !orderValue.equals("0")) {
            Order order = orderRepository.findByProductionOrder(orderValue)
                    .orElseGet(() -> {
                        Order newOrder = new Order();
                        newOrder.setProductionOrder(orderValue);
                        return orderRepository.save(newOrder);
                    });

            block.setProductionOrder(orderValue);
        } else {
            block.setProductionOrder(null);
        }

        blockRepository.save(block);
    }

    @GetMapping("/fragmento-formulario")
    public String carregarFragmentoFormulario(Model model) {
        model.addAttribute("tag", new TagWriteRequest());
        return "fragments/formulario :: clp-write-fragment";
    }

    @PostMapping("/manual-refresh")
    public ResponseEntity<String> manualRefresh() {
        simulatorService.triggerManualUpdate();
        return ResponseEntity.ok("Atualização solicitada");
    }
}

/*
     * Descrição do Funcionamento:
     * 
       1 - O usuário acessa http://localhost:8080/ → o método index() retorna o HTML.

       2 - O HTML carrega o JavaScript (scripts.js), que cria:

            const eventSource = new EventSource('/clp-data-stream');

       3 - O navegador faz uma requisição GET para /clp-data-stream.

       4 - O Spring chama simulatorService.subscribe() e devolve um SseEmitter ao navegador.

       5 - A cada X milissegundos, o ClpSimulatorService envia eventos como:

            clp1-data
            clp2-data
            clp3-data
            clp4-data

       6 - O JavaScript escuta cada evento separadamente e atualiza a interface conforme os dados recebidos.
 */

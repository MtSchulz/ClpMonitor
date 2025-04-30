package com.example.clpmonitor.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.clpmonitor.model.Tag;
import com.example.clpmonitor.model.TagLog;
import com.example.clpmonitor.model.TagWriteRequest;
import com.example.clpmonitor.plc.PlcConnector;
import com.example.clpmonitor.service.ClpSimulatorService;

@Controller
public class ClpController {

    // Injeta automaticamente uma instância da classe ClpSimulatorService.
    // Essa classe é responsável por simular os dados dos CLPs e gerenciar
    // os eventos SSE que serão enviados ao frontend.
    @Autowired
    private ClpSimulatorService simulatorService;

    // Mapeia a URL raiz (http://localhost:8080/) para o método index().
    // Retorna a view index.html, localizada em src/main/resources/templates/index.html (Thymeleaf).
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("tag", new TagWriteRequest());
        return "index";
    }

    // Rota "/clp-data-stream" — Comunicação via SSE (Server-Sent Events)
    // Essa rota é chamada no JavaScript pelo EventSource:
    @GetMapping("/clp-data-stream")

    // Retorna um objeto SseEmitter, que é a classe do Spring para enviar
    // dados do servidor para o cliente continuamente usando Server-Sent Events.
    public SseEmitter streamClpData() {
        // Esse método delega a lógica para simulatorService.subscribe() que:
        //  Cria o SseEmitter.
        //  Armazena ele numa lista de ouvintes (clientes conectados).
        //  Inicia o envio periódico dos dados simulados
        return simulatorService.subscribe();
    }

    @GetMapping("/write-tag")
    public String showWriteForm(Model model) {
        model.addAttribute("tag", new Tag());
        return "clp-write-fragment"; // ou o nome da sua página que contém o fragmento
    }

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
                    break;
                    
                case "BYTE":
                    success = plc.writeByte(tag.getDb(), tag.getOffset(), Byte.parseByte(tag.getValue().trim()));
                    operationDetails = String.format("DB%d.%d (BYTE) = %d", tag.getDb(), tag.getOffset(), Byte.parseByte(tag.getValue()));
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
                model.addAttribute("mensagem", "Escrita no CLP realizada com sucesso!");
                
                // Atualização imediata da matriz (para CLP1)
                if (tag.getIp().equals("10.74.241.10") && tag.getDb() == 9 && tag.getType().equalsIgnoreCase("BYTE")) {
                    simulatorService.triggerManualUpdate(); // Método público alternativo
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

    @GetMapping("/fragmento-formulario")
    public String carregarFragmentoFormulario(Model model) {
        model.addAttribute("tag", new TagWriteRequest()); // substitua pelo seu DTO real
        return "fragments/formulario :: clp-write-fragment";
    }

    @PostMapping("/manual-refresh")
    public ResponseEntity<String> manualRefresh() {
        simulatorService.triggerManualUpdate();
        return ResponseEntity.ok("Atualização solicitada");
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
}

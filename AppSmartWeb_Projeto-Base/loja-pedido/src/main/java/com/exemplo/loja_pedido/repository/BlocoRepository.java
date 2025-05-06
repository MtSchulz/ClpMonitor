package com.exemplo.loja_pedido.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.exemplo.loja_pedido.model.Bloco;

public interface BlocoRepository extends JpaRepository<Bloco, Long> {
}

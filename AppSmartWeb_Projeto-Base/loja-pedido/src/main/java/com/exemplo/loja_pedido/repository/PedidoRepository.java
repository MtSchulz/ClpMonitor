package com.exemplo.loja_pedido.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.exemplo.loja_pedido.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}


package com.exemplo.loja_pedido.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.exemplo.loja_pedido.model.Lamina;

public interface LaminaRepository extends JpaRepository<Lamina, Long> {
}

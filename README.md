# Projeto de Programação Distribuída

Este projeto consiste na implementação de um **Matching Engine** utilizando os protocolos **UDP**, **TCP** e **HTTP**. O projeto faz uso de padrões distribuídos para garantir a eficiência e robustez na comunicação entre nós.

## Padrões Distribuídos Implementados

Os seguintes padrões distribuídos foram aplicados:

- **Heartbeat**: Verificação periódica para assegurar que os nós estão ativos.
- **Leader and Followers**: Um nó líder coordena as ações dos nós seguidores.
- **Majority Quorum**: Decisões são tomadas com base no consenso da maioria dos nós.
- **Generation Clock**: Controle de eleição para evitar split-brain.
- **Single-Socket Channel**: Comunicação eficiente entre os nós através de um único canal de conexão.

## Tecnologias Utilizadas

- **Java**
- **PostgreSQL**
- **Docker**
- **Maven** (gerenciamento de dependências)

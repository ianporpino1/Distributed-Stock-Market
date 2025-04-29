# Projeto de Programação Distribuída

Este projeto consiste na implementação de um **Matching Engine** utilizando os protocolos **UDP**, **TCP** e **HTTP**. O projeto faz uso de padrões distribuídos para garantir a eficiência e robustez na comunicação entre nós.

# Protocolo
VERSION|OPERATION|DADOS

ex: 1|ORDER_REQUEST|BUY;AAPL;10;150
    +1|VOTE_REQUEST|generation=1;candidateId=1;leaderId=-1


## Padrões Distribuídos Implementados

Os seguintes padrões distribuídos foram aplicados:

- **Heartbeat**: Verificação periódica para assegurar que os nós estão ativos.
- **Leader and Followers**: Um nó líder coordena as ações dos nós seguidores.
- **Generation Clock**: Controle de eleição para evitar split-brain.
- **Single-Socket Channel**: Comunicação eficiente entre os nós através de um único canal de conexão.

## Tecnologias Utilizadas

- **Java**
- **PostgreSQL**
- **Docker**
- **Maven** (gerenciamento de dependências)